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

package com.exactprosystems.clearth.tools;


public class DictionaryValidationResult
{
	private String dictionaryName;
	private String validationConfigName;
	private String originalText;
	private String encodedText;
	private boolean validatedSuccessfully;
	private String validationDetails;

	public DictionaryValidationResult()
	{
		this.dictionaryName = null;
		this.validationConfigName = null;
		this.originalText = null;
		this.encodedText = null;
		this.validatedSuccessfully = false;
		this.validationDetails = null;
	}
	
	public DictionaryValidationResult(String dictionaryName, String validationConfigName, String originalText,
											String encodedText, boolean validatedSuccessfully, String validationDetails)
	{
		this.dictionaryName = dictionaryName;
		this.validationConfigName = validationConfigName;
		this.originalText = originalText;
		this.encodedText = encodedText;
		this.validatedSuccessfully = validatedSuccessfully;
		this.validationDetails = validationDetails;
	}

	public String getDictionaryName()
	{
		return dictionaryName;
	}

	public void setDictionaryName(String dictionaryName)
	{
		this.dictionaryName = dictionaryName;
	}

	public String getValidationConfigName()
	{
		return validationConfigName;
	}

	public void setValidationConfigName(String validationConfigName)
	{
		this.validationConfigName = validationConfigName;
	}

	public String getOriginalText()
	{
		return originalText;
	}

	public void setOriginalText(String originalText)
	{
		this.originalText = originalText;
	}

	public String getEncodedText()
	{
		return encodedText;
	}

	public void setEncodedText(String encodedText)
	{
		this.encodedText = encodedText;
	}

	public boolean isValidatedSuccessfully()
	{
		return validatedSuccessfully;
	}

	public void setValidatedSuccessfully(boolean validatedSuccessfully)
	{
		this.validatedSuccessfully = validatedSuccessfully;
	}

	public String getValidationDetails()
	{
		return validationDetails;
	}

	public void setValidationDetails(String validationDetails)
	{
		this.validationDetails = validationDetails;
	}
}
