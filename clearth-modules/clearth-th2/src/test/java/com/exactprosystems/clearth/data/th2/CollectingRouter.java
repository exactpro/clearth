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

package com.exactprosystems.clearth.data.th2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.exactpro.th2.common.schema.message.ExclusiveSubscriberMonitor;
import com.exactpro.th2.common.schema.message.MessageListener;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.MessageRouterContext;
import com.exactpro.th2.common.schema.message.SubscriberMonitor;

public class CollectingRouter<M> implements MessageRouter<M>
{
	private final List<M> sent = new ArrayList<>();
	
	@Override
	public void close() throws Exception
	{
	}
	
	@Override
	public void init(@NotNull MessageRouterContext context)
	{
	}
	
	@Override
	public ExclusiveSubscriberMonitor subscribeExclusive(MessageListener<M> callback)
	{
		return null;
	}
	
	@Override
	public SubscriberMonitor subscribe(MessageListener<M> callback, String... queueAttr)
	{
		return null;
	}
	
	@Override
	public SubscriberMonitor subscribeAll(MessageListener<M> callback, String... queueAttr)
	{
		return null;
	}
	
	@Override
	public void sendExclusive(String queue, M message) throws IOException
	{
		store(message);
	}
	
	@Override
	public void send(M message, String... queueAttr) throws IOException
	{
		store(message);
	}
	
	@Override
	public void sendAll(M message, String... queueAttr) throws IOException
	{
		store(message);
	}
	
	
	public List<M> getSent()
	{
		return sent;
	}
	
	public void clearSent()
	{
		sent.clear();
	}
	
	
	private void store(M message)
	{
		sent.add(message);
	}
}