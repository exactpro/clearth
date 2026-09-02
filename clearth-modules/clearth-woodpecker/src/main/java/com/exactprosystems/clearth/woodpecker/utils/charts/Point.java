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

package com.exactprosystems.clearth.woodpecker.utils.charts;

import java.text.DecimalFormat;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerMathUtils.OUTPUT_NUMBER_PATTERN;

public class Point
{
	private final long x;
	private final double y;
	
	public Point(long x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public long getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public String getAsJSON()
	{
		DecimalFormat df = new DecimalFormat(OUTPUT_NUMBER_PATTERN);
		return "{x:" + x + ", y:" + df.format(y) + "}";
	}

	@Override
	public String toString()
	{
		return getAsJSON();
	}
}
