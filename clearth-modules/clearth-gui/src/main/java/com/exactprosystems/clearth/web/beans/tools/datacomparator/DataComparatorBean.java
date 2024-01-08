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

package com.exactprosystems.clearth.web.beans.tools.datacomparator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import com.exactprosystems.clearth.web.misc.DbConnectionsCache;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.datacomparator.ComparisonResult;
import com.exactprosystems.clearth.tools.datacomparator.ComparisonResultWriter;
import com.exactprosystems.clearth.tools.datacomparator.ComparisonSettings;
import com.exactprosystems.clearth.tools.datacomparator.DataComparatorTool;
import com.exactprosystems.clearth.tools.datacomparator.DataComparisonTask;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.StringDataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.MappingDesc;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

public class DataComparatorBean extends ClearThBean
{
	private final DataComparatorTool comparator;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final File uploadsStorage;
	private final Path outputStorage;
	private final List<MappingEntry> mappingEntries = new ArrayList<>();
	private DataComparisonTask task;
	private String errorText;
	private File resultFile;
	private final DbConnectionsCache cache;
	private final DataSourceSettings expData, actData;
	
	public DataComparatorBean()
	{
		ClearThCore core = ClearThCore.getInstance();
		comparator = core.getToolsFactory().createDataComparatorTool();
		cache = createDbConnectionsCache();
		
		String toolDir = "data_comparator",
				userName = UserInfoUtils.getUserName();
		uploadsStorage = Path.of(core.getUploadStoragePath(), toolDir, userName).toFile();
		outputStorage = Path.of(core.getTempDirPath(), toolDir, userName);
		
		expData = createDataSourceSettings();
		actData = createDataSourceSettings();
	}
	
	public DataSource[] getDataSources()
	{
		return DataSource.values();
	}

	
	public boolean isRunning()
	{
		return task != null && task.isRunning();
	}
	
	public ComparisonResult getResult()
	{
		return task != null ? task.getResult() : null;
	}
	
	public String getError()
	{
		if (errorText != null)
			return errorText;
		
		if (task == null || task.getError() == null)
			return null;
		
		errorText = ExceptionUtils.getDetailedMessage(task.getError());
		return errorText;
	}
	
	public void compare()
	{
		if (isRunning())
		{
			MessageUtils.addWarningMessage("Comparison is already running", null);
			return;
		}
		
		BasicTableDataReader<String, String, ?> expectedReader = null,
				actualReader = null;
		boolean canContinue = false;
		try
		{
			if ((expectedReader = createExpectedReader()) == null)
				return;
			
			if ((actualReader = createActualReader()) == null)
				return;
			
			canContinue = true;
		}
		finally
		{
			if (!canContinue)
			{
				Utils.closeResource(actualReader);
				Utils.closeResource(expectedReader);
			}
		}
		
		getLogger().info("starts data comparison");
		DataMapping<String> mapping = createMapping();
		ComparisonSettings settings = new ComparisonSettings(outputStorage, mapping, ClearThCore.comparisonUtils());
		errorText = null;
		resultFile = null;
		task = new DataComparisonTask(expectedReader, actualReader, settings, comparator);
		executor.execute(task);
		MessageUtils.addInfoMessage("Comparison started", null);
	}
	
	public void stop()
	{
		if (!isRunning())
			return;
		
		task.interrupt();
	}
	
	public StreamedContent downloadResult()
	{
		ComparisonResult result = getResult();
		if (result == null)
			return null;
		
		try
		{
			if (resultFile == null)
				resultFile = createResultFile(result);
			
			return WebUtils.downloadFile(resultFile, "result.zip");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not create result file", e, getLogger());
			return null;
		}
	}
	
	
	public List<MappingEntry> getMappingEntries()
	{
		return mappingEntries;
	}
	
	public void addMappingEntry()
	{
		mappingEntries.add(new MappingEntry());
	}
	
	public void clearMapping()
	{
		mappingEntries.clear();
	}
	
	public void removeMappingEntry(MappingEntry entry)
	{
		mappingEntries.remove(entry);
	}
	
	public void uploadMapping(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if ((file == null) || (file.getContent().length == 0))
			return;
		
		File storedMapping;
		try
		{
			Files.createDirectories(uploadsStorage.toPath());
			storedMapping = WebUtils.storeUploadedFile(file, uploadsStorage, "mapping_", ".xml");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Error while storing uploaded mapping", e, getLogger());
			return;
		}
		
		try
		{
			MappingDesc desc = XmlUtils.unmarshalObject(MappingDesc.class, storedMapping.getAbsolutePath());
			descToMapping(desc);
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not load mapping from file", e, getLogger());
			return;
		}
	}
	
