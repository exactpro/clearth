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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;

public class FavoritesSortedCache extends ProcessedCache<ClearThConnection, ClearThConnection>
{
	private final Set<String> favorites;
	private List<String> connectionNames;
	
	public FavoritesSortedCache(Set<String> favorites)
	{
		this.favorites = favorites;
	}
	
	@Override
	protected List<ClearThConnection> processData(List<ClearThConnection> connections)
	{
		List<ClearThConnection> result = new ArrayList<>(connections);
		
		if (!isSorted(result))
		{
			result.sort((o1, o2) ->
			{
				return connectionComparison(o1, o2);
			});
		}
		connectionNames = createConnectionNamesList(result);
		return result;
	}
	
	@Override
	protected boolean isNeedRefresh(List<ClearThConnection> currentConnections)
	{
		return super.isNeedRefresh(currentConnections) || !isSorted(getProcessedData());  //Checks if state of processed connections has changed so we need to re-process them
	}
	
	public Set<String> getFavorites()
	{
		return favorites;
	}
	
	public List<String> getConnectionNames()
	{
		return connectionNames;
	}
	
	private List<String> createConnectionNamesList(List<ClearThConnection> connections)
	{
		return connections.stream().map(ClearThConnection::getName).collect(Collectors.toList());
	}
	
	private boolean isSorted(List<ClearThConnection> connections)
	{
		if (connections.size() < 2)
			return true;
		
		ClearThConnection prev = null;
		for (ClearThConnection current : connections)
		{
			if (prev != null && connectionComparison(prev, current) > 0)
				return false;
			prev = current;
		}
		return true;
	}
	
	private boolean isFavorite(ClearThConnection con)
	{
		return getFavorites().contains(con.getName());
	}
	
	private int connectionComparison(ClearThConnection o1, ClearThConnection o2)
	{
		boolean isC1Fav = isFavorite(o1);
		return isC1Fav == isFavorite(o2) ?  o1.getName().compareToIgnoreCase(o2.getName()) : (isC1Fav) ? -1 : 1;
	}
}
