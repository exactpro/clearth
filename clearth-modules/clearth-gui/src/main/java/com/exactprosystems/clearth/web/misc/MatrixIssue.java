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

package com.exactprosystems.clearth.web.misc;


public class MatrixIssue
{
	private final static int MATRIX_NAME = 1,
			MESSAGE_TYPE = 2,
			MESSAGE_KIND = 3,
			ISSUE = 4;
	
	private int issueParam = 0;
	private String value;
	
	private MatrixIssue(String value)
	{
		this.value = value;
	}
	
	public static MatrixIssue matrixName(String value)
	{
		MatrixIssue result = new MatrixIssue(value);
		result.issueParam = MATRIX_NAME;
		return result;
	}
	
	public static MatrixIssue messageType(String value)
	{
		MatrixIssue result = new MatrixIssue(value);
		result.issueParam = MESSAGE_TYPE;
		return result;
	}
	
	public static MatrixIssue messageKind(String value)
	{
		MatrixIssue result = new MatrixIssue(value);
		result.issueParam = MESSAGE_KIND;
		return result;
	}
	
	public static MatrixIssue issue(String value)
	{
		MatrixIssue result = new MatrixIssue(value);
		result.issueParam = ISSUE;
		return result;
	}

	public boolean isMatrixName()
	{
		return issueParam == MATRIX_NAME;
	}

	public boolean isMessageType()
	{
		return issueParam == MESSAGE_TYPE;
	}

	public boolean isMessageKind()
	{
		return issueParam == MESSAGE_KIND;
	}

	public boolean isIssue()
	{
		return issueParam == ISSUE;
	}

	public String getValue()
	{
		return value;
	}
}
