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

package com.exactprosystems.clearth.utils.sql.conversion;

import com.exactprosystems.clearth.utils.CSVEntityReader;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

public class DBFieldMappingReader extends CSVEntityReader<DBFieldMapping>
{
	@Override
	protected DBFieldMapping createEntity(InputParamsHandler paramsHandler)
	{
		String matrixField = paramsHandler.getString("Matrix Field", "");
		String dbField = paramsHandler.getString("DB Field", "");
		BidiMap<String, String> conversions = new DualLinkedHashBidiMap<>(paramsHandler.getParamsMap("Conversions", ";", false));
		Map<String, String> visualisations = paramsHandler.getParamsMap("Visualisations", ";", false);

		return new DBFieldMapping(matrixField, dbField, conversions, visualisations);
	}
}
