---
name: clearth-dependency-cves
description: Run OWASP dependency-check on ClearTH without an NVD API key and remediate its findings. Use when scanning for vulnerable dependencies, triaging a new CVE report, bumping a dependency version, deciding between resolutionStrategy.force and constraints, or debugging a "Conflict(s) found for the following module(s)" / failOnVersionConflict error in this build.
---

# ClearTH dependency CVE remediation

How to get a trustworthy dependency-check report on a developer machine with no NVD API key, and how to
remediate findings in *this* build, whose pinning mechanism is unusual and easy to get wrong.

**Standing rule: never write a suppression.** Findings that cannot be cleared by a version bump are reported
to the user with evidence, and they decide. `dependency-check/suppressions.xml` is edited only on explicit
instruction.

## 1. Running the scan, keyless

The build is wired for CI: `checkApiKey`, `dataFeedUrl` and `knownExploitedVulnerabilitiesUrl` are `-P`
properties defaulting to empty/null (`build.gradle:42-50`). No build-file change is needed to run without a
key — `ConfiguredTask.groovy:94` applies the key with `setStringIfNotEmpty`, so the empty default is ignored
rather than sent as a broken header.

The scan is driven by an init script, `dc-local.gradle`, in the repo root. It is **gitignored on purpose**
(it encodes one machine's network constraints, and keeps CI's committed config untouched), so it will not be
in a fresh clone. Recreate it verbatim from the listing below, then invoke:

```groovy
// Local-only dependency-check overrides for keyless (no NVD API key) runs.
// NOT for CI. Invoke with:  ./gradlew -I dc-local.gradle dependencyCheckAggregate
//
//   -PdcOffline               skip the NVD update (same-day repeat runs)
//   -PdcProjects=:a,:b        scan only these projects (narrowed rescans)
//   -PdcStrict                keep the project gate (failBuildOnCVSS 7.6) instead of 11
//
// Never run dependencyCheckPurge: without an API key a full NVD rebuild takes hours.
gradle.rootProject { root ->
	root.afterEvaluate {
		if (!root.extensions.findByName('dependencyCheck'))
			return
		root.dependencyCheck {
			// cisa.gov returns HTTP 403 from this network; the KEV update would abort the run.
			analyzers {
				kev {
					enabled = false
				}
			}
			// The committed list names 13 gretty configurations that do not exist in this
			// project. 'jaxb' is resolvable, non-test and IS scanned: build-time XJC only.
			skipConfigurations = ['jaxb']
			// 'format' and 'formats' are UNIONed (AbstractAnalyze.groovy:188), so the
			// committed format='ALL' must be overridden or all six reports are written.
			format = 'JSON'
			formats = ['HTML']
			// Report everything; do not abort on the first finding over the 7.6 gate.
			// -PdcStrict keeps the project's real gate (7.6) to prove the build passes it.
			if (!root.hasProperty('dcStrict'))
				failBuildOnCVSS = 11
			if (root.hasProperty('dcProjects'))
				scanProjects = root.dcProjects.split(',').collect { it.trim() }
			if (root.hasProperty('dcOffline'))
				autoUpdate = false
		}
	}
}
```

```bash
./gradlew -I dc-local.gradle dependencyCheckAggregate     # full scan
./gradlew -I dc-local.gradle dependencyCheckAggregate -PdcOffline
./gradlew -I dc-local.gradle dependencyCheckAggregate -PdcProjects=:clearth-modules:clearth-th2
```

- `-PdcOffline` sets `autoUpdate = false` — use on same-day repeat runs.
- `-PdcProjects` sets `scanProjects` — the only way to narrow the scan, since `dependencyCheckAnalyze` at the
  root scans nothing (see §2).
- `-PdcStrict` keeps the project's real `failBuildOnCVSS = 7.6` instead of the reporting default of 11. Use it
  as the final check: it proves the build passes its own gate, rather than merely that you have a report.
- **Never run `dependencyCheckPurge`.** Keyless, the full NVD rebuild runs at 5 requests/30 s and takes hours.
  The warm H2 cache lives at `~/.gradle/dependency-check-data/11.0/odc.mv.db` (~676 MB);
  `dependencycheck.properties:17` declares `data.directory=[JAR]/data/11.0`, so DC 12.2.2 uses it directly
  and updates incrementally.

The init script also overrides `format`, because **`format` and `formats` are UNIONed**
(`AbstractAnalyze.groovy:188-209`) — leaving the committed `format = 'ALL'` in place writes all six report
formats on every run regardless of what `formats` says.

