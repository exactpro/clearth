<#--
	External parameters: resultObject, containerId
-->
<#import "result.ftl" as resultTemplate>
<!DOCTYPE HTML>
<html>
	<head>
	</head>
	<body>
		<div id="result">
		<@resultTemplate.printComparisonTable
			result = resultObject
			resultId = containerId + "_comparison"
		/>
		</div>
	</body>
</html>