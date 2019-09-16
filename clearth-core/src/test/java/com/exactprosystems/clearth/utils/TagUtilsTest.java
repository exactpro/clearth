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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.exactprosystems.clearth.utils.TagUtils.indexClosingTag;
import static org.testng.Assert.*;

/**
 * 06 August 2019
 */
public class TagUtilsTest
{
	@DataProvider(name = "getParamsForIndexClosingTag")
	public static Object[][] getParamsForIndexClosingTag()
	{
		return new Object[][]
				{
						{"(12345678)", "(", ")", 0, 9},
						{"01(345678)", "(", ")", 2, 9},
						{"01(345678)", "(", ")", 0, 9},
						{"(1234)6789", "(", ")", 0, 5},
						{"012(456)89", "(", ")", 3, 7},
						{"0123()6789", "(", ")", 4, 5},
						
						{"@{2345678}", "@{", "}", 0, 9},
						{"012@{5678}", "@{", "}", 3, 9},
						{"@{2345}789", "@{", "}", 0, 6},
						{"01@{4567}9", "@{", "}", 2, 8},
						{"0123@{}789", "@{", "}", 4, 6},
						{"0123@{}789", "@{", "}", 0, 6},
						
						{"<<234567>>", "<<", ">>", 0, 8},
						{"0123<<67>>", "<<", ">>", 4, 8},
						{"<<2345>>89", "<<", ">>", 0, 6},
						{"01<<45>>89", "<<", ">>", 2, 6},
						{"01<<45>>89", "<<", ">>", 0, 6},
						{"012<<>>789", "<<", ">>", 3, 5},

						{"01@{456789}1234@{789}123@{67}9", "@{", "}", 0, 10},
						{"01@{456789}1234@{789}123@{67}9", "@{", "}", 2, 10},
						{"01@{456789}1234@{789}123@{67}9", "@{", "}", 15, 20},
						{"01@{456789}1234@{789}123@{67}9", "@{", "}", 24, 28},

						{"0@{345@{890@{3456}8901}34567}9", "@{", "}", 0, 28},
						{"0@{345@{890@{3456}8901}34567}9", "@{", "}", 1, 28},
						{"0@{345@{890@{3456}8901}34567}9", "@{", "}", 6, 22},
						{"0@{345@{890@{3456}8901}34567}9", "@{", "}", 11, 17},
						
						{"(123(5678)012)(()789012345678)", "(", ")", 0, 13},
						{"(123(5678)012)(()789012345678)", "(", ")", 4, 9},
						{"(123(5678)012)(()789012345678)", "(", ")", 14, 29},
						{"(123(5678)012)(()789012345678)", "(", ")", 15, 16},

						{"@{234567@{0123456789012345678}", "@{", "}", 0, -1},
						{"(((())678)012((56((90)234)678)", "(", ")", 13, -1},

						{null, "(", ")",  0, -1},
						{"", "(", ")",  0, -1},
						{"  ", "(", ")",  0, -1},
						{"12345", "(", ")",  0, -1},
						{"@{2345678}", "@{", "}",  10, -1}
				};
	}

	@DataProvider(name = "getInvalidParamsForIndexClosingTag")
	public static Object[][] getInvalidParamsForIndexClosingTag()
	{
		return new Object[][]
				{
						{"@{2345678}", null, "}", 0, "openingTag='null'. Non-blank string is expected."},
						{"@{2345678}", "", "}", 0, "openingTag=''. Non-blank string is expected."},
						
						{"@{2345678}", "@{", null, 0, "closingTag='null'. Non-blank string is expected."},
						{"@{2345678}", "@{", "", 0, "closingTag=''. Non-blank string is expected."},

						{"@{2345678}", "'", "'", 0, "openingTag=closingTag='\''. Different values are expected."}
				};
	}

	@Test(dataProvider = "getParamsForIndexClosingTag", timeOut = 1000)
	public void testIndexClosingTag(String text, String openingTag, String closingTag, int indexStart, int expectedIndex)
	{
		assertEquals(indexClosingTag(text, openingTag, closingTag, indexStart), expectedIndex);
	}

	@Test(dataProvider = "getInvalidParamsForIndexClosingTag", timeOut = 1000)
	public void testIndexClosingTagOnInvalidParams(String text, String openingTag, String closingTag, int indexStart,
	                                               String expectedError)
	{
		try
		{
			indexClosingTag(text, openingTag, closingTag, indexStart);
		}
		catch (IllegalArgumentException e)
		{
			assertEquals(e.getMessage(), expectedError);
		}
	}
}
