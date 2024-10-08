/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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
package com.exactprosystems.clearth.helpers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;

public class JsonAssert
{
	private String pathDelimiter = ">";
	private Set<String> ignoredValueNames;


	public JsonAssert setCustomPathDelimiter(String delimiter)
	{
		pathDelimiter = delimiter;
		return this;
	}

	public JsonAssert setIgnoredValueNames(Set<String> ignoredValueNames)
	{
		this.ignoredValueNames = ignoredValueNames;
		return this;
	}

	public void assertEquals(File expectedReport, File actualReport) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode expectedNodes = mapper.readTree(expectedReport);
		JsonNode actualNodes = mapper.readTree(actualReport);

		SoftAssertions assertions = new SoftAssertions();
		Stack<String> path = new Stack<>();
		path.push(actualReport.toString());
		compareTrees(assertions, expectedNodes, actualNodes, path);
		assertions.assertAll();
	}


	private void compareTrees(SoftAssertions assertions, JsonNode expectedNodes, JsonNode actualNodes,
	                          Stack<String> path)
	{
		if (expectedNodes.isObject())
		{
			compareObjectTree(assertions, expectedNodes, actualNodes, path);
		}
		else if (expectedNodes.isArray())
		{
			compareArrayTree(assertions, expectedNodes, actualNodes, path);
		}
		else
		{
			assertions.assertThat(actualNodes).as(getPath(path)).isEqualTo(expectedNodes);
		}
	}

	private void compareObjectTree(SoftAssertions assertions, JsonNode expectedNodes, JsonNode actualNodes,
	                               Stack<String> path)
	{
		expectedNodes.fieldNames().forEachRemaining(fieldName ->
		{
			JsonNode expected = expectedNodes.path(fieldName);
			JsonNode actual = actualNodes.get(fieldName);

			if (actual == null && expected.isContainerNode())
			{
				assertions.fail("%s %s missed element <\"%s\">", getPath(path), pathDelimiter, fieldName);
				return;
			}
			if (ignoredValueNames != null && ignoredValueNames.contains(fieldName))
				return;
			path.push(fieldName);
			compareTrees(assertions, expected, actual, path);
			path.pop();
		});
	}

	private void compareArrayTree(SoftAssertions assertions, JsonNode expectedNodes, JsonNode actualNodes,
	                              Stack<String> path)
	{
		String currentPath = path.peek();
		if (expectedNodes.size() != actualNodes.size())
		{
			assertions.assertThat(actualNodes.size())
					.as(MessageFormat.format("{0} {1} [incorrect arrays size]", getPath(path), pathDelimiter))
					.isEqualTo(expectedNodes.size());
			return;
		}
		for (int i = 0; i < expectedNodes.size(); i++)
		{
			path.pop();
			path.push(MessageFormat.format("{0}[{1}]", currentPath, i));
			compareTrees(assertions, expectedNodes.get(i), actualNodes.get(i), path);
		}
	}

	private String getPath(Stack<String> path)
	{
		String fileName = path.firstElement();
		String joinedPath = String.join(MessageFormat.format(" {0} ", pathDelimiter), path.subList(1, path.size()));
		return MessageFormat.format("{0}: {1}", fileName, joinedPath);
	}
}
