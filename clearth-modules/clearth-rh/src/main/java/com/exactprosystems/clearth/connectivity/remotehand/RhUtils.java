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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.remotehand.http.HttpRhClient;
import com.exactprosystems.clearth.connectivity.remotehand.tcp.TcpRhAcceptor;
import com.exactprosystems.clearth.connectivity.remotehand.tcp.TcpRhConnectionHandler;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.connectivity.remotehand.tcp.TcpRhClient;

public class RhUtils
{
	private static final Logger logger = LoggerFactory.getLogger(RhUtils.class);
	
	private static final String INCLUDE = "#include",
			FILE_PARAM = "file", SHUTDOWN_SCRIPT_HEADER = "// SHUTDOWN SCRIPT";
	public static final String LINE_SEPARATOR = "&#13";

	private static final int DICT_RESPONSE_TIMEOUT = 120, // 2 min
			SHUTDOWN_SCRIPT_RESPONSE_TIMEOUT = 120; // 2 min
	
	public static RhClient createRhConnection(String host) throws IOException, RhException
	{
		return createRhConnection(host, null, null);
	}
	
	public static RhClient createRhConnection(String host, Path pathToDictionary) throws IOException, RhException
	{
		return createRhConnection(host, pathToDictionary, null);
	}
	
	public static RhClient createRhConnection(String host, Path pathToDictionary, Path pathToShutdownScript)
			throws IOException, RhException
	{
		logger.trace("Establishing connection with RemoteHand at '{}'...", host);
		RhClient client = new HttpRhClient(HttpClientBuilder.create().build(), host);
		try
		{
			loginToRh(client, pathToDictionary, pathToShutdownScript);
			return client;
		}
		catch (Exception e)
		{
			Utils.closeResource(client);
			throw e;
		}
	}
	
	public static RhClient createTcpRhConnection(TcpRhAcceptor acceptor, Path pathToDictionary) throws IOException, RhException
	{
		return createTcpRhConnection(acceptor, pathToDictionary, null);
	}
	
	public static RhClient createTcpRhConnection(TcpRhAcceptor acceptor, Path pathToDictionary, Path pathToShutdownScript)
			throws IOException, RhException
	{
		TcpRhConnectionHandler handler = acceptor.getConnectionHandler();
		if (handler == null || !handler.isActive())
			throw new RhException("RemoteHand is not connected");
		
		RhClient client = new TcpRhClient(acceptor);
		try
		{
			loginToRh(client, pathToDictionary, pathToShutdownScript);
			return client;
		}
		catch (Exception e)
		{
			Utils.closeResource(client);
			throw e;
		}
	}
	
	public static void sendDictionary(RhClient rhClient, Path dictionaryPath) throws RhException, IOException
	{
		logger.trace("Sending dictionary to RemoteHand...");
		sendScript(rhClient, getScriptFromFile(dictionaryPath), DICT_RESPONSE_TIMEOUT);
		logger.trace("Dictionary '{}' has been successfully sent to RemoteHand", dictionaryPath);
	}
	
	public static void sendShutdownScript(RhClient rhClient, Path shutdownScriptPath) throws RhException
	{
		logger.trace("Sending shutdown script to RemoteHand...");
		sendScript(rhClient, prepareShutdownScript(shutdownScriptPath, SHUTDOWN_SCRIPT_HEADER),
				SHUTDOWN_SCRIPT_RESPONSE_TIMEOUT);
		logger.trace("Shutdown script '{}' has been successfully sent to RemoteHand", shutdownScriptPath);
	}
	
	
	private static void sendScript(RhClient rhClient, String script, int responseTimeout) throws RhException
	{
		try
		{
			rhClient.send(script);
			rhClient.waitAndGet(responseTimeout);
		}
		catch (IOException e)
		{
			String msg = "Error occurred while sending script to RemoteHand";
			logger.error(msg, e);
			throw new RhException(msg, e);
		}
	}
	
