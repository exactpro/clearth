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

package com.exactprosystems.clearth.automation.expressions;

import java.util.Map;

public class MvelExpressionUtils
{

	/**
	 * Tests whether character is valid action id in matrix.
	 * Action id doesn't have to be valid MVEL (Java) identifier.
	 * It can be started from digit.
	 * @param c char to test.
	 * @return true if char can be used in action id.
	 */
	public static boolean isValidActionIdChar(char c)
	{
		return (c == '_') || Character.isLetter(c) || Character.isDigit(c);
	}


	public static String fixActionIdForMvel(String idInMatrix)
	{
		if ((!idInMatrix.isEmpty()) && ((Character.isDigit(idInMatrix.charAt(0))) || (idInMatrix.charAt(0)=='_')))
			return "MVELFIXED_" + idInMatrix;
		else
			return idInMatrix;
	}

	public static String resolveFixedId(String id, Map<String, String> fixedIdToIdInMatrix)
	{
		if ((fixedIdToIdInMatrix != null) && (fixedIdToIdInMatrix.containsKey(id)))
			return fixedIdToIdInMatrix.get(id);
		else
			return id;
	}
}
