<#--
	External parameters: userName, scriptName, testPassed, stepsData
-->

<#import "step.ftl" as stepTemplate>

<!DOCTYPE HTML>
<html>
	<head>
		<title>${scriptName} (${testPassed?then("PASSED", "FAILED")})</title>
	</head>
	<body>
		<table width="100%" border="1" id="header">
			<tr>
				<td>Script: ${scriptName}</td>
				<td width="30%">Result: <span class="${testPassed?then('passed','failed')}" style="font-weight: bold;">${testPassed?then('PASSED','FAILED')}</span></td>
			</tr>
			<tr>
				<td>User: ${userName}</td>
			</tr>
			<tr>
				<td>Description: <span style="white-space: pre-wrap">${description!""}</span></td>
			</tr>
		</table>
		<div class="nodelist">
			<#list stepsData as stepData>
				<@stepTemplate.printStep
					step = stepData.step
					status = stepData.stepStatus
					containerId = "cont" + stepData?counter
					pathToActionsFile = stepData.pathToActionsFile!""
					expanded = stepData.statusExpanded
					stepName = stepData.stepName
					async = stepData.async />
			</#list>
		</div>
	</body>
</html>