### Rescans go UP-TO-DATE and hand back a stale report — always force them

`dependencyCheckAggregate`'s declared inputs do **not** capture resolved dependency versions. After changing a
version and rerunning you will get:

```
> Task :dependencyCheckAggregate UP-TO-DATE
BUILD SUCCESSFUL in 2s
```

and `build/reports/` still holds the **previous** report. Diffing that against your baseline shows "nothing
changed" and looks like your fix did not work. It is a caching artifact, not a result.

Compounding it: the root project applies neither the `java` nor the `base` plugin, so **the root has no
`clean` task** and `./gradlew clean` never removes the root `build/reports/`. A stale report can therefore
survive a `clean` too.

Always rescan with both:

```bash
rm -rf build/reports
./gradlew -I dc-local.gradle dependencyCheckAggregate -PdcOffline --rerun-tasks
```

Sanity-check every report before drawing conclusions from it: confirm the jar **filenames** in the findings
carry the versions you just set (e.g. `httpclient5-5.6.4.jar`, not `httpclient5-5.6.2.jar`), and check the
report file's mtime is from this run.

### Network reality on the corporate network (probed, don't re-investigate)

| Endpoint | Status |
| --- | --- |
| `services.nvd.nist.gov/rest/json/cves/2.0` | 200 — keyless works |
| `repo1.maven.org`, `s01.oss.sonatype.org` | 200 |
| `dependency-check.github.io` (hosted suppressions, version check) | 200 |
| `raw.githubusercontent.com` (RetireJS repo) | 200 |
| `search.maven.org/solrsearch` (Central analyzer) | 200 |
| **`cisa.gov` KEV feed** | **403 — aborts the run.** The init script sets `analyzers.kev.enabled = false` |
| `nvd.handsonsecurity.com` (old community datafeed mirror) | DNS failure — the mirror is gone |
| `api.github.com` | 504 — so self-hosting a `vulnz` NVD mirror is not available either |

There is no working `datafeedUrl` route from here. Keyless incremental update against the warm cache is the
only viable option; do not spend time looking for a mirror.

## 2. What this scan can and cannot see

Establish this before triaging, or effort goes to findings that aren't in the report and to "vulnerabilities"
no Gradle edit can fix.

- **`dependencyCheckAggregate` is the only usable entry point.** The plugin is applied at root only
  (`build.gradle:14`), and the root project declares no configurations of its own (they live inside
  `subprojects { }`), so `dependencyCheckAnalyze` scans nothing.
- **Test configurations are NOT scanned.** `skipTestGroups` defaults true
  (`DependencyCheckExtension.groovy:134`) and the check walks the whole `extendsFrom` hierarchy
  (`AbstractAnalyze.groovy:380-412`). mockito, testng, h2, sqlite-jdbc and junit never appear in a report —
  but they still trip `failOnVersionConflict()`, so they still constrain edits.
- **The `jaxb` configuration IS scanned** — resolvable, non-test, and absent from `skipConfigurations`
  (`build.gradle:205, 223-226`). `jaxb-xjc:2.3.9` and `jsr173:1.0` (2005) surface as build-time-only noise.
  The init script scopes it out via `skipConfigurations = ['jaxb']`.
- **The committed `skipConfigurations` is stale:** 13 `gretty*` entries for configurations that don't exist in
  this project (gretty is only in `for_new_project/build.gradle:3`), and no `jaxb`.
- **Jetty is invisible.** 9.4.56 / 12.0.16 are distribution ZIPs downloaded from Central
  (`build.gradle:22-23, 55-64, 334-351`), never classpath entries. Their CVEs need a filesystem or container
  scanner. The `jetty12` + `ee8` module path is the realistic upgrade route and does *not* require the
  jakarta migration — but it is its own piece of work.
- **`src/main/resources` and `src/main/webapp` are scanned as files** when `scanSet` is unset
  (`AbstractAnalyze.groovy:467-478`), so RetireJS reports vendored JS:
  `clearth-modules/clearth-gui/src/main/webapp/js/jquery.cookie.js` (1.4.1),
  `jquery.easing.1.3.js` (1.3). Fix by updating or deleting the asset; no Gradle lever applies.
- **Buildscript dependencies are not scanned** (`scanBuildEnv` defaults false), so the dependency-check and
  artifactory plugins themselves are out of scope.
