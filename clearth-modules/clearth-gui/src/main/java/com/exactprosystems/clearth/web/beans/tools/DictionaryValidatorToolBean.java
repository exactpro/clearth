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
import com.exactprosystems.clearth.web.misc.WebUtils;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.util.*;

import javax.annotation.PostConstruct;

public class DictionaryValidatorToolBean extends ClearThBean
{
	DictionaryValidatorTool dictionaryValidatorTool;

	protected CodecsStorage codecs;
	protected Set<String> validationConfigs;
	
	protected String textToParse = "", textToParseFormat = "", currValidationConfig = "";
	protected DictionaryValidationResult result;
	
	protected TreeNode comparisonTree = new DefaultTreeNode();
	
	@PostConstruct
	public void init()
	{
		dictionaryValidatorTool = ClearThCore.getInstance().getToolsFactory().createDictionaryValidatorTool();
		codecs = ClearThCore.getInstance().getCodecs();
		
		currValidationConfig = DictionaryValidatorTool.getDefaultValidationConfig();
		validationConfigs = new LinkedHashSet<>(DictionaryValidatorTool.getValidationConfigs());
		validationConfigs.remove(currValidationConfig);
		
		textToParseFormat = getCodecDefault();
	}
	
	public void validateDictionary()
	{
		if (getLogger().isInfoEnabled())
			getLogger().info("Validating format: {}; Parsing text: {}", textToParseFormat, textToParse);
		
		List<DictionaryValidatorError> errors = new LinkedList<>();
		try
		{
			result = dictionaryValidatorTool.validateDictionary(textToParse, textToParseFormat, currValidationConfig, errors);
			createComparisonTree(result);
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException(textToParseFormat+": could not validate dictionary", e, getLogger());
		}
		finally
		{
			handleErrors(errors);
		}
	}

	protected void handleErrors(List<DictionaryValidatorError> errors)
	{
		for (DictionaryValidatorError e : errors)
			MessageUtils.addErrorMessage(e.getText(), ExceptionUtils.getDetailedMessage(e.getError()));
	}
	
	protected void createComparisonTree(DictionaryValidationResult result)
	{
		comparisonTree = new DefaultTreeNode();
		String[] originalStrings, encodedStrings = result.getEncodedText().split(Utils.EOL);
		
		String config = result.getValidationConfigName();
		switch (config)
		{
			case DictionaryValidatorTool.BY_CHARACTERS_CONFIG:
				originalStrings = result.getOriginalText().split(Utils.EOL);
				break;
			case DictionaryValidatorTool.NO_LINE_SEPARATION_CONFIG:
				originalStrings = formatStringByPattern(result.getOriginalText(), encodedStrings);
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
			new DefaultTreeNode(new ComparationNode(original, encoded, original.equals(encoded)), comparisonTree);
		}
		
		// checking for extra strings in both arrays
		for (int i = count; i < originalStrings.length; i++)
		{
			new DefaultTreeNode(new ComparationNode(originalStrings[i], "", false), comparisonTree);
		}
		for (int i = count; i < encodedStrings.length; i++)
		{
			new DefaultTreeNode(new ComparationNode("", encodedStrings[i], false), comparisonTree);
		}
	}
	
	protected String[] formatStringByPattern(String original, String[] pattern)
	{
		List<String> originalStringsList = new LinkedList<>();
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
		
		return originalStringsList.toArray(new String[0]);
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
			return codecs.getCodecNames().iterator().next();
		return DictionaryValidatorTool.AUTO_FORMAT;
	}
	
	public String getTextToParseFormat()
	{
		return textToParseFormat;
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
	
	
	public String getEncodedText()
	{
		return result == null ? "" : result.getEncodedText();
	}
	
	
	public boolean isValidatedSuccessfully()
	{
		return result != null && result.isValidatedSuccessfully();
	}
	
	public String getValidationDetails()
	{
		return result == null ? "" : result.getValidationDetails();
	}
	
	public String getValidationMessage()
	{
		return getEncodedText().isEmpty() ? "" : (isValidatedSuccessfully() ? "passed" : "failed");
	}
	
	public String getValidationColor()
	{
		return getEncodedText().isEmpty() ? "" : isValidatedSuccessfully() ? "green" : "red";
	}
	
	public boolean isComparisonTreeAvailable()
	{
		return comparisonTree != null && !comparisonTree.getChildren().isEmpty();
	}
	
	public TreeNode getComparisonTree()
	{
		return comparisonTree;
	}
}
