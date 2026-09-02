		try
		{
			Woodpecker.init(new WoodpeckerObjectsFactory());
		}
		catch (WoodpeckerException e)
		{
			throw new ClearThException("Error while initializing Woodpecker", e);
		}