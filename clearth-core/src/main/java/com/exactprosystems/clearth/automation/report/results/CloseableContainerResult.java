/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.report.Result;

/**
 * Container for results that may require closing of various resources
 * @author vladimir.panarin
 */
public class CloseableContainerResult extends ContainerResult implements AutoCloseable
{
	private static final long serialVersionUID = -8529020779935525633L;

	public CloseableContainerResult()
	{
	}

	protected CloseableContainerResult(String header, boolean isBlockView)
	{
		super(header, isBlockView);
	}
	
	/**
	 * Creates a blank result with a header which may contain other results.
	 */
	public static CloseableContainerResult createPlainResult(String header)
	{
		return new CloseableContainerResult(header, false);
	}
	
	/**
	 * Creates an open/close block which could contain results inside.
	 */
	public static CloseableContainerResult createBlockResult(String header)
	{
		return new CloseableContainerResult(header, true);
	}
	
	
	@Override
	public void close() throws Exception
	{
		for (Result r : details)
		{
			if (r instanceof AutoCloseable)
				((AutoCloseable)r).close();
		}
	}
	
	@Override
	protected ContainerResult createContainerWrapper(String header)
	{
		return CloseableContainerResult.createBlockResult(header);
	}
}
