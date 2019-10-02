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

package com.exactprosystems.clearth.connectivity.swift;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.MessageValidator;
import com.exactprosystems.clearth.utils.SpecialValue;
import com.prowidesoftware.swift.io.ConversionService;
import com.prowidesoftware.swift.io.IConversionService;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftBlock1;
import com.prowidesoftware.swift.model.SwiftBlock2;
import com.prowidesoftware.swift.model.SwiftBlock2Input;
import com.prowidesoftware.swift.model.SwiftBlock2Output;
import com.prowidesoftware.swift.model.SwiftBlock3;
import com.prowidesoftware.swift.model.SwiftBlock4;
import com.prowidesoftware.swift.model.SwiftBlock5;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.connectivity.Dictionary.MSG_DESC_NOT_FOUND_IN_DICTIONARY;
import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescDoesNotFitError;
import static com.exactprosystems.clearth.utils.Utils.EOL;

/**
 * 
 * @author vladimir.panarin
 * 
 */

public class SwiftCodec implements ICodec
{
	private static final Logger logger = LoggerFactory.getLogger(SwiftCodec.class);
	private static final String DEFAULT_SEPARATOR = "//";
	
	public static final String DEFAULT_CODEC_NAME = "Swift";
	public static final String MSG_DESC_DOES_NOT_FIT = "Message definition, founded by %s (type = %s), does not fit by %s.";

	protected SwiftDictionary dictionary;
	protected ValueGenerator generator;
	protected final MessageValidator messageValidator;
	
	protected static final SwiftMetaData emptySwiftMetaData = new SwiftMetaData(Collections.emptyMap());

	public SwiftCodec(SwiftDictionary swiftDictionary)
	{
		this(swiftDictionary, ClearThCore.getInstance().getCommonGenerator());
	}
	
	public SwiftCodec(SwiftDictionary swiftDictionary, ValueGenerator generator)
	{
		this.dictionary = swiftDictionary;
		this.generator = generator;
		this.messageValidator = createMessageValidator();
	}
	
	protected boolean ignoreEmptyValues()
	{
		return false;
	}
	
	protected MessageValidator createMessageValidator()
	{
		return new MessageValidator();
	}

	protected SwiftBlock1 encodeSwiftBlock1(ClearThMessage message)
	{
		SwiftMetaData smd = (message instanceof ClearThSwiftMessage) ? 
				((ClearThSwiftMessage)message).getMetaData() : emptySwiftMetaData;
		return new SwiftBlock1((smd.getApplicationId() != null) ? smd.getApplicationId() : "F",
				(smd.getServiceId() != null) ? smd.getServiceId() : "01",
				(smd.getLogicalTerminal() != null) ? smd.getLogicalTerminal() : "XXXXXXXXXXXX",
				(smd.getSessionNumber() != null) ? smd.getSessionNumber() : "0001",
				(smd.getSequenceNumber() != null) ? smd.getSequenceNumber() : "000000");
	}
	
	protected SwiftBlock2 encodeSwiftBlock2(ClearThMessage message)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
		String curDate = formatter.format(Calendar.getInstance().getTime());

		SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmm");
		String curTime = timeFormatter.format(Calendar.getInstance().getTime());

		SwiftMetaData metaData = (message instanceof ClearThSwiftMessage) ? 
				((ClearThSwiftMessage)message).getMetaData() : emptySwiftMetaData;
		SwiftBlock2 swiftBlock2;
		if (metaData.isIncoming())
		{
			swiftBlock2 = new SwiftBlock2Input(metaData.getMsgType(),
					(metaData.getReceiverAddress() != null) ? metaData.getReceiverAddress() : "XXXXXXXXXXXX",
					(metaData.getMessagePriority() != null) ? metaData.getMessagePriority() : "N",
					metaData.getDeliveryMonitoring(),
					metaData.getObsolescencePeriod());
		}
		else
		{
			swiftBlock2 = new SwiftBlock2Output(metaData.getMsgType(),
					(metaData.getSenderInputTime() != null) ? metaData.getSenderInputTime() : "1200",
					(metaData.getMIRDate() != null) ? metaData.getMIRDate() : curDate,
					(metaData.getMIRLogicalTerminal() != null) ? metaData.getMIRLogicalTerminal() : "XXXXXXXXXXXX",
					(metaData.getMIRSessionNumber() != null) ? metaData.getMIRSessionNumber() : "0000",
					(metaData.getMIRSequenceNumber() != null) ? metaData.getMIRSequenceNumber() : "000000",
					(metaData.getReceiverOutputDate() != null) ? metaData.getReceiverOutputDate() : curDate,
					(metaData.getReceiverOutpuTime() != null) ? metaData.getReceiverOutpuTime() : curTime,
					(metaData.getMessagePriority() != null) ? metaData.getMessagePriority() : "N");
		}

