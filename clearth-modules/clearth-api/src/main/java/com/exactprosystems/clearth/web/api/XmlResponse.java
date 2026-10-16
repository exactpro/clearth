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

import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XmlRootElement(name="response")
@XStreamAlias("response")
public class XmlResponse
{
	private String message, error;
	private Object data;
	
	public XmlResponse()
	{
		message = null;
		error = null;
		data = null;
	}
	
	public XmlResponse(String message, String error, Object data)
	{
		this.message = message;
		this.error = error;
		this.data = data;
	}
	
	
	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	
	public String getError()
	{
		return error;
	}

	public void setError(String error)
	{
		this.error = error;
	}

	
	public Object getData()
	{
		return data;
	}

	public void setData(Object data)
	{
		this.data = data;
	}
	
	
	public String toXml(Class[] dataClasses)
	{
		return toXml(dataClasses, new XStream());
	}
	
	public String toXml(Class[] dataClasses, XStream xs)
	{
		Class[] annotated;
		if (dataClasses != null)
		{
			annotated = new Class[dataClasses.length+1];
			for (int i = 0; i < dataClasses.length; i++)
				annotated[i] = dataClasses[i];
			annotated[annotated.length-1] = this.getClass();
		}
		else
		{
			annotated = new Class[]{this.getClass()};
		}
		
		xs.processAnnotations(annotated);
		return xs.toXML(this);
	}
}
