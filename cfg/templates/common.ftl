<#function getStatusWithInfo detail>
	<#if detail.info>
		<#return "info">
	<#elseif detail.identical>
		<#return "passed">
	<#else>
		<#return "failed">
	</#if>
</#function>

<#function getFailStatus failReason='FAILED'>
	<#switch failReason>
		<#case "COMPARISON">
			<#return "failed_comparison">
		<#case "CALCULATION">
			<#return "failed_calculation">
		<#case "NOT_EXECUTED">
			<#return "not_executed">
		<#default>
			<#return "failed">
	</#switch>
</#function>

<#function getFailStatusTitle failReason='FAILED'>
	<#switch failReason>
		<#case "NOT_EXECUTED">
			<#return "NOT EXECUTED">
		<#default>
			<#return "FAILED">
	</#switch>
</#function>

<#function getHeaderColor result>
	<#if instanceOf(result, TableResult) || instanceOf(result, ContainerResult)>
		<#if !result.hasStatus>
			<#return "">
		<#elseif result.success>
			<#return "passed">
		<#else>
			<#return result.useFailReasonColor?then(getFailStatus(result.failReason), "failed")>
		</#if>
	<#elseif instanceOf(result, CsvDetailedResult)>
		<#if result.success>
			<#return "passed">
		<#else>
			<#return getFailStatus(result.failReason)>
		</#if>
	</#if>
</#function>

<#function isSpecialValue expected>
	<#list specialCompValues as specialValue>
		<#if specialValue == expected>
			<#return true>
			<#break>
		</#if>
	</#list>
	<#return false>
</#function>

<#function prepareParam value>
	<#if statics["com.exactprosystems.clearth.utils.SpecialValue"].isSpecialValue(value)>
		<#return statics["com.exactprosystems.clearth.utils.SpecialValue"].convert(value)>
	<#else>
		<#return value>
	</#if>
</#function>

<#function isForCompareValues value>
	<#if comparisonUtils.isForCompareValues(value)>
		<#return true>
	<#else>
		<#return false>
	</#if>
</#function>

<#macro printDetails details isMaxWidth>
	<#escape x as (x!"")?html>
		<#list details>
			<table border="1" frame="box" rules="all" <#if isMaxWidth>class="w-limited"</#if>>
				<tr class="tablehead">
					<td>Name</td>
					<td>Expected</td>
					<td>Actual</td>
					<td>Status</td>
				</tr>
				<#items as detail>
					<tr class="comparison_line_${detail.identical?then('passed','failed')}">
						<td>${detail.param}</td>
						<#if detail.errorMessage??>
							<td><div class="node">${detail.expected!""}</div><div class="error_message">${detail.errorMessage}</div></td>
						<#elseif isForCompareValues(detail.expected!"")>
							<td><div class="node">${detail.expected!""}</div></td>
						<#else>
							<td>${detail.expected!""}</td>
						</#if>
						<td>${detail.actual!""}</td>
						<#assign status = getStatusWithInfo(detail)>
						<td class="comparison_result ${status}">${status?upper_case}</td>
					</tr>
				</#items>
			</table>
		<#else>
			<div>None</div>
		</#list>
	</#escape>
</#macro>

<#macro printStatusTable status statusLabel containerId statusExpanded>
	<div class="status">
		<#assign statusForClass = status.passed?then("passed", getFailStatus(status.failReason))>
		<span class="node ${statusForClass} switch" ${(statusExpanded!false)?then('style="background-image: url(hide.gif);"','')} onclick="showhide(this, '${containerId}');">${statusLabel}</span>
		<div class="container" id="${containerId}" ${(statusExpanded!false)?then('style="display: block;"', '')}>
			<table border="1" class="big-w-limited">
				<tr>
					<td>Status</td>
					<td>${status.passed?then('PASSED', 'FAILED')}</td>
				</tr>
				<#if status.started??>
					<tr>
						<td>Started</td>
						<td>${status.started?string["HH:mm:ss:SSS"]}</td>
					</tr>
				</#if>
				<#if status.finished??>
					<tr>
						<td>Finished</td>
						<td>${status.finished?string["HH:mm:ss:SSS"]}</td>
					</tr>
				</#if>
				<#if status.actualTimeout &gt; 0>
					<tr>
						<td>Actual timeout</td>
						<td>${status.actualTimeout}</td>
					</tr>
				</#if>
				<#if status.waitBeforeAction &gt; 0>
					<tr>
						<td>Wait before action</td>
						<td>${status.waitBeforeAction}</td>
					</tr>
				</#if>
				<#if status.comments??>
					<tr>
						<td style="vertical-align: top;">Description</td>
						<td><#escape x as (x!"")?html?replace("\r\n|\r","<br/>","r")>${status.comments?join("\n")}</#escape></td>
					</tr>
				</#if>
				<#if status.error??>
					<#assign id = containerId + "_error">
					<#assign error = status.error>
					<tr valign="top">
						<td>Exception</td>
						<td class="exception">
							<@printErrorMessage
								error = status.error
								id = id
							/>
						</td>
					</tr>
				</#if>
			</table>
		</div>
	</div>
