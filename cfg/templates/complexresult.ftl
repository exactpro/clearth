<#function getStatus row>
	<#if row.info>
		<#return "info">
	<#elseif row.identical>
		<#return "passed">
	<#else>
		<#return "failed">
	</#if>
</#function>

<#macro formatStatus status>
	<#assign s = status?then('passed','failed') >
	<span class="comparison_result ${s}">${s?upper_case}</span>
</#macro>

<#function isForCompareValues value>
	<#if comparisonUtils.isForCompareValues(value)>
		<#return true>
	<#else>
		<#return false>
	</#if>
</#function>

<#macro printComparisonRows rows>
	<table border="1" class="w-limited">
		<tr class="tablehead">
			<td>Name</td>
			<td>Expected</td>
			<td>Actual</td>
			<td>Status</td>
		</tr>
		<#list rows as row>
			<tr>
				<#if row.breaker>
					<td colSpan="4"><hr></td>
				<#else>
					<#if row.highlighted>
						<td><span style="color: blue; font-weight: bold;">${row.param!""}</span></td>
					<#else>
						<td>${row.param!""}</td>
					</#if>
					<#if row.errorMessage??>
						<td><div class="node">${row.expected!""}</div><div class="error_message">${row.errorMessage}</div></td>
					<#elseif isForCompareValues(row.expected!"")>
						<td><div class="node">${row.expected!""}</div></td>
					<#else>
						<td>${row.expected!""}</td>
					</#if>
					<td>${row.actual!""}</td>
					<#assign status = getStatus(row)>
					<td class="comparison_result ${status}">${status?upper_case}</td>
				</#if>
			</tr>
		</#list>
	</table>
</#macro>

<#macro printComparisonTable result resultId>
	<#escape x as x?html>
		<#list result.resultDetails as resultDetail>
			<#assign mainComparison = resultDetail.mainComparison >
				<div>
					<#if resultDetail.comment?? && resultDetail.comment != "">${resultDetail.comment} - </#if><@formatStatus status = resultDetail.passed /><br>
                    <#if mainComparison.comment?? && mainComparison.comment != "">${mainComparison.comment} - </#if><@formatStatus status = mainComparison.passed />
					<@printComparisonRows rows = mainComparison.rows![] />
				</div>
				    <#if resultDetail.subComparisons??>
                        <#assign subcomparisons = resultDetail.subComparisons >
                        <#if subcomparisons?? && subcomparisons?size != 0>
                            <#list subcomparisons?keys as actionId>
                                <#if subcomparisons[actionId]??>
                                    <#assign comparison = subcomparisons[actionId]>
                                    <br>
                                    <#if comparison.comment?? && comparison.comment != "">${comparison.comment} </#if><@formatStatus status = comparison.passed /><br>
                                    <#if comparison.blocks??>
                                        <#assign blocks = comparison.blocks>
                                        <#list blocks as block>
                                            <div>
                                                <#if block.comment?? && block.comment != "">
                                                    ${block.comment}
                                                </#if>
                                                <#if block.rows??>
                                                    <@printComparisonRows rows = block.rows />
                                                </#if>
                                                <br>
                                            </div>
                                        </#list>
                                    </#if>
                                </#if>
                            </#list>
                        </#if>
                    </#if>
			<#sep><hr><br></#sep>
		</#list>
	</#escape>
</#macro>