- The report's `includedBy` chain is built per project + configuration
  (`AbstractAnalyze.groovy:504-530`), so the report itself tells you who pulls a given jar. Read it before
  running any `dependencyInsight`.

## 3. Which lever fixes a finding

**Default lever for a transitive bump: the `force` list at `build.gradle:127-188`.** Three reasons:

1. `failOnVersionConflict()` (`build.gradle:117`) makes every module with two candidate versions a hard
   resolution error. `force` both *selects* a version and *marks the conflict resolved*. A bare `constraints`
   entry is just another candidate, so it typically **introduces** the conflict it was meant to fix. The
   65-entry force list exists precisely because of this.
2. `force` sits in `subprojects { configurations.all { } }`, so it covers every resolvable configuration in
   every subproject — including `jaxb`, which does not extend `implementation` and is therefore invisible to
   the `constraints` block.
3. `force` **overrides the th2 `platform()` BOM** (`clearth-modules/clearth-th2/build.gradle:35`). If a GA is
   in the root force list, editing `bomVersion` alone will not move it.

**But `force` is build-local and never published.** `for_new_project/build.gradle` has no force list and no
`failOnVersionConflict`, so deployed ClearTH applications resolve from published POMs with plain
newest-wins. A force-only fix cleans this repo's report and leaves every deployed application unpatched.

| Where the vulnerable jar reaches | Lever |
| --- | --- |
| Only this repo's build/tooling classpath | `force` alone |
| Application runtime, transitive | `force` (to satisfy `failOnVersionConflict`) **plus** a published pin — a direct declaration bump in the owning module, or a `constraints` entry (Gradle emits those as POM `<dependencyManagement>` + Gradle Module Metadata) |
| Application runtime, declared directly | Bump the literal at its declaration site **and** the matching `force` entry in the same commit — otherwise the stale force silently downgrades the bump |

Verify a published pin by inspecting `shared/**/*.pom` after `./gradlew publishAll`.

## 4. Per-finding decision tree

```
File finding, not a jar (webapp/js, resources)
  -> update or delete the vendored asset. No Gradle lever.

Only in the `jaxb` configuration
  -> build-time XJC only; already scoped out. Optionally bump build.gradle:223-226.

Declared directly by a module
  -> bump the literal at its declaration site AND the matching force entry, same commit.
     FIRST check a force entry EXISTS. If the coordinate has no force entry, ADD one -
     bumping declaration sites alone is not enough (see the sqlite-jdbc incident in
     "Verification ladder": it passes here and breaks ClearTHCore-internal).

Transitive, owner is th2
  -> 1st: bomVersion (clearth-th2/build.gradle:9) or the th2 artifacts (:39-50)
     2nd: clearth-th2's own force list (:15-28)
     3rd: root force list, last resort - it overrides the BOM for ALL modules

Transitive, single owner, no BOM
  -> root force list. If it reaches app runtime, ALSO publish the pin (§3).

Jar not reachable from source at all
  -> delete the declaration or exclude it. Best fix available - but PROVE unreachability
     first, including reflective and container-driven use (see the commons-fileupload
     lesson in §6).

No fixed version exists, or the fix crosses javax -> jakarta
  -> REPORT IT. Do not suppress. Give the user: CVE, score, jar, owning module,
     what pins it, what breaks on a bump, and the options.
```

### Attributing a transitive jar

```bash
# Which resolvable configurations exist (what the scanner walks)
./gradlew :clearth-core:resolvableConfigurations
./gradlew :clearth-core:dependencies                    # no --configuration = all of them

# What the scanner actually skipped, from the plugin itself
./gradlew -I dc-local.gradle dependencyCheckAggregate --info \
  | grep "is considered a test configuration"

# Who pulls a coordinate, per module + configuration
./gradlew :clearth-modules:clearth-th2:dependencyInsight \
          --configuration runtimeClasspath --dependency jackson-databind
./gradlew :clearth-modules:clearth-th2:dependencyInsight \
          --configuration runtimeClasspath --dependency protobuf-java --singlepath
```

`dependencyInsight` prints the **selection reason** — `forced`, `by constraint`, `by conflict resolution`.
That is the authoritative answer to "did my edit take effect", and it is how dead pins (§5) are detected.

## 5. Dead pins — verify before trusting the force list

Several force entries have been provably dead. The pattern: a coordinate that no module ever requests, so
the force silently applies to nothing. Detect it by checking whether the GA exists on Central and whether it
appears in `~/.gradle/caches/modules-2/files-2.1/<group>/`.

