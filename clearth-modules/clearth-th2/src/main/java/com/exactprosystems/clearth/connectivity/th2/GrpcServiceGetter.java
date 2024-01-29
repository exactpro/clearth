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

public abstract class GrpcServiceGetter<S>
{
	protected final GlobalContext globalContext;
	protected final Th2DataHandlersFactory th2Factory;
	
	public GrpcServiceGetter(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		this.globalContext = globalContext;
		this.th2Factory = th2Factory;
	}
	
	protected abstract String getContextName();
	protected abstract String getServiceName();
	protected abstract Class<S> getServiceClass();
	
	
	public S get() throws ConnectionException
	{
		String contextName = getContextName();
		S service = globalContext.getLoadedContext(contextName);
		if (service != null)
			return service;
		
		GrpcRouter router = getRouter();
		try
		{
			service = router.getService(getServiceClass());
			globalContext.setLoadedContext(contextName, service);
			return service;
		}
		catch (Exception e)
		{
			throw new ConnectionException("Could not get "+getServiceName()+" service", e);
		}
	}
	
	
	protected GrpcRouter getRouter() throws ConnectionException
	{
		return new GrpcRouterGetter(globalContext, th2Factory)
				.get();
	}
}