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

package com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings;

import com.exactprosystems.clearth.automation.actions.db.checkers.RecordChecker;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.ObjectToStringTransformer;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;

public class ResultSetProcessorSettings
{
	protected TableResult result;
	protected ObjectToStringTransformer objectToStringTransformer;
	protected IValueTransformer valueTransformer;
	protected ConversionSettings conversionSettings;
	protected int maxDisplayedRows;
	protected RecordChecker recordChecker;

	public ResultSetProcessorSettings()
	{
	}

	public TableResult getResult()
	{
		return result;
	}

	public void setResult(TableResult result)
	{
		this.result = result;
	}

	public ObjectToStringTransformer getObjectToStringTransformer()
	{
		return objectToStringTransformer;
	}

	public void setObjectToStringTransformer(ObjectToStringTransformer objectToStringTransformer)
	{
		this.objectToStringTransformer = objectToStringTransformer;
	}

	public IValueTransformer getValueTransformer()
	{
		return valueTransformer;
	}

	public void setValueTransformer(IValueTransformer valueTransformer)
	{
		this.valueTransformer = valueTransformer;
	}

	public ConversionSettings getConversionSettings()
	{
		return conversionSettings;
	}

	public void setConversionSettings(ConversionSettings conversionSettings)
	{
		this.conversionSettings = conversionSettings;
	}

	public int getMaxDisplayedRows()
	{
		return maxDisplayedRows;
	}

	public void setMaxDisplayedRows(int maxDisplayedRows)
	{
		this.maxDisplayedRows = maxDisplayedRows;
	}

	public RecordChecker getRecordChecker()
	{
		return recordChecker;
	}

	public void setRecordChecker(RecordChecker recordChecker)
	{
		this.recordChecker = recordChecker;
	}
}
