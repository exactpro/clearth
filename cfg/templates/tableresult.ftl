<#import "common.ftl" as common>

<#macro printComparisonTable result resultId>
	<@printDetails
	    result = result
	/>
</#macro>

<#macro printDetails result>
	<#escape x as (x!"")?html>
		<#if result.header??> 
			<div class="node ${common.getHeaderColor(result)}">${result.header}</div>
		</#if>
		<table border="1" frame="box" rules="all" class="w-limited">
			<tr class="tablehead">
				<#list result.columns as col>
					<td>${col}</td>
				</#list>
				<#if result.hasStatus>
					<td>Status</td>
				</#if>
			</tr>
				
			<#list result.details as detail>
				<tr>
					<#list detail.values as val>
						<td>${val}</td>
					</#list>
					<#if result.hasStatus>
						<#assign status = detail.info?then('info',detail.identical?then('passed','failed'))>
						<td class="comparison_result ${status}">${status?upper_case}</td>
					</#if>
				</tr>
			</#list>
		</table>
	</#escape>
</#macro>