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

package com.exactprosystems.clearth.connectivity.http;

import java.io.File;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.SettingsException;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.ICodec;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class HttpDictionary 
{
	protected final Map<String, HttpRequestDesc> requestDescriptions;
	protected final Map<String, ICodec> codecs;
	private Map<String, Set<String>> httpParametersNames = new HashMap<String, Set<String>>();
	
	public HttpDictionary(String fileName) throws DictionaryLoadException, SettingsException
	{
		HttpDictionaryDesc dictionaryDesc = loadDictionary(fileName);
		requestDescriptions = toDescriptionsMap(dictionaryDesc);
		codecs = initCodecs(dictionaryDesc);
	}

	protected Map<String, ICodec> initCodecs(HttpDictionaryDesc dictionaryDesc) throws SettingsException 
	{
		Map<String, ICodec> result = new HashMap<String, ICodec>();
		for(HttpRequestDesc rd : dictionaryDesc.getRequestDesc()) 
		{
			if(rd.getBody() != null) 
			{
				String codecName = rd.getBody().getCodec();
				if(!result.containsKey(codecName) && !isBlank(codecName)) 
					result.put(codecName, ClearThCore.getInstance().createCodec(codecName));
			}
		}
		return result;
	}

	protected HttpDictionaryDesc loadDictionary(String fileName) throws DictionaryLoadException
	{
		try 
		{
			Unmarshaller u = JAXBContext.newInstance(getClassesToBeBound()).createUnmarshaller();
			@SuppressWarnings("unchecked") JAXBElement<HttpDictionaryDesc> root = 
					u.unmarshal(new StreamSource(new File(fileName)), getDictionaryDescClass());
			return root.getValue();
		}
		catch(JAXBException e) 
		{
			throw new DictionaryLoadException("Error occurred while parsing dictionary", e);
		}
	}
	
	protected Class[] getClassesToBeBound()
	{
		return new Class[] { HttpDictionaryDesc.class };
	}
	
	protected Class getDictionaryDescClass()
	{
		return HttpDictionaryDesc.class;
	}
	
	private Map<String, HttpRequestDesc> toDescriptionsMap(HttpDictionaryDesc dictionaryDesc) throws DictionaryLoadException
	{
		Map<String, HttpQueryParametersBlock> queryParametersBlockMap = new HashMap<>();
		for (HttpQueryParametersBlock params : dictionaryDesc.getCommonQueryParameters())
		{
			queryParametersBlockMap.put(params.getName(), params);
		}

		Map<String, HttpRequestDesc> map = new LinkedHashMap<String, HttpRequestDesc>();
		for (HttpRequestDesc rd : dictionaryDesc.getRequestDesc())
		{
			String requestType = rd.getType();
			if(rd.getMethod() == null)
				throw new DictionaryLoadException(format("Invalid description for request '%s': Method should be one of: %s",
						requestType, Arrays.asList(HttpMethod.values())));

			solveReferences(queryParametersBlockMap, rd.getQueryParametersBlock(), requestType, new HashSet<>());
			
			map.put(requestType, rd);
		}
		return map;
	}

	private void solveReferences(Map<String, HttpQueryParametersBlock> queryParametersBlockMap, HttpQueryParametersBlock queryParametersBlock,
			String requestType, Set<String> references) throws DictionaryLoadException
	{
		if(queryParametersBlock == null)
			return;

		ListIterator<HttpCommonParametersLink> it = queryParametersBlock.getCommonParameters().listIterator();
		while (it.hasNext())
		{
			String reference = it.next().getReference();
			if(!references.add(reference))
				throw new DictionaryLoadException(String.format("Duplicate reference to commonQueryParameters" +
						" with name='%s' found in requestDesc='%s'", reference, requestType));
			HttpQueryParametersBlock commonQueryParams = findCommonQueryParams(queryParametersBlockMap, reference);
			solveReferences(queryParametersBlockMap, commonQueryParams, requestType, references);
			queryParametersBlock.getParameters().addAll(commonQueryParams.getParameters());
			it.remove();
		}
	}

	private HttpQueryParametersBlock findCommonQueryParams(Map<String, HttpQueryParametersBlock> queryParametersBlockMap, String reference)
			throws DictionaryLoadException
	{
		HttpQueryParametersBlock params = queryParametersBlockMap.get(reference);
		if (params == null)
			throw new DictionaryLoadException(String.format("No commonQueryParameters with name='%s' found in dictionary", reference));

		return params;
	}

	public HttpRequestDesc findRequestDesc(String httpRequestType) 
	{
		return requestDescriptions.get(httpRequestType);
	}
	
	public ICodec getCodec(HttpRequestDesc requestDesc) 
	{
		HttpBody body = requestDesc.getBody();
		return (body != null) ? codecs.get(body.getCodec()) : null;
	}
	
	public Set<String> getHttpParametersNames(String httpRequestType)
	{
		HttpRequestDesc rd = findRequestDesc(httpRequestType);
		if(rd == null)
			return null;
		
		Set<String> names = httpParametersNames.get(httpRequestType);
		if(names == null) 
		{
			names = new HashSet<String>();
			
			if(rd.getPathParametersBlock() != null) 
			{
				for(HttpParameter pp : rd.getPathParametersBlock().getParameters())
				{
					names.add(pp.getName());
				}
			}
			if(rd.getQueryParametersBlock() != null) 
			{
				for(HttpParameter qp : rd.getQueryParametersBlock().getParameters())
				{
					names.add(qp.getName());
				}
			}			
			httpParametersNames.put(httpRequestType, names);
		}
		return names;
	}

	public Map<String, ICodec> getCodecs()
	{
		return codecs;
	}
}
