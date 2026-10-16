/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThRunnableConnection;
import com.exactprosystems.clearth.utils.CollectionUtils;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.api.helpers.ClearTHConnectionDataResource;
import com.exactprosystems.clearth.web.api.helpers.ClearTHConnectionSettingsDataResource;
import com.exactprosystems.clearth.web.api.helpers.ClearTHListenerDataResource;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.exactprosystems.clearth.ClearThCore.connectionStorage;
import static com.exactprosystems.clearth.web.api.Automation.PARAMETER_NOT_SPECIFIED_MSG;

@Path("connections")
public class Connectivity
{
	private final File uploadStorage = new File(ClearThCore.uploadStoragePath());
	protected ConnectionActionGetter connectionActionGetter = new ConnectionActionGetter();

	private List<ClearThConnection> findConnections(String type)
	{
		List<ClearThConnection> cons = connectionStorage().getConnections(type);
		if (cons != null)
			return cons;
		else
			throw new WebApplicationException(ApiUtils.badRequest(type + " connections are not found"));
	}
	
	private ClearThConnection findConnection(String name)
	{
		ClearThConnection con = connectionStorage().getConnection(name);
		if (con != null)
			return con;
		else
			throw new WebApplicationException(ApiUtils.badRequest("Connection with name '" + name + "' not found"));
	}
	
	private static String joinMessageList(List<String> messageList)
	{
		return CollectionUtils.join(messageList, "", System.lineSeparator());
	}
	
