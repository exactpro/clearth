/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.Message.Header;
import quickfix.field.ApplVerID;
import quickfix.field.BeginString;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public abstract class FixApplication extends BasicClearThClient implements Application
{
	private static final Logger logger = LoggerFactory.getLogger(FixApplication.class);
	protected static final String TRANSPORT_DATA_DICT = "TransportDataDictionary",
			APP_DATA_DICT = "AppDataDictionary",
			DATA_DICT = "DataDictionary",
			BEGIN_STRING = "BeginString",
			SENDER_COMP_ID = "SenderCompID",
			TARGET_COMP_ID = "TargetCompID",
			DEFAULT_VER_ID = "DefaultApplVerID",
			ERROR_SETTING_MISSING = "Mandatory setting '%s' is missing in both connection and default settings",
			ERROR_DICTIONARY_NOT_SET = "Setting '%s' and setting with default value '%s' are missing in both connection and default settings";
	
	public static final String SETTING_WRITE_LOG = "WriteLog",
			DEFAULT_SETTINGS_FILE = ClearThCore.rootRelative("cfg/fixsettings.cfg"),
			DICT_EXT = ".xml";
	protected static final String[] DICTS_PATHS_PARAMS = {TRANSPORT_DATA_DICT, APP_DATA_DICT, DATA_DICT};
	protected String transportDictFile, appDictFile,
			beginString, senderCompId, targetCompId;
	protected ApplVerID appVerID;
	protected SessionID sessionID;
	protected DataDictionary transportDict,
			appDict;
	protected int beginStringTag, senderCompIdTag, targetCompIdTag;
	
	
	protected abstract void processConnectionSettings(FixConnectionSettings connectionSettings) throws SettingsException;
	protected abstract void startFix(Map<String, String> fixSettings, SessionSettings sessionSettings, 
			MessageStoreFactory msgStoreFactory, LogFactory logFactory, MessageFactory msgFactory) throws ConnectionException, SettingsException;
	protected abstract void stopFix() throws ConnectionException;
	protected abstract void sendFixMessage(Message message, ClearThMessageMetadata metadata) throws ConnectionException;
	
	
	public FixApplication(FixConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void connect() throws ConnectivityException, SettingsException
	{
		FixConnectionSettings conSettings = (FixConnectionSettings) owner.getSettings();
		processConnectionSettings(conSettings);
		
		Map<String, String> fixSettings = KeyValueUtils.parseKeyValueString(conSettings.getFixSettings(), "\\R", false);
		changeRelativePaths(fixSettings);
		
		try
		{
			SessionSettings defaultSettings = new SessionSettings(new FileInputStream(DEFAULT_SETTINGS_FILE));
			changeRelativePaths(defaultSettings.getDefaultProperties());
			String dataDictFile = getSessionSetting(DATA_DICT, false, fixSettings, defaultSettings);
			
			transportDictFile = getDictionarySetting(TRANSPORT_DATA_DICT, fixSettings, defaultSettings, dataDictFile);
			appDictFile = getDictionarySetting(APP_DATA_DICT, fixSettings, defaultSettings, dataDictFile);
			beginString = getSessionSetting(BEGIN_STRING, true, fixSettings, defaultSettings);
			senderCompId = getSessionSetting(SENDER_COMP_ID, true, fixSettings, defaultSettings);
			targetCompId = getSessionSetting(TARGET_COMP_ID, true, fixSettings, defaultSettings);
			appVerID = new ApplVerID(getSessionSetting(DEFAULT_VER_ID, false, fixSettings, defaultSettings));
			
			fixSettings.remove(BEGIN_STRING);
			fixSettings.remove(SENDER_COMP_ID);
			fixSettings.remove(TARGET_COMP_ID);
			
			sessionID = new SessionID(new BeginString(beginString), new SenderCompID(senderCompId), new TargetCompID(targetCompId));
			//Passing connection-level FIX settings to session settings, replacing default ones
			for (Entry<String, String> fs : fixSettings.entrySet())
				defaultSettings.setString(sessionID, fs.getKey(), fs.getValue());
			
			MessageStoreFactory storeFactory = createMessageStoreFactory(fixSettings, defaultSettings);
			MessageFactory msgFactory = createMessageFactory(fixSettings, defaultSettings);
			LogFactory logFactory = createLogFactory(fixSettings, defaultSettings);
			
			startFix(fixSettings, defaultSettings, storeFactory, logFactory, msgFactory);
			
			transportDict = createTransportDictionary();
			appDict = createAppDictionary();
			
			beginStringTag = transportDict.getFieldTag(BEGIN_STRING);
			senderCompIdTag = transportDict.getFieldTag(SENDER_COMP_ID);
			targetCompIdTag = transportDict.getFieldTag(TARGET_COMP_ID);
		}
		catch (IOException | ConfigError e)
		{
			throw new SettingsException("Could not configure FIX", e);
		}
	}
	
	@Override
	protected void closeConnections() throws ConnectivityException
	{
		stopFix();
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		return false;
	}
	
	@Override
	protected boolean isNeedNotifySendListeners()  //It will notify send listeners from toApp() method with real FIX message being sent
	{
		return false;
	}
	
	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return null;  //Never needed such kind of thread here. Messages come via socket without explicit reading request
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(Object message) throws IOException, ConnectivityException
	{
		return doSendMessage(message, null);
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		return doSendMessage(message.getPayload(), message.getMetadata());
	}
	
	
	protected final String getSessionSetting(String name, boolean mandatory,
			Map<String, String> fixSettings, SessionSettings defaultSettings) throws SettingsException
	{
		String result = fixSettings.get(name);
		if (result != null)
			return result;
		
		try
		{
			if (defaultSettings.isSetting(name))
				return defaultSettings.getString(name);
		}
		catch (Exception e)
		{
			logger.warn("Error while getting '{}' from default settings", name, e);
		}
		
		if (mandatory)
			throw new SettingsException(String.format(ERROR_SETTING_MISSING, name));
		return null;
	}
	
	protected String getDictionarySetting(String name, Map<String, String> fixSettings, SessionSettings defaultSettings, String defaultValue) throws SettingsException
	{
		String result = getSessionSetting(name, false, fixSettings, defaultSettings);
		if (result != null)
			return result;
		
		if (defaultValue != null)
			return defaultValue;
		
		throw new SettingsException(String.format(ERROR_DICTIONARY_NOT_SET, name, DATA_DICT));
	}
	
	
	//FIX Application methods implementation
	@Override
	public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon
	{
		logger.trace("{} received admin message:{}{}", name, Utils.EOL, message);
	}
	
	@Override
	public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType
	{
		logger.trace("{} received message:{}{}", name, Utils.EOL, message);
		receivedMessageQueue.add(EncodedClearThMessage.newReceivedMessage(message.toString()));
	}
	
	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend
	{
		if (isNeedSentProcessorThread())
		{
			if (message instanceof FixMessage)  //FixMessage has metadata. Keeping it so that messageHandler and sendListeners will get it
				sentMessageQueue.add(new EncodedClearThMessage(message, ((FixMessage)message).getMetadata()));
			else
				sentMessageQueue.add(EncodedClearThMessage.newSentMessage(message, Instant.now()));
		}
	}
	
	
	public DataDictionary getTransportDictionary() throws ConfigError
	{
		if (transportDict == null)  //If FIX application is not connected yet
			return createDataDictionary(transportDictFile);
		return transportDict;
	}
	
	public DataDictionary getAppDictionary() throws ConfigError
	{
		if (appDict == null)  //If FIX application is not connected yet
			return createDataDictionary(appDictFile);
		return appDict;
	}
	
	
	protected EncodedClearThMessage doSendMessage(Object payload, ClearThMessageMetadata metadata) throws IOException, ConnectivityException
	{
		//Updated metadata will be added to Message (actually, FixMessage) created below and kept till message sending event is fired, i.e. in toApp()
		//Thus, updated metadata will be included in EncodedClearThMessage that messageHandler and send listeners are notified with
		EncodedClearThMessage updatedMessage = createUpdatedMessage(payload, metadata);
		ClearThMessageMetadata updatedMetadata = updatedMessage.getMetadata();
		
		Message m = createMessageFromString(updatedMessage.getPayload().toString(), updatedMetadata);
		prepareMessageToSend(m, updatedMetadata);
		sendFixMessage(m, updatedMetadata);
		logger.trace("{} has sent message successfully", name);
			
		return new EncodedClearThMessage(m, updatedMetadata);
	}
	
	
	protected DataDictionary createTransportDictionary() throws ConnectivityException
	{
		try
		{
			return createDataDictionary(transportDictFile);
		}
		catch (ConfigError e)
		{
			throw new ConnectivityException("Error while loading transport dictionary", e);
		}
	}
	
	protected DataDictionary createAppDictionary() throws ConnectivityException
	{
		try
		{
			return createDataDictionary(appDictFile);
		}
		catch (ConfigError e)
		{
			throw new ConnectivityException("Error while loading app dictionary", e);
		}
	}
	
	
	protected MessageStoreFactory createMessageStoreFactory(Map<String, String> fixSettings, SessionSettings sessionSettings)
	{
		return new FileStoreFactory(sessionSettings);
	}
	
	protected MessageFactory createMessageFactory(Map<String, String> fixSettings, SessionSettings sessionSettings)
	{
		return new DefaultMessageFactory();
	}
	
	protected LogFactory createLogFactory(Map<String, String> fixSettings, SessionSettings sessionSettings) throws ConfigError
	{
		try
		{
			if (sessionSettings.isSetting(sessionID, FileLogFactory.SETTING_FILE_LOG_PATH)
					&& !StringUtils.isBlank(sessionSettings.getString(sessionID, FileLogFactory.SETTING_FILE_LOG_PATH))
					&& (!(sessionSettings.isSetting(sessionID, SETTING_WRITE_LOG))
						|| sessionSettings.getBool(sessionID, SETTING_WRITE_LOG)))
				return new FileLogFactory(sessionSettings);
		}
		catch (FieldConvertError e)
		{
			throw new ConfigError(String.format("Could not convert setting '%s' to boolean. Use Y or N as value",
					SETTING_WRITE_LOG),	e);
		}
		return null;
	}
	
	protected Message createMessageFromString(String message, ClearThMessageMetadata metadata) throws ConnectivityException
	{
		try
		{
			return FixMessage.createFromString(message, metadata, transportDict, appDict, false);
		}
		catch (Exception e)
		{
			throw new ConnectivityException("Could not create FIX message from string", e);
		}
	}
	
	protected void prepareMessageToSend(Message message, ClearThMessageMetadata metadata)
	{
		setTagIfAbsent(beginStringTag, beginString, message);
		setTagIfAbsent(senderCompIdTag, senderCompId, message);
		setTagIfAbsent(targetCompIdTag, targetCompId, message);
	}
	
	
	private void setTagIfAbsent(int tag, String value, Message message)
	{
		Header header = message.getHeader();
		if (!header.isSetField(tag) && !message.isSetField(tag))
			header.setField(new StringField(tag, value));
	}
	
	private DataDictionary createDataDictionary(String fileName) throws ConfigError
	{
		return new DataDictionary(ClearThCore.rootRelative(fileName));
	}
	
	protected void changeRelativePaths(Map<String, String> settings)
	{
		for (String paramName : DICTS_PATHS_PARAMS)
		{
			String path = settings.get(paramName);
			if (isValidDictionarySetting(path))
				settings.put(paramName, ClearThCore.rootRelative(path));
		}
	}
	
	protected void changeRelativePaths(Properties properties)
	{
		for (String paramName : DICTS_PATHS_PARAMS)
		{
			String path = properties.getProperty(paramName);
			if (isValidDictionarySetting(path))
				properties.put(paramName, ClearThCore.rootRelative(path));
		}
	}
	
	protected boolean isValidDictionarySetting(String filePath)
	{
		return filePath != null && filePath.endsWith(DICT_EXT);
	}
}
