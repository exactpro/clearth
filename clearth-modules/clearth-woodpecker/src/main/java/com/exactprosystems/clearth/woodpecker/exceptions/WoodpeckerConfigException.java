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

package com.exactprosystems.clearth.woodpecker.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class WoodpeckerConfigException extends WoodpeckerException
{
	private static final Logger log = LoggerFactory.getLogger(WoodpeckerConfigException.class);

	public static WoodpeckerConfigException whenFileNotFound(Path path)
	{
		return new WoodpeckerConfigException(format("File '%s' not found.", path.toAbsolutePath().normalize()));
	}
	
	public static WoodpeckerConfigException whenFileNotFound(Path path, String fileDescription)
	{
		return new WoodpeckerConfigException(format("%s file '%s' not found.", 
				fileDescription, 
				path.toAbsolutePath().normalize()));
	}
	
	public static WoodpeckerConfigException whenFileNotFound(String relativePath, List<Path> checkedLocations)
	{
		return new WoodpeckerConfigException(format("File '%s' not found in the following locations: %s", relativePath,
				checkedLocations.stream()
						.map(p -> "'" + p + "'")
						.collect(joining(", "))));
	}
	
	public static WoodpeckerConfigException whenInvalidContent(Path path, String fileDescription, String errorDescription)
	{
		return new WoodpeckerConfigException(format("Error in %s file '%s': %s.", 
				fileDescription, path, errorDescription));
	}
	
	public static WoodpeckerConfigException whenLoadingFailed(Path path, String fileDescription, Exception e)
	{
		if ((e instanceof FileNotFoundException) || (e instanceof NoSuchFileException))
			return whenFileNotFound(path, fileDescription);
		
		if (e instanceof IOException)
			return new WoodpeckerConfigException(format("Error while loading %s from file '%s': %s", 
					fileDescription, path, e.getMessage()), e);
		
		if (e instanceof JAXBException)
			return new WoodpeckerConfigException(format("Error while loading %s from file '%s'. Invalid XML content: %s",
					fileDescription, path, getMessage((JAXBException) e)), e);
		
		if (e instanceof RuntimeException)
		{
			String msg = format("Unexpected error while loading %s from file '%s'.", fileDescription, path);
			log.error(msg, e); // Let's log it here additionally to avoid missing of stacktrace.
			return new WoodpeckerConfigException(msg, e);
		}

		return new WoodpeckerConfigException(format("Error while loading %s from file '%s'.",
				fileDescription, path), e);
	}
	
	private static String getMessage(JAXBException e)
	{
		Throwable linkedEx = e.getLinkedException();
		if (linkedEx != null)
		{
			if (linkedEx instanceof SAXParseException)
			{
				SAXParseException saxEx = (SAXParseException) linkedEx;
				return format("Line: %d; Column: %d; %s", 
						saxEx.getLineNumber(), saxEx.getColumnNumber(), saxEx.getMessage());
			}
			else 
				return linkedEx.getMessage();
		}
		else 
			return e.getMessage();
	}
	
	
	public WoodpeckerConfigException(String message)
	{
		super(message);
	}
	
	public WoodpeckerConfigException(String messageTemplate, Object... messageTemplateArgs)
	{
		this(format(messageTemplate, messageTemplateArgs));
	}
	
	public WoodpeckerConfigException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public WoodpeckerConfigException(Throwable cause, String messageTemplate, Object... messageTemplateArgs)
	{
		this(format(messageTemplate, messageTemplateArgs), cause);
	}
}
