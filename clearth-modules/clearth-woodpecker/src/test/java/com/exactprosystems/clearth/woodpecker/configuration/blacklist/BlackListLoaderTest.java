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

package com.exactprosystems.clearth.woodpecker.configuration.blacklist;

import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class BlackListLoaderTest
{
	private static final Path RES_DIR = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", BlackListLoaderTest.class.getSimpleName());
	private static final String NAME = "name";
	@Test
	public void testLoad() throws WoodpeckerException
	{
		BlackListLoader loader = new BlackListLoader();
		BlackList list = loader.load(NAME, RES_DIR.resolve("file.csv"));
		assertThat(list).usingRecursiveComparison().isEqualTo(createBlackList());
	}
	
	private BlackList createBlackList()
	{
		BlackList blackList = new BlackList(NAME);
		blackList.addId("type", "1");
		blackList.addIdPredicate("type2", new Matches(Pattern.compile("2")));
		return blackList;
	}
}