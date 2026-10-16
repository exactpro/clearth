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

package com.exactprosystems.clearth.web.api;

import static com.exactprosystems.clearth.ClearThCore.connectionStorage;
import static com.exactprosystems.clearth.web.api.RestConstants.ACTION_CHECK;
import static com.exactprosystems.clearth.web.api.RestConstants.ACTION_REMOVE;
import static com.exactprosystems.clearth.web.api.RestConstants.ACTION_START;
import static com.exactprosystems.clearth.web.api.RestConstants.ACTION_STOP;

import javax.ws.rs.WebApplicationException;

import com.exactprosystems.clearth.connectivity.connections.ClearThCheckableConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThRunnableConnection;

public class ConnectionActionGetter
{
	public ConnectionAction getAction(String connectionAction)
	{
		connectionAction = connectionAction.toLowerCase();
		switch (connectionAction)
		{
			case ACTION_START:
				return connection ->
				{
					if (!(connection instanceof ClearThRunnableConnection))
						return notRunnable(connection);
					
					((ClearThRunnableConnection)connection).start();
					return String.format("Connection '%s' started successfully", connection.getName());
				};
			case ACTION_STOP:
				return connection ->
				{
					if (!(connection instanceof ClearThRunnableConnection))
						return notRunnable(connection);
					
					((ClearThRunnableConnection)connection).stop();
					return String.format("Connection '%s' stopped successfully", connection.getName());
				};
			case ACTION_REMOVE:
				return connection ->
				{
					connectionStorage().removeConnection(connection);
					return String.format("Connection '%s' removed successfully", connection.getName());
				};
			case ACTION_CHECK:
				return connection ->
				{
					if (!(connection instanceof ClearThCheckableConnection))
						return String.format("Connection '%s' is not checkable", connection.getName());
					((ClearThCheckableConnection) connection).check();
					return String.format("Connection '%s' checked successfully", connection.getName());
				};
			default:
				throw new WebApplicationException(ApiUtils.badRequest(String.format("Invalid action - '%s'", connectionAction)));
		}
	}
	
	private String notRunnable(ClearThConnection connection)
	{
		return String.format("Connection '%s' is not runanble", connection.getName());
	}
}
