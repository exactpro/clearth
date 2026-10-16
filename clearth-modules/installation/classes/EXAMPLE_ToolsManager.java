package

import com.exactprosystems.clearth.tools.ToolInfo;
import com.exactprosystems.clearth.tools.ToolsManager;

import java.io.File;
import java.util.List;

public class EXAMPLE_ToolsManager extends ToolsManager
{
	public EXAMPLE_ToolsManager(File userSettingsDir)
	{
		super(userSettingsDir);
	}

	@Override
	protected List<ToolInfo> createToolsList()
	{
		List<ToolInfo> result = super.createToolsList();
		// add new tool info here like this: result.add(new ToolInfo(TOOL_INDEX, "TOOL_NAME"));
		return result;
	}
}