function saveScrollPos(cookieName, elementId)
{
	jQuery.cookie(cookieName, jQuery("#"+elementId+" .ui-datatable-scrollable-body").scrollTop());
}

function restoreScrollPos(cookieName, elementId)
{
	scrollPos = jQuery.cookie(cookieName);
	if (scrollPos)
		jQuery("#"+elementId+" .ui-datatable-scrollable-body").scrollTop(scrollPos);
}

function saveColumnsWidths(formAndTableId)
{
	var table = document.getElementById(formAndTableId);
	var resizableColumns = $(table).find(".ui-datatable-scrollable-header-box .ui-resizable-column");
	var tableWidth = $(table).find("table").width();
	var factor;
	var widthsArray = $(resizableColumns).map(function(){
		factor = $(this).outerWidth(true)/tableWidth;
        return factor*100+"%";
    }).get();
	jQuery.cookie(formAndTableId+"_columnsWidths", JSON.stringify(widthsArray), { expires: 10*365 });
}

function restoreColumnsWidths(formAndTableId)
{
	var cookie = jQuery.cookie(formAndTableId+"_columnsWidths")
	if (!cookie)
		return;
	var widthsArray = JSON.parse(cookie);
	if (!widthsArray)
		return;
	var resizableColumns = $(document.getElementById(formAndTableId)).find(".ui-datatable-scrollable-header-box .ui-resizable-column");
	if (widthsArray.length != resizableColumns.length)
		return;
    $(resizableColumns).width(function(i,value){
		return widthsArray[i];
    });
    // For scrollable body
	resizableColumns = $(document.getElementById(formAndTableId)).find(".ui-datatable-scrollable-theadclone .ui-resizable-column");
	$(resizableColumns).width(function(i,value){
		return widthsArray[i];
    });
}

function resetColumnsWidths(formAndTableId)
{
	jQuery.cookie(formAndTableId+"_columnsWidths", null);
}
