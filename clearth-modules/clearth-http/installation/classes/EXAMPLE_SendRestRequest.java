package

import com.exactprosystems.clearth.automation.actions.http.SendRestRequest;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.messages.MessageBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.Set;

public class EXAMPLE_SendRestRequest extends SendRestRequest
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

	@Override
	protected ClearThHttpClient getHttpClient() throws ResultException
	{
		return null;
	}

	@Override
	protected void closeResourcesAfterSending(ClearThHttpClient clearThHttpClient,
	                                          CloseableHttpResponse closeableHttpResponse)
	{

	}

	@Override
	public MessageBuilder getMessageBuilder(Set set, Set set1)
	{
		return null;
	}

	@Override
	protected String getDefaultCodecName()
	{
		return null;
	}
}
