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

import java.util.List;

/**
 * Created by victor.akimov on 2/9/16.
 */
public abstract class MessageHelper
{
	public static final String MARGIN = "<b>&#151;</b> ";

	private final String[] mandatoryOptions = new String[] {"", "Mandatory", "Optional" };
	private String[] repetitiveOptions = new String[] {"", "Repetitive", "Single" };
	
	public MessageHelper(String dictionary)
	{
	}

	public abstract void getMessageDescription(List<MessageColumnNode> data, String typeMessage);

	public abstract List<String> getColumns();

	public abstract List<String> getMessagesNames();

	public abstract String getDirection();

	public abstract List<String> getKeys();

	public String[] getMandatoryOptions() {
		return mandatoryOptions;
	}

	public String[] getRepetitiveOptions() {
		return repetitiveOptions;
	}

}
