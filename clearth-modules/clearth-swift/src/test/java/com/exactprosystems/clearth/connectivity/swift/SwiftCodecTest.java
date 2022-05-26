/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.swift;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.generators.LegacyValueGenerator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.exactprosystems.clearth.connectivity.swift.SwiftCodec.DATE_FORMATTER;
import static com.exactprosystems.clearth.connectivity.swift.SwiftCodec.DATE_TIME_FORMATTER;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class SwiftCodecTest
{
	private static final String SWIFT_DICTIONARY_PATH = "cfg/dicts/dictionary.xml";
	private static final Path TEST_OUTPUT_DIR = Paths.get("testOutput/");
	private static final String GENERATOR_FILE = TEST_OUTPUT_DIR + "/value_generator.txt";

	private static final String DATE = "2020-04-19T10:00:00Z";

	private static final Clock CLOCK = Clock.fixed(Instant.parse(DATE), ZoneId.systemDefault());
	private static final String DATE_FORMATTED = DATE_FORMATTER.format(LocalDateTime.now(CLOCK));
	private static final String DATE_WITH_TIME =  DATE_FORMATTED + DATE_TIME_FORMATTER.format(LocalDateTime.now(CLOCK));
	
	private SwiftCodec codec;
	
	@BeforeClass
	public void init() throws Exception
	{
		LegacyValueGenerator valueGenerator = new LegacyValueGenerator(GENERATOR_FILE, "default");
		codec = new SwiftCodec(new SwiftDictionary(resourceToAbsoluteFilePath(SWIFT_DICTIONARY_PATH)), 
				valueGenerator, CLOCK);
	}

	@DataProvider(name = "encodeMessageTest")
	public static Object[][] createEncodeMessageData()
	{
		return new Object[][]
				{
						//messageToEncode, encodedMessage
						{
								message(map("MsgType", "001", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED + 
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" + 
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "IncomingMessage", "true", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:I001XXXXXXXXXXXXN}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "ApplicationId", "A", "ServiceId", "99",
										"SessionNumber", "9999", "SequenceNumber", "123456", "LogicalTerminal", 
										"1234567890AB", "A", "AA")),
								"{1:A991234567890AB9999123456}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "IncomingMessage", "true", "ReceiverAddress",
										"1234567890AB", "MessagePriority", "1", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:I0011234567890AB1}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "IncomingMessage", "true", "ReceiverAddress", 
										"1234567890AB", "MessagePriority", "1", "DeliveryMonitoring", "CD", 
										"ObsolescencePeriod", "EF", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:I0011234567890AB1CDEF}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "SenderInputTime","2222", "MIRDate", "200101",
										"MIRLogicalTerminal", "1234567890AB", "MIRSessionNumber", "9999",
										"MIRSequenceNumber", "123456", "ReceiverOutputDate", "200101",
										"ReceiverOutputTime", "1200", "MessagePriority", "1", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O00122222001011234567890AB999912345620010112001}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "Block3_103", "AAA", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{3:{103:AAA}}{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "AddBlock5", "false", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}"
						},
						{
								message(map("MsgType", "001", "Mac", "11223344",
										"Chk", "112233445566", "A", "AA")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:{16R:MANDATORY}{20C::A//AA}{16S:MANDATORY}}" +
										"{5:{MAC:11223344}{CHK:112233445566}}"
						},
						{
								message(map("MsgType", "002"), 
										message(map("SubMsgType", "B", "C", "CC", "D", "DD")),
										message(map("SubMsgType", "B", "C", "CCC"))),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0021200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:{16R:A}{16R:B}{13A::C//CC}{13B::D//DD}{16S:B}{16R:B}" +
										"{13A::C//CCC}{16S:B}{16S:A}}{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "003")),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0031200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:{23G:BBB}}{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "A", null, "B", null, "C", null,
										"D", null, "E", null, "F", null)),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{4:}{5:{MAC:12345678}{CHK:123456789ABC}}"
						},
						{
								message(map("MsgType", "001", "Block3_103", null)),
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200" + DATE_FORMATTED +
										"XXXXXXXXXXXX0000000000" + DATE_WITH_TIME + "N}" +
										"{3:}{4:}{5:{MAC:12345678}{CHK:123456789ABC}}"
						}
				};
	}

	@DataProvider(name = "decodeMessageTest")
	public static Object[][] createDecodeMessageData()
	{
		return new Object[][]
				{
						//messageToDecode, decodedMessage
						{
								"{1:F01XXXXXXXXXXXX0001000000}{2:O0011200200419XXXXXXXXXXXX00000000002004191111N}" +
										"{4:\r\n:16R:MANDATORY\r\n:20C::A//12\r\n:23G:DD\r\n:98C::C//34\r\n" +
										":16R:LINK\r\n:20C::D//AA\r\n:16S:LINK\r\n:16R:LINK\r\n:20C::E//BB\r\n" +
										":16S:LINK\r\n:16R:LINK\r\n:20C::F//CC\r\n:16S:LINK\r\n:16S:MANDATORY\r\n-}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}",
								message(map("A", "12", "B", "DD", "C","34", "D", "AA", "E", "BB", "F", "CC"))

						},
						{
								"{1:F01AAAAA11BBBBB0001000000}{2:O0011200200419AAAABBBB123400000000002004191111N}" +
										"{3:{103:AAA}}" +
										"{4:\r\n:16R:MANDATORY\r\n:20C::A//12\r\n:23G:DD\r\n:98C::C//34\r\n" + 
										":16R:LINK\r\n:20C::D//AA\r\n:16S:LINK\r\n:16R:LINK\r\n:20C::E//BB\r\n" + 
										":16S:LINK\r\n:16R:LINK\r\n:20C::F//CC\r\n:16S:LINK\r\n:16S:MANDATORY\r\n-}" +
										"{5:{MAC:12345678}{CHK:123456789ABC}}",
								message(map("A", "12", "B", "DD", "C","34", "D", "AA", "E", "BB", "F", "CC"))
						},
						{
								"{1:F01AAAAA11BBBBB0001000000}{2:O0011200200419AAAABBBB123400000000002004191111N}" +
										"{3:{103:AAA}}" + 
										"{4:\r\n:16R:MANDATORY\r\n:20C::A//12\r\n:23G:DD\r\n:98C::C//34\r\n" +
										":16R:LINK\r\n:20C::D//AA\r\n:16S:LINK\r\n:16R:LINK\r\n:20C::E//BB\r\n" +
										":16S:LINK\r\n:16R:LINK\r\n:20C::F//CC\r\n:16S:LINK\r\n:16S:MANDATORY\r\n-}",
								message(map("A", "12", "B", "DD", "C", "34", "D", "AA", "E", "BB", "F", "CC"))
						},
						{
								"{1:F01AAAAA11BBBBB0001000000}{2:O0051200200419AAAABBBB123400000000002004191111N}" +
										"{4:\r\n:20C::A.1\r\n-}",
								message(map("A", "1"))
						},
						{
								"{1:F01AAAAA11BBBBB0001000000}{2:O0021200200419AAAABBBB123400000000002004191111N}" +
										"{4:\r\n:16R:A\r\n:16R:B\r\n:13A::C//CC\r\n:13B::D//DD\r\n:16S:B\r\n" +
										":16R:B\r\n:13A::C//CCC\r\n:16S:B\r\n:16S:A\r\n-}",
								message(map(), message(map("SubMsgType", "B", "C", "CC", "D", "DD")),
										message(map("SubMsgType", "B", "C", "CCC")))
						}
				};
	}

	@DataProvider(name = "mandatoryTagBlock4Test")
	public static Object[][] mandatoryTagBlock4Data()
	{
		return new Object[][]
				{
						//messageToDecode
						{
								"{1:F01XXXXXXXXXXXX0001000000}{2:I0011234567890AB1}{4:\r\n:20C::A.1\r\n-}"
						}
				};
	}
	
	@DataProvider(name = "searchNotExistMessageTypeEncode")
	public static Object[][] searchNotExistMessageTypeEncodeData()
	{
		return new Object[][]
				{
						//messageToEncode
						{
								message(map("MsgType", "999")),
								"999"
						}
				};
	}
	
	@Test(dataProvider = "encodeMessageTest")
	public void testEncodeMessage(ClearThSwiftMessage messageToEncode, String encodedMessage) throws Exception
	{
		String actualMessage = codec.encode(messageToEncode);
		assertEquals(encodedMessage, actualMessage);
	}

	@Test(dataProvider = "decodeMessageTest")
	public void testDecodeMessage(String messageToDecode, ClearThSwiftMessage decodedMessage) throws Exception
	{
		ClearThSwiftMessage actualMessage = codec.decode(messageToDecode);
		assertEquals(decodedMessage, actualMessage);
	}

	@Test(dataProvider = "mandatoryTagBlock4Test")
	public void testWithoutMandatoryTagBlock4(String messageToDecode)
	{
		assertThatThrownBy(() -> codec.decode(messageToDecode))
				.isInstanceOf(DecodeException.class)
				.hasMessage("Mandatory block MANDATORY (field MANDATORY) not found in message");
		
	}

	@Test(dataProvider = "searchNotExistMessageTypeEncode")
	public void testSearchNotExistMessageTypeEncode(ClearThSwiftMessage messageToEncode, String msgType)
	{
		assertThatThrownBy(() -> codec.encode(messageToEncode))
				.isInstanceOf(EncodeException.class)
				.hasMessage(String.format("There is no messages with type '%s' in the dictionary.", msgType));
	}

	private static ClearThSwiftMessage message(Map<String, String> fields, ClearThSwiftMessage... subMessages)
	{
		return new ClearThSwiftMessage(new LinkedHashMap<>(fields), subMessages.length == 0 ? null :
				Arrays.asList(subMessages), null);
	}

}