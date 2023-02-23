/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.ClearThCore.getInstance;
import static com.exactprosystems.clearth.connectivity.ListenerType.listenerTypeByLabel;
import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Rule to check that there are no started connections with listeners
 * that configured to read the same files as listeners of connection being checked.
 */
public class ListenersNotWritingToBusyFilesRule implements ClearThConnectionValidationRule
{
	@Override
	public boolean isConnectionSuitable(ClearThConnection connectionToCheck)
	{
		return connectionToCheck instanceof ClearThMessageConnection;
	}

	@Override
	public String check(ClearThConnection connectionToCheck)
	{
		if (!isConnectionSuitable(connectionToCheck))
			return null;

		ClearThMessageConnection msgConnectionToCheck = (ClearThMessageConnection) connectionToCheck;
		List<ClearThMessageConnection> anotherMsgConnections = getAllStartedMsgConnections();
		
		String conflicts = checkListenersWritingToSameFiles(msgConnectionToCheck, anotherMsgConnections);
		if (isNotEmpty(conflicts))
			return format("Can't start connection '%s' - \n%s", connectionToCheck.getName(), conflicts);
		else 
			return null;
	}

	private List<ClearThMessageConnection> getAllStartedMsgConnections()
	{
		return getInstance().getConnectionStorage()
				.getConnections(conn -> (isConnectionSuitable(conn) && ((ClearThMessageConnection) conn).isRunning()), 
						ClearThMessageConnection.class);
	}

	private String checkListenersWritingToSameFiles(ClearThMessageConnection connectionToCheck,
	                                                List<ClearThMessageConnection> otherConnections)
	{
		List<String> conflicts = new ArrayList<>();
		
		Map<Path, List<String>> checkedConnectionPaths = findWrittenFilePathsOfCheckedConnection(connectionToCheck);
		Map<Path, String> otherConnectionsPaths = findWrittenFilesPathOfRunningConnections(otherConnections);
		
		for (Map.Entry<Path, List<String>> entry : checkedConnectionPaths.entrySet())
		{
			Path path = entry.getKey();
			List<String> checkedConnectionWriters = entry.getValue();
			String otherConnectionWriters = otherConnectionsPaths.get(path);
			
			if (checkedConnectionWriters.size() > 1 || (otherConnectionWriters != null))
			{
				String conflictInfo = format("Listeners %s to file '%s'", join(checkedConnectionWriters), path);
				
				if (otherConnectionWriters != null)
					conflictInfo += format(" which is already written to by connection's listener %s",
					                              otherConnectionWriters);
				conflicts.add(conflictInfo);
			}
		}

		if (conflicts.size() != 0)
			return format("Connections' listeners are writing the same files:\n%s.", String.join(";\n", conflicts));
		else 
			return null;
	}
	
	private Map<Path, List<String>> findWrittenFilePathsOfCheckedConnection(ClearThMessageConnection connection)
	{
		Map<Path, List<String>> paths = new HashMap<>();
		for (ListenerConfiguration cfg : connection.getListeners())
		{
			for (Path path : getListenersWrittenFilesPaths(cfg))
			{
				List<String> writers = paths.computeIfAbsent(path, p -> new ArrayList<>());
				writers.add(cfg.getName());
			}
		}
		return paths;
	}

	private Map<Path, String> findWrittenFilesPathOfRunningConnections(List<ClearThMessageConnection> connections)
	{
		Map<Path, String> pathsWithWriters = new HashMap<>();
		for (ClearThMessageConnection connection : connections)
		{
			for (ListenerConfiguration cfg : connection.getListeners())
			{
				for (Path path : getListenersWrittenFilesPaths(cfg))
				{
					String writer = format("'%s'('%s')", cfg.getName(), connection.getName());
					pathsWithWriters.put(path, writer);
				}
			}
		}
		return pathsWithWriters;
	}

	private Set<Path> getListenersWrittenFilesPaths(ListenerConfiguration cfg)
	{
		String type = cfg.getType();
		String settings = cfg.getSettings();
		Set<Path> filesPaths = new HashSet<>();
		switch (listenerTypeByLabel(type))
		{
			case File:
			{
				addFilePath(settings, filesPaths);
				break;
			}
			case Collector:
			{
				Map<String, String> settingsMap = KeyValueUtils.parseKeyValueString(settings, ";", true);
				addFilePath(settingsMap.get(ClearThMessageCollector.CONTENTSFILENAME_SETTING), filesPaths);
				break;
			}
			default:
			{
				filesPaths.addAll(getCustomListenersWrittenFilesPath(cfg));
				break;
			}
		}
		return filesPaths;
	}

	protected void addFilePath(String fileName, Set<Path> filesPaths)
	{
		if (StringUtils.isNotBlank(fileName))
		{
			filesPaths.add(Paths.get(ClearThCore.rootRelative(fileName)).normalize());
		}
	}

	protected Set<Path> getCustomListenersWrittenFilesPath(@SuppressWarnings("unused") ListenerConfiguration cfg)
	{
		return Collections.emptySet();
	}
}
