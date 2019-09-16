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

package com.exactprosystems.clearth.tools.calculator;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by alexander.magomedov on 11/29/16.
 */
public class CalculatorVariable
{
	/*Validation constants*/
	public static final String ID_REGEX = "^[a-zA-Z_\\$][a-zA-Z_0-9\\$]*$";
	public static final Pattern ID_PATTERN = Pattern.compile(ID_REGEX);
	
	/*Status codes*/
	public static final int UNCHECKED = -1;        //Unchecked (default status)
	public static final int PASSED = 0;            //No errors
	public static final int DUPLICATED_NAME = 10;  //Already present a variable with the same name
	public static final int INVALID_ID = 20;       //Unexpected container. name
	public static final int WRONG_PARAM = 30;      //Unexpected .variable name
	public static final int NAME_NOT_DEFINED = 40; //No name defined
	public static final int WRONG_FORMAT = 50;     //Variable name is invalid by some means

	/*Statuses as text*/
	private static final Map<Integer, String> STATUS_INFOS = new LinkedHashMap<Integer,String>();
	static {
		STATUS_INFOS.put(UNCHECKED, "Variable is unchecked");
		STATUS_INFOS.put(PASSED, "Variable is correct");
		STATUS_INFOS.put(INVALID_ID, "Invalid ID");
		STATUS_INFOS.put(WRONG_PARAM, "Parameter declaration is wrong");
		STATUS_INFOS.put(DUPLICATED_NAME, "Name is duplicated");
		STATUS_INFOS.put(NAME_NOT_DEFINED, "Name is not defined");
		STATUS_INFOS.put(WRONG_FORMAT, "Name has wrong format");
	}

	private String name;
	private String value;
	private int statusCode;
	private String statusDetails;	//Contains more information about status that can help user to find errors. Optional.
	
	public CalculatorVariable(String name, String value)
	{
		this.name = name;
		this.value = value;
		this.statusCode = UNCHECKED;
		this.statusDetails = null;
	}

	public String getStatus()
	{
		String status = STATUS_INFOS.get(statusCode);
		if (statusDetails != null)
			status += ". " + statusDetails;
		return status;
	}
	
	
	public int getStatusCode()
	{
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		statusDetails = null;
	}

	
	public boolean isChecked() {
		return statusCode != UNCHECKED;
	}

	public boolean isValid() {
		return statusCode == PASSED;
	}

	
	public static boolean validateId(String id) {
		return ID_PATTERN.matcher(id).matches();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getStatusDetails()
	{
		return statusDetails;
	}

	public void setStatusDetails(String statusDetails)
	{
		this.statusDetails = statusDetails;
	}

}
