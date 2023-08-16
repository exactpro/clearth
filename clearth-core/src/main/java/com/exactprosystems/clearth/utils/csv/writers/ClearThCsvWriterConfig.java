/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.csv.writers;

import com.exactprosystems.clearth.utils.csv.ClearThCsvConfig;
import com.exactprosystems.clearth.utils.csv.ClearThQuoteMode;

public class ClearThCsvWriterConfig extends ClearThCsvConfig
{
	private String nullString = "";
	private ClearThQuoteMode quoteMode = ClearThQuoteMode.MINIMAL;
	private Character escapeCharacter;

	public String getNullString()
	{
		return nullString;
	}

	public void setNullString(String nullString)
	{
		this.nullString = nullString;
	}

	public String getQuoteModeString()
	{
		return quoteMode != null ? quoteMode.toString() : ClearThQuoteMode.MINIMAL.toString();
	}

	public ClearThQuoteMode getQuoteMode()
	{
		return quoteMode;
	}

	public void setQuoteMode(ClearThQuoteMode cthQuoteMode)
	{
		this.quoteMode = cthQuoteMode;
	}

	public Character getEscapeCharacter()
	{
		return escapeCharacter;
	}

	public void setEscapeCharacter(Character escapeCharacter)
	{
		this.escapeCharacter = escapeCharacter;
	}
}