Known cases (fixed 2026-09; re-check if the list is edited):

| Coordinate as forced | Reality |
| --- | --- |
| `commons-codec:commons-compress` | wrong group — commons-compress is `org.apache.commons`; the GA 404s |
| `tools.jackson.core:jackson-bom` | wrong group — the Jackson 3 BOM is `tools.jackson:jackson-bom`; 404s |
| `log4j-slf4j-impl` | the code uses `log4j-slf4j2-impl`; only that artifact is ever requested |
| `com.squareup.okio:okio` | okhttp 4.12.0 pulls `okio-jvm`; only `okio-jvm` is ever requested |
| `jackson-annotations:2.22.1` (in `constraints`) | **2.22.1 does not exist** — Central 404s, releases stop at `2.22`. The force at `:147` overrides it, which is the only reason the build resolves. Align the constraint *down* to `2.22`, never the force up |
| 13 explicit `io.netty:*` forces | fully redundant with the `eachDependency` rule that rewrites every `io.netty` artifact except `netty-tcnative*` |

`resolutionStrategy` blocks apply to clearth-th2 twice (root + module). `force "com.exactpro.th2:bom:<v>"`
pins the BOM's *own version*; it does not make BOM constraints win over the root force list. Before editing
either, enumerate the GA overlap: protobuf, netty, jackson, kotlin, guava, gson, grpc.

Expired suppressions are dropped at parse time with an INFO log only (`SuppressionHandler.java:202`) and
cannot be caught by `failBuildOnUnusedSuppressionRule` (`UnusedSuppressionRuleAnalyzer.java:91`). A
suppression with a past `until` date is silently inert. Prefer PURL regex over sha1 — a sha1 breaks on the
next version bump.

## 6. Known-immovable register

Do not re-litigate these each round. Each is pinned by a verified constraint, not by inertia.

| Pinned | Why it cannot move |
| --- | --- |
| **myfaces 2.3.10 + primefaces 13.0.4** (`clearth-gui/build.gradle:4-6`) | MyFaces 4.x / PrimeFaces 14+ are `jakarta.faces` only. Moving means migrating 33 Java files that import `javax.faces`/`org.primefaces`, all xhtml, `web.xml`, the servlet API and Jetty. The hardest wall in the build |
| **javax.servlet-api 3.1.0** (`clearth-core/build.gradle:93`) | same namespace wall; 13 files import `javax.servlet`. 4.0.1 is the only safe bump (still javax) |
| **jaxb-api / jaxb-impl / jsr173** | 66 files import `javax.xml.bind`, and XJC generates the `xmldata`, `flat`, `json`, `xml` and `swift` sources. jakarta.xml.bind 3/4 is the namespace wall plus regenerating every XJC output |
| **protobuf 3.25.5** (`build.gradle:124, 164-165`) | 3.25.5 *is* the fix for CVE-2024-7254. protobuf 4.x generated code calls `RuntimeVersion.validateProtobufGencodeVersion`, absent in 3.25.5 → `NoSuchMethodError` at runtime. Moves only together with grpc and the th2 BOM (6 files use `com.google.protobuf` directly). Note the root force overrides the th2 BOM here |
| **xstream 1.4.21** (`clearth-core/build.gradle:81`) | end of the line — no upgrade exists. 7 consumers incl. `ActionState`, `ExecutorStateInfo`, `XmlUtils`. Steady CVE stream. The relevant question is the threat model: input is locally-produced automation state, not untrusted data |
| **mvel2 2.4.0.Final** (`clearth-core/build.gradle:75`) | arbitrary code execution by design, so it keeps matching CVE patterns. 5 files use `org.mvel2`. 2.5.x is usually source-compatible but changes `ParserContext`/sandbox behaviour; gate on `MvelExpressionValidatorTest` and `MatrixFunctionsTest` |
| **commons-fileupload 1.6.0** (`clearth-core/build.gradle:49`) | **Do not remove despite zero Java imports.** `web.xml:79-83` registers `org.primefaces.webapp.filter.FileUploadFilter`, whose bytecode hard-references `ServletFileUpload`, `DiskFileItemFactory`, `FileItemFactory` and `FileCleanerCleanup`. Removing it is a `NoClassDefFoundError` at filter init and the webapp fails to start. 1.6.0 is already the latest 1.x; 2.x is jakarta-only |
| **activemq 5.19.x** (`clearth-activemq/build.gradle:2-4`) | pinned by `jakarta.jms:jakarta.jms-api:2.0.3` (`clearth-core/build.gradle:84`), the javax-compatible artifact. ActiveMQ 6.x moves to JMS 3.x / `jakarta.jms` and breaks core. Patch within 5.19.x only |
| **quickfixj 2.3.2** (`clearth-fix/build.gradle:2`) | root forces `mina-core:2.2.7`, which QFJ 2.3.x expects; 10 files use `quickfix`. 3.x needs Java 17 and a different mina/slf4j baseline, and the generated `quickfix.*` message classes change |
| **com.ibm.mq.allclient 10.0.0.0** (`clearth-ibmmq/build.gradle:2`) | vendor uber-jar. DC unpacks the shaded contents, so findings cannot be pinned transitively — the only lever is the whole jar, on the vendor's fixpack cadence |
| **pw-swift-core SRU2021-9.2.13** (`clearth-swift/build.gradle:2`) | SRU is the SWIFT *standards release year*, a business version. Bumping changes the message model. Needs product sign-off; never a unilateral security bump |

