<#--
	External parameters: userName, scriptName, execStart, execTime, testPassed, pathToStyles, pathToJS, revision, host, stepsData
-->

<#import "step.ftl" as stepTemplate>

<!DOCTYPE HTML>
<html>
	<head>
		<title>${scriptName}<#if execStart??> ${execStart}</#if>  (${testPassed?then("PASSED", "FAILED")})</title>
		<meta content="text/html; charset=UTF-8" http-equiv="Content-Type" />
		<style>
			<@includeFile name=pathToStyles />
		</style>
		<script type="text/javascript">
			<@includeFile name=pathToJS />
		</script>
	</head>
	<body>
		<table width="100%" border="1" id="header">
			<tr>
				<td rowspan="4" class="logo" width="20%">
					<img src="app_logo.gif">
					<div style="font-size: smaller; padding-bottom: 10px">revision #${revision}</div>
				</td>
				<td>Script: ${scriptName}</td>
				<td width="30%">Result: <span class="${testPassed?then('passed','failed')}" style="font-weight: bold;">${testPassed?then('PASSED','FAILED')}</span></td>
				<td rowspan="4" class="logo" width="20%"><img src="logo.gif"></td>
			</tr>
			<tr>
				<td>Execution started: ${execStart}</td>
				<td>Execution time: ${execTime}</td>
			</tr>
			<tr>
				<td>Host: ${host}</td>
				<td>User: ${userName}</td>
			</tr>
			<tr>
				<td>Description: <span style="white-space: pre-wrap">${description!""}</span></td>
				<td>Constants:<br>
				
				<#assign key_list = constants?keys/>
				<#assign value_list = constants?values/>
				<#list key_list as key>
					<#assign seq_index = key_list?seq_index_of(key) />
					<#assign key_value = value_list[seq_index]/>
					<#if key?index gt 4>
						<#assign class = 'hiddenConstant'/>
					<#else>
						<#assign class = 'constant'/>
					</#if>
					<#if formulas?? && formulas?size != 0 && formulas[key]??>
						<#assign
						contId = "matrix_subformula" + key?counter
						>
						<span class="${class}" >
							<span  class="node switch" onclick="showhide(this, '${contId}');">${key}: ${key_value}</span> </br>
							<div class="container" id="${contId}">${formulas[key]}</div>
						</span>
					<#else>
						<span class="${class}" style="margin-left: 10px;" >${key}: ${key_value} <br> </span>
					</#if>
				</#list>
				<#if class?? && class == 'hiddenConstant'>
					<a id="showAllConstButton" style="cursor: pointer; text-decoration: underline;margin-left: 10px;"
							onclick="showAllConstants('hiddenConstant')">all..</a>
				</#if>
				</td>
			</tr>
		</table>
		<div class="nodelist">
			<form action="">
				<label for="ShowPassed"><input type="checkbox" id="ShowPassed" onclick="togglePassed(this.checked)" checked>Show passed actions</label>
				<label for="ShowPassedFields"><input type="checkbox" id="ShowPassedFields" onclick="togglePassedFields(this.checked)" checked disabled>Show passed fields</label>
				<label for="ShowInverted"><input type="checkbox" id="ShowInverted" onclick="toggleInverted(this.checked)" unchecked>Show only inverted actions</label>
				<br><label for="ExpandAll"><input type="checkbox" id="ExpandAll" onclick="toggleExpandAll(this.checked)" unchecked>Expand all</label>
			</form>
			<#list stepsData as stepData>
				<@stepTemplate.printStep
					step = stepData.step
					status = stepData.stepStatus
					containerId = "cont" + stepData?counter
					pathToActionsFile = stepData.pathToActionsFile!""
					expanded = stepData.statusExpanded
					stepName = stepData.stepName
				/>
			</#list>
		</div>
	</body>
</html>
