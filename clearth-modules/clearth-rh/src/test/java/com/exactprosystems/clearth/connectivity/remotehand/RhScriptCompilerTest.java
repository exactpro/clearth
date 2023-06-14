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

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RhScriptCompilerTest
{
	@Test
	public void compile() throws Exception
	{
		RhScriptCompiler compiler = new RhScriptCompiler();
		String script = "_____%param1%_____%param2%%param3%____";
		Map<String, String> arguments = new HashMap<String, String>();
		arguments.put("param1", "%");
		arguments.put("param2", "%");
		arguments.put("param3", "value");
		String compiled = compiler.compile(script, arguments);
		Assert.assertEquals(compiled, "_____%_____%value____");
	}
	
	@Test(expectedExceptions = RhException.class, expectedExceptionsMessageRegExp = ".*no such argument.*")
	public void missingArgument() throws Exception
	{
		RhScriptCompiler compiler = new RhScriptCompiler();
		String script = "#action,#text\r\n"
				+ "SendText,%param%";
		compiler.compile(script, null);
	}
}
