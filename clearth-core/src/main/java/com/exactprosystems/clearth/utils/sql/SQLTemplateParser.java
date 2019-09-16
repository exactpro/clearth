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

package com.exactprosystems.clearth.utils.sql;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.utils.sql.SQLUtils.*;

public class SQLTemplateParser
{
	private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("'?[^\\\\][#$][\\w]+'?");
	
	public ParametrizedQuery parseParametrizedQueryTemplate(File templateFile) throws SQLException, IOException
	{
		String templateText = FileUtils.readFileToString(templateFile, Charset.defaultCharset());
		if(templateText.isEmpty())
			throw new IllegalArgumentException(String.format("The file '%s' is empty", templateFile));
		return parseParametrizedQueryTemplate(templateText);
	}
	
	public ParametrizedQuery parseParametrizedQueryTemplate(String templateText) throws SQLException
	{
		
		Matcher paramMatcher = TEMPLATE_PARAM_PATTERN.matcher(templateText);
		List<String> queryParams = new ArrayList<>();
		
		StringBuffer queryBuilder = new StringBuffer();
		while (paramMatcher.find())
		{
			String foundParam = getTemplateParamName(paramMatcher.group());
			queryParams.add(foundParam);
			paramMatcher.appendReplacement(queryBuilder, "?");
		}
		paramMatcher.appendTail(queryBuilder);
		if(queryBuilder != null)
		{
			replaceAll(queryBuilder, TABLE_NAME_WITH_DOLLAR.toString(), Character.toString(CONVERT_BEGINNER));
			replaceAll(queryBuilder,TABLE_NAME_WITH_SHARP.toString(),Character.toString(CUSTOM_BEGINNER));
		}
		return new ParametrizedQuery(queryBuilder.toString(), queryParams);
	}

	public static void replaceAll(StringBuffer builder, String from, String to)
	{
		int index = builder.indexOf(from);
		while (index != -1)
		{
			builder.replace(index, index + from.length(), to);
			index += to.length();
			index = builder.indexOf(from, index);
		}
	}
	
	protected String getTemplateParamName(String templateParam)
	{
		if (templateParam.length() < 2)
			throw new IllegalArgumentException("Template param should contain at least 2 characters.");
		
		return templateParam.replaceAll("[#$']*", "");
	}
}
