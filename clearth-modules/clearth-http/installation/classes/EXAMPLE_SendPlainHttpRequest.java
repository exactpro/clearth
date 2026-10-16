package

import com.exactprosystems.clearth.automation.actions.http.SendPlainHttpRequest;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;

public class EXAMPLE_SendPlainHttpRequest extends SendPlainHttpRequest
{

	@Override
	protected String getDefaultParentUrl()
	{
		return null;
	}

	@Override
	protected String getDefaultUser()
	{
		return null;
	}

	@Override
	protected String getDefaultPassword()
	{
		return null;
	}

	@Override
	protected HttpDictionary getHttpDictionary()
	{
		return null;
	}
}
