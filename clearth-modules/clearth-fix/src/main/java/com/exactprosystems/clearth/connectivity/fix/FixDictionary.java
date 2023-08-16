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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import quickfix.ConfigError;

import java.io.File;
import java.util.Map;

/**
 * Wrapper for FIX dictionary, required for {@link FixCodec} and {@link com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory DefaultCodecFactory}
 */
public class FixDictionary
{
	public static final String TAG_MSGTYPE = "MsgType",
			DICT_TRANSPORTPREFIX = "transport_",
			DICT_TRANSPORTARGUMENT = "TransportDictionary",
			DEFAULT_DICT_TRANSPORT = "FIXT11.xml";
	
	protected final ClearThDataDictionary appDictionary,
			transportDictionary;
	protected final int typeTag;
	
	public FixDictionary(String fileName) throws ConfigError, DictionaryLoadException
	{
		File dictFile = new File(fileName);
		appDictionary = createAppDictionary(dictFile);
		transportDictionary = findTransportDictionary(dictFile);
		typeTag = getMessageTypeTag();
	}
	
	public FixDictionary(String fileName, Map<String, String> extraArguments, String filesRoot) throws ConfigError, DictionaryLoadException
	{
		String transportDictFileName = extraArguments.get(DICT_TRANSPORTARGUMENT);
		File dictFile = new File(fileName);
		if (transportDictFileName == null) 
			transportDictionary = findTransportDictionary(dictFile);
		else 
		{
			File transportDictFile = getDictionaryFile(transportDictFileName, filesRoot);
			transportDictionary = createTransportDictionary(transportDictFile);
		}
		appDictionary = createAppDictionary(dictFile);
		typeTag = getMessageTypeTag();
	}
	
	public FixDictionary(String fileName, Map<String, String> extraArguments) throws ConfigError, DictionaryLoadException
	{
		this(fileName, extraArguments, ClearThCore.dictsPath());
	}
	
	public FixDictionary(String appDictFileName, String transportDictFileName) throws ConfigError, DictionaryLoadException
	{
		appDictionary = createAppDictionary(new File(appDictFileName));
		transportDictionary = createTransportDictionary(new File(transportDictFileName));
		typeTag = getMessageTypeTag();
	}
	
	public FixDictionary(ClearThDataDictionary appDictionary, ClearThDataDictionary transportDictionary) throws ConfigError, DictionaryLoadException
	{
		this.appDictionary = appDictionary;
		this.transportDictionary = transportDictionary;
		typeTag = getMessageTypeTag();
	}
	
	
	public ClearThDataDictionary getAppDictionary()
	{
		return appDictionary;
	}
	
	/**
	 * @return transport dictionary part. Sometimes it is called "session dictionary". Will be null if no transport dictionary is used, e.g. FIX version < 5.0
	 */
	public ClearThDataDictionary getTransportDictionary()
	{
		return transportDictionary;
	}
	
	public int getTypeTag()
	{
		return typeTag;
	}
	
	
	protected ClearThDataDictionary createDataDictionary(File file) throws ConfigError
	{
		ClearThDataDictionary result = new ClearThDataDictionary(file);
		result.setCheckFieldsOutOfOrder(false);
		result.setCheckUnorderedGroupFields(false);
		return result;
	}
	
	protected ClearThDataDictionary createAppDictionary(File file) throws ConfigError
	{
		return createDataDictionary(file);
	}
	
	protected ClearThDataDictionary createTransportDictionary(File file) throws ConfigError
	{
		return createDataDictionary(file);
	}
	
	protected ClearThDataDictionary findTransportDictionary(File appDictFile) throws ConfigError
	{
		File file = new File(appDictFile.getParentFile(), DICT_TRANSPORTPREFIX+appDictFile.getName());
		if (!file.isFile())
		{
			file = new File(appDictFile.getParentFile(), DEFAULT_DICT_TRANSPORT);
			if (!file.isFile())
				return null;
		}
		return createTransportDictionary(file);
	}
	
	protected int getMessageTypeTag() throws DictionaryLoadException
	{
		int tag = appDictionary.getFieldTag(TAG_MSGTYPE);
		if (tag > -1)
			return tag;
		
		if (transportDictionary != null)
		{
			tag = transportDictionary.getFieldTag(TAG_MSGTYPE);
			if (tag > -1)
				return tag;
		}
		
		throw new DictionaryLoadException("Unknown tag '"+TAG_MSGTYPE+"'");
	}
	
	
	private File getDictionaryFile(String fileName, String filesRoot)
	{
		File f = new File(fileName);
		if (f.isAbsolute() || filesRoot == null)
			return f;
		return new File(filesRoot, fileName);
	}
}
