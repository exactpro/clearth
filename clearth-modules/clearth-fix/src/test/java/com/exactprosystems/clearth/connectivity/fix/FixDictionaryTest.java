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

package com.exactprosystems.clearth.connectivity.fix;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FixDictionaryTest
{
	private final String FIX50_FILENAME = "src/test/resources/FIX50.xml";
	private final String FIX_TRANSPORT_FILENAME = "src/test/resources/transportDicts/FIX50Transport.xml";

	@DataProvider(name = "dictionaries")
	public Object[][] generateDictionaries() throws Exception
	{
		Map<String, String> sampleMap = new HashMap<>();
		sampleMap.put(FixDictionary.DICT_TRANSPORTARGUMENT, FIX_TRANSPORT_FILENAME);
		ClearThDataDictionary transportDict = new ClearThDataDictionary(new File(FIX_TRANSPORT_FILENAME));
		ClearThDataDictionary generalDict = new ClearThDataDictionary(new File(FIX50_FILENAME));
		return new Object[][] 
		{
			{new FixDictionary(FIX50_FILENAME), generalDict, null},
			{new FixDictionary(FIX50_FILENAME, new HashMap<>(), null), generalDict, null},
			{new FixDictionary(FIX50_FILENAME, sampleMap, null), generalDict, transportDict},
			{new FixDictionary(FIX50_FILENAME, FIX_TRANSPORT_FILENAME), generalDict, transportDict}
		};
	}

	@Test(dataProvider = "dictionaries")
	void testSubdictionaries(FixDictionary mainDictionary, ClearThDataDictionary appDictionary, ClearThDataDictionary transportDictionary) {
		Assertions.assertThat(mainDictionary.getAppDictionary().getMessagesInfo())
							.usingRecursiveComparison()
							.isEqualTo(appDictionary.getMessagesInfo());
		if (transportDictionary != null)
			Assertions.assertThat(mainDictionary.getTransportDictionary().getMessagesInfo())
							.usingRecursiveComparison()
							.isEqualTo(transportDictionary.getMessagesInfo());
		else 
			Assert.assertNull(mainDictionary.getTransportDictionary());
	}
}
