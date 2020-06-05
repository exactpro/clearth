<#macro printStepsTable stepsData>
	<#escape x as (x!"")?html>
		<table border="1" frame="box" rules="all">
			<tr>
				<th>Name</th>
				<th>Kind</th>
				<th>Start at</th>
				<th>Ask for continue</th>
				<th>Ask if failed</th>
				<th>Execute</th>
				<th>Started</th>
				<th>Actions successful</th>
				<th>Finished</th>
			</tr>
			<#list stepsData as stepData>
				<#assign step=stepData.stepData>
				<tr>
					<td>${stepData.stepName}</td>
					<td>${step.kind}</td>
					<td>${step.startAt}</td>
					<td>${step.askForContinue?then("true", "false")}</td>
					<td>${step.askIfFailed?then("true", "false")}</td>
					<td>${step.execute?then("true", "false")}</td>
					<td>
						<#if step.started??>
							${step.started?string["dd.MM.yy HH:mm:ss"]}
						</#if>
					</td>
					<td>${step.executionProgress}</td>
					<td>
						<#if step.finished??>
							${step.finished?string["dd.MM.yy HH:mm:ss"]}
						</#if>
					</td>
				</tr>
			</#list>
		</table>
	</#escape>
</#macro>