<#import "common.ftl" as common>

<#function formatHeader comment status>
    <#if comment != "">
        <#return comment + " (" + status?upper_case + ")">
    <#else>    
        <#return status?upper_case>
    </#if>
</#function>

<#macro printComparisonTable result resultId>
	<#escape x as x?html>
		<#list result.details>
			<#items as block>
				<#assign id = resultId + "_block" + block?counter>
				<div class="status">
					<#assign status = block.success?then('passed','failed')>
					<span class="node ${status} switch" onclick = "showhide(this, '${id}');">${block?counter}. ${formatHeader(block.comment!"", status)}</span>
					<div class="container" id="${id}">
						<@common.printDetails 
							details = block.details![]
							isMaxWidth = result.maxWidth!true
						/>
					</div>
				</div>
			</#items>
		<#else>
			<div>None</div>
		</#list>
	</#escape>
</#macro>
