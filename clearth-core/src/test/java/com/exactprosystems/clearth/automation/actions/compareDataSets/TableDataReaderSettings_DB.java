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

package com.exactprosystems.clearth.automation.actions.compareDataSets;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.sql.DbConnectionSupplier;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;

import java.util.Map;

public class TableDataReaderSettings_DB extends TableDataReaderSettings
{
	protected boolean needCloseDbConnection;

	public TableDataReaderSettings_DB(Map<String, String> params, boolean forExpectedData, DbConnectionSupplier supplier,
					boolean needCloseDbConnection) throws ParametersException
	{
		super(params, forExpectedData, supplier);
		this.needCloseDbConnection = needCloseDbConnection;
	}

	@Override
	public boolean isNeedCloseDbConnection()
	{
		return needCloseDbConnection;
	}

}
