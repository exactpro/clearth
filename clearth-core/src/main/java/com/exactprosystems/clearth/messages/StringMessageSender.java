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

package com.exactprosystems.clearth.messages;

import java.io.IOException;

import com.exactprosystems.clearth.connectivity.ConnectivityException;

/**
 * Interface for classes that send string messages
 * @author vladimir.panarin
 */
public interface StringMessageSender
{
	/**
	 * Sends given message. Can return some text as outcome
	 * @param message to send
	 * @return sending outcome, if present
	 * @throws IOException if message cannot be sent due to I/O error
	 * @throws ConnectivityException if connection to message destination is broken
	 */
	public String sendMessage(String message) throws IOException, ConnectivityException;
}
