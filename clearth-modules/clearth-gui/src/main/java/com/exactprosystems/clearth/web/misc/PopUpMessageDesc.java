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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PopUpMessageDesc
{
	private static final String CREATION_TIME_PATTERN = "HH:mm:ss";
	private static final SimpleDateFormat CREATION_TIME_FORMAT = new SimpleDateFormat(CREATION_TIME_PATTERN);
	
	private String summary;
	private String detail;
	private String creationTime;
	private Severity severity;

	public enum Severity
	{
		INFO("Info"),
		WARN("Warning"),
		ERROR("Error");

		private String desc;
		Severity(String desc)
		{
			this.desc = desc;
		}

		public String getDesc() { return desc; }

		@Override
		public String toString()
		{
			return desc;
		}
	}

	public PopUpMessageDesc(String detail, String severity, String summary)
	{
		this.detail = detail;
		this.summary = summary;
		Date now = Calendar.getInstance().getTime();
		creationTime = CREATION_TIME_FORMAT.format(now);
		this.severity = Severity.valueOf(severity);
	}

	public String getDetail()
	{
		return detail;
	}

	public Severity getSeverity()
	{
		return severity;
	}

	public String getSummary()
	{
		return summary;
	}

	public String getCreationTime()
	{
		return creationTime;
	}
}
