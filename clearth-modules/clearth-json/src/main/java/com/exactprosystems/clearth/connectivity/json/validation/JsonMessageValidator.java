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

package com.exactprosystems.clearth.connectivity.json.validation;

import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.json.ClearThJsonMessage;
import com.exactprosystems.clearth.connectivity.json.JsonDictionary;
import com.exactprosystems.clearth.connectivity.json.JsonFieldDesc;
import com.exactprosystems.clearth.connectivity.json.JsonFieldType;
import com.exactprosystems.clearth.connectivity.json.JsonMessageDesc;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.LineBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;
import static com.exactprosystems.clearth.connectivity.json.JsonCodec.ROOT_TYPE_ARRAY;
import static java.lang.String.format;

public class JsonMessageValidator
{
	protected static final Logger log = LoggerFactory.getLogger(JsonMessageValidator.class);

	protected final JsonDictionary dictionary;

	/**
	 * Override to implement project-specific validations.
	 * @param message can be a subMessage
	 * @param fieldDesc
	 * @param inSubMsg
	 * @param rgKey
	 */
	protected void validateFieldExt(ClearThJsonMessage message,
	                                JsonFieldDesc fieldDesc,
	                                boolean inSubMsg,
	                                String rgKey,
	                                List<MessageValidationError> errors,
	                                MultiValuedMap errorsInRg)
	{
	}

	public JsonMessageValidator(JsonDictionary dictionary)
	{
		this.dictionary = dictionary;
	}

	public String validateMessage(ClearThJsonMessage message, String msgType) throws EncodeException
	{
		JsonMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if (messageDesc == null)
			throw new EncodeException(msgDescWithTypeNotFoundError(msgType));
		
		List<JsonFieldDesc> fieldDescList = messageDesc.getFieldDesc();
		if (ROOT_TYPE_ARRAY.equals(messageDesc.getRootType()) && fieldDescList.size() > 1)
			throw new EncodeException(String.format("Message definition with type '%s' has 'rootType=\"array\"' and cannot have more than one fieldDesc.", msgType));
		
		return validateMessage(message, fieldDescList, false, null);
	}

	protected String validateMessage(ClearThJsonMessage message, List<JsonFieldDesc> fieldDescs, boolean inSubMsg, String rgKey)
	{
		List<MessageValidationError> errors = new ArrayList<>();
		MultiValuedMap errorsInRg = new ArrayListValuedHashMap();

		for (JsonFieldDesc fieldDesc : fieldDescs)
		{
			validateField(message, fieldDesc, inSubMsg, rgKey, errors, errorsInRg);
			
			List<JsonFieldDesc> subDescs = fieldDesc.getFieldDesc();
			if (fieldDesc.isRepeat())
			{
				String fieldName = getFieldName(fieldDesc);
				for (ClearThJsonMessage subMessage : message.getSubMessages(fieldName))
				{
					validateMessage(subMessage, subDescs, true, format("%s (%s)", 
							subMessage.getField(ClearThJsonMessage.SUBMSGSOURCE), fieldName));
				}
			}
			else if (CollectionUtils.isNotEmpty(subDescs))
			{
				validateMessage(message, subDescs, inSubMsg, rgKey);
			}
		}

		return buildErrorMessage(errors, errorsInRg);
	}
	
	protected void appendError(MessageValidationError error,
	                           boolean inSubMsg,
	                           String rgKey,
	                           List<MessageValidationError> errors,
	                           MultiValuedMap errorsInRg)
	{
		if (inSubMsg)
			errorsInRg.put(rgKey, error);
		else 
			errors.add(error);
	}
	
	protected void validateField(ClearThJsonMessage message,
	                             JsonFieldDesc fieldDesc,
	                             boolean inSubMsg,
	                             String rgKey,
	                             List<MessageValidationError> errors,
	                             MultiValuedMap errorsInRg)
	{
		checkPresence(message, fieldDesc, inSubMsg, rgKey, errors, errorsInRg);
		
		String value = message.getField(getFieldName(fieldDesc));
		if (StringUtils.isEmpty(value)) // Possible valid but nothing to validate
			return;
		
		if (JsonFieldType.NUMBER == fieldDesc.getType())
			checkNumberFormat(value, fieldDesc, inSubMsg, rgKey, errors, errorsInRg);
		
		validateFieldExt(message, fieldDesc, inSubMsg, rgKey, errors, errorsInRg);
	}
	