## 7. Verification ladder

Cheapest gate first, after every batch.

```bash
# 1. Resolution gate (seconds). failOnVersionConflict() is a free linter.
#    `./gradlew dependencies` at the ROOT is useless - root declares no configurations.
./gradlew :clearth-core:dependencies --configuration runtimeClasspath \
          :clearth-modules:clearth-gui:dependencies --configuration runtimeClasspath \
          :clearth-modules:clearth-th2:dependencies --configuration runtimeClasspath

# 2. Confirm the pin took effect (catches the §5 class of dead entries)
./gradlew :<module>:dependencyInsight --configuration runtimeClasspath --dependency <name>

# 3. TEST-classpath resolution gate. assembleAll does NOT cover this, and it is what
#    CI fails on first (`:clearth-core:compileTestJava`). Never skip it.
./gradlew clean compileTestJava

# 4. Compile gate, all 11 subprojects
./gradlew assembleAll            # build.gradle:330

# 5. Full gate
./gradlew clean buildAll         # build.gradle:331
```

`buildAll` → `subprojects*.build` → `check`, and `check` is wired to **both** test tasks: JUnit `test`
(`build.gradle:208-210`) and TestNG `testNg` (`:212-219`), over the same source set with different engines.
A green `test` alone is not sufficient. Do not pass `-Dtest.ignoreFailures=true` while verifying.

### Downstream consumers re-declare dependencies — a green build here is not enough

`ClearTHCore-public` is not the only consumer of this root `build.gradle`. **`ClearTHCore-internal`** builds
`clearth-core` alongside private modules (`clearth-api`, `clearth-fixmlcomparator`, `clearth-functions`,
`clearth-http`, `clearth-matrixmaker`, `clearth-nft`) and **re-declares some of the same dependencies**. Its
Jenkins job runs:

```
gradle -Dtest.ignoreFailures=false --no-daemon clean build -p ClearTHCore-internal
```

**Incident (2026-09-08).** sqlite-jdbc was bumped 3.45.3.0 -> 3.53.2.0 at both of its declaration sites in
this repo (`build.gradle` testImplementation, `clearth-core/build.gradle` api). Both agreed, so nothing
conflicted locally and every gate passed. But sqlite-jdbc had **no entry in the force list**, and internal
still requested 3.45.3.0, so `failOnVersionConflict()` failed there:

```
Execution failed for task ':clearth-core:compileTestJava'.
> Conflict found for the following module:
    - org.xerial:sqlite-jdbc between versions 3.53.2.0 and 3.45.3.0
```

Fixed by adding `"org.xerial:sqlite-jdbc:<version>"` to the root force list. Two lessons:

1. **Before bumping any coordinate, grep the force list for it.** No force entry means the version is decided
   by conflict resolution across *all* consumers, including ones not in this repo. Add the force.
2. **Consistency inside this repo proves nothing about downstream.** For any coordinate declared more than
   once, or exported as `api` from clearth-core, a force entry is the only thing that makes the version
   authoritative.

Root `testImplementation` coordinates *without* force entries — latent instances of the same trap:
`org.mockito:mockito-core`, `org.testng:testng`, `com.h2database:h2`, `org.assertj:assertj-core`. They are
currently fine only because internal happens to request the same versions. Add a force before bumping any of
them.

