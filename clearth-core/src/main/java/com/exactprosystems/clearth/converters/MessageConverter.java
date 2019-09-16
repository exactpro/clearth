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

package com.exactprosystems.clearth.converters;

import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.StringOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlMessageConverterConfig;

public abstract class MessageConverter extends Converter
{
	protected static final String IDFIELD = ActionGenerator.HEADER_DELIMITER + ActionGenerator.COLUMN_ID, 
			ACTIONFIELD = ActionGenerator.HEADER_DELIMITER + ActionGenerator.COLUMN_ACTION,
			HEAD_AND_VAL_DELIMITER = Utils.EOL;

	protected int idNumber = 1;
	protected String lastScriptHeader = null;

	public abstract String convert(String messageToConvert, XmlMessageConverterConfig config, ICodec codec) throws Exception;

	protected String deepFieldsToStrings(ClearThMessage<?> message, String id, String rgActionName, XmlMessageConverterConfig config)
	{
		if (message.getSubMessages().size() == 0) {
			final Pair<String, String>
					startHeadAndVal = new Pair<String, String>(
							config.getScriptHeaderStrings(),
							config.getScriptValueStrings().replaceAll(ACTIONFIELD, rgActionName).replaceAll(IDFIELD, id)),
					endHeadAndVal  = fieldsToStrings(message, config);
			return fieldsToScript(startHeadAndVal, endHeadAndVal);
		}

		final StringBuilder repGroups = new StringBuilder(),
				result = new StringBuilder();
		int idCounter = 1;
		for (ClearThMessage<?> subMsg : message.getSubMessages())
		{
			String subId = id + "_" + idCounter;
			result.append(deepFieldsToStrings(subMsg, subId, rgActionName, config)).append(Utils.EOL);

			if (repGroups.length() > 0)
				repGroups.append(",");
			repGroups.append(subId);

			idCounter++;
		}

		final Pair<String, String> startHeadAndVals = new Pair<String, String>(
				config.getScriptHeaderStrings(),
				config.getScriptValueStrings().replaceAll(ACTIONFIELD, rgActionName).replaceAll(IDFIELD, id));

		final Pair<String, String> endHeadAndVals = fieldsToStrings(message, config);
		endHeadAndVals.setFirst("#" + MessageAction.REPEATINGGROUPS + "," + endHeadAndVals.getFirst());
		endHeadAndVals.setSecond("\"" + repGroups + "\"," + endHeadAndVals.getSecond());

		result.append(Utils.EOL).append(fieldsToScript(startHeadAndVals, endHeadAndVals));
		return result.toString();
	}

	protected String fieldsToScript(Pair<String, String>... headValPairs) {
		String fullHeader = "", fullValues = "",
			resultHeader = "";

		if (headValPairs != null && headValPairs.length != 0) {
			for (Pair<String, String> headValPair : headValPairs) {
				fullHeader = concat(fullHeader, headValPair.getFirst());
				fullValues = concat(fullValues, headValPair.getSecond());
			}

			if (!headerIsDuplicates(fullHeader)) {
				resultHeader = fullHeader + HEAD_AND_VAL_DELIMITER;
			}
		}

		return resultHeader + fullValues;
	}

	/**
	 * Returns false if header from param equals to the last checked header. Memorises last checked header.
	 * @param header
	 * @return
	 */
	protected boolean headerIsDuplicates(String header) {
		if (lastScriptHeader == null || !lastScriptHeader.equals(header)) {
			lastScriptHeader = header;
			return false;
		}
		return true;
	}

	/**
	 * Creates a pair of Strings containing message's fields as a script. First element of pair is header, second is values.
	 * @param message
	 * @param config
	 * @return
	 */
	protected static Pair<String, String> fieldsToStrings(ClearThMessage<?> message, XmlMessageConverterConfig config) {
		StringBuilder header = new StringBuilder(),
				values = new StringBuilder();
		for (String fieldName : message.getFieldNames())
		{
			if (header.length() > 0)
			{
				header.append(",");
				values.append(",");
			}
			header.append(StringOperationUtils.quote("#" + fieldName));

			String value = message.getField(fieldName);
			if (value.isEmpty())
				value = config.getEmptyValue();
			values.append(StringOperationUtils.quote(value));
		}

		return new Pair<String, String>(header.toString(), values.toString());
	}
	
	protected static String concat(String str1, String str2)
	{
		if (str2 == null)
			return str1;
		
		if (str1.length() > 0)
			str1 += ",";
		
		return str1 + str2;
	}

	public void resetId()
	{
		idNumber = 1;
	}
}
