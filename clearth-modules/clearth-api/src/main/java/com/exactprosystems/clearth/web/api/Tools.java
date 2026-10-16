/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.html.ReportParser;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.messages.CollectorMessageSource;
import com.exactprosystems.clearth.messages.StringMessageSource;
import com.exactprosystems.clearth.tools.*;
import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdater;
import com.exactprosystems.clearth.utils.DateTimeUtils;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.web.api.helpers.DictionaryValidatorDataResource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.exactprosystems.clearth.web.api.Automation.getUserName;
import static com.exactprosystems.clearth.web.api.RestConstants.*;

@Path("tools")
public class Tools
{
	private static final Logger logger = LoggerFactory.getLogger(Tools.class);
	
	private static final String TIME_FORMAT_STRING = "HH:mm:ss";
	private static final String TIME_FORMAT_MILLIS_STRING = "HH:mm:ss:SSS";
	private static final String CDATA_PREFIX = "<![CDATA[", CDATA_SUFFIX = "]]>";
	
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(TIME_FORMAT_STRING);
	public static final SimpleDateFormat TIME_FORMAT_MILLIS = new SimpleDateFormat(TIME_FORMAT_MILLIS_STRING);
	
	private static final XStream VALIDATE_DICTIONARY_TOOL_XSTREAM = new XStream
	(
		new XppDriver()
		{
			@Override
			public HierarchicalStreamWriter createWriter(Writer out)
		    {
				return new PrettyPrintWriter(out)
				{
					@Override
					protected void writeText(QuickWriter writer, String text)
					{
						if (text.startsWith(CDATA_PREFIX) && text.endsWith(CDATA_SUFFIX))
							writer.write(text);
						else
							super.writeText(writer, text);
					}
				};
		    }
		}
	);
	
	private final ToolsFactory toolsFactory = ClearThCore.getInstance().getToolsFactory();
	private final File uploadStorage = new File(ClearThCore.uploadStoragePath());
	private final File tempDir = new File(ClearThCore.tempPath());
	
	/** Tools */
	private ConfigMakerTool configMakerTool = null;
	private MessageToScriptTool messageToScriptTool = null;
	private ScriptToMessageTool scriptToMessageTool = null;
	private ReportParser reportParser = null;
	private DictionaryValidatorTool dictionaryValidatorTool = null;

	@POST
	@Path("/makeconfig")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response makeConfig(@FormDataParam(PARAM_MATRIX) InputStream matrixStream,
	                           @FormDataParam(PARAM_MATRIX) FormDataContentDisposition matrixDetail,
	                           @Context HttpServletRequest request)
	{
		final File configFile;

		try
		{
			configFile = configFromMatrix(matrixStream, matrixDetail);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while making config", e, logger);
		}

		return ApiUtils.fileResponse(configFile.getAbsolutePath(), configFile.getName());
	}
	
