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

package com.exactprosystems.clearth.connectivity.connections2;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.SettingsException;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface ClearThConnectionStorage
{
	void registerType(ConnectionTypeInfo info) throws SettingsException;

	public Collection<String> getTypes();

	List<ClearThConnection> getConnections();

	default List<ClearThConnection> getConnections(String type)
	{
		return getConnections(type, null, null);
	}

	default List<ClearThConnection> getConnections(Predicate<ClearThConnection> predicate)
	{
		return getConnections(predicate, null);
	}

	<T extends ClearThConnection> List<T> getConnections(Predicate<ClearThConnection> predicate, Class<T> clazz);

	<T extends ClearThConnection> List<T> getConnections(String type, Predicate<ClearThConnection> predicate,
	                                                     Class<T> clazz);

	List<String> listConnections(Predicate<ClearThConnection> predicate);

	boolean containsConnection(String name);

	ClearThConnection getConnection(String name);

	ClearThConnection getConnection(String name, String type);

	ClearThConnection findRunningConnection(String conName) throws ConnectivityException;

	ClearThConnection findRunningConnection(String conName, String type) throws ConnectivityException;

	void autoStartConnections();

	void stopAllConnections();

	ClearThConnection createConnection(String type) throws ConnectivityException;

	void addConnection(ClearThConnection connection) throws ConnectivityException,
			SettingsException;

	void renameConnection(ClearThConnection connection, String newName)
			throws ConnectivityException, SettingsException;

	void modifyConnection(ClearThConnection connectionToModify, ClearThConnection connectionWithNewSettings)
			throws ConnectivityException, SettingsException;

	void removeConnection(ClearThConnection connection) throws ConnectivityException;

	void loadConnections();

	void reloadConnections();

	void validateConnectionStart(ClearThConnection connection) throws ConnectivityException, SettingsException;

}
