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

package com.exactprosystems.clearth.web.beans.tree;

public class ComparationNode
{
	private String value1;
	private String value2;
	private boolean equal;
	
	
	public ComparationNode(String value1, String value2, boolean equal)
	{
		this.value1 = value1;
		this.value2 = value2;
		this.equal = equal;
	}
	
	public ComparationNode(String value1, String value2)
	{
		this.value1 = value1;
		this.value2 = value2;
		this.equal = value1.equals(value2);
	}
	
	
	public String getValue1() {
		return value1;
	}

	public void setValue1(String value1) {
		this.value1 = value1;
	}
	
	
	public String getValue2() {
		return value2;
	}

	public void setValue2(String value2) {
		this.value2 = value2;
	}
	
	
	public boolean isEqual() {
		return equal;
	}

	public void setEqual(boolean equal) {
		this.equal = equal;
	}
}
