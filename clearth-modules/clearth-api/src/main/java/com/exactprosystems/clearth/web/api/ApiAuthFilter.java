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

package com.exactprosystems.clearth.web.api;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.exactprosystems.clearth.ClearThCore;

public class ApiAuthFilter implements Filter
{
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
	{
		if (ClearThCore.getInstance().isApiAuthRequired())
		{
			if (req instanceof HttpServletRequest)
			{
				HttpServletRequest request = (HttpServletRequest)req;
				String[] authInfo = ApiUtils.getAuthorizationUser(request.getHeader(ApiUtils.HEADER_AUTH));
				if ((authInfo == null) || (ClearThCore.getInstance().getUsersManager().isUserAllowed(authInfo[0], authInfo[1])) == null)
					throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
			}
		}
		chain.doFilter(req, resp);
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException
	{
	}
}
