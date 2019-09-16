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

package com.exactprosystems.clearth.connectivity.connections;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.Utils;

/**
 * @author daria.plotnikova
 *
 */
public abstract class ClearThConnectionFactory<C extends ClearThConnection<C,?>>
{
	protected final Unmarshaller unmarshaller;

	public abstract C createConnection();
	public abstract String getDirName();
	abstract protected Class[] getClassesToBeBound();
	
	
	public ClearThConnectionFactory() throws ConnectivityException
	{
		unmarshaller = createUnmarshaller();
	}
	
	
	protected Unmarshaller createUnmarshaller() throws ConnectivityException
	{
		try
		{
			return JAXBContext.newInstance(getClassesToBeBound()).createUnmarshaller();
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException("Could not create unmarshaller for connections", e);
		}
	}
	
	public C loadConnection(File file) throws ConnectivityException
	{
		FileInputStream is = null;
		try
		{
			is = new FileInputStream(file);
			//noinspection unchecked
			return (C)unmarshaller.unmarshal(is);
		}
		catch (FileNotFoundException e)
		{
			throw new ConnectivityException(e, "Could not load connection settings from file '%s'. File not found.", 
					file.getAbsolutePath());
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Could not load connection settings from file '%s'.", 
					file.getAbsolutePath());
		}
		finally
		{
			Utils.closeResource(is);
		}
	}
}
