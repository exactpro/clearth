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

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;

/**
 * Interface for classes that obtain messages from some underlying source
 * @author vladimir.panarin
 */
public interface MessageSource
{
	/**
	 * @return next message from underlying source or null if no messages are available
	 * @throws IOException if error occurred while obtaining message from underlying source
	 */
	public ClearThMessage<?> nextMessage() throws IOException;
	/**
	 * Removes current message from source if this operation is supported. Else does nothing
	 */
	public void removeMessage();
	/**
	 * Removes given message from source if this operation is supported. Else does nothing
	 */
	public void removeMessage(ClearThMessage<?> message);
}
