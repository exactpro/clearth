<#import "common.ftl" as common>
<#import "result.ftl" as resultTemplate>

<#macro printComparisonTable result resultId>
	<#escape x as (x!"")?html>
		<#assign color = common.getHeaderColor(result)>
		<#assign statusLine = result.success?then("PASSED","FAILED")>
		<div class="status">
			<#assign blockId = "block_" + statics["java.lang.System"].nanoTime()>
			<span class="node ${color} switch" onclick="showhide(this, '${blockId}');">
				<#if result.name??>${result.name}. </#if>${statusLine}
			</span>
			
			<div class="container" id="${blockId}">
				<#if result.comment??>
					<div>Comment: ${result.comment}</div>
				</#if>
				<#if result.header??>
					<#noescape><div> ${result.header}. </div></#noescape> 
				</#if>
				<@showDetails
					result = result
				/>
			</div>
		</div>
	</#escape>
</#macro>

<#macro showDetails result>
	<#list result.details as detail>
		<div class="status">
			<#assign blockId = "block_" + statics["java.lang.System"].nanoTime()>
			<span class="node ${color} switch" onclick="showhide(this, '${blockId}');">
				<#if detail.comment??>${detail.comment}. </#if>${statusLine}
			</span>
			
			<div class="container" id="${blockId}">
				<@showAdditionalInfoWithoutComment
					result = result
					detail = detail
				/>
				<@resultTemplate.printComparisonTable
					result = detail
					resultId = "resultId_" + statics["java.lang.System"].nanoTime()
				/>
			</div>
		</div>
	</#list>
</#macro>

<#macro showAdditionalInfo result detail>
	<#escape x as (x!"")?html>
		<#if detail.comment??>
			<div><span class="comment">Comment: </span>${detail.comment}</div>
		</#if>
		<@showAdditionalInfoWithoutComment
			result = result
			detail = detail
		/>
	</#escape>
</#macro>

<#macro showAdditionalInfoWithoutComment result detail>
	<#escape x as (x!"")?html>
		<#if detail.error??>
			<@common.printErrorMessage
				error = detail.error
				id = "error_block_" + statics["java.lang.System"].nanoTime()
			/>
		</#if>
		<#if detail.message??>
			<div class="message"><span>Info: </span>${detail.message}</div>
		</#if>
	</#escape>
</#macro>