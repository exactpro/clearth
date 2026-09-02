/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialDataLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile.START_FILE;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException.whenLoadingFailed;
import static java.lang.System.arraycopy;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

public abstract class StartScriptReader<S extends StartScript>
{
	protected final WoodpeckerObjectsFactory objectsFactory;
	
	protected final Class<? extends S> startScriptClass;
	protected final Class[] extendedClasses;

	protected final Path replicaBaseDir;
	
	protected S startScript;
	
	protected Set<String> executableOperationNames;
	
	
	protected abstract void checkContent(S startScript, Path path) throws WoodpeckerConfigException;
	
	protected abstract Set<String> findExecutableOperationNames(S startScript) throws WoodpeckerConfigException;
	
	
	protected StartScriptReader(Path replicaBaseDir, Class<? extends S> extendedStartScriptClass,
	                            Class[] otherExtendedClasses)
	{
		objectsFactory = woodpecker().getObjectsFactory();
		
		this.replicaBaseDir = replicaBaseDir;
		
		startScriptClass = extendedStartScriptClass;

		int extCount = otherExtendedClasses.length;
		extendedClasses = new Class[extCount + 1];
		extendedClasses[0] = startScriptClass;
		if (extCount > 0)
			arraycopy(otherExtendedClasses, 0, extendedClasses, 1, extCount);
	}


	public Collection<String> getExecutableOperationNames()
	{
		return executableOperationNames;
	}

	public Path getReplicaBaseDir()
	{
		return replicaBaseDir;
	}
	
	
	public final void readScript() throws WoodpeckerException
	{
		Path startScriptPath = replicaBaseDir.resolve(START_FILE.fileName());
		startScript = read(startScriptPath);

		checkContent(startScript, startScriptPath);
		
		executableOperationNames = findExecutableOperationNames(startScript);
	}
	
	private S read(Path path) throws WoodpeckerConfigException
	{
		try (InputStream is = newInputStream(path, READ))
		{
			Unmarshaller u = JAXBContext.newInstance(extendedClasses)
					.createUnmarshaller();
			u.setEventHandler(new DefaultValidationEventHandler());
			JAXBElement<? extends S> e = u.unmarshal(new StreamSource(is), startScriptClass);
			return e.getValue();
		}
		catch (JAXBException | IOException e)
		{
			throw whenLoadingFailed(path, START_FILE.description(), e);
		}
	}


	public InitialData loadInitialData(String daemonName, DaemonContext context) throws WoodpeckerException
	{
		InitialDataBlockDesc initialDataBlock = startScript.getInitialDataBlock();
		if (initialDataBlock != null)
		{
			InitialDataLoader initialDataLoader = objectsFactory.createInitialDataLoader(daemonName);
			return initialDataLoader.load(initialDataBlock, replicaBaseDir, context);
		}
		else
			return objectsFactory.createInitialData();
	}
}
