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

package com.exactprosystems.clearth.automation.actions.th2.check1;

import com.exactpro.th2.check1.grpc.Check1Service;
import com.exactpro.th2.common.grpc.Checkpoint;
import com.exactpro.th2.common.schema.grpc.router.GrpcRouter;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;

public class Check1Utils
{
	public static final String CONTEXT_GRPC_ROUTER = "Th2GrpcRouter",
			CONTEXT_SERVICE = "Th2Check1Service";
	
	public static Check1Service getService(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		synchronized (globalContext)
		{
			Check1Service service = globalContext.getLoadedContext(CONTEXT_SERVICE);
			if (service != null)
				return service;
			
			try
			{
				//Router is not closed here. It is stored in GlobalContext for re-use by other actions and will be closed when Scheduler finishes execution
				GrpcRouter router = getRouter(globalContext, th2Factory);
				service = router.getService(Check1Service.class);
				globalContext.setLoadedContext(CONTEXT_SERVICE, service);
				return service;
			}
			catch (Exception e)
			{
				throw ResultException.failed("Could not get Check1 service", e);
			}
		}
	}
	
	public static GrpcRouter getRouter(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		synchronized (globalContext)
		{
			GrpcRouter router = globalContext.getCloseableContext(CONTEXT_GRPC_ROUTER);
			if (router == null)
			{
				router = th2Factory.createGrpcRouter();
				globalContext.setCloseableContext(CONTEXT_GRPC_ROUTER, router);
			}
			return router;
		}
	}
	
	public static String getCheckpointName(String actionId)
	{
		return "Th2Checkpoint_"+actionId;
	}
	
	public static Checkpoint getCheckpoint(String actionId, MatrixContext context)
	{
		return context.getContext(getCheckpointName(actionId));
	}
	
	public static void setCheckpoint(String actionId, MatrixContext context, Checkpoint checkpoint)
	{
		context.setContext(getCheckpointName(actionId), checkpoint);
	}
}
