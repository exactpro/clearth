<#--
	External parameters: action, status, containerId, statusExpanded
-->

<#import "result.ftl" as resultTemplate>
<#import "common.ftl" as common>

<#macro printSubActionsData subActions containerId margin>
	<#list subActions?keys as key>
	    <#if subActions[key]??>
            <#assign 
                subActionData = subActions[key]			
                containerIdSubAction = containerId+"_"+key
                subActionStatus = subActionData.success
                statusSubAction = subActionStatus.passed?then("passed", "failed")
            >
            <div class="subaction">
                <span class="node ${statusSubAction} switch" onclick="showhide(this, '${containerIdSubAction}');">
                    ${key} - ${subActionData.name!""} - ${statusSubAction} 
                </span>
                <#if subActionData.idInTemplate?? && subActionData.idInTemplate != "">
                    <span style="color:#9370DB">&#160;&#160;&#160;IdInTemplate:  </span>
                    <span style="color:Black; font-weight:normal">${subActionData.idInTemplate}</span>
                </#if>
                <div class="container" id="${containerIdSubAction}">
                    <@common.printStatusTable
                        status = subActionStatus
                        statusLabel = "Sub-action status"
                        containerId = containerIdSubAction + "_status"
                        statusExpanded = false
                    />
                    <@common.printInputParametersTable 
                        containerId = containerIdSubAction
                        parameters = subActionData.params![]
                        keys = subActionData.params?keys![]
                        formulas = subActionData.formulas![]
                        margin = margin!0
                    />
                    <#if subActionData.subActionData?? && subActionData.subActionData?size != 0>
                        <div class="node">Sub-actions of ${key}</div>
                        <@printSubActionsData 
                            subActions = subActionData.subActionData 
                            containerId = containerIdSubAction 
                            margin = margin!0 
                        />
                    </#if>
                    <br>
                </div>
            </div>
        </#if>
	</#list>
</#macro>

<#escape x as x?html>
	<div class="action">
		<#assign 
			statusForClass = action.inverted?then("inverted ", "") + status.passed?then("passed", common.getFailStatus(status.failReason))
			
			statusForTitle = action.inverted?then("INVERTED TO ", "") + status.passed?then("PASSED", "FAILED")
		>
		<#if action.async && !action.payloadFinished>
			<span class="node async switch" onclick="showhide(this, '${containerId}');">${action.idInMatrix} - ${action.name}
				<#if action.comment?? && action.comment != "">
					<span style="color:#9370DB">&#160;&#160;&#160;Comment: </span><span style="color:Black; font-weight:normal">${action.comment}</span>
				</#if>
				<#if action.idInTemplate?? && action.idInTemplate != "">
					<span style="color:#9370DB">&#160;&#160;&#160;IdInTemplate:  </span>
					<span style="color:Black; font-weight:normal">${action.idInTemplate}</span>
				</#if>
			</span>
		</#if>
		<#if !action.async || (action.async && action.payloadFinished)>
			<span class="node ${statusForClass} switch" onclick="showhide(this, '${containerId}');">${action.idInMatrix} - ${action.name} (${statusForTitle})
				<#if action.comment?? && action.comment != "">
					<span style="color:#9370DB">&#160;&#160;&#160;Comment: </span><span style="color:Black; font-weight:normal">${action.comment}</span>
				</#if>
				<#if action.idInTemplate?? && action.idInTemplate != "">
					<span style="color:#9370DB">&#160;&#160;&#160;IdInTemplate:  </span>
					<span style="color:Black; font-weight:normal">${action.idInTemplate}</span>
				</#if>
        	</span>
		</#if>
		<div class="container" id="${containerId}">
			<#if action.timeOut?? && action.timeOut != 0>
				<div class="desc"><span>Timeout: </span>${action.timeOut}</div>
			</#if>
			<#if action.async>
				<div class="desc">Action executed asynchronously</div>
				<#if action.asyncGroup?? && action.asyncGroup != "">
					<div class="desc"><span>Asynchronous group: </span>${action.asyncGroup}</div>
				</#if>
				<div class="desc"><span>Wait for end: </span>${action.waitAsyncEnd}</div>
			</#if>
			<@common.printStatusTable
				status = status
				statusLabel = "Action status"
				containerId = containerId + "_status"
				statusExpanded = statusExpanded
			/>			
			<table class="action_data">
				<tr>
					<td class="params">
						<div class="node">Input parameters</div>
						<#assign keys = action.matrixInputParams![]>
						<#if keys?size != 0>
							<@common.printInputParametersTable 
								containerId = containerId
								parameters = action.inputParams![]
								keys = keys
								formulas = action.formulas![]
								margin = 0
							/>
						<#else>
							<div>None</div>	
						</#if>
						<#if action.formulaExecutable?? || action.formulaComment?? || action.formulaTimeout?? || action.formulaInverted??>
							<@common.printMainInputParametersTable 
								containerId = containerId
								action = action
								margin = 0
							/>
						</#if>
						<br>
						<div class="node">Sub-actions</div>
						<#if action.subActionData??>
							<@printSubActionsData 
								subActions = action.subActionData 
								containerId = containerId 
								margin = 0 
							/>
						<#else>
							<div>None</div>
						</#if>
						<#assign outputParameters = action.outputParams![]>
						<#if outputParameters?size != 0>
							<br>
							<div class="node">Output parameters</div>
								<table border="1" frame="box" rules="all" class="w-limited">
									<tr class="tablehead">
										<td>Name</td>
										<td>Value</td>
									</tr>
									<#list outputParameters?keys as key>
										<tr>
											<td>${key}</td>
											<td>${outputParameters[key]!""}</td>
										</tr>
									</#list>
								</table>
						</#if>
					</td>
					<td class="comps">
						<div class="node">Comparison table</div>
						<#if action.result??>
							<@resultTemplate.printComparisonTable
								result = action.result
								resultId = containerId + "_comparison"
							/>
						<#else>
							<div>None</div>
						</#if>
					</td>
				</tr>
				<tr>
					<td class="text-elements">
						<div>
							<#if action.result??>
								<@resultTemplate.printAdditionalInfo
									result = action.result
									resultId = containerId + "_text_element"
								/>
							</#if>
						</div>
					</td>
				</tr>
			</table>
			<#if instanceOf(action, MacroAction)>
				<div class="node">Nested actions (passed: ${action.passedActionsCount}/${action.executedActionsCount}, hidden: ${action.hiddenActionsCount})</div>
				<#if action.executedActionsCount != action.hiddenActionsCount>
					<@includeFile name=action.nestedActionsReportsPath />
				</#if>
			</#if>
			<#if (action.result.message)??>
                <#noescape><div class="message"><span>Info: </span>${action.result.message}</div></#noescape>
            </#if>
		</div>
	</div>
</#escape>