	private static String prepareShutdownScript(Path shutdownScriptPath, String header) throws RhException
	{
		StringBuilder shutdownScript = new StringBuilder();
		try
		{
			shutdownScript.append(header).append(LINE_SEPARATOR).append(getScriptFromFile(shutdownScriptPath));
		}
		catch (IOException e)
		{
			String msg = "Error occurred while preparing shutdown script";
			logger.error(msg, e);
			throw new RhException(msg, e);
		}
		return shutdownScript.toString();
	}
	
	public static String getScriptFromFile(Path file) throws IOException, RhException
	{
		StringBuilder sb = new StringBuilder();
		loadScriptFromFile(sb, file, Collections.emptyMap());
		return sb.toString();
	}
	
	public static void loadScriptFromFile(StringBuilder scriptBuilder, Path file, Map<String, String> parameters)
			throws IOException, RhException
	{
		loadScriptFromFile(scriptBuilder, file, parameters, Collections.emptySet());
	}
	
	
	private static void loginToRh(RhClient client, Path pathToDictionary, Path pathToShutdownScript)
			throws IOException, RhException
	{
		client.logon();
		logger.trace("Connection with RemoteHand established, session '{}'", client.getSessionId());
		
		if (pathToDictionary != null)
			sendDictionary(client, pathToDictionary);
		if (pathToShutdownScript != null)
			sendShutdownScript(client, pathToShutdownScript);
	}
	
	private static void loadScriptFromFile(StringBuilder scriptBuilder, Path file, Map<String, String> parameters,
			Set<Path> appliedFiles) throws IOException, RhException
	{
		try 
		(
			InputStream input = new BOMInputStream(new FileInputStream(file.toFile()));
			BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
		)
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.startsWith(INCLUDE))
					includeFile(scriptBuilder, line, file.getParent(), new HashSet<>(appliedFiles));
				else
				{
					if (line.contains(RhScriptCompiler.PARAMETER_MARK))
						line = addIncludeParameters(line, parameters);
					scriptBuilder.append(line).append(LINE_SEPARATOR);
				}
			}
		}
	}

	private static void includeFile(StringBuilder scriptBuilder, String includeLine, Path baseDir,
			Set<Path> appliedFiles) throws IOException, RhException
	{
		Map<String, String> parameters = parseParameters(includeLine);
		String fileName = parameters.remove(FILE_PARAM);
		if (fileName == null)
			throw inclusionError("Please specify file name by parameter " + FILE_PARAM);
		
		Path file = baseDir.resolve(fileName);
		if (!Files.isRegularFile(file))
			throw inclusionError("File '" + fileName + "' doesn't exist");
		if (!appliedFiles.add(file))
			throw inclusionError("File '" + fileName + "' is already included");
		loadScriptFromFile(scriptBuilder, file, parameters, appliedFiles);
	}
	
	static Map<String, String> parseParameters(String includeLine)
	{
		if (StringUtils.isBlank(includeLine))
			return Collections.emptyMap();
		
		includeLine = includeLine.replace(INCLUDE, "");
		
		String[] keyValuePairs = includeLine.split(",\\s*");
		Map<String, String> parameters = new LinkedHashMap<>(keyValuePairs.length);
		for (String keyValuePair : keyValuePairs)
		{
			int eqIndex = keyValuePair.indexOf('=');
			if (eqIndex != -1)
			{
				parameters.put(keyValuePair.substring(0, eqIndex).trim().replace("'", ""),
						StringUtils.substring(keyValuePair, eqIndex + 1).trim().replace("'", ""));
			}
			else
				parameters.put(keyValuePair, "");
		}
		return parameters;
	}
	
	public static String addIncludeParameters(String line, Map<String, String> parameters)
	{
		for (Map.Entry<String, String> e : parameters.entrySet())
		{
			String key = wrapByMarks(e.getKey());
			if (line.contains(key))
				line = line.replace(key, e.getValue());
		}
		return line;
	}
	
	private static String wrapByMarks(String s)
	{
		return RhScriptCompiler.PARAMETER_MARK + s + RhScriptCompiler.PARAMETER_MARK;
	}
	
	private static RhException inclusionError(String msg)
	{
		return new RhException(INCLUDE+" statement is invalid. "+msg); 
	}
}