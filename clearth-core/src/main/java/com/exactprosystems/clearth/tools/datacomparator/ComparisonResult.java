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

package com.exactprosystems.clearth.tools.datacomparator;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class ComparisonResult
{
	private int total,
			passed,
			failed,
			notFound,
			extra;
	private String description;
	private Path passedDetails,
			failedDetails,
			notFoundDetails,
			extraDetails,
			errors;
	private LocalDateTime started,
			finished;
	
	
	public int getTotal()
	{
		return total;
	}
	
	public void setTotal(int total)
	{
		this.total = total;
	}
	
	public void incTotal()
	{
		total++;
	}
	
	
	public int getPassed()
	{
		return passed;
	}
	
	public void setPassed(int passed)
	{
		this.passed = passed;
	}
	
	public void incPassed()
	{
		passed++;
	}
	
	
	public int getFailed()
	{
		return failed;
	}
	
	public void setFailed(int failed)
	{
		this.failed = failed;
	}
	
	public void incFailed()
	{
		failed++;
	}
	
	
	public int getNotFound()
	{
		return notFound;
	}
	
	public void setNotFound(int notFound)
	{
		this.notFound = notFound;
	}
	
	public void incNotFound()
	{
		notFound++;
	}
	
	
	public int getExtra()
	{
		return extra;
	}
	
	public void setExtra(int extra)
	{
		this.extra = extra;
	}
	
	public void incExtra()
	{
		extra++;
	}
	
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
	public Path getPassedDetails()
	{
		return passedDetails;
	}
	
	public void setPassedDetails(Path passedDetails)
	{
		this.passedDetails = passedDetails;
	}
	
	
	public Path getFailedDetails()
	{
		return failedDetails;
	}
	
	public void setFailedDetails(Path failedDetails)
	{
		this.failedDetails = failedDetails;
	}
	
	
	public Path getNotFoundDetails()
	{
		return notFoundDetails;
	}
	
	public void setNotFoundDetails(Path notFoundDetails)
	{
		this.notFoundDetails = notFoundDetails;
	}
	
	
	public Path getExtraDetails()
	{
		return extraDetails;
	}
	
	public void setExtraDetails(Path extraDetails)
	{
		this.extraDetails = extraDetails;
	}
	
	
	public Path getErrors()
	{
		return errors;
	}
	
	public void setErrors(Path errors)
	{
		this.errors = errors;
	}
	
	
	public LocalDateTime getStarted()
	{
		return started;
	}
	
	public void setStarted(LocalDateTime started)
	{
		this.started = started;
	}
	
	
	public LocalDateTime getFinished()
	{
		return finished;
	}
	
	public void setFinished(LocalDateTime finished)
	{
		this.finished = finished;
	}
}