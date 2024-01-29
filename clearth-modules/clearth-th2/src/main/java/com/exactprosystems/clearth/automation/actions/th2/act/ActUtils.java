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

package com.exactprosystems.clearth.automation.actions.th2.act;

import com.exactprosystems.clearth.automation.MatrixContext;

public class ActUtils
{
	public static String getResponseName(String actionId)
	{
		return "Th2ActResponse_"+actionId;
	}
	
	public static <T> T getResponse(String actionId, MatrixContext context)
	{
		return context.getContext(getResponseName(actionId));
	}
	
	public static void setResponse(String actionId, MatrixContext context, Object response)
	{
		context.setContext(getResponseName(actionId), response);
	}
}
