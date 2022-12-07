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

package com.exactprosystems.clearth.utils.sql;

import com.exactprosystems.clearth.utils.sql.LegacyValueTransformer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.exactprosystems.clearth.utils.TagUtils.indexClosingTag;
import static org.testng.Assert.*;

public class LegacySQLTransformerTest
{
	@DataProvider(name="defaultTestData")
	public static Object[][] getParamsForIndexClosingTag()
	{
		return new Object[][]
			{
				{null, "NOVALUE"},
				{"0.", "."},
				{"1.000", "1.000"},
				{"-0.0", "-.0"},
				{"", ""},
				{"abcd", "abcd"}
			};
	}

	@Test(dataProvider="defaultTestData")
	public void testDefaultTransformer(String from, String expected)
	{
		LegacyValueTransformer transformer = new LegacyValueTransformer();
		assertEquals(transformer.transform(from), expected);
	}
}