		return swiftBlock2;
	}
	
	protected SwiftBlock3 encodeSwiftBlock3(ClearThMessage message, SwiftBlock4 swift4)
	{
		SwiftMetaData metaData = (message instanceof ClearThSwiftMessage) ? ((ClearThSwiftMessage)message).getMetaData() : null;
		if (metaData == null || !metaData.hasBlock3Fields()) {
			return null;
		}
		
		Set<String> block3Names = metaData.getCurrentBlock3Names();
		List<Tag> tags = new ArrayList<>(block3Names.size());
		for (String currentBlock3Name : block3Names) {
			tags.add(new Tag(currentBlock3Name, metaData.getBlock3Tag(currentBlock3Name)));
		}

		return new SwiftBlock3(tags);
	}
	
	protected SwiftBlock4 encodeSwiftBlock4(ClearThMessage message) throws EncodeException
	{
		SwiftBlock4 b4 = new SwiftBlock4();
		SwiftMessageDesc msgDesc = findMessageDescEncode(message);
		List<Tag> bodyTags = createBody(message, msgDesc);
		b4.setTags(bodyTags);
		return b4;
		
	}
	
	protected SwiftBlock5 encodeSwiftBlock5(ClearThMessage message)
	{
		SwiftMetaData smd = (message instanceof ClearThSwiftMessage) ? 
				((ClearThSwiftMessage)message).getMetaData() : emptySwiftMetaData;
		
		SwiftBlock5 b5 = new SwiftBlock5();
		List<Tag> endTags = new ArrayList<Tag>();
		endTags.add(new Tag("MAC:" + (smd.getMac() == null ? "12345678" : smd.getMac())));
		endTags.add(new Tag("CHK:" + (smd.getChk() == null ? "123456789ABC" : smd.getChk())));
		b5.setTags(endTags);
		return b5;
	}


	@Override
	public String encode(ClearThMessage<?> message) throws  EncodeException
	{
		logger.trace("Message encoding started");

		IConversionService srv = new ConversionService();

		logger.trace("Getting message parameters");
		
		logger.trace("Creating SWIFT message");
		SwiftMessage swiftMsg = new SwiftMessage();
		
		swiftMsg.setBlock1(encodeSwiftBlock1(message));
		swiftMsg.setBlock2(encodeSwiftBlock2(message));
		SwiftBlock4 swift4 = encodeSwiftBlock4(message);
		swiftMsg.setBlock3(encodeSwiftBlock3(message, swift4));
		swiftMsg.setBlock4(swift4);
		SwiftMetaData smd = (message instanceof ClearThSwiftMessage) ?
				((ClearThSwiftMessage)message).getMetaData() : emptySwiftMetaData;
		if (smd.isBlock5())
			swiftMsg.setBlock5(encodeSwiftBlock5(message));
		return srv.getFIN(swiftMsg);
	}
	
	
	private List<Tag> createBody(ClearThMessage message, SwiftMessageDesc messageDesc) throws EncodeException
	{
		List<Tag> result = new ArrayList<Tag>();
		for (SwiftFieldDesc fd : messageDesc.getFieldDesc())
		{
			List<Tag> encoded = encodeField(fd, message);
			if (encoded!=null)
				result.addAll(encoded);
		}
		return result;
	}
	
	private List<Tag> encodeField(SwiftFieldDesc fd, ClearThMessage<?> message) throws EncodeException
	{
		if (fd.getTag() != null)  //Simple fields are in tag=value format
			return encodeSimpleField(fd, message);
		else
		{
			if (fd.isRepeat())
			{
				List<Tag> result = null;
				for (ClearThMessage subMsg : message.getSubMessages(fd.getName()))
				{
					List<Tag> subResult = encodeContainerField(fd, subMsg);
					if (subResult != null)
					{
						if (result == null)
							result = new ArrayList<Tag>();
						result.addAll(subResult);
					}
				}
				return result;
			}
			
			return encodeContainerField(fd, message);
		}
	}
	
	protected List<Tag> encodeSimpleField(SwiftFieldDesc fd, ClearThMessage message) throws EncodeException
	{
		String fieldName = fd.getName();
		String fieldContent = (fd.getGenerate() != null) ? generateValue(fd.getGenerate()) : message.getField(fieldName);

		if (fd.getDefault() != null && !isValuePresent(fieldContent))
		{
				fieldContent = fd.getDefault();
		}

		if (!isValuePresent(fieldContent))
		{
			if (fd.isMandatory())
				throw new EncodeException("Mandatory field " + fieldName + " not found in message");
			else
			{
				logger.trace("Optional field " + fieldName + " not found in message");
				return null;
			}
		}
		
		if (isSpecialValue(fieldContent))
			fieldContent = convertSpecialValue(fieldContent);

		if (fd.getSubvalue() != null)
			fieldContent = appendEncodedSubValues(fieldContent, fd, message);

		if (fd.getValuePrefix() != null)
			fieldContent = fd.getValuePrefix() + fieldContent;		
		
		String value;
		if (fd.isFullValue() || (fd.getQualifier() == null))
			value = fieldContent;
		else
		{
			String separator = fd.getSeparator() == null ? DEFAULT_SEPARATOR : fd.getSeparator();
			value = ":" + fd.getQualifier() + separator + fieldContent;
		}
		
		List<Tag> result = new ArrayList<Tag>();
		result.add(new Tag(fd.getTag(), value));
		return result;
	}
	
	protected boolean isValuePresent(String value)
	{
		if (value == null)
			return false;
		
		if (value.isEmpty())
			return !ignoreEmptyValues();
		
		return true;
	}
	
	protected boolean isSpecialValue(String value)
	{
		return SpecialValue.isSpecialValue(value);
	}
	
	protected String convertSpecialValue(String value)
	{
		return SpecialValue.convert(value);
	}
	
	protected String appendEncodedSubValues(String fieldContent, SwiftFieldDesc fd, ClearThMessage message)
		throws EncodeException
	{
		String[] subValuesNames = fd.getSubvalue().split("\\|"),
				subQualifiers;
		if (fd.getSubqualifier() != null)
			subQualifiers = fd.getSubqualifier().split("\\|");
		else
			subQualifiers = null;

		for (int i = 0; i < subValuesNames.length; i++)
		{
			String subvalue = message.getField(subValuesNames[i]);
			if (isValuePresent(subvalue))
			{
				if (isSpecialValue(subvalue))
					subvalue = convertSpecialValue(subvalue);
				
				fieldContent += EOL;
				if ((subQualifiers != null) && (i < subQualifiers.length))
				{
					String separator;
					if (fd.getSeparator() != null)
						separator = fd.getSeparator();
					else
						separator = DEFAULT_SEPARATOR;
					fieldContent += subQualifiers[i] + separator;
				}

				//Dividing long string with line feeds if needed
				if ((fd.getDivideSubvalueBy()>0) && (subvalue.length() > fd.getDivideSubvalueBy()))
				{
					int divBy = fd.getDivideSubvalueBy();
					StringBuilder sb = new StringBuilder();
					while (subvalue.length() > divBy)
					{
						if (sb.length()>0)
							sb.append(EOL);
						sb.append(subvalue.substring(0, divBy));
						subvalue = subvalue.substring(divBy);
					}
					subvalue = sb.toString()+EOL+subvalue; // Don't forget the tail!
				}
				fieldContent += subvalue;
			}
		}
		return fieldContent;
	}
	
	private List<Tag> encodeContainerField(SwiftFieldDesc fd, ClearThMessage message) throws EncodeException
	{
		List<Tag> result = null;
		for (SwiftFieldDesc subFD : fd.getFieldDesc())
		{
			List<Tag> subResult = encodeField(subFD, message);
			if (subResult != null)
			{
				if (result == null)
					result = new ArrayList<Tag>();
				result.addAll(subResult);
			}
		}
		
		if ((fd.getSequenceType() != null) && (result != null))
		{
			result.add(0, new Tag("16R", fd.getSequenceType()));
			result.add(new Tag("16S", fd.getSequenceType()));
		}
		return result;
	}

	protected SwiftMessageDesc findMessageDescEncode(ClearThMessage<?> message) throws EncodeException
	{
		SwiftMetaData smd;
		String msgType;
		if(message instanceof ClearThSwiftMessage)
		{
			smd = ((ClearThSwiftMessage)message).getMetaData();
			msgType = ((ClearThSwiftMessage)message).getDictionaryMsgType();
			if (msgType == null)
			{
				msgType = smd.getMsgType();
				if (msgType == null)  //In some cases MsgType is stored as field, e.g. after encoding with ScriptConverter
					msgType = message.getField(ClearThSwiftMessage.MSGTYPE);
			}
		}
		else
		{
			smd = emptySwiftMetaData;
			msgType = message.getField(ClearThSwiftMessage.MSGTYPE);
		}

		boolean isInbound = smd.isIncoming();
		SwiftMessageDesc md = dictionary.getMessageDesc(msgType);
		if(msgDescFits(md, isInbound))
			return md;

		throw new EncodeException(msgDescDoesNotFitError(msgType) + " ("
				+ (isInbound ? "INBOUND" : "OUTBOUND") + ")");
	}

	protected String generateExcMsg(String msgType, SwiftMetaData smd, Boolean foundByType, boolean fitsPassed)
	{
		String template = MSG_DESC_DOES_NOT_FIT;
		if(!fitsPassed)
			template += " Expected: " + (smd.isIncoming() ? "INBOUND" : "OUTBOUND");

		String foundBy = foundByType ? "msgType" : "typeConditions";
		String notFitBy = fitsPassed ? "conditions" : "direction";

		return String.format(template, foundBy, msgType, notFitBy);
	}
	
	protected SwiftMessageDesc findMessageDescDecode(ClearThSwiftMessage msg, SwiftBlock4 block4, String msgType) throws DecodeException
	{
		if(msgType == null)
			msgType = getMessageType(msg.getMetaData(), block4);
		SwiftMetaData swiftMetaData = msg.getMetaData();
		boolean isInbound = swiftMetaData.isIncoming();

		SwiftMessageDesc md = dictionary.getMessageDesc(msgType);
		boolean fitsPassed = false;
		if(md != null)
		{
			if (msgDescFits(md, isInbound))
			{
				fitsPassed = true;
				if(messageValidator.isValid(msg.getEncodedMessage(), dictionary.getConditions(msgType)))
					return md;
			}

			throw new DecodeException(generateExcMsg(msgType, swiftMetaData, true, fitsPassed));
		}
		else
		{
			for(SwiftMessageDesc smd : dictionary.getMessageDescMap().values())
			{
				if (messageValidator.isValid(msg.getEncodedMessage(), dictionary.getTypeConditions(smd.getType())))
				{
					if(msgDescFits(smd, isInbound))
					{
						fitsPassed = true;
						if(messageValidator.isValid(msg.getEncodedMessage(), dictionary.getConditions(smd.getType())))
							return smd;
					}

					throw new DecodeException(generateExcMsg(smd.getType(), swiftMetaData, false, fitsPassed));
				}
			}

			throw new DecodeException(MSG_DESC_NOT_FOUND_IN_DICTIONARY);
		}
	}

	public boolean msgDescFits(SwiftMessageDesc msgDesc, boolean isIncoming)
	{
		return ((msgDesc.isInbound() == null || msgDesc.isInbound() == isIncoming));
	}
	
	protected String getMessageType(SwiftMetaData metaData, @SuppressWarnings("unused") SwiftBlock4 block4) throws DecodeException
	{
		return metaData.getMsgType();
	}

	protected void decodeSwiftBlock1(SwiftBlock1 swiftBlock, ClearThSwiftMessage swiftMessage)
	{
		SwiftMetaData metaData = swiftMessage.getMetaData();
		metaData.setApplicationId(swiftBlock.getApplicationId());
		metaData.setServiceId(swiftBlock.getServiceId());
		metaData.setLogicalTerminal(swiftBlock.getLogicalTerminal());
		metaData.setSessionNumber(swiftBlock.getSessionNumber());
		metaData.setSequenceNumber(swiftBlock.getSequenceNumber());
	}

	protected void decodeSwiftBlock2(SwiftBlock2 swiftBlock, ClearThSwiftMessage swiftMessage) throws  DecodeException
	{
		SwiftMetaData metaData = swiftMessage.getMetaData();
		if (swiftBlock instanceof SwiftBlock2Input)
		{
			SwiftBlock2Input sw2input = (SwiftBlock2Input) swiftBlock;
			metaData.setIncoming(true);
			metaData.setMsgType(sw2input.getMessageType());
			metaData.setReceiverAddress(sw2input.getReceiverAddress());
			metaData.setMessagePriority(sw2input.getMessagePriority());
			metaData.setDeliveryMonitoring(sw2input.getDeliveryMonitoring());
			metaData.setObsolescencePeriod(sw2input.getObsolescencePeriod());
		}
		else if (swiftBlock instanceof SwiftBlock2Output)
		{
			SwiftBlock2Output sw2output = (SwiftBlock2Output) swiftBlock;
			metaData.setIncoming(false);
			metaData.setMsgType(sw2output.getMessageType());
			metaData.setSenderInputTime(sw2output.getSenderInputTime());
			metaData.setMIRDate(sw2output.getMIRDate());
			metaData.setMIRLogicalTerminal(sw2output.getMIRLogicalTerminal());
			metaData.setMIRSessionNumber(sw2output.getMIRSessionNumber());
			metaData.setMIRSequenceNumber(sw2output.getMIRSequenceNumber());
			metaData.setReceiverOutputDate(sw2output.getReceiverOutputDate());
			metaData.setReceiverOutpuTime(sw2output.getReceiverOutputTime());
			metaData.setMessagePriority(sw2output.getMessagePriority());
		}
		else
			throw new DecodeException("Unknown SwiftBlock2 implementation");
	}

	protected void decodeSwiftBlock3(SwiftBlock3 swiftBlock, ClearThSwiftMessage swiftMessage)
	{
		List<Tag> tags;
		if (swiftBlock != null && (tags = swiftBlock.getTags()) != null) {
			for (Tag block3tag : tags) {
				swiftMessage.getMetaData().addBlock3Tag(block3tag.getName(), block3tag.getValue());
			}
		}
	}

	protected void decodeSwiftBlock4(SwiftBlock4 swiftBlock, ClearThSwiftMessage swiftMessage, String msgType) throws DecodeException
	{
		SwiftMessageDesc msgDesc = findMessageDescDecode(swiftMessage, swiftBlock, msgType);
		messageValidator.validate(swiftMessage.getEncodedMessage(), dictionary.getConditions(msgDesc.getType()));
		int tagIndex = 0;
		for (SwiftFieldDesc fd : msgDesc.getFieldDesc())
			tagIndex = parseField(fd, swiftBlock, swiftMessage, tagIndex);
	}

	protected void decodeSwiftBlock5(SwiftBlock5 swiftBlock, ClearThSwiftMessage swiftMessage)
	{
		swiftMessage.getMetaData().setBlock5(swiftBlock != null);
		if (swiftBlock != null)
		{
			Tag tag = swiftBlock.getTagByName("MAC");
			if (tag != null)
				swiftMessage.getMetaData().setMac(tag.getValue());
			tag = swiftBlock.getTagByName("CHK");
			if (tag != null)
				swiftMessage.getMetaData().setChk(tag.getValue());
		}
	}
	
	
	@Override
	public ClearThSwiftMessage decode(String message) throws DecodeException
	{
		return decode(message, null);
	}

	@Override
	public ClearThSwiftMessage decode(String message, String messageType) throws DecodeException
	{
		logger.trace("Decoding SWIFT message started");
		
		SwiftMessage swiftMessage;
		try
		{
			swiftMessage = new SwiftParser().parse(message);
		}
		catch (IOException e)
		{
			String msg = "Error while parsing message";
			logger.warn(msg, e);
			throw new DecodeException(msg, e);
		}

		ClearThSwiftMessage result = new ClearThSwiftMessage();
		result.setEncodedMessage(message);

		if (swiftMessage.getBlock1() == null || swiftMessage.getBlock2() == null || swiftMessage.getBlock4() == null)
			throw new DecodeException("Swift message must contain 1, 2 and 4 blocks. Message: \n " + message);

		this.decodeSwiftBlock1(swiftMessage.getBlock1(), result);
		this.decodeSwiftBlock2(swiftMessage.getBlock2(), result);
		this.decodeSwiftBlock3(swiftMessage.getBlock3(), result);
		this.decodeSwiftBlock4(swiftMessage.getBlock4(), result, messageType);
		this.decodeSwiftBlock5(swiftMessage.getBlock5(), result);

		logger.trace("Decoding SWIFT message finished");
		return result;
	}
	
	protected int parseField(SwiftFieldDesc fd, SwiftBlock4 block4, ClearThSwiftMessage parsedMessage, int tagIndex) throws DecodeException
	{
		if (fd.getTag() != null)
			return parseSimpleField(fd, block4, parsedMessage, tagIndex);
		else
			return parseContainerField(fd, block4, parsedMessage, tagIndex);
	}
	
	private int tagNotFound(SwiftFieldDesc fd, int tagIndex) throws DecodeException
	{
		if (fd.isMandatory())
		{
			String msg = "Mandatory tag " + fd.getTag() + " (field " + fd.getName() + ") not found in message";
			logger.debug(msg);
			throw new DecodeException(msg);
		}
		logger.trace("Optional tag {} (field {}) not found in message", fd.getTag(), fd.getName());
		return tagIndex;
	}
	
	private int parseSimpleField(SwiftFieldDesc fd, SwiftBlock4 block4, ClearThSwiftMessage parsedMessage, int tagIndex) throws DecodeException
	{
		logger.trace("Handling field '" + fd.getName() + "'");
		Tag tag = null;
		if (tagIndex < block4.size())
		{
			tag = block4.getTag(tagIndex);
			if (!fd.getTag().equals(tag.getName()))
				tag = null;
		}
		
		if (tag == null)
			return tagNotFound(fd, tagIndex);
		
		
		String fieldValue = null,
				withQualifier = "",
				withPrefix = null;
		if (fd.getQualifier() != null)
		{
			if (tag.getValue().startsWith(":"))
			{
				String separator = fd.getSeparator() == null ? DEFAULT_SEPARATOR : fd.getSeparator();
				int sepIndex = tag.getValue().indexOf(separator);
				if (sepIndex > -1)
				{
					String qualifier = tag.getValue().substring(1, sepIndex);
					withQualifier = qualifier+separator;
					if (qualifier.equals(fd.getQualifier()))
						fieldValue = tag.getValue().substring(tag.getValue().indexOf(separator) + separator.length());
				}
			}
		}
		else
			fieldValue = tag.getValue();

		if (fieldValue == null)
			return tagNotFound(fd, tagIndex);
		
		
		Map<String, String> subValues = new LinkedHashMap<String, String>();
		if (fd.getSubvalue() != null)
		{
			String[] svNames = fd.getSubvalue().split("\\|"), 
					values = fieldValue.split(EOL, svNames.length + 1),
					qualifs = null;
			String separator = fd.getSeparator() == null ? DEFAULT_SEPARATOR : fd.getSeparator();
			if (fd.getSubqualifier() != null)
				qualifs = fd.getSubqualifier().split("\\|", svNames.length + 1);
			fieldValue = values[0];
			for (int i = 1; i<values.length; i++)
			{
				String sv = values[i];
				if (qualifs != null)
				{
					String sq = sv.substring(0, sv.indexOf(separator));
					if (sq.equals(qualifs[i-1]))
						sv = sv.substring(sv.indexOf(separator) + separator.length());
				}
				subValues.put(svNames[i-1], sv);
			}
		}

		if (fd.getValuePrefix() != null)
		{
			Pattern pat = Pattern.compile(fd.getValuePrefix());
			Matcher mat = pat.matcher(fieldValue);
			
			if (mat.find() && mat.start() == 0)
			{
				withPrefix = fieldValue;
				fieldValue = fieldValue.substring(mat.group().length());
			}
		}
		
		if (fd.getGetFirst() != null)
		{
			String[] dividedResult;
			if (fd.getDivideBy() != null)
			{
				int pos = fieldValue.indexOf(fd.getDivideBy());
				if (pos > -1)
					dividedResult = new String[] { fieldValue.substring(0, pos + 1), fieldValue.substring(pos + fd.getDivideBy().length()) };
				else
					dividedResult = null;
			}
			else
				dividedResult = null;
			
			if (dividedResult != null)
			{
				fieldValue = "";
				for (String div : dividedResult)
				{
					if (fieldValue.length() > 0)
						fieldValue += fd.getDivideBy();
					fieldValue += div.substring(0, Math.min(fd.getGetFirst(), div.length()));
				}
			}
			else
				fieldValue = fieldValue.substring(0, Math.min(fd.getGetFirst(), fieldValue.length()));
		}
		
		if (fd.isFullValue())
		{
			if (withPrefix == null)
				fieldValue = withQualifier+fieldValue;
			else
				fieldValue = withQualifier+withPrefix;
		}
		parsedMessage.addField(fd.getName(), fieldValue);
		
		for (String sv : subValues.keySet())
			parsedMessage.addField(sv, subValues.get(sv));
		
		tagIndex++;
		return tagIndex;
	}
	
	private boolean checkTag(String tagName, String tagValue, SwiftBlock4 block4, int tagIndex)
	{
		Tag tag = null;
		if (tagIndex<block4.size())
		{
			tag = block4.getTag(tagIndex);
			if ((!tagName.equals(tag.getName())) || (!tagValue.equals(tag.getValue())))
				tag = null;
		}
		return tag != null;
	}
	
	private int parseContainerField(SwiftFieldDesc fd, SwiftBlock4 block4, ClearThSwiftMessage parsedMessage, int tagIndex) throws DecodeException
	{
		logger.trace("Handling container field '" + fd.getName() + "'");
		int iteration = 1;
		do
		{
			if (fd.getSequenceType() != null)
			{
				if (!checkTag("16R", fd.getSequenceType(), block4, tagIndex))
				{
					if (iteration < 2)
					{
						if (fd.isMandatory())
							throw new DecodeException("Mandatory block " + fd.getSequenceType() + " (field " + fd.getName() + ") not found in message");
						logger.trace("Optional block " + fd.getSequenceType() + " (field " + fd.getName() + ") not found in message");
					}
					return tagIndex;
				}
				tagIndex++;
			}
			
			int initTagIndex = tagIndex;
			
			ClearThSwiftMessage msg = new ClearThSwiftMessage();
			int newTagIndex = tagIndex;
			try
			{
				for (SwiftFieldDesc subFD : fd.getFieldDesc())
					newTagIndex = parseField(subFD, block4, msg, newTagIndex);
				tagIndex = newTagIndex;
			}
			catch (DecodeException e)
			{
				if (iteration < 2)
					throw e;
				msg = new ClearThSwiftMessage();  // Contents of clean message won't be added to parsedMessage, tagIndex remain unchanged
			}
			
			if (fd.getSequenceType() != null)
			{
				if (tagIndex != initTagIndex) {
					do
						if (!checkTag("16S", fd.getSequenceType(), block4, tagIndex))
							tagIndex++;
						else
							break;
					while (tagIndex < block4.size());
					if (tagIndex >= block4.size())
						throw new DecodeException("Incorrect message: block " + fd.getSequenceType() + " is not closed with tag 16S in expected place");
					tagIndex++;
				} else
					tagIndex--;
			}

			
			if ((msg.getFieldNames().size() == 0) && (msg.getSubMessages().size() == 0))
				return tagIndex;
			
			
			ClearThSwiftMessage destMsg;
			if (fd.isRepeat())
			{
				destMsg = new ClearThSwiftMessage();
				destMsg.addField(ClearThSwiftMessage.SUBMSGTYPE, fd.getName());
				parsedMessage.addSubMessage(destMsg);
			}
			else
				destMsg = parsedMessage;
			
			for (String fieldName : msg.getFieldNames())
				destMsg.addField(fieldName, msg.getField(fieldName));
			for (ClearThSwiftMessage subMsg : msg.getSubMessages())
				destMsg.addSubMessage(subMsg);
			
			
			iteration++;
		}
		while (fd.isRepeat());
		return tagIndex;
	}
	

	public String generateValue(int length) throws EncodeException
	{
		try
		{
			return generator.generateValue(length);
		}
		catch (NumberFormatException e)
		{
			throw new EncodeException("Error in generatable value length, check your dictionary", e);
		}
	}

	public String getLastGeneratedValue()
	{
		return generator.getLastGeneratedValue();
	}

	public SwiftDictionary getDictionary()
	{
		return this.dictionary;
	}
}
