<#import "common.ftl" as common>

<#macro printComparisonTable result resultId>
	<@common.printDetails 
		details = result.resultDetails![]  
		isMaxWidth = result.maxWidth!true
	/>
</#macro>
