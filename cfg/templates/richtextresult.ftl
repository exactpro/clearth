<#macro printDataTable table elementId >
	<div>
		<span class="node switch" onclick="showhide(this, '${elementId}');">${table.title}</span>
			<div id="${elementId}" class="container">
				<#if table.comment?? >
					<span>${table.comment}</span>
				</#if>
				<table border="1" frame="box" rules="all">
					<tr class="tablehead">
						<#list table.headers as header>
							<td>${header}</td>
						</#list>
					</tr>
						<#list table.records as record>
							<tr>
								<#list record as value>
									<td>${value!""}</td>
								</#list>
							</tr>
						</#list>
				</table>
			</div>
	</div>
</#macro>

<#macro printTextLabel label elementId >
	<div id="${elementId}">${label.text}</div>
</#macro>

<#macro printAdditionalInfo result resultId>
	<#escape x as x?html>
		<#list result.elements>
			<#if result.title??>
				<span class="node switch" onclick="showhide(this, '${resultId}');">${result.title}</span>
				<div id="${resultId}" class="container">
			</#if>
			<#items as element>
				<#assign elementId = resultId + element?counter>
				<#if instanceOf(element, DataTable)>
					<@printDataTable table = element elementId = elementId />
				<#elseif instanceOf(element, TextLabel)>
					<@printTextLabel label = element elementId = elementId />
				</#if>
			</#items>
			<#if result.title??>
				</div>
			</#if>
		</#list>
	</#escape>
</#macro>
