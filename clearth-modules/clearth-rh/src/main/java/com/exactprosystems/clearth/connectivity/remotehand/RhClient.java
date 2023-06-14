/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.connectivity.remotehand;

import java.io.File;
import java.io.IOException;
import java.lang.AutoCloseable;
import java.nio.file.Path;
import java.util.Map;

import com.exactprosystems.clearth.connectivity.remotehand.data.RhResponseCode;
import com.exactprosystems.clearth.connectivity.remotehand.data.RhScriptResult;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.Utils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.exactprosystems.clearth.connectivity.remotehand.data.RhResponseCode.TOOL_BUSY;

/**
 * Class to cover interaction with RemoteHand. Way to transfer data must be implemented in children
 */
public abstract class RhClient implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(RhClient.class);
	
	public static final int CODE_SUCCESS = 200,
			CODE_ERROR = 500;
	protected final String LOGON_DELIMETER = ";";
	
	protected final RhScriptProcessor processor;
	protected final RhScriptCompiler compiler;
	protected final ObjectMapper jsonSerializer;
	
	protected String sessionId;
	protected String usedBrowser;
	
	public RhClient(RhScriptProcessor processor)
	{
		this.processor = processor;
		compiler = createScriptCompiler();
		jsonSerializer = createJsonSerializer();
	}
	
	public RhClient()
	{
		this(new DefaultRhScriptProcessor());
	}
	
	@Override
	public void close() throws Exception
	{
		try
		{
			logout();
		}
		finally
		{
			disposeResources();
		}
	}
	
	protected abstract RhResponse sendLogon() throws IOException;
	protected abstract RhResponse sendScript(String script) throws IOException;
	protected abstract RhResponse queryStatus() throws IOException;
	protected abstract RhResponse downloadFile(String type, String id) throws IOException;
	protected abstract RhResponse sendLogout() throws IOException;
	protected abstract void disposeResources() throws Exception;
	public abstract RhResponse sendFile(File f, String path) throws IOException;
	
	
	public void logon() throws IOException, RhException
	{
		RhResponse response = sendLogon();
		if (isNotSuccess(response))
			throw new RhException("Could not login to RemoteHand: %s", response.getDataString());
		
		String responseMsg = response.getDataString();
		sessionId = null;
		usedBrowser = null;
		if (responseMsg.contains(LOGON_DELIMETER))
		{
			String[] s = responseMsg.split(LOGON_DELIMETER);
			sessionId = s[0].replace("sessionId=", "");
			if (s.length > 1)
				usedBrowser = s[1].replace("browser=", "");
		}
		else 
			sessionId = responseMsg;
	}
	
	public void logout() throws IOException
	{
		if (sessionId == null)
			return;
		
		RhResponse response = sendLogout();
		if (isNotSuccess(response))
			logger.warn("Logout from RemoteHand failed: {}", response.getDataString());
		
		sessionId = null;
		usedBrowser = null;
	}
	
	public String send(String message, boolean needToReconnect) throws IOException, RhException
	{
		if (needToReconnect)
		{
			logout();
			logon();
			logger.debug("Successfully reconnected to RemoteHand, new session ID: {}", sessionId);
		}
		
		RhResponse response = sendScript(message);
		String result = response.getDataString();
		logger.trace("Message sent. Received response: '{}'", result);
		
		if (isNotSuccess(response))
		{
			if (!needToReconnect && response.getCode() == 404
					&& result.equalsIgnoreCase("<h1>404 Not Found</h1>No context found for request"))
			{
				logger.warn("RemoteHand session '{}' was closed, will re-login to RemoteHand and send message via new session",
						sessionId);
				return send(message, true);
			}
			else
			{
				throw new RhException("RemoteHand response contains error: %s", result);
			}
		}
		return result;
	}
	
	public String send(String message) throws IOException, RhException
	{
		return send(message, false);
	}
	
	
	public RhScriptResult get() throws RhException, IOException
	{
		RhResponse response = queryStatus();
		String rawResult = response.getDataString();
		logger.trace("Getting resource. Received response: '{}'", rawResult);
		
		if (isNotSuccess(response))
			throw new RhException("Error while executing script: '%s'", rawResult);
		
		try
		{
			return jsonSerializer.readValue(rawResult, RhScriptResult.class);
		}
		catch (IOException e)
		{
			throw new RhException(e, "Unable to parse RemoteHand response. Probably you are using incompatible version of RemoteHand. Raw response: [%s].", rawResult);
		}
	}
	
	public RhScriptResult waitAndGet(int seconds) throws RhException, IOException
	{
		Stopwatch sw = Stopwatch.createAndStart(seconds * 1000);
		do
		{
			RhScriptResult response = get();
			if (!TOOL_BUSY.equals(RhResponseCode.byCode(response.getCode())))
				return response;
			
			if (sw.isExpired())
				throw new RhException("Timeout after " + seconds + " second(s) of waiting for result");
			
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				
				logger.debug("Script execution for session '{}' has been interrupted", sessionId, e);
				RhScriptResult scriptResult = new RhScriptResult();
				scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
				scriptResult.setErrorMessage("Script execution has been interrupted");
				return scriptResult;
			}
		}
		while (true);
	}
	
	public String processScript(String script, Path templatesDir) throws IOException, RhException
	{
		return processor.process(script, templatesDir);
	}
	
	public String compileScript(String script, Map<String, String> arguments) throws RhException
	{
		return compiler.compile(script, arguments);
	}
	
	public RhScriptResult executeScript(Path scriptFile, Map<String, String> arguments, int waitInSeconds, Path templatesDirectory)
			throws RhException, IOException
	{
		String script = RhUtils.getScriptFromFile(scriptFile);
		return executeScript(script, arguments, waitInSeconds, templatesDirectory);
	}
	
	public RhScriptResult executeScript(Path scriptFile, Map<String, String> arguments, int waitInSeconds) throws RhException, IOException
	{
		return executeScript(scriptFile, arguments, waitInSeconds, null);
	}
	
	public RhScriptResult executeScript(String script, Map<String, String> arguments, int waitInSeconds, Path templatesDirectory)
			throws RhException, IOException
	{
		String processedScript = processScript(script, templatesDirectory);
		String compiledScript = compileScript(processedScript, arguments);
		logger.debug("Compiled script:{}{}", Utils.EOL, compiledScript);
		
		String response = send(compiledScript);
		logger.debug("Script has been sent. Response:{}{}", Utils.EOL, response);
		return waitAndGet(waitInSeconds);
	}
	
	public RhScriptResult executeScriptFromString(String script, Map<String, String> arguments, int waitInSeconds) throws RhException, IOException
	{
		return executeScript(script, arguments, waitInSeconds, null);
	}
	
	
	public byte[] downloadScreenshot(String screenshotId) throws RhException, IOException
	{
		return downloadFileFromRh("screenshot", screenshotId);
	}
	
	public byte[] downloadDownloadedFile(String filePath) throws RhException, IOException
	{
		return downloadFileFromRh("downloaded", filePath);
	}
	
	
	public String getSessionId()
	{
		return sessionId;
	}
	
	public String getUsedBrowser()
	{
		return usedBrowser;
	}
	
	
	protected RhScriptCompiler createScriptCompiler()
	{
		return new RhScriptCompiler();
	}
	
	protected ObjectMapper createJsonSerializer()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper;
	}
	
	protected byte[] downloadFileFromRh(String type, String id) throws RhException, IOException
	{
		RhResponse response = downloadFile(type, id);
		if (isNotSuccess(response))
		{
			String message = response.getDataString();
			throw new RhException("Unable to download " + type + ": " + message);
		}
		
		return response.getData();
	}
	
	
	private boolean isNotSuccess(RhResponse response)
	{
		return response.getCode() != CODE_SUCCESS;
	}
}
