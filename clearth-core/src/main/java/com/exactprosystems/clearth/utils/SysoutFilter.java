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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SysoutFilter extends PrintStream
{
	public SysoutFilter(OutputStream out)
	{
		super(out);
	}
	
	@Override
	public void print(String s)
	{
		if (s == null) {
			s = "null";
		}
		if (!censored(s))
			super.print(s);
	}
	
	@Override
	public void println(String x)
	{
		if (x == null) {
			x = "null";
		}
		if (!censored(x))
			super.println(x);
	}

	@Override
	public PrintStream printf(String format, Object... args)
	{
		if (!censored(format))
			return super.printf(format, args);
		return this;
	}
	
	@Override
	public PrintStream printf(Locale l, String format, Object... args)
	{
		if (!censored(format))
			return super.printf(l, format, args);
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq)
	{
		if (!censored(csq.toString()))
			return super.append(csq);
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end)
	{
		if (!censored(csq.toString()))
			return super.append(csq, start, end);
		return this;
	}

	private Pattern pat = Pattern.compile("Unable to find component with clientId \\'.*\\'\\, no need to remove it\\.");
	
	private boolean censored(String s)
	{
		Matcher mat = pat.matcher(s);
		boolean match = mat.matches();
		return match;
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		if (!censored(new String(b)))
			super.write(b);
	}
}
