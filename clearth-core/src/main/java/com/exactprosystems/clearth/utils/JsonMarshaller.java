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

package com.exactprosystems.clearth.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMarshaller<T>
{
	private ObjectMapper objectMapper;


	public JsonMarshaller()
	{
		objectMapper = new ObjectMapper();
		configure();
	}

	public JsonMarshaller(JsonFactory jsonFactory)
	{
		objectMapper = new ObjectMapper(jsonFactory);
		configure();
	}
	
	public JsonMarshaller(ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}

	protected void configure()
	{
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.registerModule(new JavaTimeModule());
	}


	public String marshal(T obj) throws IOException
	{
		return objectMapper.writeValueAsString(obj);
	}

	public void marshal(T obj, String destPath) throws IOException
	{
		objectMapper.writeValue(new File(destPath), obj);
	}


	/**
	 * To unmarshal generic types use {@link #unmarshal(String, Class)}
	 * method, created because of Type Erasure.
	 */
	public T unmarshal(String jsonString) throws IOException
	{
		return objectMapper.readValue(jsonString, new TypeReference<T>(){});
	}

	/**
	 * This method is equivalent of {@link #unmarshal(String)} method
	 * for generic types due to Type Erasure.
	 */
	public T unmarshal(String jsonString, Class<T> targetClass) throws IOException
	{
		return objectMapper.readValue(jsonString, targetClass);
	}

	/**
	 * To unmarshal generic types use {@link #unmarshal(File, Class)}
	 * method, created because of Type Erasure.
	 */
	public T unmarshal(File src) throws IOException
	{
		return objectMapper.readValue(src, new TypeReference<T>(){});
	}

	/**
	 * This method is equivalent of {@link #unmarshal(File)} method
	 * for generic types due to Type Erasure.
	 */
	public T unmarshal(File src, Class<T> targetClass) throws IOException
	{
		return objectMapper.readValue(src, targetClass);
	}

	/**
	 * To unmarshal generic types use {@link #unmarshal(Path, Class)}
	 * method, created because of Type Erasure.
	 */
	public T unmarshal(Path destPath) throws IOException
	{
		return unmarshal(destPath.toFile());
	}

	/**
	 * This method is equivalent of {@link #unmarshal(Path)} method
	 * for generic types due to Type Erasure.
	 */
	public T unmarshal(Path destPath, Class<T> targetClass) throws IOException
	{
		return unmarshal(destPath.toFile(), targetClass);
	}
	
	
	public ObjectMapper getObjectMapper()
	{
		return objectMapper;
	}
}
