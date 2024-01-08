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

package com.exactprosystems.clearth.web.misc;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DbConnectionsCache extends ProcessedConnectionsCache
{
	private Map<String, DbConnection> connectionMap;
	private List<String> connNames;
	private final ClearThConnectionStorage storage;
	
	public DbConnectionsCache(ClearThConnectionStorage storage)
	{
		this.storage = storage;
		refreshIfNeeded(getConnectionsFromStorage());
	}
	
	@Override
	protected List<ClearThConnection> processConnections(List<ClearThConnection> connections)
	{
		connectionMap = createConnectionMap(connections);
		connNames = new ArrayList<>(connectionMap.keySet());
		return connections;
	}
	
	private List<ClearThConnection> getConnectionsFromStorage()
	{
		return storage.getConnections("DB");
	}
	
	private Map<String, DbConnection> createConnectionMap(List<ClearThConnection> connectionList)
	{
		return connectionList.stream().collect(Collectors.toMap(ClearThConnection::getName,
				connection -> (DbConnection) connection, (a, b) -> b, () -> new LinkedHashMap<>(connectionList.size())));
	}
	
	public DbConnection getConnection(String connectionName)
	{
		refreshIfNeeded(getConnectionsFromStorage());
		return connectionMap.get(connectionName);
	}
	
	public List<String> getConnectionNames()
	{
		refreshIfNeeded(getConnectionsFromStorage());
		return connNames;
	}
}