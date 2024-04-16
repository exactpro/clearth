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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS)
public abstract class Result implements Serializable
{
	public static final String DETAILS_DIR = "details/";

	protected boolean success = true,
			crashed = false,
			inverted = false;
	@JsonIgnore
	private Throwable error = null;
	private String message = null,
			comment = null;
	
	private Collection<EncodedClearThMessage> linkedMessages;
	
	protected FailReason failReason = null;
	private FailoverException failoverData;

	/* default constructor is required for all types of results to have ability to deserialize them from JSON */
	public Result() {}

	@JsonIgnore
	public boolean isSuccessWithoutInversionRegard()
	{
		return success;
	}

	public boolean isSuccess()
	{
		return isInverted() != isSuccessWithoutInversionRegard();
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public boolean isInverted()
	{
		return inverted;
	}

	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
	}

	public boolean isCrashed()
	{
		return crashed;
	}

	public void setCrashed(boolean crashed)
	{
		this.crashed = crashed;
	}

	public Throwable getError()
	{
		return error;
	}

	public void setError(Throwable error)
	{
		this.error = error;
		if (error != null)
		{
			if (isSuccess())
				setSuccess(false);
			setFailReason(FailReason.EXCEPTION);
		}
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	public void appendComment(String comment)
	{
		if (this.comment == null)
			this.comment = comment;
		else
			this.comment += ' ' + comment;
	}
	
	
	public Collection<EncodedClearThMessage> getLinkedMessages()
	{
		return linkedMessages == null ? Collections.emptyList() : Collections.unmodifiableCollection(linkedMessages);
	}
	
	public void addLinkedMessage(EncodedClearThMessage message)
	{
		if (linkedMessages == null)
			linkedMessages = new ArrayList<>();
		linkedMessages.add(message);
	}
	
	public void clearLinkedMessages()
	{
		linkedMessages = null;
	}
	
	
	public FailReason getFailReason()
	{
		return failReason;
	}

	public void setFailReason(FailReason failReason)
	{
		this.failReason = failReason;
	}

	public FailoverException getFailoverData()
	{
		return failoverData;
	}

	public void setFailoverData(FailoverException failoverData)
	{
		this.failoverData = failoverData;
	}

	public void processDetails(File reportDir, Action linkedAction)
	{

	}

	protected final Path getDetailsPath(File reportDir, Action linkedAction)
	{
		return reportDir.toPath().resolve(DETAILS_DIR);
	}

	public abstract void clearDetails();

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add("Class: ").add(getClass().getSimpleName()).eol();
		builder.add(prefix).add("Status: ")
				.add(success ? "SUCCESS" : failReason)
				.add(inverted ? " (INVERTED)" : "")
				.add(crashed ? ", CRASHED" : "").eol();
		if (error != null)
			builder.add(prefix).add("Error: ").add((error != null ? ExceptionUtils.getStackTrace(error) : null)).eol();
		if (message != null)
			builder.add(prefix).add("Message: ").add(message).eol();
		if (comment != null)
			builder.add(prefix).add("Comment: ").add(comment).eol();
		return builder; 
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result result = (Result) o;
		return success == result.success && crashed == result.crashed && inverted == result.inverted &&
				Objects.equals(error, result.error) && Objects.equals(message, result.message) &&
				Objects.equals(comment, result.comment) &&
				Objects.equals(linkedMessages, result.linkedMessages) && failReason == result.failReason &&
				Objects.equals(failoverData, result.failoverData);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(success, crashed, inverted, error, message, comment, linkedMessages, failReason,
				failoverData);
	}
}