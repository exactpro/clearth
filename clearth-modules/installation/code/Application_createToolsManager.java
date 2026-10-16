	@Override
	protected ToolsManager createToolsManager()
	{
		return new EXAMPLE_ToolsManager(new File(this.getUserSettingsPath()));
	}