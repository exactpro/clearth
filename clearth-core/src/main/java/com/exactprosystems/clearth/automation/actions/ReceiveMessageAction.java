/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.messages.*;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import static com.exactprosystems.clearth.utils.inputparams.InputParamsUtils.getBooleanOrDefault;
import static java.util.Collections.unmodifiableSet;

public abstract class ReceiveMessageAction<T extends ClearThMessage<T>> extends MessageAction<T>
		implements TimeoutAwaiter
{
	protected static final String PARAM_KEYFIELDS = "KeyFields",
			PARAM_RG_KEYFIELDS = "RGKeyFields",
			PARAM_OUTPUTPARAMS = "OutputParams",
			IGNORE_EXTRA_REPEATING_GROUPS = "IgnoreExtraRepeatingGroups",
			LOG_SUBMESSAGES_OUTPUT = "LogSubMsgsOutput",
			SAVE_OUTPUT_IF_FAILED = "SaveOutputIfFailed",
			REMOVE_IF_FAILED = "RemoveIfFailed",
			REMOVE_FROM_COLLECTOR = "RemoveFromCollector",
			REMOVE_FILE = "RemoveFile",
			REVERSE_ORDER = "ReverseOrder";
	
	protected static final Set<String> SERVICE_PARAMETERS = unmodifiableSet(new HashSet<String>()
	{{
		add(MessageAction.CONNECTIONNAME);
		add(ClearThMessage.MSGTYPE);
		add(MessageAction.REPEATINGGROUPS);
		add(ClearThMessage.MSGCOUNT);
		add(ClearThMessage.SUBMSGTYPE);
		add(ClearThMessage.SUBMSGSOURCE);
		add(PARAM_KEYFIELDS);
		add(PARAM_RG_KEYFIELDS);
		add(IGNORE_EXTRA_REPEATING_GROUPS);
		add(PARAM_OUTPUTPARAMS);
		add(MessageAction.FILENAME);
		add(MessageAction.CODEC);
		add(LOG_SUBMESSAGES_OUTPUT);
		add(SAVE_OUTPUT_IF_FAILED);
		add(REMOVE_IF_FAILED);
		add(REMOVE_FROM_COLLECTOR);
		add(REMOVE_FILE);
		add(REVERSE_ORDER);
	}});
	
	protected long awaitedTimeout;
	
	protected abstract void afterSearch(GlobalContext globalContext, List<T> messages) throws ResultException;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		T expectedMessage = buildMessage(matrixContext);
		MessageSource messageSource = getMessageSource(globalContext);
		
		AllKeyFieldsData allKeys = getAllKeys(stepContext, matrixContext, globalContext, expectedMessage);
		checkKeys(allKeys);
		
		List<T> foundMessages = findMessages(allKeys.getKeys(), allKeys.getKeysInRgs(), messageSource, globalContext);
		MessageComparator<T> comparator = getMessageComparator();
		Result result = compareMessages(comparator, expectedMessage, foundMessages, allKeys.getRgKeyFieldNames());
		
		if (StringUtils.isEmpty(result.getComment()))
			result.setComment("Message found by type "+createKeyFieldsComment(allKeys.getKeys(), allKeys.getKeysInRgs()));
		
		if (result.isSuccess() || isRemoveIfFailed())
			removeMessages(messageSource, foundMessages);
		if (result.isSuccess() || isSaveOutputIfFailed())
			saveOutputParams(foundMessages, comparator);
		
		return result;
	}

	@Override
	public Set<String> getServiceParameters()
	{
		return SERVICE_PARAMETERS;
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
	
	
	//*** Getters for objects to work with messages ***
	
	protected MessageSource getMessageSource(GlobalContext globalContext) throws FailoverException
	{
		if (!getInputParam(CONNECTIONNAME, "").isEmpty())
			return getCollectorMessageSource(globalContext);
		if (!getInputParam(FILENAME, "").isEmpty())
			return getFileMessageSource(globalContext);

		MessageSource result = getCustomMessageSource(globalContext);
		if (result == null)
			throw ResultException.failed("No '"+CONNECTIONNAME+"' and '"+FILENAME+"' parameters specified");
		return result;
	}
	
	protected ConnectionFinder getConnectionFinder()
	{
		return new ConnectionFinder();
	}
	
	protected CollectorMessageSource getCollectorMessageSource(GlobalContext globalContext) throws FailoverException
	{
		try
		{
			return new CollectorMessageSource(getConnectionFinder().findCollector(inputParams), !isReverseOrder());
		}
		catch (ConnectivityException e)
		{
			throw new FailoverException(e.getMessage(), FailoverReason.CONNECTION_ERROR);
		}
	}
	
	protected FileMessageSource getFileMessageSource(GlobalContext globalContext)
	{
		File file = InputParamsUtils.getRequiredFile(inputParams, FILENAME);
		return new FileMessageSource(file, getCodec(globalContext));
	}
	
	protected MessageSource getCustomMessageSource(GlobalContext globalContext)
	{
		return null;
	}
	
	protected AllKeyFieldsData getAllKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, T message)
	{
		KeyFieldsData keys = getKeys(stepContext, matrixContext, globalContext, message);
		RgKeyFieldNames rgKeyFieldNames = getRgKeyFieldNames(stepContext, matrixContext, globalContext);
		List<KeyFieldsData> keysInRgs = getKeysInRgs(stepContext, matrixContext, globalContext, message, rgKeyFieldNames);
		return new AllKeyFieldsData(keys, rgKeyFieldNames, keysInRgs);
	}
	
	protected void fillKeyFields(KeyFieldsData keyFields, T message, Set<String> keyFieldNames)
	{
		for (String n : keyFieldNames)
		{
			String value = message.getField(n);
			if (!StringUtils.isEmpty(value))
				keyFields.addKey(new MessageKeyField(n, value));
		}
	}
	
	protected KeyFieldsData getKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, T message)
	{
		KeyFieldsData result = new KeyFieldsData();
		result.setMsgType(getMessageType());
		
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		Set<String> keyNames = handler.getSet(PARAM_KEYFIELDS, ",");
		handler.check();
		
		fillKeyFields(result, message, keyNames);
		return result;
	}
	
	protected RgKeyFieldNames getRgKeyFieldNames(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		//Expected format: RGType1=Field1,Field2;RGType2=Field3,Field4
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		Set<String> rgKeyNames = handler.getSet(PARAM_RG_KEYFIELDS);
		if (rgKeyNames.isEmpty())
			return null;
		
		RgKeyFieldNames result = new RgKeyFieldNames();
		for (String k : rgKeyNames)
		{
			Pair<String, String> rgKeys = KeyValueUtils.parseKeyValueString(k);
			String type = rgKeys.getFirst();
			if (type != null)
				type = type.trim();
			if (StringUtils.isEmpty(type))
				continue;
			
			Set<String> keys = new LinkedHashSet<>();
			for (String oneKey : rgKeys.getSecond().split(","))
			{
				oneKey = oneKey.trim();
				if (!oneKey.isEmpty())
					keys.add(oneKey);
			}
			
			if (!keys.isEmpty())
				result.addRgKeyFields(type, keys);
		}
		return result;
	}
	
	private List<KeyFieldsData> findKeysInRgs(T message, String type, Set<String> names)
	{
		List<KeyFieldsData> result = new ArrayList<>();
		for (T subMsg : message.getSubMessages())
		{
			if (type.equals(subMsg.getField(ClearThMessage.SUBMSGTYPE)))
			{
				KeyFieldsData kfd = new KeyFieldsData();
				kfd.setMsgType(type);
				kfd.setActionId(subMsg.getField(ClearThMessage.SUBMSGSOURCE));
				fillKeyFields(kfd, subMsg, names);
				if (kfd.hasKeys())
					result.add(kfd);
			}
			
			result.addAll(findKeysInRgs(subMsg, type, names));
		}
		return result;
	}
	
	protected List<KeyFieldsData> getKeysInRgs(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, 
			T message, RgKeyFieldNames rgKeyFieldNames)
	{
		if (rgKeyFieldNames == null)
			return null;
		
		List<KeyFieldsData> result = new ArrayList<>();
		for (Entry<String, Set<String>> keys : rgKeyFieldNames.getRgKeyFields())
		{
			List<KeyFieldsData> foundKeys = findKeysInRgs(message, keys.getKey(), keys.getValue());
			result.addAll(foundKeys);
		}
		return result;
	}
	
	protected void checkKeys(AllKeyFieldsData allKeys)
	{
		if (!allKeys.hasAnyKeys())
			throw ResultException.failed("Key fields are not declared");
	}
	
	protected MessageFinder<T> getMessageFinder()
	{
		return new MessageFinder<T>();
	}
	
	protected MessageMatcher<T> getMessageMatcher(KeyFieldsData keys, List<KeyFieldsData> keysInRgs)
	{
		if (keysInRgs != null)
			return new MatchesByMainAndRgsKeys<T>(keys, keysInRgs);
		return new MatchesByMainKeys<T>(keys, false);
	}
	
	protected MessageComparator<T> getMessageComparator()
	{
		return new MessageComparator<T>(getServiceParameters(), !isIgnoreExtraRgs(), true, isLogSubMessagesOutput());
	}
	
	
	//*** Search for messages and comparison ***
	
	protected List<T> findMessages(KeyFieldsData keys, List<KeyFieldsData> keysInRgs, MessageSource source,
			GlobalContext globalContext) throws ResultException
	{
		MessageFinder<T> finder = getMessageFinder();
		MessageMatcher<T> matcher = getMessageMatcher(keys, keysInRgs);
		
		List<T> found;
		int expectedMessageCount  = InputParamsUtils.getIntOrDefault(inputParams, ClearThMessage.MSGCOUNT, -1);
		try
		{
			found = finder.findAll(source, matcher, timeout, false, expectedMessageCount < 0);
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while searching for message", e);
		}
		finally
		{
			awaitedTimeout = finder.getLastSearchDuration();
		}
		afterSearch(globalContext, found);
		
		if (found == null)
			throw ResultException.failed(createFailComment(keys, keysInRgs));
		if (expectedMessageCount < 0)
			return found;
		
		int foundMessages = found.size();
		if (expectedMessageCount == foundMessages)
			return found;
		else
			throw ResultException.failed((expectedMessageCount < foundMessages) ? "Too many messages found" : "Not enough messages found"
					+ ": expected " + expectedMessageCount + ", found " + foundMessages);
	}
	
	protected String createKeyFieldsComment(KeyFieldsData keys, List<KeyFieldsData> keysInRgs)
	{
		StringBuilder result = new StringBuilder();
		if (keys != null)
			result.append(keys.keysToString());
		
		if (keysInRgs != null)
		{
			CommaBuilder rgsString = new CommaBuilder();
			for (KeyFieldsData rgKeys : keysInRgs)
			{
				String s = rgKeys.actionKeysToString();
				if (!StringUtils.isEmpty(s))
					rgsString.append(s);
			}
			
			if (rgsString.length() > 0)
			{
				if (result.length() > 0)
					result.append("\r\n");
				result.append("Key fields in RGs: ").append(rgsString.toString());;
			}
		}
		return result.toString();
	}
	
	protected String createFailComment(KeyFieldsData keys, List<KeyFieldsData> keysInRgs)
	{
		return "Could not find message with type "+createKeyFieldsComment(keys, keysInRgs);
	}
	
	
	protected Result compareMessages(MessageComparator<T> comparator, T expectedMessage, List<T> actualMessages, 
			RgKeyFieldNames rgKeyFieldNames)
	{
		return comparator.compareMessages(expectedMessage, actualMessages.get(0), rgKeyFieldNames);
	}
	
	
	protected void customRemoveMessages(MessageSource messageSource, List<T> foundMessages)
	{
	}
	
	protected void removeMessages(MessageSource messageSource, List<T> foundMessages)
	{
		if (messageSource instanceof CollectorMessageSource)
		{
			if (isRemoveFromCollector())
			{
				for (T tmp : foundMessages)
					messageSource.removeMessage(tmp);
			}
			return;
		}
		
		if (messageSource instanceof FileMessageSource)
		{
			if (isRemoveFile())
			{
				Path p = ((FileMessageSource)messageSource).getFile().toPath();
				try
				{
					Files.deleteIfExists(p);
				}
				catch (IOException e)
				{
					logger.warn("Could not delete file '"+p+"'", e);
				}
			}
			return;
		}
		
		customRemoveMessages(messageSource, foundMessages);
	}
	
	
	protected void saveComparatorOutput(MessageComparator<T> comparator)
	{
		setOutputParams(comparator.getOutputFields());
		setSubOutputParams(comparator.getSubOutputFields());
	}
	
	protected void saveOutputParams(List<T> messages, MessageComparator<T> comparator)
	{
		List<Pair<String, String>> outputFields = getOutputFields();
		if (outputFields == null)
		{
			saveComparatorOutput(comparator);
			return;
		}
		
		T message = messages.get(0);
		for (Pair<String, String> pair : outputFields)
		{
			if (getLogger().isTraceEnabled())
				getLogger().trace("Setting output param value for field '" + pair.getFirst() + "' (" + pair.getSecond() + ")");
			String field = message.getField(pair.getSecond());
			addOutputParam(pair.getFirst(), field);
		}
	}
	
	
	protected String getMessageType()
	{
		return inputParams.get(ClearThMessage.MSGTYPE);
	}
	
	
	//*** Various action settings ***
	
	protected boolean isReverseOrderByDefault()
	{
		return false;
	}

	protected boolean isReverseOrder()
	{
		return getBooleanOrDefault(inputParams, REVERSE_ORDER, isReverseOrderByDefault());
	}


	protected boolean isIgnoreExtraRgsByDefault()
	{
		return true;
	}

	protected boolean isIgnoreExtraRgs()
	{
		return getBooleanOrDefault(inputParams, IGNORE_EXTRA_REPEATING_GROUPS, isIgnoreExtraRgsByDefault());
	}


	protected boolean isSaveOutputIfFailedByDefault()
	{
		return false;
	}

	protected boolean isSaveOutputIfFailed()
	{
		return getBooleanOrDefault(inputParams, SAVE_OUTPUT_IF_FAILED, isSaveOutputIfFailedByDefault());
	}


	protected boolean isRemoveIfFailedByDefault()
	{
		return false;
	}

	protected boolean isRemoveIfFailed()
	{
		return getBooleanOrDefault(inputParams, REMOVE_IF_FAILED, isRemoveIfFailedByDefault());
	}


	protected boolean isRemoveFromCollectorByDefault()
	{
		return true;
	}

	protected boolean isRemoveFromCollector()
	{
		return getBooleanOrDefault(inputParams, REMOVE_FROM_COLLECTOR, isRemoveFromCollectorByDefault());
	}


	protected boolean isRemoveFileByDefault()
	{
		return false;
	}

	protected boolean isRemoveFile()
	{
		return getBooleanOrDefault(inputParams, REMOVE_FILE, isRemoveFileByDefault());
	}


	protected boolean isLogSubMessagesOutputByDefault()
	{
		return true;
	}

	protected boolean isLogSubMessagesOutput()
	{
		return getBooleanOrDefault(inputParams, LOG_SUBMESSAGES_OUTPUT, isLogSubMessagesOutputByDefault());
	}
}
