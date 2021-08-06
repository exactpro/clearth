/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.actions.CompareDataSets;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.TableDataReaderFactory;

import java.util.Map;

public class CompareDataSets2 extends CompareDataSets {

    @Override
    protected TableDataReaderFactory<String, String> createTableDataReaderFactory() {
        return new StringTableDataReaderFactory2();
    }

    @Override
    protected TableDataReaderSettings createTableDataReaderSettings(Map<String, String> actionParameters, boolean forExpectedData) throws ParametersException {
        return new TableDataReaderSettings2(actionParameters, forExpectedData);
    }

}
