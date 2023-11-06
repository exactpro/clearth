<#--
	External parameters: pathToStyles, pathToJS, stepsData, matricesData, reportsData, revision
-->
<#import "stepstable.ftl" as stepsTable>
<#import "matricestable.ftl" as matricesTable>
<#import "reportstable.ftl" as reportsTable>
<!DOCTYPE HTML>
<html>
	<head>
		<title>Information about ClearTH scheduler run</title>
		<meta content="text/html; charset=UTF-8" http-equiv="Content-Type" />
		<style>
			<@includeFile name=pathToStyles />
		</style>
	</head>
	<body>
		<div class="info-panel">
			<div>revision: #${revision}</div>
			<div>© 2011-2023 Exactpro Systems, LLC</div>
		</div>
		<h2>Information about ClearTH scheduler run</h2>
		<div id="tabs">
			<div class="tabsBlock">
				<div class="tab active">Steps</div><div class="tab">Matrices</div><div class="tab">Reports</div>
			</div>
			<div class="tabContent">
				<@stepsTable.printStepsTable stepsData=stepsData />
			</div>
			<div class="tabContent">
				<@matricesTable.printMatricesTable matricesData=matricesData />
			</div>
			<div class="tabContent">
				<#if reportsData??>
					<@reportsTable.printReportsTable reportsData=reportsData />
				<#else>
					<div>No reports available</div>
				</#if>
			</div>
		</div>
		<script type="text/javascript">
			<@includeFile name=pathToJS />
		</script>
	</body>
</html>
