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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.SettingsException;

public abstract class MessageAction<T extends ClearThMessage<T>> extends Action implements Preparable
{
	public static final String CONNECTIONNAME = "ConnectionName", 
			REPEATINGGROUPS = "RepeatingGroups", 
			FILENAME = "FileName",
			CODEC = "Codec",
			META_FIELDS = "MetaFields",
			READ_FROM_CONTEXT_PARAM = "ReadFromContext",
			
			CODECNAME_POSTFIX = " codec";
	
	public static String defaultCodecNameInContext(String codecName)
	{
		return codecName+CODECNAME_POSTFIX;
	}
	
	
	public abstract MessageBuilder<T> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields);
	protected abstract String getDefaultCodecName();
	public abstract boolean isIncoming();
	
	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus status) throws Exception
	{
		prepareCodec(globalContext);
	}
	
	
	public String getCodecName()
	{
		return getInputParam(CODEC, getDefaultCodecName());
	}
	
	
	public T buildMessage(MatrixContext matrixContext)
	{
		Map<String, String> ip = getInputParams();
		return getMessageBuilder(getServiceParameters(), getMetaFields())
				.fields(ip)
				.metaFields(ip)
				.rgs(matrixContext, this)
				.type(inputParams.get(ClearThMessage.MSGTYPE))
				.build();
	}
	
	
	protected Set<String> getServiceParameters()
	{
		Set<String> result = new HashSet<String>();
		result.add(CONNECTIONNAME);
		result.add(REPEATINGGROUPS);
		result.add(CODEC);
		result.add(META_FIELDS);
		return result;
	}
	
	protected Set<String> getMetaFields()
	{
		MetaFieldsGetter metaGetter = getMetaFieldsGetter();
		Set<String> result = metaGetter.getFields(inputParams);
		metaGetter.checkFields(result, inputParams);
		return result;
	}
	
	protected String getCodecNameInContext()
	{
		return defaultCodecNameInContext(getCodecName());
	}
	
	protected ICodec createCodec() throws SettingsException
	{
		return ClearThCore.getInstance().createCodec(getCodecName());
	}
	
	protected void prepareCodec(GlobalContext globalContext) throws SettingsException
	{
		if (StringUtils.isEmpty(getCodecName()))  //I.e. no codec is used by this action
			return;
		
		String name = getCodecNameInContext();
		ICodec codec = (ICodec)globalContext.getLoadedContext(name);
		if (codec == null)
		{
			codec = createCodec();
			globalContext.setLoadedContext(name, codec);
		}
	}
	
	protected ICodec getCodec(GlobalContext globalContext)
	{
		return (ICodec)globalContext.getLoadedContext(getCodecNameInContext());
	}
	
	protected MetaFieldsGetter getMetaFieldsGetter()
	{
		return new SimpleMetaFieldsGetter(false);
	}
}
