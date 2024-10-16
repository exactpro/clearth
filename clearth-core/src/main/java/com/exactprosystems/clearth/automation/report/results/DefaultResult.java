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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;

public class DefaultResult extends Result
{
	private static final long serialVersionUID = 8202546326536563880L;

	@Override
	public void clearDetails()
	{
	}
	
	public static Result failed(String comment)
	{
		Result r = new DefaultResult();
		r.setSuccess(false);
		r.setComment(comment);
		r.setFailReason(FailReason.FAILED);
		return r;
	}
	
	public static Result failed(Exception e)
	{
		Result r = new DefaultResult();
		r.setError(e);
		return r;
	}

	public static Result failed(String comment, Exception e)
	{
		Result r = failed(comment);
		r.setError(e);
		return r;
	}

	public static Result passed(String comment)
	{
		Result r = new DefaultResult();
		r.setSuccess(true);
		r.setFailReason(FailReason.NO);
		r.setComment(comment);
		return r;
	}

	public static Result passed(String comment, String message)
	{
		Result r = new DefaultResult();
		r.setSuccess(true);
		r.setFailReason(FailReason.NO);
		r.setComment(comment);
		r.setMessage(message);
		return r;
	}
}
