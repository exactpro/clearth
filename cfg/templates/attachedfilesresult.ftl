<#macro printFiles filesHolder containerId>
	<br/>
	<div class="node">Attached files</div>
	<table border="1" frame="box" rules="all" class="w-limited">
		<tr class="tablehead">
			<td>Id</td>
			<td>File</td>
		</tr>
		<#assign attachedFiles = filesHolder.ids![]>
		<#list attachedFiles as item>
			<tr>
				<td>${item}</td>
				<td><a href="details/${filesHolder.getPath(item).fileName}">${filesHolder.getPath(item).fileName}</a></td>
			</tr>
		</#list>
	</table>
</#macro>