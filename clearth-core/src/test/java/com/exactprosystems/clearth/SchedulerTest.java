/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class SchedulerTest
{
	private static final Path USER_DIR = Paths.get(System.getProperty("user.dir")),
			TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/SchedulerTestData");

	private static final String TEST_OUTPUT = "testOutput/",
			LOGS_DIR = USER_DIR.getParent().resolve(TEST_OUTPUT).resolve("logs").toString(),
			TEST_DATA_DIR = TEST_OUTPUT + "appRoot/",
			TEST_REPORT_DIR = TEST_OUTPUT + "SchedulerTestData/",
			LOG_PROPERTIES_FILE_PATH = USER_DIR + "/src/test/resources/log.properties",
			APP_ROOT = USER_DIR.getParent().resolve(TEST_DATA_DIR).toString(),
			WEB_APP_DIR = USER_DIR.getParent().resolve("clearth-modules/clearth-gui/src/main/webapp").toString(),
			WEB_UI_RESTRICTED = WEB_APP_DIR + "/ui/restricted/",
			DEFAULT_REPORT_FILES_DIR = WEB_APP_DIR + "/WEB-INF/report_files/",
			REALTIME_REPORT_DIR = APP_ROOT + "/ui/restricted/";

	private static boolean clearAppRoot = true, clearLogs = true;
	private final String userName = "test", schedulerName = "Test";

	@Parameterized.Parameter
	public Path testConfigPath;

	@Parameterized.Parameters(name = "config: {0}")
	public static Iterable<?> data() throws IOException
	{
		try (Stream<Path> list = Files.list(TEST_CONFIG_DIR))
		{
			return list
					.filter(f -> f.toFile().isDirectory())
					.sorted()
					.collect(Collectors.toList());
		}
	}

	protected List<File> getMatricesFiles(Path matrixDir) throws IOException
	{
		try (Stream<Path> list = Files.list(matrixDir))
		{
			return list
					.map(Path::toString)
					.filter(s -> "csv".equalsIgnoreCase(FilenameUtils.getExtension(s)))
					.map(File::new)
					.collect(Collectors.toList());
		}
	}

	protected File getSchedulerConfig(Path pathCfg) throws ClearThException
	{
		File schedulerConfig = pathCfg.toFile();
		if (!schedulerConfig.exists())
			throw new ClearThException("File 'config.cfg' with scheduler parameters is not found");

		return schedulerConfig;
	}

	@Test
	public void executeTest() throws ClearThException, AutomationException
	{
		Scheduler scheduler = getScheduler();
		List<String> warnings = loadSteps(scheduler);
		if (!warnings.isEmpty())
			throw new AutomationException("Steps loading errors:" + warnings);
		loadMatrices(scheduler);
		scheduler.start(userName);

		try
		{
			while (scheduler.isRunning())
			{
				TimeUnit.SECONDS.sleep(1);
			}
		}
		catch (InterruptedException e)
		{
			throw new ClearThException(e);
		}

		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		if (launchesInfo == null || launchesInfo.isEmpty())
			throw new ClearThException("Launches data is not found");

		allSuccessVerify(launchesInfo.get(0));
	}

	@Before
	public void startTestApplication() throws Exception
	{
		if (ClearThCore.getInstance() == null)
		{
			ClearThCore application = getCoreInstance();
			DeploymentConfig dc = getDeploymentConfig();
			ConfigFiles configFiles = getConfigFiles();
			dc.init(configFiles);
			application.init(configFiles, dc);
		}
	}

	@After
	public void clearSchedulerData()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(userName);
		userSchedulers.clear();
	}

	@AfterClass
	public static void dispose() throws Exception
	{
		ClearThCore coreInstance = ClearThCore.getInstance();
		if (coreInstance != null)
		{
			coreInstance.beforeShutdown();
			if (clearAppRoot)
			{
				File appRoot = new File(coreInstance.getAppRoot());
				if (appRoot.exists())
					FileUtils.cleanDirectory(appRoot);
			}
		}

		if (clearLogs)
		{
			File logs = new File(LOGS_DIR);
			if (logs.exists())
				FileUtils.cleanDirectory(logs);
		}
	}

	protected Scheduler getScheduler() throws ClearThException
	{
		SchedulersManager schedulersManager = ClearThCore.getInstance().getSchedulersManager();
		Scheduler scheduler = schedulersManager.getSchedulerByName(schedulerName, userName);
		if (scheduler == null)
		{
			try
			{
				scheduler = schedulersManager.addScheduler(userName, schedulerName);
			}
			catch (Exception e)
			{
				String msg = String.format("An error occurred while creating scheduler '%s' for user '%s'",
						schedulerName, userName);
				throw new ClearThException(msg, e);
			}
		}

		return scheduler;
	}

	protected List<String> loadSteps(Scheduler scheduler) throws ClearThException
	{
		File stepCfg = getSchedulerConfig(testConfigPath.resolve("configs/config.cfg"));
		List<String> warnings = new ArrayList<>();
		try
		{
			scheduler.uploadSteps(stepCfg, stepCfg.getName(), warnings, false);
		}
		catch (Exception e)
		{
			String msg = String.format("An error occurred while uploading scheduler configuration from file '%s'",
					stepCfg.getName());
			throw new ClearThException(msg, e);
		}

		return warnings;
	}

	private void loadMatrices(Scheduler scheduler) throws ClearThException
	{
		List<File> matrixFiles;
		try
		{
			matrixFiles = getMatricesFiles(testConfigPath.resolve("matrices"));
		}
		catch (IOException e)
		{
			throw new ClearThException("Error on matrix files getting", e);
		}

		matrixFiles.forEach(scheduler::addMatrix);
	}

	protected ConfigFiles getConfigFiles()
	{
		ConfigFiles cfg = new ConfigFiles("clearth.cfg");
		ConfigFiles spy = spy(cfg);

		when(spy.getReportFilesDir()).thenReturn(DEFAULT_REPORT_FILES_DIR);
		when(spy.getReportsDir()).thenReturn(TEST_REPORT_DIR + cfg.getReportsDir());
		when(spy.getAutomationStorageDir()).thenReturn(TEST_DATA_DIR + cfg.getAutomationStorageDir());
		when(spy.getScriptsDir()).thenReturn(TEST_DATA_DIR + cfg.getScriptsDir());
		when(spy.getSchedulersDir()).thenReturn(TEST_DATA_DIR + cfg.getSchedulersDir());
		when(spy.getSchedulersFileName()).thenReturn(TEST_DATA_DIR + cfg.getSchedulersFileName());
		when(spy.getRealTimeReportDir()).thenReturn(REALTIME_REPORT_DIR);
		when(spy.getConnectionsDir()).thenReturn(TEST_DATA_DIR + cfg.getConnectionsDir());
		when(spy.getLogsDir()).thenReturn(TEST_DATA_DIR + cfg.getLogsDir());
		when(spy.getLogCfgFileName()).thenReturn(LOG_PROPERTIES_FILE_PATH);

		return spy;
	}

	protected DeploymentConfig getDeploymentConfig()
	{
		DeploymentConfig dc = mock(DeploymentConfig.class, CALLS_REAL_METHODS);
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		ClearThVersion testRelease = new ClearThVersion("TestRelease", dateFormat.format(new Date()));
		when(dc.getCTHVersion()).thenReturn(testRelease);
		doAnswer((Answer<Void>) invocation ->
		{
			Whitebox.setInternalState(dc, "appRoot", APP_ROOT);
			return null;
		}).when(dc).configureAppRoot();

		return dc;
	}

	protected ClearThCore getCoreInstance() throws ClearThException
	{
		Logger coreLogger = LoggerFactory.getLogger(ClearThCore.class);
		ClearThCore core = spy(ClearThCore.class);
		when(core.getAdditionalTemplateParams()).thenReturn(null);
		when(core.isUserSchedulersAllowed()).thenReturn(false);
		when(core.getLogger()).thenReturn(coreLogger);

		//loadConfig() implementation
		try
		{
			doNothing().when(core).loadConfig(anyString());
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		//initOtherEntities() implementation
		try
		{
			doAnswer((Answer<Void>) invocation ->
			{
				File src = new File(WEB_UI_RESTRICTED);
				File dst = new File(core.getRootRelative(core.getRealTimeReportPath()));
				FileUtils.copyDirectory(src, dst);
				return null;
			}).when(core).initOtherEntities(Matchers.<Object>anyVararg());
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		//createDirs() implementation
		try
		{
			doAnswer((Answer<Void>) invocation ->
			{
				Files.createDirectories(Paths.get(core.getRealTimeReportPath()));
				return null;
			}).when(core).createDirs();
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		return core;
	}

	private void allSuccessVerify(XmlSchedulerLaunchInfo launch)
	{
		assertNotNull("Scheduler launch info is not available", launch);
		assertTrue("Scheduler launch info has not-success status", launch.isSuccess());
		assertTrue("Reports by last launch are not generated", checkLastReportDir(launch.getReportsPath()));
	}

	private boolean checkLastReportDir(String lastReportPath)
	{
		if (lastReportPath.isEmpty())
			return false;
		Path reportPath = Paths.get(ClearThCore.getInstance().getReportsPath()).resolve(lastReportPath);

		try (Stream<Path> list = Files.list(reportPath))
		{
			return Files.exists(reportPath) && list.count() > 0;
		}
		catch (IOException e)
		{
			return false;
		}
	}

}
