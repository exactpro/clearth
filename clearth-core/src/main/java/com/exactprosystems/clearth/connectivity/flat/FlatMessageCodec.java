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

package com.exactprosystems.clearth.connectivity.flat;

import static com.exactprosystems.clearth.connectivity.Dictionary.MSG_DESC_NOT_FOUND_IN_DICTIONARY;
import static com.exactprosystems.clearth.connectivity.Dictionary.MSG_TYPE_NOT_FOUND;
import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescDoesNotFitError;
import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;
import static com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary.LEFT_ALIGNMENT;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.MessageValidator;
import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.utils.Utils;

public class FlatMessageCodec implements ICodec {

	private static final Logger logger = LoggerFactory.getLogger(FlatMessageCodec.class);
	public static final String DEFAULT_CODEC_NAME = "Flat";

	FlatMessageDictionary dictionary;
	private final MessageValidator messageValidator;

	public FlatMessageCodec(FlatMessageDictionary dictionary)
	{
		this.dictionary = dictionary;
		this.messageValidator = new MessageValidator();
	}

	public FlatMessageDictionary getDictionary()
	{
		return this.dictionary;
	}

	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		logger.debug("Trying to encode Flat message:{}{}", Utils.EOL, message);

		String messageType = getMessageType(message);
		FlatMessageDesc messageDesc = dictionary.getMessageDesc(messageType);
		if (messageDesc == null)
			throw new EncodeException(msgDescWithTypeNotFoundError(messageType));

		StringBuilder result = new StringBuilder();
		int currentPosition = 0;
		for (FlatMessageFieldDesc fieldDesc : messageDesc.getFieldDesc())
		{
			String fieldNameDesc = fieldDesc.getName();
			String valueMsg = (message.getField(fieldNameDesc) == null) ? "" : message.getField(fieldNameDesc);
			int valueLengthMsg = valueMsg.length();

			int position = fieldDesc.getPosition() - 1;
			if(position > currentPosition)
			{
				appendSpaces(result, position - currentPosition);
			}

			int valueLengthDesc = fieldDesc.getLength();
			currentPosition = position + valueLengthDesc;

			if(valueLengthDesc < valueLengthMsg)
			{
				valueMsg = valueMsg.substring(0, valueLengthDesc);
				valueLengthMsg = valueLengthDesc;
			}
			if(StringUtils.equals(fieldDesc.getAlignment(), LEFT_ALIGNMENT))
			{
				result.append(valueMsg);
				appendSpaces(result, valueLengthDesc - valueLengthMsg);
			}
			else // right alignment
			{
				appendSpaces(result, valueLengthDesc - valueLengthMsg);
				result.append(valueMsg);
			}

		}
		String encodedMessage = result.toString();

		logger.debug("Encoded message:{}{}", Utils.EOL, encodedMessage);

		return encodedMessage;
	}

	protected String getMessageType(ClearThMessage message) throws EncodeException
	{
		String type = message.getField(MSGTYPE);
		if (StringUtils.isEmpty(type))
			throw new EncodeException(MSG_TYPE_NOT_FOUND);
		else
			return type;
	}

	private void appendSpaces(StringBuilder stringBuilder, int numSpaces) {
		for (int i = 0; i < numSpaces; i++)
		{
			stringBuilder.append(' ');
		}
	}

	@Override
	public ClearThMessage<?> decode(String encodedMessage) throws DecodeException
	{
		return decode(encodedMessage, null);
	}
	
	@Override
	public ClearThMessage<?> decode(String encodedMessage, String messageType) throws DecodeException
	{
		logger.debug("Trying to decode Flat message:{}{}", SystemUtils.LINE_SEPARATOR, encodedMessage);

		FlatMessageDesc messageDesc = null;

		if (messageType == null)
		{
			messageDesc = findMessageDesc(encodedMessage);
			if(messageDesc == null)
				throw new DecodeException(MSG_DESC_NOT_FOUND_IN_DICTIONARY);
		}
		else if (isMsgDescFits(messageType, encodedMessage))
			messageDesc = dictionary.getMessageDesc(messageType);
		else
			throw new DecodeException(msgDescDoesNotFitError(messageType));

		messageValidator.validate(encodedMessage, dictionary.getConditions(messageDesc.getType()));

		SimpleClearThMessage flatMessage = new SimpleClearThMessage();
		for(FlatMessageFieldDesc fieldDesc: messageDesc.getFieldDesc())
		{
			String fieldName = fieldDesc.getName();
			int position = fieldDesc.getPosition() - 1;
			int length = fieldDesc.getLength();

			int endIndex = position + length;
			if (endIndex > encodedMessage.length())
				throw new DecodeException("Unexpected end of message: expected it no be at least " + endIndex + " characters long");

			String value = encodedMessage.substring(position, endIndex);
			flatMessage.addField(fieldName, value);
		}
		flatMessage.addField(MSGTYPE, messageDesc.getType());
		flatMessage.setEncodedMessage(encodedMessage);

		return flatMessage;
	}

	protected FlatMessageDesc findMessageDesc(String messageText)
	{
		for(String messageDescType: dictionary.getTypeConditionsMap().keySet())
		{
			if(isMsgDescFits(messageDescType, messageText))
				return dictionary.getMessageDesc(messageDescType);
		}

		return null;
	}

	boolean isMsgDescFits(String messageDescType, String messageText)
	{
		List<MessageValidatorCondition> typeConditions = dictionary.getTypeConditions(messageDescType);
		return (typeConditions != null) && messageValidator.isValid(messageText, typeConditions);
	}
}