	@POST
	@Path("/makeconfigandapply")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response makeConfigAndApply(@FormDataParam(PARAM_MATRIX) InputStream matrixStream,
	                                   @FormDataParam(PARAM_MATRIX) FormDataContentDisposition matrixDetail,
	                                   @QueryParam(RestConstants.PARAM_NAME) String schedulerName,
	                                   @QueryParam(RestConstants.PARAM_APPEND) String append,
	                                   @Context HttpServletRequest request)
	{
		final Scheduler selectedScheduler = Automation.findScheduler(schedulerName, getUserName(request));
		final File configFile;

		try
		{
			configFile = configFromMatrix(matrixStream, matrixDetail);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while storing uploaded file", e, logger);
		}

		try
		{
			boolean appendCurrent = InputParamsUtils.YES.contains(append);			
			List<String> warnings = new ArrayList<String>();
			selectedScheduler.uploadSteps(configFile, configFile.getName(), warnings, appendCurrent);
			if (warnings.isEmpty())
				return ApiUtils.ok("Scheduler configuration uploaded successfully");
			else
				return ApiUtils.error("Scheduler configuration uploaded with errors:" + StringUtils.join(warnings, "; "));
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while uploading scheduler configuration from created configuration",
					e, logger);
		}
	}

	@POST
	@Path("/scripttomsg")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public Response scriptToMessage(@FormDataParam(PARAM_SCRIPT) InputStream scriptStream,
	                                @FormDataParam(PARAM_SCRIPT) FormDataContentDisposition scriptDetail,
	                                @Context HttpServletRequest request)
	{
		String convertedScript;

		try
		{
			String scriptString = IOUtils.toString(scriptStream, Utils.UTF8);
			convertedScript = getScriptToMessageTool().convertScript(scriptString);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while converting script", e, logger);
		}

		return Response.ok(convertedScript).build();
	}

	@POST
	@Path("/msgtoscript")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public Response messageToScript(@FormDataParam(PARAM_MESSAGE) InputStream messageStream,
	                                @FormDataParam(PARAM_MESSAGE) FormDataContentDisposition messageDetail,
	                                @QueryParam(PARAM_FORMAT) String format,
	                                @Context HttpServletRequest request)
	{
		String convertedMessage;
		try
		{
			String messageString = IOUtils.toString(messageStream, Utils.UTF8);
			if (StringUtils.isEmpty(format))
				format = "auto";
			convertedMessage = getMessageToScriptTool().convertMessage(messageString, format);
		}
		/*catch (DecodeException e)
		{
			return ApiUtils.errorWithLog("Could not convert message", e, logger);
		}
		catch(DictionaryLoadException e)
		{
			return ApiUtils.errorWithLog("Could not load dictionary",  e, logger);
		}*/
		catch(Exception e)
		{
			return ApiUtils.errorWithLog("Error while converting message",  e, logger);
		}

		return Response.ok(convertedMessage).build();
	}

	@POST
	@Path("/makematrix")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response makeMatrix(@FormDataParam(PARAM_REPORT) InputStream reportStream,
	                           @FormDataParam(PARAM_REPORT) FormDataContentDisposition reportDetail,
	                           @Context HttpServletRequest request)
	{
		final File matrixFile;
		final ReportParser rp = getReportParser();
		try
		{
			File storedFile = FileOperationUtils.storeToFile(reportStream, uploadStorage, "reportformatrix_", ".htm");
			matrixFile = rp.writeMatrix(storedFile, tempDir.getCanonicalPath());
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while making matrix", e, logger);
		}

		return ApiUtils.fileResponse(matrixFile.getAbsolutePath(), matrixFile.getName());
	}
	
	@GET
	@Path("collectedmsgs")
	@Produces({MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
	public Response getCollectedMessages(@QueryParam(PARAM_NAME) String name,
	                                     @QueryParam(PARAM_TIME) String time,
	                                     @QueryParam(PARAM_FORMAT) String format)
	{
		ClearThConnection con = ClearThCore.connectionStorage().getConnection(name);
		if (con == null)
			return ApiUtils.error("Unknown connection name");
		
		ClearThMessageConnection selected;
		if (con instanceof ClearThMessageConnection)
			selected = (ClearThMessageConnection)con;
		else 
			return ApiUtils.error("Connection "+ name + " is not suitable for sending messages!");
		
		if (!selected.isRunning())
			return ApiUtils.error("Selected connection isn't running");
		
		MessageListener listener = selected.findListener(ListenerType.Collector.getLabel());
		if (listener == null)
			return ApiUtils.ok("Connection listener has not been initialized");
		
		long fromTime = 0;
		if (!StringUtils.isEmpty(time))
		{
			fromTime = parseTime(time, format);
		}


		ClearThMessageCollector imc = (ClearThMessageCollector)listener;
		StringMessageSource messageSource;


		if (fromTime == 0)
			messageSource = new CollectorMessageSource(imc, true);
		else
			messageSource = new CollectorMessageSource(imc, fromTime, true);

		Collection<String> msgList = new ArrayList<String>();
		try
		{
			String msg;
			while ((msg = messageSource.nextStringMessage()) != null)
			{
				msgList.add(msg);
			}
		}
		catch (IOException e)
		{
			return ApiUtils.errorWithLog("Exception while getting messages from collector", e, logger);
		}

		
		if (msgList.isEmpty())
			return ApiUtils.ok("No messages was found");
		else
			return Response.ok(StringUtils.join(msgList, Utils.EOL + Utils.EOL)).build();
	}

	@POST
	@Path("/checkmatrices")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response checkMatrices(@FormDataParam(PARAM_MATRIX) InputStream fileStream,
								  @FormDataParam(PARAM_MATRIX) FormDataContentDisposition fileDetail,
								  @QueryParam(PARAM_SCHEDULER) String schedulerName,
								  @Context HttpServletRequest request)
	{
		if (schedulerName == null)
			schedulerName = Automation.MY_SCHEDULER;
		Scheduler selectedScheduler = Automation.findScheduler(schedulerName, getUserName(request));

		File resultFile;
		try
		{
			resultFile = getCheckMatricesResult(fileStream, fileDetail, selectedScheduler);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while making files", e, logger);
		}
		if (resultFile == null)
			return Response.ok().build();
		else
			return ApiUtils.fileResponse(resultFile);
	}
	
	@POST
	@Path("/validatedictionary")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public Response validateDictionary(@FormDataParam(PARAM_MESSAGE) InputStream messageStream,
	                                @QueryParam(PARAM_FORMAT) String format,
	                                @QueryParam(PARAM_CONFIG) String config,
	                                @Context HttpServletRequest request)
	{
		try
		{
			String messageString = IOUtils.toString(messageStream, Utils.UTF8);
			
			if (StringUtils.isEmpty(format))
				format = DictionaryValidatorTool.AUTO_FORMAT;
			
			config = DictionaryValidatorTool.getValidationConfigByApi(config);
			
			DictionaryValidationResult result = getDictionaryValidatorTool()
					.validateDictionary(messageString, format, config, null);
			
			DictionaryValidatorDataResource resource = new DictionaryValidatorDataResource(result);
			resource.setOriginalText(CDATA_PREFIX+resource.getOriginalText()+CDATA_SUFFIX);
			resource.setEncodedText(CDATA_PREFIX+resource.getEncodedText()+CDATA_SUFFIX);
			
			return ApiUtils.ok(resource, DictionaryValidatorDataResource.class, VALIDATE_DICTIONARY_TOOL_XSTREAM);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while validating dictionary", e, logger);
		}
	}

	@POST
	@Path("/matrixupdater")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public Response updateMatrix(@FormDataParam(PARAM_MATRIX) InputStream fileMatrixStream,
								 @FormDataParam(PARAM_MATRIX) FormDataContentDisposition fileMatrixDetail,
								 @FormDataParam(PARAM_CONFIG) InputStream fileConfigStream,
								 @FormDataParam(PARAM_CONFIG) FormDataContentDisposition fileConfigDetail,
								 @Context HttpServletRequest request)
	{
		try
		{
			File updatedMatrix = getCheckMatrixUpdaterResult(fileMatrixStream, fileMatrixDetail,
					fileConfigStream, fileConfigDetail, getUserName(request));
			return ApiUtils.fileResponse(updatedMatrix);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while update matrix", e, logger);
		}
	}
	
	
	@GET
	@Path("/sqlexecutor/execute")
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_XML })
	public Response executeQuery(@QueryParam(RestConstants.PARAM_CONNECTION_NAME) String connectionName,
			@QueryParam(RestConstants.PARAM_QUERY) String query,
			@QueryParam(RestConstants.PARAM_MAX_ROWS) int maxRows,
			@Context HttpServletRequest request)
	{
		if (StringUtils.isEmpty(connectionName) || StringUtils.isEmpty(query))
			return ApiUtils.badRequest("'"+RestConstants.PARAM_CONNECTION_NAME+"' and '"+RestConstants.PARAM_QUERY+"' arguments are mandatory");
		
		return new SqlExecutorApi().executeQuery(connectionName, query, maxRows);
	}
	
	@POST
	@Path("/sqlexecutor/execute")
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_XML })
	public Response executeQueryPost(@FormParam(RestConstants.PARAM_CONNECTION_NAME) String connectionName,
			@FormParam(RestConstants.PARAM_QUERY) String query,
			@FormParam(RestConstants.PARAM_MAX_ROWS) int maxRows,
			@Context HttpServletRequest request)
	{
		return executeQuery(connectionName, query, maxRows, request);
	}
	
	@GET
	@Path("/sqlexecutor/check")
	@Produces({MediaType.TEXT_XML})
	public Response checkConnection(@QueryParam(RestConstants.PARAM_CONNECTION_NAME) String connectionName,
			@Context HttpServletRequest request)
	{
		if (StringUtils.isEmpty(connectionName))
			return ApiUtils.badRequest("'"+RestConstants.PARAM_CONNECTION_NAME+"' argument is mandatory");
		
		return new SqlExecutorApi().checkConnection(connectionName);
	}
	
	
	private File getCheckMatrixUpdaterResult(InputStream fileMatrixStream, FormDataContentDisposition fileMatrixDetail,
											 InputStream fileConfigStream, FormDataContentDisposition fileConfigDetail,
											 String username) throws Exception
	{
		File temp = FileOperationUtils.createTempDirectory("matrix_updater_tmp", tempDir),
			 fileMatrix = FileOperationUtils.storeToFile(fileMatrixStream, temp, fileMatrixDetail.getFileName()),
			 fileConfig = FileOperationUtils.storeToFile(fileConfigStream, temp, fileConfigDetail.getFileName());

		MatrixUpdater updater = new MatrixUpdater(username,
				Files.createDirectories(uploadStorage.toPath().resolve("matrix_updater_api")));
		updater.setConfig(fileConfig);
		return updater.update(fileMatrix);
	}

	private File getCheckMatricesResult(InputStream fileStream, FormDataContentDisposition fileDetail, Scheduler scheduler)
			throws Exception
	{
		File uploadDir = FileOperationUtils.createTempDirectory("checkmatrices_tmp", uploadStorage);
		File workDir = FileOperationUtils.createTempDirectory("checkmatrices_tmp", tempDir);

		File file = FileOperationUtils.storeToFile(fileStream, uploadDir, fileDetail.getFileName());
		List<File> fileList = FileOperationUtils.unzipIfNeed(file, "unzipped_input");

		Map<String, List<ActionGeneratorMessage>> checkingResult = checkMatrices(scheduler, fileList);

		if (checkingResult == null)
			return null;

		List<File> resultFiles = new ArrayList<File>();
		String[] header = new String[]{"Issue type", "Issue kind", "Description"};
		List<List<String[]>> tables = new ArrayList<List<String[]>>();
		for (String matrixName : checkingResult.keySet())
		{
			List<String[]> table = new ArrayList<String[]>();
			for (ActionGeneratorMessage msg : checkingResult.get(matrixName))
				table.add(new String[]{msg.type.name(), msg.kind.getDescription(), msg.message});
			tables.add(table);

			File tableFile = FileOperationUtils.createCsvFile(new File(workDir, matrixName), header, table);
			resultFiles.add(tableFile);
		}
		File resultZipFile = File.createTempFile("checkmatrices_result", ".zip", workDir);
		FileOperationUtils.zipFiles(resultZipFile, resultFiles);
		return resultZipFile;
	}

	private Map<String, List<ActionGeneratorMessage>> checkMatrices(Scheduler scheduler, List<File> matricesFiles)
			throws Exception
	{
		MatrixDataFactory mdf = ClearThCore.getInstance().getMatrixDataFactory();

		List<MatrixData> matricesData = new ArrayList<MatrixData>();
		for (File matrixFile : matricesFiles)
			matricesData.add(mdf.createMatrixData(matrixFile, new Date(), true, true));
		return scheduler.checkMatrices(scheduler.getSchedulerData().loadSteps(null), new ArrayList<Matrix>(), matricesData, null);
	}

	private long parseTime(String timeString, String format)
	{
		long t;
		if (format != null)
		{
			try
			{
				SimpleDateFormat specifiedFormat = new SimpleDateFormat(format);
				Date d = specifiedFormat.parse(timeString);
				Calendar input = Calendar.getInstance();
				input.setTime(d);
				if (input.get(Calendar.YEAR) == 1970)   //check to ERA
					t = DateTimeUtils.getTodaysMillis(d);
				else
					t = d.getTime();
			}
			catch (ParseException e)
			{
				throw new WebApplicationException(ApiUtils.error("Error while parsing date by specified format",
						ExceptionUtils.getDetailedMessage(e)));
			}
		}
		else
		{
			try
			{
				t = getMillis(timeString, TIME_FORMAT);
			}
			catch (ParseException e1)
			{
				try
				{
					t = getMillis(timeString, TIME_FORMAT_MILLIS);
				}
				catch (ParseException e2)
				{
					throw new WebApplicationException(ApiUtils.error(String.format(
							"Invalid time format. Use '%s' or '%s' formats or specify format by '%s' parameter", 
							TIME_FORMAT_STRING, TIME_FORMAT_MILLIS_STRING, PARAM_FORMAT),
							ExceptionUtils.getDetailedMessage(e2)));
				}
			}
		}
		return t;
	}

	private long getMillis(String timeString, SimpleDateFormat format) throws ParseException
	{
		Date d = format.parse(timeString);
		return DateTimeUtils.getTodaysMillis(d);
	}

	private File configFromMatrix(InputStream matrixStream, FormDataContentDisposition matrixDetail) throws Exception {
		final File matrixFile = FileOperationUtils.storeToFile(matrixStream, uploadStorage, "matrixforconfig_", ".csv"),
				   config = getConfigMakerTool().makeConfig(matrixFile, tempDir, matrixDetail.getFileName());
		return config;
	}

	/** Tools getters */

	protected ConfigMakerTool getConfigMakerTool()
	{
		if (configMakerTool == null)
		{
			synchronized (this)
			{
				if (configMakerTool == null)
					configMakerTool = toolsFactory.createConfigMakerTool();
			}
		}
		return configMakerTool;
	}

	protected MessageToScriptTool getMessageToScriptTool()
	{
		if (messageToScriptTool == null)
		{
			synchronized (this)
			{
				if (messageToScriptTool == null)
					messageToScriptTool = toolsFactory.createMessageToScriptTool();
			}
		}
		return messageToScriptTool;
	}

	protected ScriptToMessageTool getScriptToMessageTool()
	{
		if (scriptToMessageTool == null)
		{
			synchronized (this)
			{
				if (scriptToMessageTool == null)
					scriptToMessageTool = toolsFactory.createScriptToMessageTool();
			}
		}
		return scriptToMessageTool;
	}

	protected ReportParser getReportParser()
	{
		if (reportParser == null)
		{
			synchronized (this)
			{
				if (reportParser == null)
					reportParser = toolsFactory.createReportParser();
			}
		}
		return reportParser;
	}
	
	protected DictionaryValidatorTool getDictionaryValidatorTool()
	{
		if (dictionaryValidatorTool == null)
		{
			synchronized (this)
			{
				if (dictionaryValidatorTool == null)
					dictionaryValidatorTool = toolsFactory.createDictionaryValidatorTool();
			}
		}
		return dictionaryValidatorTool;
	}
}