**Java version.** There is no `sourceCompatibility`, `targetCompatibility` or toolchain anywhere in the
build, so bytecode targets whatever JVM runs Gradle — typically Temurin 21 locally, while `README.md`
promises JDK 11. A green build on 21 proves nothing about the JDK 11 contract, and several tempting upgrades
raise the floor above 11 (quickfixj 3.x → 17, myfaces 4.x, jetty 12 → 17) while compiling fine on 21 and
failing at deployment:

```bash
./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-11-openjdk-amd64 clean buildAll
```

## 8. Batching, to keep rescans cheap

Batch by dependency tree, not by CVE — one commit and at most one rescan per tree: `jackson`, `netty`,
`kotlin`, `commons-*`, `httpclient5/core5`, `th2+grpc+protobuf`, `jsf(myfaces+primefaces)`, `activemq`,
`quickfixj+mina`. Run the resolution gate (§7 gate 1) between batches; it is seconds, and
`failOnVersionConflict()` catches most mistakes without a rescan.

Also note `cacheChangingModulesFor 0, 'seconds'` (`build.gradle:197`) forces a metadata re-check on every
resolution, and clearth-th2 resolves against an extra Sonatype repo — a scan is only as fast as its slowest
repository, so leave th2 for last.

## 9. Settled decisions (2026-09-01 remediation round)

Outcomes already established. Do not re-derive these.

- **Jackson: one generation only.** `clearth-core` exported `tools.jackson.core:jackson-databind:3.2.1` as
  `api` while **zero** Java files imported `tools.jackson` and 38 imported `com.fasterxml.jackson` — commit
  `217ddd6` had performed the "upgrade" as a groupId rename without touching code. `dependencyInsight`
  confirmed that single declaration was the only root of the whole Jackson 3 tree. Replaced with
  `com.fasterxml.jackson.core:jackson-databind:2.22.1` (which the code genuinely needs: `ObjectMapper` and
  friends in 15 files, annotations in 24) and the three `tools.jackson*` forces removed. If Jackson 3 is ever
  wanted, it is a code migration, not a coordinate swap.
- **`jackson-annotations` stays at `2.22`.** `2.22.1` does not exist on Central. The constraint was corrected
  down to `2.22`; never "align" it upward.
- **The 13 netty forces were redundant** and are gone. Verified afterwards that all 40 netty artifacts still
  resolve to the pinned version via the `eachDependency` rule, with `netty-tcnative-classes` correctly exempt.
- **`dependency-check/suppressions.xml` is now empty** (header comment only). Its single entry was inert three
  times over: `until="2023-09-01"` had passed, its sha1 no longer matched the jar on the classpath
  (`task-utils` is now 0.1.4), and neither of its CVEs appeared anywhere in the report.
- **Pre-existing test failures on master.** `clean buildAll --continue` yields **125 failing testcases**, all in
  clearth-core, mostly `testNg` (`MatrixFunctionsTest` 63, `ExpressionCalculatorToolTest` 12,
  `MvelExpressionValidatorTest` 11, Scheduler tests, `ActionExecutorTest`). These fail identically on
  unmodified `HEAD` — verified by stashing and re-running. **`buildAll` cannot go green on this branch**, so
  never treat a red build as evidence your dependency change broke something: diff the failing-test set
  against a `HEAD` control run instead of reading the exit code.
- **Below-gate items left alone deliberately** (gate is CVSS >= 7.6): ActiveMQ 5.19.8 carries CVE-2026-59878 at
  **7.5**, just under. 5.19.10 exists and is a same-minor patch, so it would not hit the `jakarta.jms` wall —
  a cheap win whenever the gate tightens. Netty 4.2.16.Final carries a 6.3 across ~50 artifacts.

## 10. Don't do these

- **Do not consolidate version literals into the root `ext` block as part of a CVE commit.** A remediation
  diff should read `- 2.3.30` / `+ 2.3.34`. This project's upgrade history is a long run of "Dependencies
  upgrade" commits whose only searchable signal is the literal (`git log -S '<version>'`); replacing literals
  with `${x}` destroys that signal and bisectability. Separate mechanical commit, if at all.
- **Do not introduce a `libs.versions.toml` version catalog.** A catalog cannot express
  `resolutionStrategy.force`, which is what this build depends on for `failOnVersionConflict()`. You would
  end up with versions in two places instead of one.
- **Do not add a toolchain inside a CVE commit** — worth doing, but separately.
- **Do not write suppressions.** Report and let the user decide.
