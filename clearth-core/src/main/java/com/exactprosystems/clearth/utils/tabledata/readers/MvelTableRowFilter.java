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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.exactprosystems.clearth.utils.Utils.EOL;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

/**
 * 25 March 2019
 */
public class MvelTableRowFilter<A, B> implements TableRowFilter<A, B>
{
	private static final Logger log = LoggerFactory.getLogger(MvelTableRowFilter.class);

	private static final String ROW_VAR = "row";

	private final String expressionText;
	private final CompiledTemplate expression;
	private final Object functions;
	

	@Override
	public boolean filter(TableRow<A, B> row) throws IOException
	{
		Map<String, Object> vars = singletonMap(ROW_VAR, row);

		Object result;
		try
		{
			result = (functions != null)
					? TemplateRuntime.execute(expression, functions, vars)
					: TemplateRuntime.execute(expression, vars);
		}
		catch (Exception e)
		{
			throw new IOException(format("Expression [%s] calculation failed.", expressionText), e);
		}
		if (log.isTraceEnabled())
			log.trace("Filter condition evaluated to {} for row{}{}", new Object[]{result, EOL, row});

		if (result instanceof Boolean)
			return (Boolean) result;
		else
			throw new IOException(format("Expression [%s] result [%s] isn't boolean.", expressionText, result));
	}


	public static <A, B> MvelTableRowFilter<A, B> compile(String expressionText) throws IOException
	{
		return compile(expressionText, null);
	}
	
	public static <A, B> MvelTableRowFilter<A, B> compile(String expressionText, Object functions) throws IOException
	{
		requireNonNull(expressionText, "expressionText");

		String preparedExpression = prepareExpression(expressionText);
		if (log.isTraceEnabled())
			log.trace("Source expression:{}{}{}Prepared expression:{}{}",
					new Object[]{EOL, expressionText, EOL, EOL, preparedExpression});

		CompiledTemplate expression;
		try
		{
			expression = TemplateCompiler.compileTemplate(preparedExpression);
		}
		catch (Exception e)
		{
			throw new IOException(format("Invalid expression [%s]([%s]).", expressionText, preparedExpression), e);
		}
		
		return new MvelTableRowFilter<A, B>(expressionText, expression, functions);
	}
	
	private static String prepareExpression(String expression)
	{
		return expression.replaceAll("row\\.(\\w+)", "row.getValue('$1')");
	}
	
	private MvelTableRowFilter(String expressionText, CompiledTemplate expression, Object functions)
	{
		this.expressionText = expressionText;
		this.expression = expression;
		this.functions = functions;
	}
}
