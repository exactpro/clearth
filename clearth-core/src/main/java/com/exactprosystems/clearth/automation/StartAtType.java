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

package com.exactprosystems.clearth.automation;

/**
 * Created by alexey.karpukhin on 6/9/16.
 */
public enum StartAtType {

	END_STEP("End of previous step"),
	START_STEP("Start of previous step"),
	START_SCHEDULER("Start of the scheduler"),
	START_EXECUTION("Start of execution");

	public static final StartAtType DEFAULT = END_STEP;

	private final String stringType;

	StartAtType(String stringType) {
		this.stringType = stringType;
	}

	public String getStringType() {
		return stringType;
	}

	public static StartAtType getValue(String stringType) {
		for (StartAtType val: StartAtType.values()) {
			if (val.stringType.equals(stringType)) {
				return val;
			}
		}
		return DEFAULT;
	}
}
