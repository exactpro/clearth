/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.SubActionData;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.Utils;

/**
 * Builder of {@link ClearThMessage} descendants
 * @author vladimir.panarin
 * @param <M>
 */
public abstract class MessageBuilder<M extends ClearThMessage<M>>
{
	private static final Logger logger = LoggerFactory.getLogger(MessageBuilder.class);
	
	protected M message;
	protected final Set<String> serviceParameters,
			metaFields;
	protected MatrixContext matrixContext;  //Context to search for RGs by action parameter
	protected Action action;  //Action to add sub-actions data while searching for RGs
	protected List<String> rgErrors;
	
	public MessageBuilder()
	{
		message = createMessage();
		serviceParameters = null;
		metaFields = null;
	}
	
	public MessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		message = createMessage();
		this.serviceParameters = serviceParameters != null ? Collections.unmodifiableSet(serviceParameters) : null;
		this.metaFields = metaFields != null ? Collections.unmodifiableSet(metaFields) : null;
	}
	
	
	/**
	 * @return new instance of {@link ClearThMessage} descendant to be used within the builder
	 */
	protected abstract M createMessage();
	
	
	/**
	 * @return set of parameter names that builder skips while adding fields to message
	 */
	public Set<String> getServiceParameters()
	{
		return serviceParameters;
	}
	
	/**
	 * @return set of field names whose values to be used as metadata fields and not as message fields
	 */
	public Set<String> getMetaFields()
	{
		return metaFields;
	}

	
	/**
	 * Sets message type
	 * @param type to set for message being built
	 */
	public MessageBuilder<M> type(String type)
	{
		checkAndInit();
		message.removeField(ClearThMessage.SUBMSGTYPE);
		message.addField(ClearThMessage.MSGTYPE, type);
		return this;
	}
	
	/**
	 * Sets sub-message type. Valid only for sub-messages
	 * @param subMessageType to set for message being built
	 */
	public MessageBuilder<M> subMessageType(String subMessageType)
	{
		checkAndInit();
		message.removeField(ClearThMessage.MSGTYPE);
		return field(ClearThMessage.SUBMSGTYPE, subMessageType);
	}
	
	public MessageBuilder<M> field(String name, String value)
	{
		checkAndInit();
		if (isServiceParameter(name) || isMetaField(name))
			return this;
		
		message.addField(name, value);
		return this;
	}
	
	public MessageBuilder<M> fields(Map<String, String> fields)
	{
		//TODO add fields directly to message with addAll, not one by one. Don't forget to filter them by serviceParameters and metaFields
		for (Entry<String, String> f : fields.entrySet())
			field(f.getKey(), f.getValue());
		return this;
	}
	
	
	public MessageBuilder<M> metaField(String name, String value)
	{
		checkAndInit();
		if (!isMetaField(name))
			return this;
		
		message.addMetaField(name, value);
		return this;
	}
	
	public MessageBuilder<M> metaFields(Map<String, String> fields)
	{
		checkAndInit();
		if (CollectionUtils.isEmpty(metaFields))
			return this;
		
		for (String f : metaFields)
		{
			String value = fields.get(f);
			if (!StringUtils.isEmpty(value))
				message.addMetaField(f, value);
		}
		return this;
	}
	
	
	public MessageBuilder<M> rg(M rg)
	{
		checkAndInit();
		message.addSubMessage(rg);
		return this;
	}
	
	public MessageBuilder<M> rgs(List<M> rgs)
	{
		checkAndInit();
		message.getSubMessages().addAll(rgs);
		return this;
	}
	
	/**
	 * Adds to message RGs found in {@link MatrixContext} by IDs enumerated in rgIds parameter
	 * @param matrixContext to look for RGs as {@link SubActionData}
	 * @param rgIds enumerated IDs of RGs to look for
	 * @param action to store data about used sub-actions
	 * @throws ResultException if one or more RGs cannot be found or contain errors
	 */
	public MessageBuilder<M> rgs(MatrixContext matrixContext, String rgIds, Action action) throws ResultException
	{
		this.matrixContext = matrixContext;
		this.action = action;
		this.rgErrors = new ArrayList<String>();
		try
		{
			List<M> foundRgs = findRgs(rgIds, 0, null);
			
			if (!rgErrors.isEmpty())
				throw ResultException.failed(StringUtils.join(rgErrors, Utils.EOL));
			
			if (foundRgs != null)
				return rgs(foundRgs);
			return this;
		}
		finally
		{
			this.matrixContext = null;
			this.action = null;
			this.rgErrors = null;
		}
	}
	
	/**
	 * Adds to message RGs referenced in given action and found in {@link MatrixContext}
	 * @param matrixContext to look for RGs as {@link SubActionData}
	 * @param action to get IDs of RGs to look for and to store data about them
	 * @return builder to continue with current message
	 * @throws ResultException if one or more RGs cannot be found or contain errors
	 */
	public MessageBuilder<M> rgs(MatrixContext matrixContext, Action action) throws ResultException
	{
		return rgs(matrixContext, action.getInputParam(MessageAction.REPEATINGGROUPS), action);
	}
	
	
	public M build()
	{
		M result = message;
		message = null;
		return result;
	}
	
	protected boolean isServiceParameter(String name)
	{
		return (serviceParameters != null) && (serviceParameters.contains(name));
	}
	
	protected boolean isMetaField(String name)
	{
		return (metaFields != null) && (metaFields.contains(name));
	}
	
	
	//*** Methods to find and process RGs ***
	protected List<M> findRgs(String rgIds, int recursionDepth, SubActionData parent) throws ResultException
	{
		if ((rgIds == null) || (rgIds.trim().length() == 0))
			return null;
		
		if (recursionDepth > 10)
			throw ResultException.failed((rgErrors.isEmpty() ? "" 
					: StringUtils.join(rgErrors, Utils.EOL) + Utils.EOL + Utils.EOL) +"It seems like repeating groups are referencing each other");
		
		List<M> result = new ArrayList<M>();
		String[] groupsIds = rgIds.split(",");
		for (String groupId : groupsIds)
		{
			groupId = groupId.trim();
			if (isNeedSkipRg(groupId))
				continue;
			
			SubActionData subActionData = matrixContext.getSubActionData(groupId);
			if (subActionData == null)
			{
				rgErrors.add("Sub-action with ID '"+ groupId+"' has not been set in matrix. Make sure this sub-action exists.");
				continue;
			}
			
			processRepeatingGroup(groupId, subActionData, parent, result, recursionDepth);
		}
		
		return result;
	}
	
	protected M createRgMessage(String groupId, SubActionData rgData)
	{
		M result = createMessage();
		result.addField(ClearThMessage.SUBMSGSOURCE, groupId);
		for (String paramName : rgData.getParams().keySet())
		{
			if (paramName.equals(MessageAction.REPEATINGGROUPS))
				continue;

			String paramValue = rgData.getParams().get(paramName);
			result.addField(paramName, paramValue);
		}
		return result;
	}
	
	protected List<M> findRgChildren(SubActionData rgData, int recursionDepth)
	{
		String rgIds = rgData.getParams().get(MessageAction.REPEATINGGROUPS);
		List<M> result = null;
		try
		{
			result = findRgs(rgIds, recursionDepth + 1, rgData);
		}
		catch (ResultException e)
		{
			rgErrors.add(e.getMessage());
		}
		return result;
	}

	protected void processRepeatingGroup(String groupId, SubActionData rgData, SubActionData parent, List<M> result, int recursionDepth)
	{
		if (parent == null)
			action.addSubActionData(groupId, rgData);
		else
			parent.getSubActionData().put(groupId, rgData);
		
		if (!rgData.getSuccess().passed)
			rgErrors.add("Sub-action with ID '" + groupId + "' has errors. See details below.");
		
		M rgMessage = createRgMessage(groupId, rgData);
		List<M> childrenRgs = findRgChildren(rgData, recursionDepth);
		if (childrenRgs != null)
			rgMessage.getSubMessages().addAll(childrenRgs);
		result.add(rgMessage);
	}

	protected boolean isNeedSkipRg(String groupId)
	{
		boolean skipState = action.getMatrix().getNonExecutableActions().contains(groupId);
		if (skipState)
			logger.info("Repeating group '{}' skipped", groupId);
		return skipState;
	}

	private void checkAndInit()
	{
		if(message == null)
			message = createMessage();
	}
}
