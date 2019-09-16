/* Copyright */

/* Usage */

/*
 * 1. Change Column priority
 * 2. Add column with <p:rowToggler />
 * 3. Add <p:rowExpansion>...</p:rowExpansion>
 * 4. Add class "responsiveTable" to <p:dataTable>
 */

/*
 * Use with PrimeFaces >= 5.2
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
				$(this).responsiveTables("resizeAllTable");
				return this;
			},
			
			resizeAllTable: function()
			{
				if (respTblList == null)
					respTblList = $(".responsiveTable");
				
				respTblList.each(function(tblIndex, curTbl)
				{
					var needShowToggler = false;
					$(curTbl).find("th").each(function(tblIndex, curTh)
					{
						if ($(curTh).css("display") == "none")
							{
								needShowToggler = true;
								return false;
							}
					});
					$(curTbl).responsiveTables("setExpandButtonsVisible", needShowToggler);
				});
			},
			
			setExpandButtonsVisible: function(visible)
			{
				var columnIndex;
				this.find("tr").each(function(rowIndex, curRow)
				{
					if (visible == true)
						$(curRow).find(".ui-row-toggler").css("display", "table-cell");
					else
						$(curRow).find(".ui-row-toggler").css("display", "none");
				});
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
