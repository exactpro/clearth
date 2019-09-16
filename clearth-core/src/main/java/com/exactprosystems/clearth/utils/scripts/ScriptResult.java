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

package com.exactprosystems.clearth.utils.scripts;

import com.exactprosystems.clearth.utils.CommaBuilder;

public class ScriptResult 
{
	public final int result;
	public final String outStr, errStr;
	
	public ScriptResult()
	{
		result = 0;
		outStr = null;
		errStr = null;
	}
	
	public ScriptResult(int result, String outStr, String errStr)
	{
		this.result = result;
		this.outStr = outStr;
		this.errStr = errStr;
	}

	@Override
	public String toString() {
		return new CommaBuilder().append("Result code=").add(result)
				.append("Output=").add(outStr)
				.append("Error string=").add(errStr)
				.toString();
	}
}
