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

import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//TODO Implement MetaData for the abstract ClearThMessage.
//     Add method getMetaDataValue(key) to the ClearThMessage.
//     => get rid of blacklistParams & whitelistParams.
public class SwiftMetaData
{
	protected HashMap<String, String> metadata;
	
	public static final String APPLICATION_ID = "ApplicationId",
			SERVICE_ID = "ServiceId",
			LOGICAL_TERMINAL = "LogicalTerminal",
			SESSION_NUMBER = "SessionNumber",
			SEQUENCE_NUMBER = "SequenceNumber",
			
			MESSAGE_PRIORITY = "MessagePriority",
			RECEIVER_ADDRESS = "ReceiverAddress",
			DELIVERY_MONITORING = "DeliveryMonitoring",
			OBSOLESCENCE_PERIOD = "ObsolescencePeriod",
			SENDER_INPUT_TIME = "SenderInputTime",
			MIR_DATE = "MIRDate",
			MIR_LT = "MIRLogicalTerminal",
			MIR_SESSION_NUMBER = "MIRSessionNumber",
			MIR_SEQUENCE_NUMBER = "MIRSequenceNumber",
			RECEIVER_OUTPUT_DATE = "ReceiverOutputDate",
			RECEIVER_OUTPUT_TIME = "ReceiverOutputTime",
			TAG108 = "Tag108",
			
			MAC = "Mac",
			CHK = "Chk";

	/**
	 * Params which should be removed from msgFields
	 */
	protected final Set<String> blacklistParams = new HashSet<String>()
	{{
		add(SwiftMetaData.APPLICATION_ID);
		add(SwiftMetaData.SERVICE_ID);
		add(SwiftMetaData.LOGICAL_TERMINAL);
		add(SwiftMetaData.SESSION_NUMBER);
		add(SwiftMetaData.SEQUENCE_NUMBER);

		add(SwiftMetaData.MESSAGE_PRIORITY);
		add(SwiftMetaData.RECEIVER_ADDRESS);
		add(SwiftMetaData.DELIVERY_MONITORING);
		add(SwiftMetaData.OBSOLESCENCE_PERIOD);
		add(SwiftMetaData.SENDER_INPUT_TIME);
		add(SwiftMetaData.MIR_DATE);
		add(SwiftMetaData.MIR_LT);
		add(SwiftMetaData.MIR_SESSION_NUMBER);
		add(SwiftMetaData.MIR_SEQUENCE_NUMBER);
		add(SwiftMetaData.RECEIVER_OUTPUT_DATE);
		add(SwiftMetaData.RECEIVER_OUTPUT_TIME);

		add(SwiftMetaData.TAG108);

		add(SwiftMetaData.CHK);
		add(SwiftMetaData.MAC);
	}};

	/**
	 * Params which shouldn't be removed from msgFields
	 */
	protected final Set<String> whitelistParams = new HashSet<String>()
	{{
		add(ClearThSwiftMessage.MSGTYPE);
		add(ClearThSwiftMessage.INCOMINGMESSAGE);
		add(ClearThSwiftMessage.USEBLOCK3);
		add(ClearThSwiftMessage.ADDBLOCK5);
	}};
	
	
	public SwiftMetaData()
	{
		this.metadata = new HashMap<String, String>();
	}
	
	public SwiftMetaData(Map<String, String> messageFields)
	{
		this();
		if (messageFields != null)
		{
			for (String param : getWhitelistParams())
				addMetaDataValue(param, messageFields.get(param));

			for (String param : getBlacklistParams())
				addMetaDataValue(param, messageFields.remove(param));
		}
	}


	public SwiftMetaData(SwiftMetaData smd)
	{
		this.metadata = new HashMap<String, String>(smd.metadata);
	}

	
	public boolean isIncoming()
	{
		String im = this.metadata.get(ClearThSwiftMessage.INCOMINGMESSAGE);
		return (im != null) && (im.equalsIgnoreCase("true"));
	}
	
