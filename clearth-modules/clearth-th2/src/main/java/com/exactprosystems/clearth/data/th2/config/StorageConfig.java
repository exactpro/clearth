/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data.th2.config;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StorageConfig
{
	private EventsConfig events;
	
	public StorageConfig()
	{
	}
	
	public StorageConfig(EventsConfig events)
	{
		this.events = events;
	}
	
	public static StorageConfig load(Path file) throws StreamReadException, DatabindException, IOException
	{
		return new ObjectMapper().readValue(file.toFile(), StorageConfig.class);
	}
	
	
	@Override
	public String toString()
	{
		return "[events: " + events + "]";
	}
	
	
	public EventsConfig getEvents()
	{
		return events;
	}
	
	public void setEvents(EventsConfig events)
	{
		this.events = events;
	}
}
