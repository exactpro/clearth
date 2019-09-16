/* Copyright */

/* Usage */

/*
 * IE8+
 */
 
/* Notes
 * Table width will be reset
 */

(function($) {
	$.fn.responsiveTables = function(method)
	{
		var respTblList; // Need test in PrimeFaces (few pages with respTbl)
		
		var methods = {
			init: function()
			{
				$(window).on("resize", function()
				{
					$(this).responsiveTables("resizeAllTable");
				});
				return this;
			},
			
			resizeAllTable: function()
			{
				if (respTblList == null)
					respTblList = $(".responsiveTable");
				
				respTblList.each(function(tblIndex, curTbl)
				{
					var tblHead = $(curTbl).find("thead");
					var tblContainer = $(curTbl).parent();
					if (tblHead.width() > tblContainer.width())
					{
						if (!$(curTbl).hasClass("respTblPrepared"))
							$(curTbl).responsiveTables("prepareTable");
						
						//if ($(this).responsiveTables("getHiddenColumnWidth")) // if getHiddenColumnWidth() != null then need show column with buttons
							$(curTbl).responsiveTables("setExpandButtonsVisible", "show");
						
						//while ((tblContainer.width() - $(this).responsiveTables("getMinTableWidth")) > tblContainer.width())
						while (tblHead.width() > tblContainer.width())
							$(curTbl).responsiveTables("setLastColumnVisible", "hide");
					}
					else
					{
						//FIX if hidden column == NULL
						
						//console.log((tblContainer.width() - $(this).responsiveTables("getMinTableWidth")) + " >= " + $(this).responsiveTables("getHiddenColumnWidth"));
						
						//if ((tblContainer.width() - $(this).responsiveTables("getMinTableWidth")) >= $(this).responsiveTables("getHiddenColumnWidth"))
						while ($(this).responsiveTables("getHiddenColumnWidth")
								&& ((tblContainer.width() - $(this).responsiveTables("getMinTableWidth")) >= $(this).responsiveTables("getHiddenColumnWidth")))
						{
							$(curTbl).responsiveTables("setLastColumnVisible", "show");
							//console.log("Free space: " + (tblContainer.width() - $(this).responsiveTables("getMinTableWidth")));
						}
						
						if (!$(this).responsiveTables("getHiddenColumnWidth"))
						{
							$(curTbl).responsiveTables("setExpandButtonsVisible", "hide");
						}
					}
					
					console.log(tblContainer.width() - $(this).responsiveTables("getMinTableWidth"));
					//console.log($(this).responsiveTables("getHiddenColumnWidth"));
				});
			},
			
			prepareTable: function()
			{
				this.find("thead").find("th").each(function(colIndex, curCol)
				{
					$(this).addClass("respTblShown");
				});
				this.find("tr").each(function(rowIndex, curRow)
				{
					if (rowIndex == 0)
					{
						$(this).find("th").each(function(colIndex, curCol)
						{
							$(this).addClass("respTblShown");
						});
						$(this).append("<th class='respTblExpandBtnColumn'></th>");
					}
					else
						$(this).append("<td><input type='button' class='respTblExpandBtn' value='>>>'></td>");
				});

				$(this).on("click", ".respTblExpandBtn", function()
				{
					var currentTr = $(this).closest("tr");
					currentTr.after("<tr class='respTblDetails'><td colspan='" + currentTr.find("td").length + "'>" + currentTr.responsiveTables("getHiddenDetails") + "</td></tr>");
					currentTr.addClass("respTblDetailed");
					$(this).removeClass("respTblExpandBtn");
					$(this).addClass("respTblCollapseBtn");
				});

				$(this).on("click", ".respTblCollapseBtn", function()
				{
					var currentTr = $(this).closest("tr");
					currentTr.removeClass("respTblDetailed");
					currentTr.next(".respTblDetails").remove();
					$(this).removeClass("respTblCollapseBtn");
					$(this).addClass("respTblExpandBtn");
				});
				
				this.addClass("respTblPrepared");
			},
			
			setExpandButtonsVisible: function(visible)
			{
				var columnIndex;
				this.find("tr").each(function(rowIndex, curRow)
				{
					var columns = $(curRow).children();
					if (rowIndex == 0)
					{
						var col = $(curRow).find(".respTblExpandBtnColumn").first();
						columnIndex = $(columns).index(col);
						if (columnIndex == -1)
							return false;
					}
					if (visible == "show")
						$($(columns).get(columnIndex)).css("display", "");
					else
						$($(columns).get(columnIndex)).css("display", "none");
				});
			},
			
			setLastColumnVisible: function(visible)
			{
				var colIndex;
				var isShow = visible == "show";
				this.find("tr").each(function(rowIndex, curRow)
				{
					var columns;
					if (rowIndex == 0)
					{
						columns = $(curRow).find("th");
						var col;
						
						if (isShow)
							col = $(curRow).find(".respTblHidden").first();
						else
							col = $(curRow).find(".respTblShown").last();
						
						if ($(col).index() == -1)
							return false;
						
						colIndex = $(columns).index(col);
						
						if (isShow)
							$($(columns).get(colIndex))
									.removeClass("respTblHidden")
									.addClass("respTblShown")
									.css("display", "");
						else
							$($(columns).get(colIndex))
									.removeClass("respTblShown")
									.addClass("respTblHidden")
									.css("display", "none");
					}
					else
					{
						columns = $(curRow).find("td");
						if (isShow)
							$($(columns).get(colIndex))
									.css("display", "");
						else
							$($(columns).get(colIndex))
									.css("display", "none");
					}
				});
			},
			
			getHiddenColumnWidth: function()
			{
				var width = $($(this).find(".respTblHidden").first()).outerWidth(true);
				return width;
			},
			
			getMinTableWidth: function()
			{
				var curTblStyle = $(this).css("display");
				$(this).css("display", "inline-table");
				$(this).css("width", "auto");
				var width = $(this).width();
				$(this).css("display", curTblStyle);
				$(this).css("width", "100%");
				return width;
			},
			
			getHiddenDetails: function()
			{
				var header = $(this).closest("table").find("th");
				
				var details = "<div class='respTblDetails'>";
				$(this).find("td").each(function(index, curCol)
				{
					if ($(curCol).css("display") == "none")
					{
						details += "<nobr><strong>" + $($(header).get(index)).text() + ":</strong> ";
						details += $(curCol).text() + "</nobr> ";
					}
				});
				details += "</div>";
				return details;
			}
		};
		
		if (methods[method])
			// if this method exists, then all params
			// except first (method name) are passed as args of the method
			// 'this' will also be passed
			return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
		else if (typeof method === 'object' || !method)
			// if first arg is a 'object' or nothing then go to 'init'
			return methods.init.apply(this, arguments);
		else
			$.error('Method "' +  method + '" not found');
	};
})(jQuery);
