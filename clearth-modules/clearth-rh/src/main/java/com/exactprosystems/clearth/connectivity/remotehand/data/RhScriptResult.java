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

package com.exactprosystems.clearth.connectivity.remotehand.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.exactprosystems.clearth.connectivity.remotehand.data.RhResponseCode.SUCCESS;

public class RhScriptResult
{
	private int code = SUCCESS.getCode();
	private String errorMessage;
	private List<String> actionResults;
	private List<String> screenshotIds;
	private List<String> encodedOutput;
	
	@JsonIgnore
	public boolean isSuccess()
	{
		return code == SUCCESS.getCode();
	}
	
	@JsonIgnore
	public boolean isFailed()
	{
		return code != SUCCESS.getCode();
	}
	

	public int getCode()
	{
		return code;
	}

	public void setCode(int code)
	{
		this.code = code;
	}

	
	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	
	public List<String> getActionResults()
	{
		return actionResults != null ? actionResults : Collections.emptyList();
	}

	public void setActionResults(List<String> actionResults)
	{
		this.actionResults = actionResults;
	}
	
	public void addToActionResults(String result)
	{
		if (actionResults == null)
			actionResults = new ArrayList<>();
		actionResults.add(result);
	}

	
	public List<String> getScreenshotIds()
	{
		return screenshotIds != null ? screenshotIds : Collections.emptyList();
	}

	public void setScreenshotIds(List<String> screenshotIds)
	{
		this.screenshotIds = screenshotIds;
	}
	
	public void addScreenshotId(String id)
	{
		if (screenshotIds == null)
			screenshotIds = new ArrayList<>();
		screenshotIds.add(id);
	}


	public List<String> getEncodedOutput()
	{
		return encodedOutput != null ? encodedOutput : Collections.emptyList();
	}

	public void setEncodedOutput(List<String> encodedOutput)
	{
		this.encodedOutput = encodedOutput;
	}
	
	public void addToEncodedOutput(String data)
	{
		if (encodedOutput == null)
			encodedOutput = new ArrayList<>();
		encodedOutput.add(data);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("RhScriptResult{");
		sb.append("code=").append(code);
		if (errorMessage != null)
			sb.append(", errorMessage='").append(errorMessage).append('\'');
		if (actionResults != null)
			sb.append(", actionResults=").append(actionResults);
		if (screenshotIds != null)
			sb.append(", screenshotIds=").append(screenshotIds);
		if (encodedOutput != null)
			sb.append(", encodedOutput=").append(encodedOutput);
		sb.append('}');
		return sb.toString();
	}
}
