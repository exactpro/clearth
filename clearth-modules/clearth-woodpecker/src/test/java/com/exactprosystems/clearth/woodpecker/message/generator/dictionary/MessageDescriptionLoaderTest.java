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

package com.exactprosystems.clearth.woodpecker.message.generator.dictionary;

import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.expressions.mvel.MvelExpressionCompiler;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageDescriptionLoaderTest
{
	private static final Path RES_DIR = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", MessageDescriptionLoaderTest.class.getSimpleName());
	private static final Path FILE_RES = RES_DIR.resolve("file.csv");

	@Test
	public void testLoad() throws WoodpeckerException
	{
		MessageDescriptionLoader loader = new MessageDescriptionLoader();
		assertThat(loader.load(FILE_RES)).usingRecursiveComparison().isEqualTo(createMessageDescription());
	}

	private MessageDescription createMessageDescription()
	{
		MvelExpressionCompiler compiler = new MvelExpressionCompiler();
		String val1 = "@{tableValueByRowNum('positions', 1, 'Currency')}";
		String val2 = "@{tableValueByRowNum('positions', 1, 'DerivativeInstrument')}";
		String val3 = "@{format(time(0), 'yyyy-MM-dd HH:mm:ss.SSS')}";
		
		MessageDescription description = new MessageDescription(FILE_RES);
		description.setMessageType("PositionRequest");
		description.addField(new FieldDescription("MsgType", "PositionRequest"));
		description.addField(new FieldDescription("PositionType", "0"));
		description.addField(new FieldDescription("Currency", val1, compiler.compile(val1)));
		description.addField(new FieldDescription("DerivativeInstrument", val2, compiler.compile(val2)));
		description.addField(new FieldDescription("CreationTimestamp", val3, compiler.compile(val3)));
		return description;
	}
}