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

package com.exactprosystems.clearth.connectivity.remotehand;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhScriptCompiler
{
	private static final Logger logger = LoggerFactory.getLogger(RhScriptCompiler.class);
	protected static final String PARAMETER_MARK = "%", COMPILE_ERROR = "Error while compiling RemoteHand script: ";
	
	private int pointer = 0;
	
	public String compile(String script, Map<String, String> arguments) throws RhException
	{
		pointer = 0;
		String expression;
		while ((expression = nextParameter(script)) != null)
		{
			String value = getValue(expression, arguments);
			script = script.replaceFirst(expression, value);
			logger.trace("Replaced '{}' with '{}'", expression, value);
			pointer += value.length();
		}
		return script;
	}
	
	
	protected String nextParameter(String rowScript) throws RhException
	{
		int startPos = rowScript.indexOf(PARAMETER_MARK, pointer);
		if (startPos < 0)
			return null;
		
		if (startPos == rowScript.length() - 1)
			throw compilationError("unclosed parameter mark at " + startPos);
		
		pointer = startPos;
		int endPos = rowScript.indexOf(PARAMETER_MARK, startPos + 1);
		if (endPos < 0)
			throw compilationError("unclosed parameter mark at " + startPos);
		
		return rowScript.substring(startPos, endPos + 1);
	}
	
	protected String getValue(String expression, Map<String, String> arguments) throws RhException
	{
		String pureExp = expression.replace(PARAMETER_MARK, "");
		String value = arguments != null ? arguments.get(pureExp) : null;
		if (value == null)
			throw compilationError("no such argument - " + pureExp);
		return value;
	}
	
	
	private RhException compilationError(String msg)
	{
		return new RhException(COMPILE_ERROR + msg);
	}
}
