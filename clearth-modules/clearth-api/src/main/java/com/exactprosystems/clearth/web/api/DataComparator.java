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

package com.exactprosystems.clearth.web.api;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.tools.datacomparator.*;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.StringDataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.MappingDesc;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;

@Path("tools/datacomparator")
public class DataComparator
{
	public static final String EXP_FILE = "expFile", ACT_FILE = "actFile",
			EXP_FILE_PATH = "expFilePath", ACT_FILE_PATH = "actFilePath",
			EXP_DELIMITER = "expDelimiter", ACT_DELIMITER = "actDelimiter",
			EXP_CONNECTION = "expConnection", ACT_CONNECTION = "actConnection",
			EXP_QUERY = "expQuery", ACT_QUERY = "actQuery",
			MAPPING_FILE = "mappingFile";
	private static final String EXP_PARAMS = EXP_FILE + ", " + EXP_FILE_PATH + ", " + EXP_CONNECTION,
			ACT_PARAMS = ACT_FILE + ", " + ACT_FILE_PATH + ", " + ACT_CONNECTION;
	private static final Logger logger = LoggerFactory.getLogger(DataComparator.class);
	private static final String TEMP_DIR = "data_comparator_";
	private final DataComparatorTool comparator = ClearThCore.getInstance().getToolsFactory().createDataComparatorTool();
	private final ClearThConnectionStorage storage = ClearThCore.connectionStorage();
	
	
	@POST
	@Produces({MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
	public Response compareData(@FormDataParam(EXP_FILE) InputStream expFileStream,
								@FormDataParam(EXP_FILE) FormDataContentDisposition expFileDetail,
								@FormDataParam(ACT_FILE) InputStream actFileStream,
								@FormDataParam(ACT_FILE) FormDataContentDisposition actFileDetail,
								@FormDataParam(EXP_FILE_PATH) String expFilePath,
								@FormDataParam(ACT_FILE_PATH) String actFilePath,
								@FormDataParam(EXP_DELIMITER) String expDelimiter,
								@FormDataParam(ACT_DELIMITER) String actDelimiter,
								@FormDataParam(EXP_CONNECTION) String expConnection,
								@FormDataParam(ACT_CONNECTION) String actConnection,
								@FormDataParam(EXP_QUERY) String expQuery,
								@FormDataParam(ACT_QUERY) String actQuery,
								@FormDataParam(MAPPING_FILE) InputStream mappingFileStream,
								@FormDataParam(MAPPING_FILE) FormDataContentDisposition mappingFileDetail)
	{
		BasicTableDataReader<String, String, ?> expReader = null, actReader = null;
		try
		{
			Source expSource = createSource(expFileStream, expFileDetail, expFilePath, expConnection, expQuery, true);
			Source actSource = createSource(actFileStream, actFileDetail, actFilePath, actConnection, actQuery, false);
			
			File tempPath = FileOperationUtils.createTempDirectory(TEMP_DIR, new File(ClearThCore.tempPath()));
			
			expReader = createReader(tempPath, expFileStream, expFileDetail, expFilePath, expDelimiter, expConnection, expQuery, expSource, true);
			actReader = createReader(tempPath, actFileStream, actFileDetail, actFilePath, actDelimiter, actConnection, actQuery, actSource, false);
			
			DataMapping<String> mapping = createMapping(mappingFileStream, mappingFileDetail, tempPath);
			
			ComparisonResult result = comparator.compare(expReader, actReader,
					new ComparisonSettings(tempPath.toPath().resolve("result"), mapping, ClearThCore.comparisonUtils()));
			return ApiUtils.fileResponse(createResultFile(result, tempPath));
		}
		catch (ParametersException e)
		{
			return ApiUtils.badRequest("Incorrect parameters. " + e.getMessage());
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while comparing data", e, logger);
		}
		finally
		{
			Utils.closeResource(expReader);
			Utils.closeResource(actReader);
		}
	}
	
	private DataMapping<String> createMapping(InputStream mappingFileStream, FormDataContentDisposition mappingFileDetail,
			File tempPath) throws JAXBException, IOException
	{
		if (mappingFileStream == null)
			return null;
		
		File file = createFile(mappingFileStream, mappingFileDetail, tempPath);
		MappingDesc desc = XmlUtils.unmarshalObject(MappingDesc.class, file.getAbsolutePath());
		
		return new StringDataMapping(desc);
	}
	
	private void checkParams(int sources, boolean expected) throws ParametersException
	{
		if (sources == 0)
			throw new ParametersException(String.format("Specify one of data sources: %s", expected ?  EXP_PARAMS : ACT_PARAMS));
		if (sources > 1)
			throw new ParametersException(String.format("Parameters %s cannot be used together. Specify only one of them", expected ? EXP_PARAMS : ACT_PARAMS));
	}
	
	private void checkConnection(String connection, String query, boolean expected) throws ParametersException
	{
		if (connection != null && query == null)
			throw new ParametersException(String.format("Parameter '%s' is missing for connection '%s'", expected ? EXP_QUERY : ACT_QUERY, connection));
	}
	
	private File createFile(InputStream fileStream, FormDataContentDisposition fileDetail, File tempPath) throws IOException
	{
		return FileOperationUtils.storeToFile(fileStream, tempPath, "uploaded_", fileDetail.getFileName());
	}
	
	protected File createResultFile(ComparisonResult comparisonResult, File tempPath) throws IOException
	{
		java.nio.file.Path result = Files.createTempFile(tempPath.toPath(), "result_", ".zip");
		new ComparisonResultWriter().write(comparisonResult, result);
		
		return result.toFile();
	}
	
	private Source createSource(InputStream fileStream, FormDataContentDisposition fileDetail, String filePath,
			String connection, String query, boolean expected) throws ParametersException
	{
		Source source = null;
		int sources = 0;
		if (fileStream != null && fileDetail != null)
		{
			source = Source.UPLOAD;
			sources++;
		}
		if (filePath != null)
		{
			source = Source.BACKEND;
			sources++;
		}
		if (connection != null)
		{
			source = Source.DB;
			sources++;
		}
		
		checkParams(sources, expected);
		if (source == Source.DB)
			checkConnection(connection, query, expected);
		
		return source;
	}

	public BasicTableDataReader<String, String, ?> createReader(File tempPath, InputStream fileStream, FormDataContentDisposition fileDetail, String filePath, String delimiter,
			String connection, String query, Source source, boolean expected) throws IOException, ConnectionException, ParametersException
	{
		String kind = expected ? "expected" : "actual";
		switch (source)
		{
			case UPLOAD :
			case BACKEND :
				File file;
				if (source == Source.BACKEND)
				{
					file = new File(ClearThCore.rootRelative(filePath));
					if (!file.exists())
						throw new ParametersException("Invalid path. Specified " + kind + " file does not exist or is not a file");
				}
				else
					file = createFile(fileStream, fileDetail, tempPath);
				char chDelimiter = (delimiter == null || delimiter.isEmpty()) ? ',' : delimiter.charAt(0);
				return createReader(file, chDelimiter, kind);
			case DB:
				return createReader(connection, query, kind);
			default :
				throw new ParametersException( "Data source '" + source + "' is not supported. Reader for " + kind + " data is not created");
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(File file, char delimiter, String kind) throws IOException
	{
		try
		{
			ClearThCsvReaderConfig config = ClearThCsvReaderConfig.withFirstLineAsHeader();
			config.setDelimiter(delimiter);
			
			return new CsvDataReader(file, config);
		}
		catch (Exception e)
		{
			throw new IOException("Could not create reader for " + kind + " file", e);
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createReader(String connectionName, String query, String kind) throws ConnectionException
	{
		if (!storage.containsConnection(connectionName))
			throw new ConnectionException(String.format("Connection '%s' does not exist.", connectionName));
		
		Connection connection = null;
		try
		{
			DbConnection dbConnection = (DbConnection) storage.getConnection(connectionName, "DB");
			connection = dbConnection.getConnection();
			return new DbDataReader(connection.prepareStatement(query), true);
		}
		catch (Exception e)
		{
			Utils.closeResource(connection);
			throw new ConnectionException("Could not create reader for " + kind + " DB connection", e);
		}
	}
	
	public enum Source
	{
		UPLOAD,
		BACKEND,
		DB
	}
}
