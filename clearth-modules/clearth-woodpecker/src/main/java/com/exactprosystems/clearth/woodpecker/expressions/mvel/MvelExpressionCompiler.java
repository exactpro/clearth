/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.expressions.mvel;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.contains;

public class MvelExpressionCompiler
{
	private static final Pattern LINK_PATTERN = Pattern.compile("link\\('([\\w\\d_]+)'\\)");
	
	public static boolean containsExpression(String value)
	{
		return contains(value, MatrixFunctions.FORMULA_START);
	}
	
	public CompiledTemplate compile(String expression)
	{
		expression = clean(expression);
		return TemplateCompiler.compileTemplate(expression);
	}
	
	private String clean(String expression)
	{
		return convertLinks(expression);
	}
	
	private String convertLinks(String expression)
	{
		Matcher matcher = LINK_PATTERN.matcher(expression);
		return matcher.find() ? LINK_PATTERN.matcher(expression).replaceAll("$1") : expression;
	}
}
