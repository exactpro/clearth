/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.Serializable;

public class MvelExpressionValidatorTest
{
	private final MvelExpressionValidator validator = new MvelExpressionValidator();
	private ParserContext functionsContext;

	@BeforeClass
	public void init()
	{
		functionsContext = new ParserContext();
		functionsContext.addImport(MatrixFunctions.class);
	}

	@DataProvider(name = "forCheckValidExpressions")
	Object[][] createValidExpressions()
	{
		return new Object[][]
				{
						// expression
						{
								"a+b"
						},
						{
								"a>b?a:b"
						},
						{
								"a+b+c+d+e+f"
						},
						{
								null
						},
						{
								"null"
						},
						{
								"min(1, 2)"
						},
						{
								"id1.A - id2.B"
						},
						{
								"min(id1.A, id2.B)"
						}
				};
	}

	@DataProvider(name = "forCheckExpressionErrors")
	Object[][] createExpressionsErrors()
	{
		return new Object[][]
				{
						// expression, expectedErrorMessage
						{
								"id.A id.B"
						},
						{
								"id.A id.B id.C"
						},
						{
								"min(id.A, id.B) max(id.A, id.B)"
						}
				};
	}

	@Test(dataProvider = "forCheckValidExpressions")
	public void checkValidExpressions(String expression) throws Exception
	{
		Serializable compiledExpression = MVEL.compileExpression(expression, functionsContext);
		validator.validateExpression(compiledExpression);
	}

	@Test(dataProvider = "forCheckExpressionErrors", expectedExceptions = Exception.class,
			expectedExceptionsMessageRegExp = "Invalid expression: '.*'. Possibly it was received from another " +
					"reference.")
	public void checkExpressionErrors(String expression) throws Exception
	{
		Serializable compiledExpression = MVEL.compileExpression(expression, functionsContext);
		validator.validateExpression(compiledExpression);
	}
}