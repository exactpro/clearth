<#import "detailedresult.ftl" as detailedResult>

<#macro printComparisonTable result resultId>
	<#if instanceOf(result, DetailedResult)>
		<@detailedResult.printComparisonTable
			result = result
			resultId = resultId />
	</#if>
</#macro>