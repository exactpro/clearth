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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.tools.DictionaryValidationResult;
import com.exactprosystems.clearth.tools.DictionaryValidatorError;
import com.exactprosystems.clearth.tools.DictionaryValidatorTool;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.beans.tree.ComparationNode;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

public class DictionaryValidatorToolBean extends ClearThBean
{
	DictionaryValidatorTool dictionaryValidatorTool;

	protected CodecsStorage codecs;
	protected Set<String> validationConfigs;
	
	protected String textToParse = "", decodedText = "", encodedText = "", validationDetails = "",
			textToParseFormat = "", currValidationConfig = "";
	
	protected boolean validatedSuccessfully = false;
	
	protected List<Integer> differencesIndexes = new ArrayList<Integer>();
	protected TreeNode comparationTree = new DefaultTreeNode();
	
	@PostConstruct
	public void init()
	{
		dictionaryValidatorTool = ClearThCore.getInstance().getToolsFactory().createDictionaryValidatorTool();
		
		codecs = ClearThCore.getInstance().getCodecs();
		
		currValidationConfig = DictionaryValidatorTool.getDefaultValidationConfig();
		
		validationConfigs = new HashSet<String>(DictionaryValidatorTool.getValidationConfigs());
		validationConfigs.remove(currValidationConfig);
		
		textToParseFormat = getTextToParseFormatDefault();
	}
	
	public void validateDictionary()
	{
		if (getLogger().isInfoEnabled())
			getLogger().info("Parsing text: " + textToParse + Utils.EOL + "Validating format: " + textToParseFormat);
		
		DictionaryValidationResult result = null;
		List<DictionaryValidatorError> errors = new ArrayList<>();
		try
		{
			result = dictionaryValidatorTool.validateDictionary(textToParse, textToParseFormat, currValidationConfig, errors);
		}
		catch (Exception e)
		{
			handleException(textToParseFormat+": could not validate dictionary", e);
			handleErrors(errors);
			return;
		}
		
		validatedSuccessfully = result.isValidatedSuccessfully();
		decodedText = result.getOriginalText();
		encodedText = result.getEncodedText();
		validationDetails = result.getValidationDetails();
		handleErrors(errors);
		createComparisonTree(result);
	}
	
	protected void handleErrors(List<DictionaryValidatorError> errors)
	{
		for (DictionaryValidatorError e : errors)
			MessageUtils.addErrorMessage(e.getText(), ExceptionUtils.getDetailedMessage(e.getError()));
	}
	
	protected void createComparisonTree(DictionaryValidationResult result)
	{	
		comparationTree = new DefaultTreeNode();
		String[] encodedStrings = result.getEncodedText().split(Utils.EOL),
				originalStrings = null;
		
		String config = result.getValidationConfigName();
		switch (config)
		{
			case DictionaryValidatorTool.BY_CHARACTERS_CONFIG:
				originalStrings = result.getOriginalText().split(Utils.EOL);
				break;
			case DictionaryValidatorTool.NO_LINE_SEPARATION_CONFIG:
				originalStrings = formateStringByPattern(result.getOriginalText(), encodedStrings);
				break;
			default:
				return;
		}
		
		int count = Math.min(originalStrings.length, encodedStrings.length);
		String original, encoded;
		for (int i = 0; i < count; i++)
		{
			original = originalStrings[i];
			encoded = encodedStrings[i];
			new DefaultTreeNode(new ComparationNode(original, encoded, original.equals(encoded)), comparationTree);
		}
		
		// checking for extra strings in both arrays
		for (int i = count; i < originalStrings.length; i++)
		{
			new DefaultTreeNode(new ComparationNode(originalStrings[i], "", false), comparationTree);
		}
		for (int i = count; i < encodedStrings.length; i++)
		{
			new DefaultTreeNode(new ComparationNode("", encodedStrings[i], false), comparationTree);
		}
	}
	
	protected String[] formateStringByPattern(String original, String[] pattern)
	{
		List<String> originalStringsList = new ArrayList<String>();
		String originalWithoutFormatting = original.replace(Utils.EOL, "");
		
		int originalLength = originalWithoutFormatting.length(),
			patternStringCount = pattern.length,
			startIndex = 0,
			endIndex;
		
		for (int i = 0; i < patternStringCount && startIndex < originalLength; i++)
		{
			endIndex = Math.min(originalLength, startIndex + pattern[i].length());
			originalStringsList.add(originalWithoutFormatting.substring(startIndex, endIndex));
			startIndex = endIndex;
		}
		
		if (startIndex < originalLength)
			originalStringsList.add(original.substring(startIndex, originalLength));
		
		return originalStringsList.toArray(new String[originalStringsList.size()]);
	}
	
	protected void handleException(String message, Exception e)
	{
		getLogger().warn(message, e);
		MessageUtils.addErrorMessage(message, ExceptionUtils.getDetailedMessage(e));
	}
	
	
	public boolean isCodecsAvailable()
	{
		return !codecs.getConfigsList().isEmpty();
	}
	
	public Set<String> getCodecs()
	{
		return codecs.getCodecNames();
	}
	
	public List<XmlCodecConfig> getCodecConfigs()
	{
		return codecs.getConfigsList();
	}
	
	public String getCodecDefault()
	{
		if (codecs == null || codecs.getConfigsList().isEmpty())
			return "";
		
		if (codecs.getConfigsList().size() == 1)
		{
			for (String value : codecs.getCodecNames())
				return value;
		}
		
		return DictionaryValidatorTool.AUTO_FORMAT;
	}
	
	
	public String getTextToParseFormat()
	{
		return textToParseFormat;
	}

	public String getTextToParseFormatDefault()
	{
		if (codecs == null || codecs.getConfigsList().isEmpty())
			return "";
		
		if (codecs.getConfigsList().size() == 1)
		{
			for (String value : codecs.getCodecNames())
				return value;
		}
		
		return DictionaryValidatorTool.AUTO_FORMAT;
	}
	
	public void setTextToParseFormat(String textToParseFormat)
	{
		this.textToParseFormat = textToParseFormat;
	}
	
	
	public Set<String> getValidationConfigs()
	{
		return validationConfigs;
	}
		
	public String getValidationConfig()
	{
		return currValidationConfig;
	}
	
	public String getValidationConfigDefault()
	{
		return DictionaryValidatorTool.getDefaultValidationConfig();
	}
	
	public void setValidationConfig(String validationConfig)
	{
		this.currValidationConfig = validationConfig;
	}
	
	
	public String getTextToParse()
	{
		return textToParse;
	}
	
	public void setTextToParse(String textToParse)
	{
		this.textToParse = textToParse;
	}
	
	
	public String getDecodedText()
	{
		return decodedText;
	}
	
	public String getEncodedText()
	{
		return encodedText;
	}
	
	
	public boolean isValidatedSuccessfully()
	{
		return validatedSuccessfully;
	}
	
	public String getValidationDetails()
	{
		return validationDetails;
	}
	
	public String getValidationMessage()
	{
		if (encodedText.isEmpty())
			return "";
		
		return "messages "+(isValidatedSuccessfully() ? "are equal" : "have differences");
	}
	
	public String getValidationColor()
	{
		if (encodedText.isEmpty())
			return "";
		
		return isValidatedSuccessfully() ? "green" : "red";
	}
	
	public boolean isComparationTreeAvaliable()
	{
		return comparationTree != null && !comparationTree.getChildren().isEmpty();
	}
	
	public TreeNode getComparationTree()
	{
		return comparationTree;
	}
}
