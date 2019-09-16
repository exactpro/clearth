<#import "common.ftl" as common>
<#import "result.ftl" as resultTemplate>

<#macro printStep step status containerId pathToActionsFile expanded stepName>
	<div class="step">
		<#assign s = status.passed?then('passed','failed')>
		<span class="node ${s} switch" onclick="showhide(this, '${containerId}');">${stepName} - ${step.kind} (${s?upper_case})</span>
		<div class="container" id="${containerId}">
			<label><input type="checkbox" class="expandStep" onclick="toggleExpandStep('${containerId}', this.checked)" unchecked>Expand all actions</label>
			<#if pathToActionsFile != "">
				<@includeFile name=pathToActionsFile />
			</#if>
			<@common.printStatusTable
				status = status
				statusLabel = "Step status"
				containerId = containerId + "status"
				statusExpanded = expanded
			/>
			<#if step.result??>
				<@resultTemplate.printAdditionalInfo
					result = step.result
					resultId = "step_" + containerId + "_text_element"
				/>
			</#if>
		</div>
	</div>
</#macro>
