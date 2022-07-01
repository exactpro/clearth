/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.web.beans.AuthBean;

public class AuthenticationFilter implements Filter
{
	private static final String PARAMETER = "requestUrl";
	
	protected String LOGIN_PAGE = "/ui/login.jsf", HOME_PAGE = "/ui/restricted/home.jsf", PAGEACCESS_CFG = "pageaccess.cfg";
	private Map<String, String[]> pages = new HashMap<String, String[]>();
	
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest)req;
		HttpSession session = request.getSession();
		String context = request.getContextPath();
		if (StringUtils.isNotEmpty(context) && !context.startsWith("/"))
			context = "/"+context;
		HttpServletResponse response = (HttpServletResponse)resp;
		
		if (session.getAttribute(AuthBean.AUTH_KEY) == null)
		{
			response.sendRedirect(context+LOGIN_PAGE+"?"+PARAMETER+"="+request.getRequestURI().replace("/", "%2F"));
		}
		else
		{
			String sessionRole = (String)session.getAttribute(AuthBean.ROLE_KEY);
			if (sessionRole == null)  //Role not specified. Go away, Anonymous!
				((HttpServletResponse)resp).sendRedirect(context+HOME_PAGE);
			
			String[] roles = pages.get(request.getRequestURI());
			if ((roles == null) || (roles.length == 0))  //No restriction specified
			{
				chain.doFilter(req, resp);
				return;
			}
			
			boolean found = false;
			for (String role : roles)
				if (role.equals(sessionRole))
				{
					found = true;
					break;
				}
			
			if (found)  //Role found in list of permitted roles for this page
				chain.doFilter(req, resp);
			else
				((HttpServletResponse)resp).sendRedirect(context+HOME_PAGE);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			BufferedReader reader = null;
			try
			{
				reader = new BufferedReader(new FileReader(ClearThCore.rootRelative("cfg")+File.separator+PAGEACCESS_CFG));
				String line;
				while ((line = reader.readLine()) != null)
				{
					String[] pageAccess = line.split("=");
					pages.put(pageAccess[0], pageAccess[1].split(","));
				}
			}
			finally
			{
				if (reader!=null)
					reader.close();
			}
		}
		catch (IOException e)
		{
			throw new ServletException("File with page-role restrictions not found, all pages are enabled for any role", e);
		}
		catch (Exception e)
		{
			throw new ServletException("Error occurred while loading page-role restrictions from file", e);
		}
	}

	public void destroy()
	{

	}
}
