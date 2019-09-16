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

package com.exactprosystems.clearth.connectivity.iface;

import java.util.ArrayList;
import java.util.List;

public class MessageColumnNode
{
	private String name;
	private boolean mandatory;
	private boolean repetitive;
	private String description;
	private boolean key;

	private String[] mandatoryOptions = new String[] {"Mandatory", "Optional" };
	private String[] repetitiveOptions = new String[] {"Repetitive", "Single" };
	
	List<MessageColumnNode> repGroups;

	public MessageColumnNode(String name, boolean mandatory, boolean repetitive, String description, boolean key)
	{
		this.name = name;
		this.mandatory = mandatory;
		this.repetitive = repetitive;
		this.description = description;
		this.key = key;
	}

	public MessageColumnNode()
	{
	}
	
	public void addRepGroup(MessageColumnNode rg)
	{
		checkExist();
		repGroups.add(rg);
	}

	public String getName()
	{
		return name;
	}

	public boolean getMandatory()
	{
		return mandatory;
	}

	public boolean getRepetitive()
	{
		return repetitive;
	}

	public String getDescription()
	{
		return description;
	}
	
	public void addInfoInDescription(String desc)
	{
		if(description == null)
		{
			description = desc;
		}
		else
		{
			description += desc;
		}
	}

	public boolean isKey()
	{
		return key;
	}

	public List<MessageColumnNode> getRepGroups()
	{
		checkExist();
		return repGroups;
	}

	private void checkExist()
	{
		if (repGroups == null)
			repGroups = new ArrayList<MessageColumnNode>();
	}

	public String getMandatoryText() {
		return this.mandatory ? this.mandatoryOptions[0] : this.mandatoryOptions[1];
	}

	public String getRepetitiveText() {
		return this.repetitive ? this.repetitiveOptions[0] :  this.repetitiveOptions[1];
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public void setRepetitive(boolean repetitive) {
		this.repetitive = repetitive;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

	public void setMandatoryOptions(String[] mandatoryOptions) {
		this.mandatoryOptions = mandatoryOptions;
	}

	public void setRepetitiveOptions(String[] repetitiveOptions) {
		this.repetitiveOptions = repetitiveOptions;
	}

	public void setRepGroups(List<MessageColumnNode> repGroups) {
		this.repGroups = repGroups;
	}

	public void setMandatoryText(String s) {
		this.mandatory = this.mandatoryOptions[0].equals(s);
	}

	public void setRepetitiveText(String s) {
		this.repetitive = this.repetitiveOptions[0].equals(s);
	}
}
