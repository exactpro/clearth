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

package com.exactprosystems.clearth.connectivity.th2;

import com.exactpro.th2.common.schema.grpc.router.GrpcRouter;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;

public class GrpcRouterGetter
{
	public static final String CONTEXT_ROUTER = "Th2GrpcRouter";
	
	protected final GlobalContext globalContext;
	protected final Th2DataHandlersFactory th2Factory;
	
	public GrpcRouterGetter(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		this.globalContext = globalContext;
		this.th2Factory = th2Factory;
	}
	
	/**
	 * Returns {@link GrpcRouter} stored in {@link GlobalContext}. If no router is stored, creates and stores new one.
	 * The router should not be closed after use. It will be closed when {@link GlobalContext} is cleaned
	 * @return GrpcRouter from GlobalContext or newly created GrpcRouter
	 * @throws ConnectionException if GrpcRouter cannot be created
	 */
	public GrpcRouter get() throws ConnectionException
	{
		GrpcRouter router = globalContext.getCloseableContext(CONTEXT_ROUTER);
		if (router != null)
			return router;
		
		try
		{
			router = th2Factory.createGrpcRouter();
			globalContext.setCloseableContext(CONTEXT_ROUTER, router);
			return router;
		}
		catch (Exception e)
		{
			throw new ConnectionException("Could not create router", e);
		}
	}
}