	public void setIncoming(boolean incoming)
	{
		this.metadata.put(ClearThSwiftMessage.INCOMINGMESSAGE, String.valueOf(incoming));
	}
	
	
	public boolean isBlock3()
	{
		String ub3 = this.metadata.get(ClearThSwiftMessage.USEBLOCK3);
		return (ub3 != null) && (ub3.equalsIgnoreCase("true"));
	}
	
	public void setBlock3(boolean block3)
	{
		this.metadata.put(ClearThSwiftMessage.USEBLOCK3, String.valueOf(block3));
	}
	
	
	public boolean isBlock5()
	{
		String ab5 = this.metadata.get(ClearThSwiftMessage.ADDBLOCK5);
		return (ab5 == null) || (ab5.equalsIgnoreCase("true"));
	}
	
	public void setBlock5(boolean block5)
	{
		this.metadata.put(ClearThSwiftMessage.ADDBLOCK5, String.valueOf(block5));
	}
	
	
	public String getApplicationId()
	{
		return this.metadata.get(APPLICATION_ID);
	}
	
	public void setApplicationId(String applicationId)
	{
		this.metadata.put(APPLICATION_ID, applicationId);
	}
	
	
	public String getServiceId()
	{
		return this.metadata.get(SERVICE_ID);
	}
	
	public void setServiceId(String serviceId)
	{
		this.metadata.put(SERVICE_ID, serviceId);
	}
	
	
	public String getLogicalTerminal()
	{
		return this.metadata.get(LOGICAL_TERMINAL);
	}
	
	public void setLogicalTerminal(String logicalTerminal)
	{
		this.metadata.put(LOGICAL_TERMINAL, logicalTerminal);
	}
	
	
	public String getSessionNumber()
	{
		return this.metadata.get(SESSION_NUMBER);
	}
	
	public void setSessionNumber(String sessionNumber)
	{
		this.metadata.put(SESSION_NUMBER, sessionNumber);
	}
	
	
	public String getSequenceNumber()
	{
		return this.metadata.get(SEQUENCE_NUMBER);
	}
	
	public void setSequenceNumber(String sequenceNumber)
	{
		this.metadata.put(SEQUENCE_NUMBER, sequenceNumber);
	}
	
	
	public void setMessagePriority(String messagePriority)
	{
		this.metadata.put(MESSAGE_PRIORITY, messagePriority);
	}
	
	public String getMessagePriority()
	{
		return this.metadata.get(MESSAGE_PRIORITY);
	}
	
	
	public void setReceiverAddress(String receiverAddress)
	{
		this.metadata.put(RECEIVER_ADDRESS, receiverAddress);
	}
	
	public String getReceiverAddress()
	{
		return this.metadata.get(RECEIVER_ADDRESS);
	}
	
	
	public void setDeliveryMonitoring(String deliveryMonitoring)
	{
		this.metadata.put(DELIVERY_MONITORING, deliveryMonitoring);
	}
	
	public String getDeliveryMonitoring()
	{
		return this.metadata.get(DELIVERY_MONITORING);
	}
	
	
	public void setObsolescencePeriod(String obsolescencePeriod)
	{
		this.metadata.put(OBSOLESCENCE_PERIOD, obsolescencePeriod);
	}
	
	public String getObsolescencePeriod()
	{
		return this.metadata.get(OBSOLESCENCE_PERIOD);
	}
	
	
	public void setSenderInputTime(String senderInputTime)
	{
		this.metadata.put(SENDER_INPUT_TIME, senderInputTime);
	}
	
	public String getSenderInputTime()
	{
		return this.metadata.get(SENDER_INPUT_TIME);
	}
	
	
	public void setMIRDate(String mirDate)
	{
		this.metadata.put(MIR_DATE, mirDate);
	}
	
	public String getMIRDate()
	{
		return this.metadata.get(MIR_DATE);
	}
	
	
	public void setMIRLogicalTerminal(String mirLt)
	{
		this.metadata.put(MIR_LT, mirLt);
	}
	
