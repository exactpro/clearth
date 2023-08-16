/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.fix;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.utils.DictionaryLoadException;

import quickfix.ConfigError;

public class FixCodecTest
{
	private final String DICT_PATH = "src/test/resources/",
			FIX50_FILENAME = DICT_PATH+"FIX50.xml",
			FIXT11_FILENAME = DICT_PATH+"transportDicts/FIXT11.xml";

	@Test
	public void skipFieldsUnknownForMessage() throws DictionaryLoadException, ConfigError, DecodeException
	{
		FixCodec codec = new FixCodec(new FixDictionary(FIX50_FILENAME), null);
		//Tags 3, 4->7 and 50->6 present in dictionary but absent in CU message definition. They shouldn't be added to parsed message  
		ClearThMessage<?> msg = codec.decode("8=FIXT.1.1|9=999|35=CU|1=T|2=123|3=test|4=1|5=111|6=222|7=333|50=1|5=X|7=Z|6=Y|10=043|"),
				grp = msg.getSubMessage(0);
		Assert.assertNull(msg.getField("Value"), "Value tag");
		Assert.assertNull(grp.getField("GrpField3"), "GrpField3 tag of 1st group");
		Assert.assertNull(grp.getSubMessage(0).getField("GrpField2"), "GrpField2 tag of 1st sub-group in 1st group");
	}
	
	@Test(description = "Tests if special values are applied in messages being encoded and empty fields are not encoded at all")
	public void emptyTags() throws DictionaryLoadException, ConfigError, EncodeException, DecodeException
	{
		FixCodec codec = new FixCodec(new FixDictionary(FIX50_FILENAME, FIXT11_FILENAME), null);
		//Tags 3, 4->7 and 50->6 present in dictionary but absent in CU message definition. They shouldn't be added to parsed message  
		SimpleClearThMessage rg = new SimpleClearThMessageBuilder()
						.subMessageType("Grp")
						.field("GrpField1", "@{empty}")
						.field("GrpField2", "")
						.build(),
				msg = new SimpleClearThMessageBuilder()
						.type("CU")
						.field("Text", "@{empty}")
						.field("Text2", "")
						.field("Number", "93")
						.rg(rg)
						.build();
		
		String encoded = codec.encode(msg);
		ClearThMessage<?> decodedMsg = codec.decode(encoded),
				decodedRg = decodedMsg.getSubMessage(0);
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(decodedMsg.getField("Text"), "", "Text tag");
		soft.assertNull(decodedMsg.getField("Text2"), "Text2 tag");
		soft.assertEquals(decodedMsg.getField("Number"), "93", "Number tag");
		soft.assertEquals(decodedRg.getField("GrpField1"), "", "GrpField1 tag of 1st group");
		soft.assertNull(decodedRg.getField("GrpField2"), "GrpField2 tag of 1st group");
		soft.assertAll();
	}
}
