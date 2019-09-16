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

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;

public interface ICodec
{
	/**
	 * Encodes ClearThMessage to String
	 * @param message to encode
	 * @return String representation of message
	 * @throws EncodeException if message cannot be encoded (if not described in dictionary, for instance) or error occurred while encoding
	 */
	public String encode(ClearThMessage<?> message) throws EncodeException;
	
	/**
	 * Decodes String message to ClearThMessage
	 * @param String representation of message
	 * @return ClearThMessage with message fields stored separately
	 * @throws DecodeException if message cannot be decoded (if message type is not recognized, for instance) or error occurred while decoding
	 */
	public ClearThMessage<?> decode(String message) throws DecodeException;

	/**
	 * Decodes String message to ClearThMessage of given type
	 * @param String representation of message
	 * @param type of message to be used instead of automatic recognition
	 * @return ClearThMessage with message fields stored separately
	 * @throws DecodeException if message cannot be decoded (if message type is not described in dictionary, for instance) or error occurred while decoding
	 */
	public ClearThMessage<?> decode(String message, String type) throws DecodeException;
}
