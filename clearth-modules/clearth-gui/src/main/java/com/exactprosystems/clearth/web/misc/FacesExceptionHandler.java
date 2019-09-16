/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import com.exactprosystems.clearth.web.misc.FacesExceptionHandler;

public class FacesExceptionHandler extends ExceptionHandlerWrapper
{
	private ExceptionHandler wrapped;
	
	public FacesExceptionHandler(ExceptionHandler wrapped)
	{
		this.wrapped = wrapped;
	}
	
	@Override
	public ExceptionHandler getWrapped()
	{
		return this.wrapped;
	}
	
	@Override
	public void handle() throws FacesException
	{
		Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator();
		while (it.hasNext())
		{
			ExceptionQueuedEvent event = it.next();
			ExceptionQueuedEventContext context = (ExceptionQueuedEventContext)event.getSource();
			Throwable t = context.getException();
			if (t != null)
			{
				do
				{
					if (t instanceof ViewExpiredException)
					{
						ViewExpiredException vee = (ViewExpiredException)t;
						FacesContext facesContext = FacesContext.getCurrentInstance();
						Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
						NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
						try
						{
							requestMap.put("currentViewId", vee.getViewId());
							navigationHandler.handleNavigation(facesContext, null, "/ui/session_expired.jsf");
							facesContext.renderResponse();
						}
						finally
						{
							it.remove();
						}
						break;
					}
					else
					{
						t = t.getCause();
					}
				}
				while (t != null);
			}
		}
		
		getWrapped().handle();
	}
}