</#macro>

<#macro printErrorMessage error id>
	<span class="error_message switch" onclick="showhide(this, '${id}');">${error.class}: <#escape x as (x!"")?html>${error.message!""}</#escape></span>
	<div class="container stack_trace" id="${id}">
		<@printErrorText
			error = error/>
	</div>
</#macro>

<#macro printErrorText error>
	<#list error.stackTrace as stackTraceElement>
		${stackTraceElement}<br>
	</#list>
	<#if error.cause??>
		Caused by: ${error.cause.class} : <#escape x as (x!"")?html>${error.cause.message!""}</#escape><br>
		<@printErrorText
			error = error.cause/>
	</#if>
</#macro>

<#macro printInputParametersTable containerId parameters keys formulas margin>
	<#escape x as (x!"")?html>
		<table border="1" frame="box" rules="all" style="margin-left:${margin}em;">
			<tr class="tablehead">
				<td>Name</td>
				<td>Value</td>
			</tr>
			<#list keys as key>
				<#if formulas?size != 0 && formulas[key]??>
					<#assign 
						contId = containerId + "_subformula" + key?counter
					>
					<tr>
						<td>${key}</td>
						<td><span class="node switch" onclick="showhide(this, '${contId}');">${prepareParam(parameters[key]!"")}<div class="container" id="${contId}">${formulas[key]}</div></span></td>
					</tr>
				<#else>
					<tr>
						<#assign value = parameters[key]!"" >
						<td>${key}</td>
						<#if isSpecialValue(value)>
							<td><@node>${value}</@node></td>
						<#else>
							<td>${value}</td>
						</#if>
					</tr>
				</#if>
			</#list>
		</table>
	</#escape>
</#macro>

<#macro printMainInputParametersTable containerId action margin>
	<#escape x as (x!"")?html>
		<#assign mainContainerId = containerId + "_mainParams">
		<span class="switch" onclick="showhide(this, '${mainContainerId}');">Basic parameters</span>
		<div class="container" id="${mainContainerId}">
			<table border="1" frame="box" rules="all" style="margin-left:${margin}em;">
				<tr class="tablehead">
					<td>Name</td>
					<td>Value</td>
				</tr>
				<#if action.formulaExecutable??>
					<#assign contId = containerId + "_formulaExecutable">
					<tr>
						<td>Execute</td>
						<td><span class="node switch" onclick="showhide(this, '${contId}');">${action.executable?then("true", "false")}<div class="container" id="${contId}">${action.formulaExecutable}</div></span></td>
					</tr>
				</#if>
				<#if action.formulaComment??>
					<#assign contId = containerId + "_formulaComment">
					<tr>
						<td>Comment</td>
						<td><span class="node switch" onclick="showhide(this, '${contId}');">${action.comment}<div class="container" id="${contId}">${action.formulaComment}</div></span></td>
					</tr>
				</#if>
				<#if action.formulaTimeout??>
					<#assign contId = containerId + "_formulaTimeout">
					<tr>
						<td>Timeout</td>
						<td><span class="node switch" onclick="showhide(this, '${contId}');">${action.timeOut}<div class="container" id="${contId}">${action.formulaTimeout}</div></span></td>
					</tr>
				</#if>
				<#if action.formulaInverted??>
					<#assign contId = containerId + "_formulaInverted">
					<tr>
						<td>Invert</td>
						<td><span class="node switch" onclick="showhide(this, '${contId}');">${action.inverted?then("true", "false")}<div class="container" id="${contId}">${action.formulaInverted}</div></span></td>
					</tr>
				</#if>
			</table>
		</div>
	</#escape>
</#macro>