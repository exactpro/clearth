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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public abstract class CSVEntityReader<E>
{
	private static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.RFC4180.withFirstRecordAsHeader();

	protected CSVFormat csvFormat = DEFAULT_CSV_FORMAT;

	public List<E> readEntities(File file) throws IOException {
		CSVParser parser = createParser(file);
		return createEntityList(parser);
	}

	protected CSVParser createParser(File file) throws IOException {
		return csvFormat.parse(new BufferedReader(new FileReader(file)));
	}

	private List<E> createEntityList(CSVParser parser) {
		List<E> entityList = new ArrayList<E>();

		try {
			for (CSVRecord record : parser) {
				InputParamsHandler paramsHandler = getParamsHandler(record);
				E entity = createEntity(paramsHandler);
				addNewEntity(entity, entityList);
			}
		} finally {
			Utils.closeResource(parser);
		}

		return entityList;
	}

	protected InputParamsHandler getParamsHandler(CSVRecord record) {
		return new InputParamsHandler(record.toMap());
	}

	protected abstract E createEntity(InputParamsHandler paramsHandler);

	protected void addNewEntity(E entity, List<E> list) {
		list.add(entity);
	}
}
