<#import "complexresult.ftl" as complexResult>
<#import "detailedresult.ftl" as detailedResult>
<#import "multidetailedresult.ftl" as multiDetailedResult>
<#import "richtextresult.ftl" as richTextResult>
<#import "tableresult.ftl" as tableResult>
<#import "containerresult.ftl" as containerResult>
<#import "attachedfilesresult.ftl" as attachedFilesResult>

<#macro printAdditionalInfo result, resultId>
	<#if instanceOf(result, RichTextResult)>
		<@richTextResult.printAdditionalInfo
			result = result
			resultId = resultId
		/>
	</#if>
</#macro>

<#macro printComparisonTable result resultId>
	<#if instanceOf(result, ComplexResult)>
		<@complexResult.printComparisonTable
			result = result
			resultId = resultId
		/>
	<#elseif instanceOf(result, DetailedResult)>
		<@detailedResult.printComparisonTable
			result = result
			resultId = resultId
		/>
	<#elseif instanceOf(result, MultiDetailedResult)>
		<@multiDetailedResult.printComparisonTable
			result = result
			resultId = resultId
		/>
	<#elseif instanceOf(result, TableResult)>
		<@tableResult.printComparisonTable
			result = result
			resultId = resultId
		/>
	<#elseif instanceOf(result, ContainerResult)>
		<@containerResult.printComparisonTable
			result = result
			resultId = resultId
		/>
	</#if>
</#macro>

<#macro printAttachedFiles result resultId>
	<#if instanceOf(result, AttachedFilesResult)>
		<@attachedFilesResult.printFiles
			filesHolder = result
			containerId = resultId
		/>
	</#if>
</#macro>