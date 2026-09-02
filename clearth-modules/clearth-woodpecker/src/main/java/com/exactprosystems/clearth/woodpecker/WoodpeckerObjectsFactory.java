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

package com.exactprosystems.clearth.woodpecker;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.woodpecker.configuration.DaemonsLoader;
import com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerSettings;
import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonsDictionary;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.daemons.action.ActionDaemonFactory;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialDataLoader;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonFactory;
import com.exactprosystems.clearth.woodpecker.initialdata.queries.InitialDataQueryFactory;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.TableTypes;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.csv.CsvTableDataLoaderFactory;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoaderFactory;
import com.exactprosystems.clearth.woodpecker.message.generator.ClearThMessageConstructor;
import com.exactprosystems.clearth.woodpecker.message.generator.MessageGenerator;
import com.exactprosystems.clearth.woodpecker.misc.SchedulerSettingsForWoodpecker;
import com.exactprosystems.clearth.woodpecker.misc.encoder.MessageEncoder;
import com.exactprosystems.clearth.woodpecker.execution.threads.managers.ManagerThreadFactory;
import com.exactprosystems.clearth.woodpecker.execution.threads.workers.WorkerThreadsFactory;
import com.exactprosystems.clearth.woodpecker.utils.WoodpeckerSettingsSaver;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerExceptionUtils.logError;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

public class WoodpeckerObjectsFactory
{
	private static final Logger log = LoggerFactory.getLogger(WoodpeckerObjectsFactory.class);
	
	
	public Class<? extends DaemonsDictionary> getDaemonsDictionaryClass()
	{
		return DaemonsDictionary.class;
	}
	
	public Class[] getExtendedDaemonsDictionaryClasses()
	{
		return new Class[]{};
	}


	public final DaemonsLoader createDaemonsLoader()
	{
		Map<String, WoodpeckerDaemonFactory> daemonFactories = new HashMap<>();
		daemonFactories.put(MessageDaemonFactory.FACTORY_NAME, createMessageDaemonFactory());
		daemonFactories.put(ActionDaemonFactory.FACTORY_NAME, createActionDaemonFactory());
		daemonFactories.putAll(createAdditionalDaemonFactories());
		return new DaemonsLoader(daemonFactories);
	}
	
	protected MessageDaemonFactory createMessageDaemonFactory()
	{
		return new MessageDaemonFactory();
	}
	
	protected ActionDaemonFactory createActionDaemonFactory()
	{
		return new ActionDaemonFactory();
	}
	
	protected Map<String, WoodpeckerDaemonFactory> createAdditionalDaemonFactories()
	{
		return emptyMap();
	}
	
	
	public WoodpeckerFunctions createFunctions(SchedulerSettingsForWoodpecker settings, InitialData initialData)
	{
		return new WoodpeckerFunctions(settings, initialData);
	}
	
	
	public Map<String, TableDataLoaderFactory> createAdditionalTableDataLoaderFactories()
	{
		return emptyMap();
	}
	
	public Map<String, InitialDataQueryFactory> createAdditionalQueryFactories()
	{
		return emptyMap();
	}
	
	public final InitialDataLoader createInitialDataLoader(String daemonName)
	{
		Map<String, TableDataLoaderFactory> dataLoaderFactories = new HashMap<>();
		dataLoaderFactories.put(TableTypes.CSV, new CsvTableDataLoaderFactory());
		dataLoaderFactories.putAll(createAdditionalTableDataLoaderFactories());
		return createInitialDataLoader(daemonName, dataLoaderFactories, createAdditionalQueryFactories());
	}
	
	public InitialDataLoader createInitialDataLoader(String daemonName,
	                                                 Map<String, TableDataLoaderFactory> tableDataLoaderFactoriesByType,
	                                                 Map<String, InitialDataQueryFactory> queryFactoriesByType)
	{
		return new InitialDataLoader(daemonName, tableDataLoaderFactoriesByType, queryFactoriesByType);
	}
	
	public InitialData createInitialData()
	{
		return new InitialData();
	}
	
	
	public ManagerThreadFactory createManagerThreadsFactory()
	{
		return new ManagerThreadFactory();
	}
	
	public WorkerThreadsFactory createWorkerThreadsFactory()
	{
		return new WorkerThreadsFactory();
	}

	
	public ClearThMessageConstructor newMessageConstructor()
	{
		return new ClearThMessageConstructor();
	}
	
	public MessageGenerator createMessageGenerator(MessageEncoder encoder)
	{
		return new MessageGenerator(encoder, newMessageConstructor());
	}
	
	
	public final MessageEncoder createMessageEncoder()
	{
		Map<String, ICodec> codecs = new HashMap<>();
		Map<String, ICodec> loadedCodecs = loadCodecs();
		if (isNotEmpty(loadedCodecs))
			codecs.putAll(loadedCodecs);
		Map<String, ICodec> additionalCodecs = createAdditionalCodecs();
		if (isNotEmpty(additionalCodecs))
			codecs.putAll(additionalCodecs);
		
		return createMessageEncoder(codecs);
	}
	
	protected MessageEncoder createMessageEncoder(Map<String, ICodec> codecs)
	{
		return new MessageEncoder(codecs);
	}

	protected WoodpeckerSettingsSaver<? extends WoodpeckerSettings> createWoodpeckerSaver()
	{
		return new WoodpeckerSettingsSaver<>(WoodpeckerSettings.class);
	}

	protected <T extends WoodpeckerSettings> T getWoodpeckerSettings(WoodpeckerSettingsSaver<? extends WoodpeckerSettings> saver) throws ReflectiveOperationException
	{
		try
		{
			return (T) saver.readWoodpeckerSettings();
		}
		catch (Exception e)
		{
			log.warn("Unable to read Woodpecker settings. Empty settings will be used", e);
			return (T) saver.createEmptySettings();
		}
	}

	protected Map<String, ICodec> loadCodecs()
	{
		Map<String, ICodec> codecs = new HashMap<>();
		ICodecFactory codecFactory = ClearThCore.getInstance().getCodecFactory();

		for (XmlCodecConfig cfg : ClearThCore.getInstance().getCodecs().getConfigsList())
		{
			ICodec codec;
			try
			{
				codec = codecFactory.createCodec(cfg);
			}
			catch (Exception e)
			{
				logError(log, e, "Error while creating codec '%s'", cfg.getName());
				continue;
			}
			
			String codecName = cfg.getName(),
					altName = cfg.getAltName();
			codecs.put(codecName, codec);
			if (altName != null && !altName.equals(codecName))
				codecs.put(altName, codec);
		}
		return codecs;
	}
	
	protected Map<String, ICodec> createAdditionalCodecs()
	{
		return null;
	}
}