	public StreamedContent downloadMapping()
	{
		MappingDesc desc = mappingToDesc();
		if (desc.getFields().isEmpty())
			return null;
		
		try
		{
			Files.createDirectories(outputStorage);
			Path file = outputStorage.resolve("mapping.xml");
			XmlUtils.marshalObject(desc, file.toAbsolutePath().toString());
			return WebUtils.downloadFile(file.toFile());
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not download mapping", e, getLogger());
			return null;
		}
	}
	
	protected DataSourceSettings createDataSourceSettings()
	{
		return new DataSourceSettings(DataSource.UPLOAD);
	}
	
	protected DbConnectionsCache createDbConnectionsCache()
	{
		return new DbConnectionsCache(ClearThCore.connectionStorage());
	}

	protected BasicTableDataReader<String, String, ?> createExpectedReader()
	{
		return createReader(expData, "expected");
	}
	
	protected BasicTableDataReader<String, String, ?> createActualReader()
	{
		return createReader(actData, "actual");
	}
	

	protected BasicTableDataReader<String, String, ?> createReader(DataSourceSettings settings, String kind)
	{
		switch (settings.getSource())
		{
			case UPLOAD :
				return createReader(settings.getUploadedFile(), settings.getCsvSettings().getDelimiter(), kind);
			case BACKEND :
				return createReader(settings.getPathOnBackend(), settings.getCsvSettings().getDelimiter(), kind);
			case DB:
				return createReader(settings.getDbSettings().getConnectionName(), settings.getDbSettings().getQuery(), kind);
			default :
			{
				MessageUtils.addWarningMessage("Unsupported source", "Selected data source ("+settings.getSource()+") is not supported");
				return null;
			}
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(UploadedFile uploadedFile, char delimiter, String kind)
	{
		if (uploadedFile == null)
		{
			MessageUtils.addWarningMessage("No file uploaded", "Please select "+kind+" file");
			return null;
		}
		
		File file;
		try
		{
			Files.createDirectories(uploadsStorage.toPath());
			file = WebUtils.storeUploadedFile(uploadedFile, uploadsStorage, kind+"_", ".csv");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not store uploaded "+kind+" file", e, getLogger());
			return null;
		}
		
		return createReader(file, delimiter, kind);
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(String path, char delimiter, String kind)
	{
		if (StringUtils.isEmpty(path))
		{
			MessageUtils.addWarningMessage("No file specified", "Please specify path to "+kind+" file");
			return null;
		}
		
		Path file = Path.of(ClearThCore.rootRelative(path));
		if (!Files.isRegularFile(file))
		{
			MessageUtils.addWarningMessage("Invalid path", "Specified "+kind+" file doesn't exist or is not a file");
			return null;
		}
		
		return createReader(file.toFile(), delimiter, kind);
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(File file, char delimiter, String kind)
	{
		try
		{
			ClearThCsvReaderConfig config = ClearThCsvReaderConfig.withFirstLineAsHeader();
			config.setDelimiter(delimiter);
			
			return new CsvDataReader(file, config);
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not create reader for "+kind+" file", e, getLogger());
			return null;
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(String connectionName, String query, String kind)
	{
		if (StringUtils.isEmpty(connectionName))
		{
			MessageUtils.addWarningMessage("No connection specified", "Please choose " + kind + " connection");
			return null;
		}
		
		if (StringUtils.isEmpty(query))
		{
			MessageUtils.addWarningMessage("Query is empty", "Please specify query for " + kind + " connection");
			return null;
		}
		
		try
		{
			return new DbDataReader(cache.getConnection(connectionName).getConnection().prepareStatement(query), true);
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not create reader for " + kind + " DB connection", e, getLogger());
			return null;
		}
	}
	
	protected MappingDesc mappingToDesc()
	{
		List<FieldDesc> fields = new ArrayList<>();
		for (MappingEntry entry : mappingEntries)
		{
			if (!StringUtils.isEmpty(entry.getName()))
				fields.add(entry.toFieldDesc());
		}
		
		MappingDesc desc = new MappingDesc();
		desc.setFields(fields);
		return desc;
	}
	
	protected void descToMapping(MappingDesc desc)
	{
		mappingEntries.clear();
		for (FieldDesc fd : desc.getFields())
			mappingEntries.add(MappingEntry.fromFieldDesc(fd));
	}
	
	protected DataMapping<String> createMapping()
	{
		MappingDesc desc = mappingToDesc();
		return new StringDataMapping(desc);
	}
	
	
	protected File createResultFile(ComparisonResult compResult) throws IOException
	{
		Path result = Files.createTempFile(outputStorage, "result_", ".zip");
		new ComparisonResultWriter().write(compResult, result);
		return result.toFile();
	}
	
	public DataSourceSettings getExpData()
	{
		return expData;
	}
	
	public DataSourceSettings getActData()
	{
		return actData;
	}
	
	public List<String> getConnections()
	{
		return cache.getConnectionNames();
	}
}
