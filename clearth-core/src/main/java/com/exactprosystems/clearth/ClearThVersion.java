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

package com.exactprosystems.clearth;

/**
 * Created by alexey.karpukhin on 8/25/16.
 */
public class ClearThVersion {

	private String buildDate = null;
	private String buildNumber = null;

	public ClearThVersion(String buildNumber, String buildDate) {
		this.buildDate = buildDate;
		this.buildNumber = buildNumber;
	}

	public String getBuildDate() {
		return buildDate;
	}

	public String getBuildNumber() {
		return buildNumber;
	}

	@Override
	public String toString() {
		return buildDate +
				", revision # " +
				buildNumber;
	}
}
