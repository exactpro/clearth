/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
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