	public String getMIRLogicalTerminal()
	{
		return this.metadata.get(MIR_LT);
	}
	
	
	public void setMIRSessionNumber(String mirSessionNumber)
	{
		this.metadata.put(MIR_SESSION_NUMBER, mirSessionNumber);
	}
	
	public String getMIRSessionNumber()
	{
		return this.metadata.get(MIR_SESSION_NUMBER);
	}
	
	
	public void setMIRSequenceNumber(String mirSeqNum)
	{
		this.metadata.put(MIR_SEQUENCE_NUMBER, mirSeqNum);
	}
	
	public String getMIRSequenceNumber()
	{
		return this.metadata.get(MIR_SEQUENCE_NUMBER);
	}
	
	
	public void setReceiverOutputDate(String receiverOutputDate)
	{
		this.metadata.put(RECEIVER_OUTPUT_DATE, receiverOutputDate);
	}
	
	public String getReceiverOutputDate()
	{
		return this.metadata.get(RECEIVER_OUTPUT_DATE);
	}
	
	
	public void setReceiverOutpuTime(String receiverOutpuTime)
	{
		this.metadata.put(RECEIVER_OUTPUT_TIME, receiverOutpuTime);
	}
	
	public String getReceiverOutpuTime()
	{
		return this.metadata.get(RECEIVER_OUTPUT_TIME);
	}
	
	public String getTag108()
	{
		return this.metadata.get(TAG108);
	}
	
	public void setTag108(String tag108)
	{
		this.metadata.put(TAG108, tag108);
	}
	
	public String getMac()
	{
		return this.metadata.get(MAC);
	}
	
	public void setMac(String mac)
	{
		this.metadata.put(MAC, mac);
	}
	
	
	public String getChk()
	{
		return this.metadata.get(CHK);
	}
	
	public void setChk(String chk)
	{
		this.metadata.put(CHK, chk);
	}
	
	
	public String getMsgType()
	{
		return this.metadata.get(ClearThSwiftMessage.MSGTYPE);
	}
	
	public void setMsgType(String msgType)
	{
		this.metadata.put(ClearThSwiftMessage.MSGTYPE, msgType);
	}

	public String getMetaDataValue(String key) {
		return this.metadata.get(key);
	}

	/**
	 * @return true if this value should be removed from msgFields (param not contains in whitelist)
	 */
	public boolean addMetaDataValue(String key, String value)
	{
		this.metadata.put(key, value);
		return checkNeedToRemove(key);
	}

	/**
	 * @return true if this value should be removed from msgFields (param not contains in whitelist)
	 */
	public boolean addIfNeeded(String key, String value)
	{
		if (isMetaDataValue(key))
			return addMetaDataValue(key, value);
		else
			return false;
	}

	protected boolean checkNeedToRemove(String key)
	{
		return !getWhitelistParams().contains(key);
	}

	public boolean isMetaDataValue(String key)
	{
		return getBlacklistParams().contains(key) || getWhitelistParams().contains(key);
	}

	public void updateMetaData(SwiftMetaData smd) {
		if (smd == null)
			return;
		this.metadata.putAll(smd.metadata);
	}

	protected Set<String> getBlacklistParams()
	{
		return blacklistParams;
	}

	protected Set<String> getWhitelistParams()
	{
		return whitelistParams;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !(obj instanceof SwiftMetaData)) {
			return false;
		}

		SwiftMetaData other = (SwiftMetaData) obj;
		EqualsBuilder equalsBuilder = new EqualsBuilder();
//		Value can be default (null) then can be rewrite real value, but they aren't equal
		equalsBuilder.append(this.metadata, other.metadata);
		return equalsBuilder.isEquals();
	}

	@Override
	public String toString()
	{
		String mt = getMsgType();
		if (mt != null)
			return "MsgType: " + mt;
		return "";
	}
}
