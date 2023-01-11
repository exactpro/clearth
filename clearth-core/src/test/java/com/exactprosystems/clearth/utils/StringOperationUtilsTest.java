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

package com.exactprosystems.clearth.utils;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class StringOperationUtilsTest
{
	@DataProvider(name = "NumericFunctions")
	public String[][] numericFunctions()
	{
		return new String[][] {
			{"@{asNumber(12)}", "12"},
			{"{asNumber(10)}", "10"},
			{"asNumber(14)", "14"},
			{"@{asNumber('99')}", "99"},
			{"@{isLessThan(24)}", "24"},
			{"@{isGreaterThan({asNumber(100)})}", "100"},  //It should be never written in matrix like this. 
			                                                //But this can be a result of reference resolution, i.e. id1.Param1=@{asNumber(100)}, id2.Param1=@{isGreaterThan(id1.Param1)}
			{"MyValue_asNumber", "MyValue_asNumber"},       //Similar to function name
			{"getasNumber(123)", "getasNumber(123)"},       //Similar function name
			{"'asNumber(123)'", "'asNumber(123)'"}          //Not a function name
		};
	}
	
	@Test(dataProvider = "NumericFunctions")
	//asNumber(), isLessThan(), etc. can't be used together with concatenation, other functions and operations. They should only be used alone.
	//The correct method behavior means that if a number comparison function is used alone, only its first argument is returned.
	//If it is used together with something else, the expression is invalid. The correct method behavior is undefined. This case should be handled during expression evaluation
	public void stripNumericFunctions(String expression, String expectedArgument)
	{
		String number = StringOperationUtils.stripNumericFunctions(expression);
		Assert.assertEquals(number, expectedArgument, "Extracted argument of "+expression);
	}
}