	/**
	 * Apply an action to all connections that have type connectionType
	 * @param connectionsType
	 * @param action
	 * @return
	 */
	@GET
	@Path("all")
	public Response doWithConnections(@QueryParam(RestConstants.PARAM_TYPE) String connectionsType,
	                                  @QueryParam(RestConstants.PARAM_ACTION) String action)
	{
		if (StringUtils.isEmpty(action))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_ACTION));
		
		if (StringUtils.isEmpty(connectionsType))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_TYPE));
		
		List<ClearThConnection> connections = findConnections(connectionsType);
		List<String> successList = new ArrayList<>();
		List<String> errorList = new ArrayList<>();
		
		ConnectionAction connectionAction = connectionActionGetter.getAction(action);
		for (ClearThConnection connection : connections)
		{
			try
			{
				successList.add(connectionAction.execute(connection));
			} catch (Exception e)
			{
				errorList.add(String.format("Could not %s connection '%s'" + System.lineSeparator() + "%s",
				                            action, connection.getName(), ExceptionUtils.getDetailedMessage(e)));
			}
		}
		String successes = joinMessageList(successList);
		if (errorList.isEmpty())
			return ApiUtils.ok(String.format("Action %s is successfully applied to all connections.", action) + System.lineSeparator() + successes);
		else
			return ApiUtils.error(joinMessageList(errorList) + System.lineSeparator() + successes);
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}")
	public Response doWithConnection(@PathParam(RestConstants.PARAM_NAME) String connectionName,
	                                 @QueryParam(RestConstants.PARAM_ACTION) String action)
	{
		if (StringUtils.isEmpty(action))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_ACTION));
		
		ClearThConnection connection = findConnection(connectionName);
		
		ConnectionAction connectionAction = connectionActionGetter.getAction(action);
		try
		{
			return ApiUtils.ok(connectionAction.execute(connection));
		}
		catch (Exception e)
		{
			return ApiUtils.error(String.format("Could not %s connection '%s'", action, connectionName),
					ExceptionUtils.getDetailedMessage(e));
		}
	}

	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/"+RestConstants.PARAM_STATUS)
	public Response getConnectionStatus(@PathParam(RestConstants.PARAM_NAME) String connectionName)
	{
		ClearThConnection connection = findConnection(connectionName);
		if (connection instanceof ClearThRunnableConnection)
			return ApiUtils.ok(((ClearThRunnableConnection)connection).isRunning() ? "Running" : "Stopped");
		return ApiUtils.ok("Not runnable");
	}

	@GET
	@Path("list")
	@Produces(MediaType.TEXT_XML)
	public Response getConnections()
	{
		List<ClearTHConnectionDataResource> cdr = new ArrayList<ClearTHConnectionDataResource>();
		for (ClearThConnection connection : connectionStorage().getConnections())
		{
			cdr.add(new ClearTHConnectionDataResource(connection));
		}
		return ApiUtils.ok(cdr, ClearTHConnectionDataResource.class);
	}

	@GET
	@Path("detailedlist")
	@Produces(MediaType.TEXT_XML)
	public Response getConnectionsWithDetails()
	{
		List<ClearTHConnectionSettingsDataResource> cdr = new ArrayList<ClearTHConnectionSettingsDataResource>();
		for (ClearThConnection con : connectionStorage().getConnections())
		{
			cdr.add(new ClearTHConnectionSettingsDataResource(con));
		}

		return ApiUtils.ok(cdr, ClearTHConnectionSettingsDataResource.class);
	}

	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/listeners")
	@Produces(MediaType.TEXT_XML)
	public Response getListeners(@PathParam(RestConstants.PARAM_NAME) String connectionName)
	{
		ClearThConnection con = findConnection(connectionName);
		if (!(con instanceof ClearThMessageConnection))
			return ApiUtils.badRequest("Connection with name '"+connectionName+"' doesn't support listeners");
		
		ClearThMessageConnection connection = (ClearThMessageConnection) con; 

		List<ClearTHListenerDataResource> ldr = new ArrayList<ClearTHListenerDataResource>();
		for (ListenerConfiguration configuration : connection.getListeners())
			ldr.add(new ClearTHListenerDataResource(configuration));

		return ApiUtils.ok(ldr, ClearTHListenerDataResource.class);
	}

	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/settings")
	@Produces(MediaType.TEXT_XML)
	public Response getSettings(@PathParam(RestConstants.PARAM_NAME) String connectionName)
	{
		ClearThConnection connection = findConnection(connectionName);
		ClearTHConnectionSettingsDataResource dataResource = new ClearTHConnectionSettingsDataResource(connection);
		return ApiUtils.ok(dataResource, ClearTHConnectionSettingsDataResource.class);
	}


	@GET
	@Path("export")
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response getConnectionsZip(@QueryParam(RestConstants.PARAM_TYPE) String connectionsType,
	                                  @Context HttpServletRequest request)
	{
		if (StringUtils.isEmpty(connectionsType))
			return ApiUtils.badRequest(RestConstants.PARAM_TYPE+" parameter is mandatory");
		
		File resultFile;
		try
		{
			resultFile = ClearThCore.getInstance().getConnectionsTransmitter().exportConnections(connectionsType);
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while exporting connections",
					ExceptionUtils.getDetailedMessage(e));
		}
		return ApiUtils.fileResponse(resultFile);
	}

	@POST
	@Path("import")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadConnectionsZip(@QueryParam(RestConstants.PARAM_TYPE) String connectionsType,
	                                     @FormDataParam("file") InputStream connectionsStream,
										 @FormDataParam("file") FormDataContentDisposition connectionsDetail,
										 @Context HttpServletRequest request)
	{
		if (connectionsStream == null)
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_FILE));
		
		if (StringUtils.isEmpty(connectionsType))
			return ApiUtils.badRequest(RestConstants.PARAM_TYPE+" parameter is mandatory");
		
		try
		{
			File storedFile = FileOperationUtils.storeToFile(connectionsStream, uploadStorage, connectionsType + "_connections_", ".zip");
			ClearThCore.getInstance().getConnectionsTransmitter().deployConnections(connectionsType, storedFile);
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while importing connections",
					ExceptionUtils.getDetailedMessage(e));
		}
		return ApiUtils.ok("Connections applied successfully");
	}
}
