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

package com.exactprosystems.clearth.messages.th2;

import java.util.Collection;

public class SendingProperties
{
	private String book, 
			sessionGroup;
	private Collection<String> routerAttrs;
	
	@Override
	public String toString()
	{
		return "[book=" + book + ", sessionGroup=" + sessionGroup + ", routerAttrs=" + routerAttrs + "]";
	}
	
	
	public String getBook()
	{
		return book;
	}
	
	public void setBook(String book)
	{
		this.book = book;
	}
	
	
	public String getSessionGroup()
	{
		return sessionGroup;
	}
	
	public void setSessionGroup(String sessionGroup)
	{
		this.sessionGroup = sessionGroup;
	}
	
	
	public Collection<String> getRouterAttrs()
	{
		return routerAttrs;
	}
	
	public void setRouterAttrs(Collection<String> routerAttrs)
	{
		this.routerAttrs = routerAttrs;
	}
}
