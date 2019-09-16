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

package com.exactprosystems.clearth.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils
{
	public static String roundValue(String value, int scale) throws Exception
	{
		BigDecimal bd = new BigDecimal(value);
		RoundingMode roundingMode = RoundingMode.HALF_UP;
		return bd.setScale(scale, roundingMode).toPlainString();
	}

	public static double roundValue(double value, int scale)
	{
		BigDecimal bd = new BigDecimal(value);
		RoundingMode roundingMode = RoundingMode.HALF_UP;
		return bd.setScale(scale, roundingMode).doubleValue();
	}
	
	
	public static double[] normalizeArray(double[] array, double normalizedSum)
	{
		double sum = 0.0;
		for (double d : array)
			sum += d;

		double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++)
			result[i] = array[i] * normalizedSum / sum;
		return result;
	}
}
