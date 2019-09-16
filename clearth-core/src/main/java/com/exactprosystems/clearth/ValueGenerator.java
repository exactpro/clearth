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

package com.exactprosystems.clearth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ValueGenerator
{
	protected static final Logger logger = LoggerFactory.getLogger(ValueGenerator.class);
	
	protected final String lastGenFileName, generatorPrefix;
	protected int genCounter;
	protected String lastGeneratedValue;
	
	public ValueGenerator(String fileNameForLastGen, String generatorPrefix)
	{
		this.lastGenFileName = fileNameForLastGen;
		this.generatorPrefix = generatorPrefix;
		
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(this.lastGenFileName));
			genCounter = Integer.parseInt(reader.readLine());
			lastGeneratedValue = reader.readLine();
		}
		catch (Exception e)
		{
			genCounter = -1;
			lastGeneratedValue = null;
			logger.warn("Could not load last generated value from file '"+lastGenFileName+"'");
		}
		finally
		{
			if (reader!=null)
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					logger.warn("Error while closing reader of last generated value", e);
				}
		}
	}
	
	
	synchronized public String generateValue(int length)
	{
		genCounter++;
		lastGeneratedValue = generatorPrefix+Integer.toString(genCounter);
		while (lastGeneratedValue.length() < length)
			lastGeneratedValue = "0"+lastGeneratedValue;

		while (lastGeneratedValue.length() > length)
			lastGeneratedValue = lastGeneratedValue.substring(1);

		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(lastGenFileName);
			writer.println(genCounter);
			writer.println(lastGeneratedValue);
		}
		catch (Exception e)
		{
			logger.warn("Could not save last generated value to file '"+lastGenFileName+"'", e);
		}
		finally
		{
			if (writer != null)
				writer.close();
		}
		
		return lastGeneratedValue;
	}
	
	public String getLastGeneratedValue()
	{
		return lastGeneratedValue;
	}

	public String getLastGenFileName()
	{
		return lastGenFileName;
	}

	public String getGeneratorPrefix()
	{
		return generatorPrefix;
	}
}
