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

package com.exactprosystems.clearth.utils.exception;

/**
 * This exception does not require to be handled after it was thrown.
 * This exception means that some exceptional situation occurred and was handled,
 * all depending operations that goes after this should not be executed.
 * The real exception should be already handled before this one was thrown.
 * Very similar to RuntimeException but this one should be caught in main method to prevent throwing up.
 */
public class HandledInterruptionException extends Exception
{
	public HandledInterruptionException(Throwable cause) {
		super(cause);
	}
}
