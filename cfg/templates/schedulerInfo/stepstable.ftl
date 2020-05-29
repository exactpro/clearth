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
				<tr>
					<td>${stepData.stepName}</td>
					<td>${stepData.step.kind}</td>
					<td>${stepData.step.startAt}</td>
					<td>${stepData.step.askForContinue?then("true", "false")}</td>
					<td>${stepData.step.askIfFailed?then("true", "false")}</td>
					<td>${stepData.step.execute?then("true", "false")}</td>
					<td>
						<#if stepData.step.started??>
							${stepData.step.started?string["dd.MM.yy HH:mm:ss"]}
						</#if>
					</td>
					<td>${stepData.step.executionProgress}</td>
					<td>
						<#if stepData.step.finished??>
							${stepData.step.finished?string["dd.MM.yy HH:mm:ss"]}
						</#if>
					</td>
				</tr>
			</#list>
		</table>
	</#escape>
</#macro>