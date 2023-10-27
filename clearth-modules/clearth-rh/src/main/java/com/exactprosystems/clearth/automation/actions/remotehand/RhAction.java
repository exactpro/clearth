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

package com.exactprosystems.clearth.automation.actions.remotehand;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.AttachedFilesResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhConnection;
import com.exactprosystems.clearth.connectivity.remotehand.RhUtils;
import com.exactprosystems.clearth.connectivity.remotehand.data.RhScriptResult;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RhAction extends Action implements TimeoutAwaiter
{
	public static final String REQUIRED_MAPPING_RECORD = "Is Required", 
			MAPPING_PARAMETER_NAME = "Parameter's Name";
	private long awaitedTimeout;
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String conName = handler.getRequiredString(MessageAction.CONNECTIONNAME);
		File script = handler.getRequiredFile("ScriptFile"),
				mappingFile = handler.getFile("MappingFile");
		
		if (mappingFile != null)
			checkMapping(mappingFile, handler);
		
		handler.check();
		
		RhClient client = getClient(conName, globalContext);
		int wait = (int)TimeUnit.SECONDS.convert(timeout, TimeUnit.MILLISECONDS);
		Stopwatch sw = Stopwatch.createAndStart();
		try
		{
			RhScriptResult scriptResult = client.executeScript(script.toPath(), inputParams, wait);
			return processScriptResult(client, scriptResult);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Error while executing script", e);
		}
		finally
		{
			awaitedTimeout = sw.stop();
		}
	}

	private Result processScriptResult(RhClient client, RhScriptResult scriptResult)
	{
		List<String> ids = scriptResult.getScreenshotIds();
		if (ids == null || ids.isEmpty())
		{
			Result result = new DefaultResult();
			processScriptResult(scriptResult, result);
			return result;
		}

		Path uploadStorageDir = Paths.get(ClearThCore.getInstance().getTempDirPath()).resolve("remote_hand");
		try
		{
			Files.createDirectories(uploadStorageDir);
		}
		catch (IOException e)
		{
			return DefaultResult.failed("Error while creating directories for screenshots", e);
		}

		AttachedFilesResult attachedFilesResult = new AttachedFilesResult();
		processScriptResult(scriptResult, attachedFilesResult);

		for (String id : ids)
		{
			Path file = uploadStorageDir.resolve(id);
			try (FileOutputStream outputStream = new FileOutputStream(file.toFile()))
			{
				byte[] bytes = client.downloadScreenshot(id);
				outputStream.write(bytes);
			}
			catch (Exception e)
			{
				String msg = String.format("Error while downloading screenshot '%s'", id);
				addComment(attachedFilesResult, msg);
				attachedFilesResult.setError(e);
				logger.error(msg, e);
				return attachedFilesResult;
			}
			attachedFilesResult.attach(createId(id), file);
		}
		return attachedFilesResult;
	}

	private String createId(String id)
	{
		if (!id.contains("_"))
			return id;
		return id.substring(id.indexOf('_') + 1);
	}

	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
	
	
	private void checkMapping(File mappingFile, InputParamsHandler handler)
	{
		try (CSVParser parser = CSVParser.parse(mappingFile, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader()))
		{
			for (CSVRecord record : parser)
			{
				if (!isTrue(record.get(REQUIRED_MAPPING_RECORD)))
					continue;
				
				String name = record.get(MAPPING_PARAMETER_NAME);
				if (!StringUtils.isEmpty(name))
					handler.getRequiredString(name);
			}
		}
		catch (IOException e)
		{
			throw ResultException.failed("Error while reading mapping file", e);
		}
	}
	
	private boolean isTrue(String required)
	{
		if (required == null)
			return false;
		
		return InputParamsUtils.YES.contains(required.toLowerCase()); 
	}
	
	protected RhClient getClient(String name, GlobalContext gc) throws ResultException
	{	
		RhClient client = gc.getCloseableContext(name);
		if (client != null)
			return client;
		
		RhConnection con = getConnection(name);
		try
		{
			client = RhUtils.createRhConnection(con.getSettings().getUrl());
		}
		catch (Exception e)
		{
			throw new ResultException("Error while establishing RemoteHand connection '" + con.getName() + "'", e);
		}
		gc.setCloseableContext(name, client);
		return client;
	}
	
	private RhConnection getConnection(String name)
	{
		RhConnection result = (RhConnection)ClearThCore.connectionStorage().getConnection(name, RhConnection.TYPE_RH);
		if (result == null)
			throw ResultException.failed("RemoteHand connection '" + name + "' doesn't exist");
		return result;
	}
	
	protected void processScriptResult(RhScriptResult scriptResult, Result result)
	{
		LineBuilder lb = new LineBuilder();
		for (String line : scriptResult.getActionResults())
		{
			lb.append(line);
			
			Pair<String, String> keyValue = KeyValueUtils.parseKeyValueString(line);
			if (!StringUtils.isEmpty(keyValue.getSecond()))
				addOutputParam(keyValue.getFirst(), keyValue.getSecond());
		}
		String error = scriptResult.getErrorMessage();
		if (!StringUtils.isEmpty(error))
		{
			if (lb.length() > 0)
				lb.eol();
			lb.add("Error: ").append(error);
		}

		addComment(result, lb.toString());

		if (scriptResult.isFailed())
			result.setSuccess(false);
	}

	private void addComment(Result result, String comment)
	{
		if (StringUtils.isEmpty(comment))
			return;

		String oldComment = result.getComment();
		if (!StringUtils.isEmpty(oldComment))
			comment = oldComment + System.lineSeparator() + comment;

		result.setComment(comment);
	}
}