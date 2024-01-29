/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages.th2;

import java.io.IOException;
import java.nio.file.Path;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactpro.th2.common.grpc.Message;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.converters.ConversionException;

public class GrpcMessageFactoryTest
{
	private GrpcMessageFactory factory;
	
	@BeforeClass
	public void init()
	{
		factory = new GrpcMessageFactory();
	}
	
	@Test
	public void buildFromSimpleMessage() throws ConversionException, IOException
	{
		SimpleClearThMessage message = MessageTestUtils.createComplexMessage();
		MessageProperties properties = MessageTestUtils.createMessageProperties();
		
		Message grpcMessage = factory.createMessage(message, properties);
		
		MessageTestUtils.assertMessages(grpcMessage, Path.of("src", "test", "resources", "GrpcMessageFactory", "expectedMessage.txt"));
	}
}
