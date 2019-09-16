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

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.primefaces.PrimeFaces;
import org.primefaces.context.RequestContext;

import com.exactprosystems.clearth.web.beans.UserBean;


public class ExpiredSessionPhaseListener extends UserBean implements PhaseListener
{
	String SESSION_EXPIRED_PAGE_CONTEXT_PARAM = "session_expired_page";

	public void afterPhase(PhaseEvent event)
	{
	}

	public void beforePhase(PhaseEvent event)
	{
		if (!UserInfoUtils.isLoggedIn())
		{
			FacesContext fc = FacesContext.getCurrentInstance();
			RequestContext rc = RequestContext.getCurrentInstance();
			ExternalContext ec = fc.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ec.getResponse();
			HttpServletRequest request = (HttpServletRequest) ec.getRequest();

			
			String page = ec.getInitParameter(SESSION_EXPIRED_PAGE_CONTEXT_PARAM);
			
			if (page == null)
			{
				getLogger().error("Redirect page for expired session is not described in web.xml. Please, add context parameter '"+SESSION_EXPIRED_PAGE_CONTEXT_PARAM+"'");
				return;
			}
			
			String url =  ec.getRequestContextPath() + page;

			if (ec.isResponseCommitted())
			{
				return;
			}

			try
			{
				if (((rc != null && PrimeFaces.current().isAjaxRequest()) || (fc.getPartialViewContext().isPartialRequest()))
						&& fc.getResponseWriter() == null && fc.getRenderKit() == null)
				{
					response.setCharacterEncoding(request.getCharacterEncoding());

					RenderKitFactory factory = (RenderKitFactory) FactoryFinder
							.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
					
					ViewHandler viewHandler = fc.getApplication().getViewHandler();

					RenderKit renderKit = factory.getRenderKit(fc, viewHandler.calculateRenderKitId(fc));

					ResponseWriter responseWriter = renderKit.createResponseWriter(response.getWriter(), null,
							request.getCharacterEncoding());
					fc.setResponseWriter(responseWriter);
					
					if (fc.getViewRoot() == null) {
						UIViewRoot view = viewHandler.createView(fc, "");
						fc.setViewRoot(view); 
					}
					
					ec.redirect(url);
					
				}

			} catch (Exception e)
			{
				getLogger().error("Redirect to the specified page '" + url + "' failed");
				throw new FacesException(e);
			}
		}
	}

	public PhaseId getPhaseId()
	{
		return PhaseId.RESTORE_VIEW;
	}

}
