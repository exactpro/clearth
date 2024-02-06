<#--
	External parameters: userName, execTime, testPassed, handledTestExecutionId, handlerName
-->

<!DOCTYPE HTML>
<html>
	<head>
	</head>
	<body>
		<table width="25%" border="1" id="header">
			<tr>
				<td width="30%">Result: <span class="${testPassed?then('passed','failed')}" style="font-weight: bold;">${testPassed?then('PASSED','FAILED')}</span></td>
			</tr>
			<tr>
				<td>Execution time: ${execTime}</td>
			</tr>
			<tr>
				<td>User: ${userName}</td>
			</tr>
			<tr>
				<td><#if handlerName?? && handledTestExecutionId??>ID in ${handlerName}: ${handledTestExecutionId}</#if></td>
			</tr>
		</table>
	</body>
</html>
