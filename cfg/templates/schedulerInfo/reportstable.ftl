<#macro printReportsTable reportsData>
	<#escape x as (x!"")?html>
		<table border="1" frame="box" rules="all">
			<tr>
				<th>Matrix</th>
				<th>Actions done</th>
				<th>Successful</th>
			</tr>
			<#list reportsData as reportData>
				<tr class='${reportData.successful?then("", "failed")}'>
					<td>
						<a href="reports/${reportData.fileName}/report.html">${reportData.name}</a>
						<a href="reports/${reportData.fileName}/report_failed.html">(only failed)</a>
					</td>
					<td>${reportData.actionsDone}</td>
					<td>${reportData.successful?then("true", "false")}</td>
				</tr>
			</#list>
		</table>
	</#escape>
</#macro>