<#macro printMatricesTable matricesData>
	<#escape x as (x!"")?html>
		<table border="1" frame="box" rules="all">
			<tr>
				<th>Matrix</th>
				<th>Uploaded</th>
				<th>Trim spaces</th>
				<th>Execute</th>
			</tr>
			<#list matricesData as matrixData>
				<tr>
					<td>
						<a href="matrices/${matrixData.name}">${matrixData.name}</a>
					</td>
					<td>
						<#if matrixData.uploadDate??>
							${matrixData.uploadDate?string["dd.MM.yy HH:mm:ss"]}
						</#if>
					</td>
					<td>${matrixData.trim?then("true", "false")}</td>
					<td>${matrixData.execute?then("true", "false")}</td>
				</tr>
			</#list>
		</table>
	</#escape>
</#macro>