	protected void checkPresence(ClearThJsonMessage message,
	                             JsonFieldDesc fieldDesc,
	                             boolean inSubMsg,
	                             String rgKey,
	                             List<MessageValidationError> errors,
	                             MultiValuedMap errorsInRg)
	{
		if (!fieldDesc.isMandatory())
			return;
		
		String fieldName = getFieldName(fieldDesc);
		if (fieldDesc.isRepeat())
		{
			List<ClearThJsonMessage> rgs = message.getSubMessages(fieldName);
			if (CollectionUtils.isEmpty(rgs))
				appendError(new RequiredRgError(fieldName), inSubMsg, rgKey, errors, errorsInRg);
		}
		else if (CollectionUtils.isEmpty(fieldDesc.getFieldDesc()))
		{
			if (StringUtils.isEmpty(message.getField(fieldName)))
				appendError(new RequiredFieldError(fieldName), inSubMsg, rgKey, errors, errorsInRg);
		}
	}
	
	protected void checkNumberFormat(String value,
	                                 JsonFieldDesc fieldDesc,
	                                 boolean inSubMsg,
	                                 String rgKey,
	                                 List<MessageValidationError> errors,
	                                 MultiValuedMap errorsInRg)
	{
		if (!NumberUtils.isNumber(value))
			appendError(new FormatError(getFieldName(fieldDesc), value, "Number"),
					inSubMsg, rgKey, errors, errorsInRg);
		else 
		{
			try
			{
				Double.parseDouble(value);
			}
			catch (NumberFormatException e)
			{
				appendError(new FormatError(getFieldName(fieldDesc), value, "Number"),
						inSubMsg, rgKey, errors, errorsInRg);
				log.trace("Error while parsing number.", e);
			}
		}		
	}
	
	@SuppressWarnings("unchecked")
	protected String buildErrorMessage(List<MessageValidationError> errors, MultiValuedMap errorsInRg)
	{
		LineBuilder lb = new LineBuilder();
		
		if (!errors.isEmpty())
			buildErrorMessage(lb, errors, "Message contains errors.");
		
		for (Object rgName: errorsInRg.keySet())
		{
			Collection rgErrors = errorsInRg.get(rgName);
			if (CollectionUtils.isNotEmpty(rgErrors))
				buildErrorMessage(lb, rgErrors, format("Repeating group '%s' contains errors.", rgName));
		}		
		return lb.toString();
	}
	
	protected void buildErrorMessage(LineBuilder lb, Collection<MessageValidationError> errors, String comment)
	{
		lb.append(comment);
		
		List<MessageError> messageErrors = filter(errors, MessageError.class);
		if (!messageErrors.isEmpty())
			for(MessageError messageError : messageErrors)
				lb.append(messageError.getMessage());
		
		List<RequiredRgError> requiredRgErrors = filter(errors, RequiredRgError.class);
		if (!requiredRgErrors.isEmpty())
			lb.append(format("The following required repeating groups are absent: %s.", joinFieldNames(requiredRgErrors)));
		
		List<RequiredFieldError> requiredFieldErrors = filter(errors, RequiredFieldError.class);
		if (!requiredFieldErrors.isEmpty())
			lb.append(format("The following required fields are absent: %s.", joinFieldNames(requiredFieldErrors)));
		
		List<FormatError> formatErrors = filter(errors, FormatError.class);
		if (!formatErrors.isEmpty())
			lb.append(format("The following fields have incorrect format: %s.", joinFieldsFormats(formatErrors)));
	}
	
	protected String joinFieldNames(Collection<? extends MessageValidationError> errors)
	{
		CommaBuilder cb = new CommaBuilder();
		for (MessageValidationError error : errors)
		{
			cb.append(error.getFieldName());
		}
		return cb.toString();
	}
	
	protected String joinFieldsFormats(Collection<? extends FormatError> errors)
	{
		CommaBuilder cb = new CommaBuilder();
		for (FormatError error : errors)
		{
			cb.append(error.getFieldName()).add(" ('").add(error.getValue()).add("', '").add(error.getFormat()).add("')");
		}
		return cb.toString();
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> filter(Collection<MessageValidationError> objects, Class<T> clazz)
	{
		List<T> filtered = null;
		for (MessageValidationError o: objects) {
			if (clazz.isInstance(o))
			{
				if (filtered == null)
					filtered = new ArrayList<>();
				filtered.add((T)o);
			}
		}
		return filtered == null ? Collections.emptyList() : filtered;
	}
	
	protected String getFieldName(JsonFieldDesc fieldDesc)
	{
		return fieldDesc.getName() != null ? fieldDesc.getName() : fieldDesc.getSource();
	}
}
