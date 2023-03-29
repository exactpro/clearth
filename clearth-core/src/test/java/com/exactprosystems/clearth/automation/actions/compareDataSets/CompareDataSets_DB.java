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

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.actions.CompareDataSets;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.sql.DbConnectionSupplier;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;

import java.util.Map;

public class CompareDataSets_DB extends CompareDataSets {

    protected boolean needCloseDbConnection;
    protected DbConnectionSupplier supplier;
    public CompareDataSets_DB(boolean needCloseDbConnection, DbConnectionSupplier supplier)
    {
        super();
        this.needCloseDbConnection = needCloseDbConnection;
        this.supplier = supplier;
    }
    @Override
    protected TableDataReaderSettings createTableDataReaderSettings(Map<String, String> actionParameters,
             boolean forExpectedData, GlobalContext globalContext) throws ParametersException
    {
        return new TableDataReaderSettings_DB(actionParameters, forExpectedData, supplier, needCloseDbConnection);
    }

}
