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

package com.exactprosystems.clearth.converters;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.ActionMetaData;
import com.exactprosystems.clearth.automation.ActionsMapping;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import com.exactprosystems.clearth.xmldata.XmlParameterList;
import com.exactprosystems.clearth.xmldata.XmlScriptConverterConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class DefaultScriptConverterTest
{
	private static final String ACTION_NAME = "CustomSendMessageAction", CODEC_NAME = "codec",
								CODEC_CLASS = "com.exactprosystems.clearth.utils.SimpleKeyValueCodec";
	private ApplicationManager clearThManager;
	private Path resDir;

	@BeforeClass
	public void init() throws ClearThException, FileNotFoundException, SettingsException
	{
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(DefaultScriptConverterTest.class.getSimpleName()));
		clearThManager = new ApplicationManager();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}

	@Test
	public void testConvert() throws Exception
	{
		String script = "#id,#GlobalStep,#Action,#Codec,#MsgType\n" +
						"id1,step1,CustomSendMessageAction,codec,message\n" +
						"id2,step1,CustomSendMessageAction,codec,message\n";
		Map<String, ActionMetaData> customActions = new ActionsMapping(resDir.resolve("actionsmapping.cfg"),
				false).getDescriptions();

		DefaultScriptConverter converter = spy(DefaultScriptConverter.class);
		doReturn(customActions).when(converter).getActions();
		doReturn(CODEC_NAME).when(converter).getCodecNameByActionName(ACTION_NAME);
		doReturn(loadActions()).when(converter).loadActions();

		converter.convert(script, createConfigContainer(), new DefaultCodecFactory(), createCodecConfig());
	}

	private Map<String, XmlScriptConverterConfig> createConfigContainer()
	{
		XmlScriptConverterConfig cfg = new XmlScriptConverterConfig();
		cfg.setName(CODEC_NAME);
		cfg.setCodec(CODEC_CLASS);
		cfg.setMessageFillerClass("com.exactprosystems.clearth.converters.MessageFiller");
		cfg.setClearThMessageClass("com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage");
		cfg.setReceiveSubMessageAction("com.exactprosystems.clearth.automation.actions.ReceiveMessageAction");
		cfg.setSendSubMessageAction("com.exactprosystems.clearth.automation.actions.SendMessageAction");

		return Map.of(CODEC_NAME, cfg);
	}

	private List<ActionData> loadActions()
	{
		List<ActionData> actionData = new ArrayList<>();
		actionData.add(new ActionData(ACTION_NAME, false, map("Param1","Value1")));
		return actionData;
	}

	private CodecsStorage createCodecConfig()
	{
		XmlCodecConfig config = new XmlCodecConfig();
		config.setName(CODEC_NAME);
		config.setCodec(CODEC_CLASS);
		config.setAltName(CODEC_NAME);
		config.setCodecParameters(new XmlParameterList());

		List<XmlCodecConfig> list = new ArrayList<>();
		list.add(config);
		return new CodecsStorage(list);
	}
}