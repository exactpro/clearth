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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.StringTableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

public class StringTableDataReaderFactory2 extends StringTableDataReaderFactory {

    public static final String LOAD_FILE = "LoadFile";
    private static final Path pathFile = USER_DIR.resolve("src/test/resources/Action/CompareDataSets/testData/expectedData.csv");

    protected CsvDataReader createCsvDataReader2() throws IOException
    {
        return new CsvDataReader(new BufferedReader(new FileReader(ClearThCore.rootRelative(pathFile.toString()))));
    }

    @Override
    protected BasicTableDataReader<String, String, ?> createCustomTableDataReader(TableDataReaderSettings settings) throws Exception {
        String sourceType = settings.getSourceType();
        if (sourceType.equalsIgnoreCase(LOAD_FILE)) {
            return createCsvDataReader2();
        }
        else return super.createCustomTableDataReader(settings);
    }
}
