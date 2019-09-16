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

package com.exactprosystems.clearth.automation.exceptions;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;

/**
 * Created by vitaly.barkhatov on 9/18/14.
 */
@SuppressWarnings("serial")
public class ResultException extends RuntimeException
{
	private Result result;

	public ResultException(Result result) {
		super(result.getComment(), result.getError());
		this.result = result;
	}

	public ResultException(String comment) {
		super(comment);
		this.result = DefaultResult.failed(comment);
	}

	public ResultException(String comment, Exception e) {
		super(comment, e);
		this.result = DefaultResult.failed(comment, e);
	}

	public ResultException(Exception e) {
		super(e);
		this.result = DefaultResult.failed(e);
	}

	public static ResultException failed(String comment)
	{
		return new ResultException(DefaultResult.failed(comment));
	}


	public static ResultException failed(String comment, Exception e)
	{
		return new ResultException(DefaultResult.failed(comment, e));
	}

	public static ResultException failed(Exception e)
	{
		return new ResultException(DefaultResult.failed(e));
	}

	public Result getResult()
	{
		return result;
	}
}
