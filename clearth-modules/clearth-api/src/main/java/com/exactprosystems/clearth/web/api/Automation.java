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
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.report.ModifiableReportsConfig;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.automation.report.html.HtmlReport;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.MatrixUploadHandler;
import com.exactprosystems.clearth.utils.exception.MatrixUploadHandlerException;
import com.exactprosystems.clearth.web.api.helpers.*;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.automation.report.ActionReportWriter.*;
import static com.exactprosystems.clearth.web.api.ApiUtils.getBooleanValue;
import static com.exactprosystems.clearth.web.api.ApiUtils.getZippedFiles;
import static com.exactprosystems.clearth.web.api.RestConstants.*;

@Path("schedulers")
public class Automation
{
	private static final UserLogger logger = new UserLogger(LoggerFactory.getLogger(Automation.class));
	
	public static final String MY_SCHEDULER = "myscheduler";

	protected static final String BASE_TIME_TEMPLATE = "HH:mm:ss", BUSINESS_DAY_TEMPLATE = "yyyyMMdd";
	protected static final SimpleDateFormat BASE_TIME_FORMAT = new SimpleDateFormat(BASE_TIME_TEMPLATE);
	protected static final SimpleDateFormat BUSINESS_DAY_FORMAT = new SimpleDateFormat(BUSINESS_DAY_TEMPLATE);
	protected static final String PARAMETER_NOT_SPECIFIED_MSG = "Mandatory parameter '%s' not specified";
	protected static final MatrixUploadHandler MATRIX_UPLOAD_HANDLER = createMatrixUploadHandler();

	private final File uploadStorage = new File(ClearThCore.uploadStoragePath());
	
	public static String getUserName(HttpServletRequest request)
	{
		String[] authInfo = ApiUtils.getAuthorizationUser(request.getHeader(ApiUtils.HEADER_AUTH));
		if (authInfo != null)
			return authInfo[0];
		return null;
	}
	
	public static Scheduler findScheduler(String schedulerName, String userName)
	{
		SchedulersManager schedulersManager = ClearThCore.getInstance().getSchedulersManager();
		if (schedulerName.equalsIgnoreCase(MY_SCHEDULER) || schedulerName.equals(userName))
		{
			Scheduler s = schedulersManager.getDefaultUserScheduler(userName);
			if (s == null)
			{
				try
				{
					return schedulersManager.addDefaultUserScheduler(userName);
				}
				catch (Exception e)
				{
					String msg = "Could not init user scheduler";
					logger.error(userName, msg, e);
					throw new WebApplicationException(ApiUtils.error(msg, ExceptionUtils.getDetailedMessage(e)));
				}
			}
			return s;
		}
		else
		{
			Scheduler s = schedulersManager.getSchedulerByName(schedulerName, userName);
			if (s != null)
				return s;
			throw new WebApplicationException(ApiUtils.badRequest("Scheduler with name '" + schedulerName + "' not found"));
		}
	}
	
	public static Response removeMatrix(String name, Scheduler scheduler)
	{
		if ((name == null) || (name.isEmpty()))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_NAME));
		
		MatrixData toRemove = null;
		for (MatrixData md : scheduler.getMatricesData())
			if (md.getFile().getName().equals(name))
			{
				toRemove = md;
				break;
			}
		if (toRemove == null)
			return ApiUtils.badRequest("Matrix with name '"+name+"' not found in scheduler");
		scheduler.removeMatrix(toRemove);
		return ApiUtils.ok("Matrix '"+name+"' removed");
	}
	
	public static Response getSchedulerHtmlReport(String matrixName, String executionStartDate, Scheduler scheduler, boolean withDetails)
	{
		if (!withDetails)
			return getSchedulerReport(matrixName, executionStartDate, scheduler, HTML_REPORT_NAME);
		return getSchedulerReportWithDetails(matrixName, executionStartDate, scheduler, HTML_REPORT_NAME, Set.of(JSON_REPORT_NAME, HTML_FAILED_REPORT_NAME));
	}
	
	public static Response getSchedulerHtmlFailedReport(String matrixName, String executionStartDate, Scheduler scheduler, boolean withDetails)
	{
		if (!withDetails)
			return getSchedulerReport(matrixName, executionStartDate, scheduler, HTML_FAILED_REPORT_NAME);
		return getSchedulerReportWithDetails(matrixName, executionStartDate, scheduler, HTML_FAILED_REPORT_NAME, Set.of(JSON_REPORT_NAME, HTML_REPORT_NAME));
	}
	
	public static Response getSchedulerJsonReport(String matrixName, String executionStartDate, Scheduler scheduler, boolean withDetails)
	{
		if (!withDetails)
			return getSchedulerReport(matrixName, executionStartDate, scheduler, JSON_REPORT_NAME);
		
		Set<String> excludedFiles = new HashSet<>(Arrays.asList(HtmlReport.resourceFiles));
		excludedFiles.add(HtmlReport.APP_LOGO);
		excludedFiles.add(HtmlReport.APP_LOGO_NY);
		excludedFiles.add(HTML_REPORT_NAME);
		excludedFiles.add(HTML_FAILED_REPORT_NAME);
		return getSchedulerReportWithDetails(matrixName, executionStartDate, scheduler, JSON_REPORT_NAME, excludedFiles);
	}
	
	private static Response getSchedulerReport(String matrixName, String executionStartDate, Scheduler scheduler, String reportFileName)
	{
		java.nio.file.Path report = getSchedulerReportPath(matrixName, executionStartDate, scheduler)
				.resolve(reportFileName);
		String resultName = getDownloadedReportName(matrixName, executionStartDate, reportFileName);
		return ApiUtils.fileResponse(report.toAbsolutePath().toString(), resultName);
	}
	
	private static Response getSchedulerReportWithDetails(String matrixName, String executionStartDate, Scheduler scheduler,
			String reportFileName, Set<String> excludedFiles)
	{
		java.nio.file.Path reportPath = getSchedulerReportPath(matrixName, executionStartDate, scheduler);
		String resultName = getDownloadedReportName(matrixName, executionStartDate, reportFileName) + ".zip";
		try (Stream<java.nio.file.Path> stream = Files.list(reportPath))
		{
			List<File> files = stream.filter(el -> !excludedFiles.contains(el.getFileName().toString()))
					.map(java.nio.file.Path::toFile)
					.collect(Collectors.toList());
			return ApiUtils.fileResponse(getZippedFiles(files, reportFileName).getAbsolutePath().toString(), resultName);
		}
		catch (IOException e)
		{
			return ApiUtils.errorWithLog(String.format("Error while downloading report '%s'", resultName), e, logger.getLogger());
		}
	}
	
	public static Response getSchedulerReportStatus(final String matrixName, final String executionStartDate, final Scheduler scheduler)
	{
		if ((matrixName == null) || (matrixName.isEmpty()))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_NAME));
		if ((executionStartDate == null) || (executionStartDate.isEmpty()))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_DATE));
		
		DateFormat df = newDateFormatter();
		Date currentStartTime = scheduler.getStartTime();
		// if scheduler.getMatrices().isEmpty(), launchInfo was already created
		if (currentStartTime != null && df.format(currentStartTime).equals(executionStartDate) && !scheduler.getMatrices().isEmpty())
		{
			for (Matrix matrix : scheduler.getMatrices())
			{
				if (matrixName.equalsIgnoreCase(matrix.getName()))
					return ApiUtils.ok(matrix.isSuccessful() ? "Passed" : "Failed");
			}
		}
		else
		{
			for (XmlSchedulerLaunchInfo launchInfo : scheduler.getSchedulerData().getLaunches().getLaunchesInfo())
			{
				if (df.format(launchInfo.getStarted()).equals(executionStartDate))
				{
					for (XmlMatrixInfo matrixInfo : launchInfo.getMatricesInfo())
					{
						if (matrixInfo.getName().equalsIgnoreCase(matrixName))
							return ApiUtils.ok(matrixInfo.isSuccessful() ? "Passed" : "Failed");
					}
					break;
				}
			}
		}
		return ApiUtils.badRequest("Could not find status of matrix '"+matrixName+"', which execution started at '"+executionStartDate+"'");
	}
	
	public static Response getSchedulerStartTimestamp(Scheduler scheduler)
	{
		Date currentStartTime = scheduler.getStartTime();
		if (currentStartTime == null)
			return ApiUtils.badRequest("Scheduler is not running");
		return Response.ok(new XmlResponse("Scheduler '"+scheduler.getName()+"' was started", null,
				newDateFormatter().format(currentStartTime)).toXml(null)).build();
	}

	public static Response getSchedulerHistory(Scheduler scheduler)
	{
		XmlSchedulerLaunches launches = scheduler.getSchedulerData().getLaunches();
		
		SchedulerLaunchesResource result = new SchedulerLaunchesResource();
		List<SchedulerLaunchInfoResource> launchesInfo = result.getLaunchesInfo();
		launches.getLaunchesInfo().stream()
				.forEach(launch -> launchesInfo.add(SchedulerLaunchInfoResource.fromLaunchInfo(launch)));
		
		String response = new XmlResponse(null, null, result)
				.toXml(new Class[]{SchedulerLaunchesResource.class, SchedulerLaunchInfoResource.class, MatrixInfoResource.class});
		return Response.ok(response).build();
	}


	public static Response setSchedulerBusinessDay(String date, Scheduler scheduler)
	{
		if (StringUtils.isEmpty(date))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_DATE));
		
		try
		{
			Date businessDay = BUSINESS_DAY_FORMAT.parse(date);
			scheduler.setBusinessDay(businessDay);
			return ApiUtils.ok("Business day has been set successfully");
		}
		catch (ParseException e)
		{
			return ApiUtils.badRequest(String.format("Value '%s' of parameter '%s' is invalid. It should be date in format '%s'",
					date, RestConstants.PARAM_DATE, BUSINESS_DAY_TEMPLATE));
		}
		catch (IOException | AutomationException e)
		{
			return ApiUtils.error("Error while setting business day", ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public static Response setSchedulerBaseTime(String time, Scheduler scheduler)
	{
		if (StringUtils.isEmpty(time))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_DATE));
			
		try
		{
			Date baseTime = BASE_TIME_FORMAT.parse(time);
			scheduler.setBaseTime(baseTime);
			return ApiUtils.ok("Base time has been set successfully");
		}
		catch (ParseException e)
		{
			return ApiUtils.badRequest(String.format("Value '%s' of parameter '%s' is invalid. It should be time in format '%s'",
					time, RestConstants.PARAM_DATE, BASE_TIME_TEMPLATE));
		}
		catch (IOException | AutomationException e)
		{
			return ApiUtils.error("Error while setting base time", ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public static Response getLastSuccessfulActionsCount(Scheduler scheduler)
	{
		if (!scheduler.isRunning())
			return ApiUtils.badRequest(String.format("Scheduler '%s' isn't running", scheduler.getName()));
		
		return ApiUtils.ok(scheduler.getCurrentStep().getExecutionProgress().toString());
	}
	
	public static Response saveSchedulerState(Scheduler scheduler)
	{
		try
		{
			scheduler.saveState();
			return ApiUtils.ok("State was successfully saved. You can restore it later when scheduler is stopped.");
		}
		catch (AutomationException e)
		{
			return ApiUtils.error("Could not save state. Only status of suspended scheduler in single run mode can be saved.");
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while saving state of '" + scheduler.getName() + "' scheduler: " + ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public static Response restoreSchedulerState(Scheduler scheduler, String userName)
	{
		try
		{
			scheduler.restoreState(userName);
			return ApiUtils.ok("Scheduler state restored. Execution continues.");
		}
		catch (AutomationException e)
		{
			return ApiUtils.error("Could not restore state. State can only be restored if saved previously and if scheduler is not running.");
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while restoring state of '" + scheduler.getName() + "' scheduler: " + ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public static DateFormat newDateFormatter()
	{
		return new SimpleDateFormat("yyyyMMddHHmmssSSS");
	}
	
	public static File storeAutomationFile(InputStream dataStream, String prefix, String postfix) throws IOException
	{
		File destDir = new File(ClearThCore.rootRelative(ClearThCore.automationStoragePath()));
		File storedFile;
		FileOutputStream output = null;
		try
		{
			storedFile = File.createTempFile(prefix, postfix, destDir);
			output = new FileOutputStream(storedFile);
			byte[] buffer = new byte[4096];
			int read;
			while ((read = dataStream.read(buffer, 0, buffer.length)) >- 1)
				output.write(buffer, 0, read);
		}
		finally
		{
			if (output != null)
			{
				output.flush();
				output.close();
			}
		}
		return storedFile;
	}

	protected static MatrixUploadHandler createMatrixUploadHandler()
	{
		return new MatrixUploadHandler(new File(ClearThCore.automationStoragePath()));
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}")
	@Produces({MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
	public Response doWithScheduler(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@QueryParam(RestConstants.PARAM_ACTION) String action,
			@QueryParam(RestConstants.PARAM_NAME) String name,
			@QueryParam(RestConstants.PARAM_DATE) String date,
			@QueryParam(PARAM_WITH_DETAILS) boolean repWithDetails,
			@Context HttpServletRequest request)
	{
		String userName = getUserName(request);
		Scheduler s = findScheduler(schedulerName, userName);
		if ((action == null) || (action.isEmpty()))
			return ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_ACTION));
		
		if ("start".equalsIgnoreCase(action))
		{
			try
			{
				s.start(userName != null ? userName+" via API" : "API");
				Date startTime = s.getStartTime();  //Here we can get null in case of very short matrix finished before we reach here.
				String startTimeStr = startTime != null ? newDateFormatter().format(startTime) : "Could not get start time";
				logger.info(userName, "Started execution of scheduler '{}'", schedulerName);
				return Response.ok(new XmlResponse("Scheduler '"+schedulerName+"' started", null,
						startTimeStr).toXml(null)).build();
			}
			catch (Exception e)
			{
				return ApiUtils.error("Could not start scheduler '"+schedulerName+"'", ExceptionUtils.getDetailedMessage(e));
			}
		}
		else if ("stop".equalsIgnoreCase(action))
		{
			try
			{
				s.stop();
				logger.info(userName, "Stopped execution of scheduler '{}'", schedulerName);
				return ApiUtils.ok("Scheduler '"+schedulerName+"' stopped");
			}
			catch (AutomationException e)
			{
				return ApiUtils.error("Could not stop scheduler '"+schedulerName+"'", ExceptionUtils.getDetailedMessage(e));
			}
		}
		else if ("continue".equalsIgnoreCase(action))
		{
			try
			{
				s.continueExecution();
				logger.info(userName, "Continued execution of scheduler '{}'", schedulerName);
				return ApiUtils.ok("Scheduler '"+schedulerName+"' execution continued");
			}
			catch (AutomationException e)
			{
				return ApiUtils.error(e.getMessage());
			}
		}
		else if ("pause".equalsIgnoreCase(action))
		{
			try
			{
				s.pause();
				logger.info(userName, "Paused scheduler '{}'", schedulerName);
				return ApiUtils.ok("Scheduler '"+schedulerName+"' paused");
			}
			catch (AutomationException e)
			{
				return ApiUtils.error("Could not pause scheduler '"+schedulerName+"'", ExceptionUtils.getDetailedMessage(e));
			}
		}
		else if ("removematrix".equalsIgnoreCase(action))
			return removeMatrix(name, s);
		else if ("removeallmatrices".equalsIgnoreCase(action))
		{
			s.removeAllMatrices();
			return ApiUtils.ok("All matrices removed");
		}
		else if ("removeallsteps".equalsIgnoreCase(action))
		{
			try
			{
				s.clearSteps();
				return ApiUtils.ok("All steps removed");
			}
			catch (IOException | AutomationException e)
			{
				return ApiUtils.error("Error occurred while removing steps", ExceptionUtils.getDetailedMessage(e));
			}
		}
		else if ("getreport".equalsIgnoreCase(action))
			return getSchedulerHtmlReport(name, date, s, repWithDetails);
		else if ("getJsonReport".equalsIgnoreCase(action))
			return getSchedulerJsonReport(name, date, s, repWithDetails);
		else if ("getFailedReport".equalsIgnoreCase(action))
			return getSchedulerHtmlFailedReport(name, date, s, repWithDetails);
		else if ("getreportstatus".equalsIgnoreCase(action))
			return getSchedulerReportStatus(name, date, s);
		else if ("getstarttimestamp".equalsIgnoreCase(action))
			return getSchedulerStartTimestamp(s);
		else if ("gethistory".equalsIgnoreCase(action))
			return getSchedulerHistory(s);
		else if ("setbusinessday".equalsIgnoreCase(action))
			return setSchedulerBusinessDay(date, s);
		else if ("setbasetime".equalsIgnoreCase(action))
			return setSchedulerBaseTime(date, s);
		else if ("actionssuccessful".equalsIgnoreCase(action))
			return getLastSuccessfulActionsCount(s);
		else if ("saveState".equalsIgnoreCase(action))
			return saveSchedulerState(s);
		else if ("restoreState".equalsIgnoreCase(action))
			return restoreSchedulerState(s, userName);
		else
			return ApiUtils.badRequest("Invalid action - '"+action+"'");
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/"+RestConstants.PARAM_STATUS)
	public Response getSchedulerStatus(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler s = findScheduler(schedulerName, getUserName(request));
		return ApiUtils.ok(s.isSuspended() ? "Execution suspended" : (s.isRunning() ? "Running" : "Stopped"));
	}

	@GET
	@Produces(MediaType.TEXT_XML)
	@Path('{' +RestConstants.PARAM_NAME+"}/"+RestConstants.PARAM_STATUS + '/' + PARAM_EXTENDED)
	public Response getSchedulerExtendedStatus(@PathParam(RestConstants.PARAM_NAME) String schedulerName, @Context HttpServletRequest request)
	{
		Scheduler scheduler = findScheduler(schedulerName, getUserName(request));
		SchedulerStatusDataResource resource = new SchedulerStatusDataResource(scheduler);
		return Response.ok(new XmlResponse(null, null, resource).toXml(new Class[]{SchedulerStatusDataResource.class})).build();
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/matrices")
	@Produces(MediaType.TEXT_XML)
	public Response getSchedulerMatrices(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler s = findScheduler(schedulerName, getUserName(request));
		
		List<MatrixDataResource> mdr = new ArrayList<MatrixDataResource>();
		for (MatrixData md : s.getMatricesData())
			mdr.add(new MatrixDataResource(md));
		
		return Response.ok(new XmlResponse(null, null, mdr).toXml(new Class[]{MatrixDataResource.class})).build();
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/steps")
	@Produces(MediaType.TEXT_XML)
	public Response getSchedulerConfig(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler s = findScheduler(schedulerName, getUserName(request));
		
		List<StepDataResource> sdr = new ArrayList<StepDataResource>();
		for (Step step : s.getSchedulerData().getSteps())
			sdr.add(new StepDataResource(step));
		
		return Response.ok(new XmlResponse(null, null, sdr).toXml(new Class[]{StepDataResource.class})).build();
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/config")
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response getSchedulerSettings(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler s = findScheduler(schedulerName, getUserName(request));
		SchedulerData schedulerData = s.getSchedulerData();
		
		File resultFile = null;
		try
		{
			resultFile = ClearThCore.getInstance().getSchedulerSettingsTransmitter().exportSettings(schedulerData);
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while exporting configuration for scheduler '"+schedulerName+"'", 
					ExceptionUtils.getDetailedMessage(e));
		}
		
		return ApiUtils.fileResponse(resultFile);
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/exportinfo")
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response exportSchedulerInfo(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		File resultFile = null;
		
		try
		{
			resultFile = ClearThCore.getInstance().getSchedulerInfoExporter().exportAllZip(selectedScheduler);
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while exporting info of scheduler '"+schedulerName+"'", ExceptionUtils.getDetailedMessage(e));
		}
		
		return ApiUtils.fileResponse(resultFile);
	}
	
	@POST
	@Path("{"+RestConstants.PARAM_NAME+"}/uploadconfig")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadSchedulerSettings(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
											@FormDataParam(PARAM_CONFIG) InputStream settingsStream,
											@FormDataParam(PARAM_CONFIG) FormDataContentDisposition settingsDetail,
											@Context HttpServletRequest request)
	{
		Scheduler scheduler = findScheduler(schedulerName, getUserName(request));

		try
		{
			File storedFile = FileOperationUtils.storeToFile(settingsStream, uploadStorage, "scheduler_settings_", ".zip");
			ClearThCore.getInstance().getSchedulerSettingsTransmitter().deploySettings(storedFile, scheduler);
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while importing scheduler configuration for '"+schedulerName+"'",
					ExceptionUtils.getDetailedMessage(e));
		}
		
		return ApiUtils.ok("Scheduler configuration applied successfully");
	}

	@POST
	@Path("{"+RestConstants.PARAM_NAME+"}/uploadmatrix")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadMatrix(@PathParam(RestConstants.PARAM_NAME) String schedulerName, 
			@FormDataParam("matrix") InputStream fileStream, @FormDataParam("matrix") FormDataContentDisposition fileDetail,
			@Context HttpServletRequest request)
	{
		Scheduler scheduler = findScheduler(schedulerName, getUserName(request));

		String fileName = fileDetail.getFileName();
		try
		{
			MATRIX_UPLOAD_HANDLER.handleUploadedFile(fileStream, fileName, scheduler);
			return ApiUtils.ok("Matrix '" + fileName + "' successfully uploaded");
		}
		catch (MatrixUploadHandlerException e)
		{
			return ApiUtils.error(e.getMessage(), e.getDetails());
		}
	}

	@POST
	@Path("{"+RestConstants.PARAM_NAME+"}/uploadsteps")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadSchedulerSteps(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@FormDataParam("configfile") InputStream fileStream, @FormDataParam("configfile") FormDataContentDisposition fileDetail,
			@Context HttpServletRequest request)
	{
		Scheduler s = findScheduler(schedulerName, getUserName(request));

		String fileName = fileDetail.getFileName();

		File storedFile;
		try
		{
			storedFile = storeAutomationFile(fileStream, "config_", "cfg");
		}
		catch (IOException e)
		{
			return ApiUtils.error("Error while storing uploaded file", ExceptionUtils.getDetailedMessage(e));
		}

		try
		{
			s.uploadSteps(storedFile, fileName, null, false);
			return ApiUtils.ok("Scheduler configuration file '"+fileName+"' applied");
		}
		catch (Exception e)
		{
			return ApiUtils.error("Error while applying scheduler configuration file '"+fileName+"'", ExceptionUtils.getDetailedMessage(e));
		}
	}

	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/usecurrentdate")
	@Produces(MediaType.TEXT_XML)
	public Response useCurrentDate(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		
		try
		{
			selectedScheduler.setBusinessDay(null);
		}
		catch (IOException | AutomationException e)
		{
			return ApiUtils.error("Error while toggling usage of current date in scheduler '"+schedulerName+"': "+ExceptionUtils.getDetailedMessage(e));
		}
		
		return ApiUtils.ok("Successfully toggled usage of current date to 'true' in scheduler '"+schedulerName+"'");
	}
	
	@GET
	@Path("{"+RestConstants.PARAM_NAME+"}/usecurrenttime")
	@Produces(MediaType.TEXT_XML)
	public Response useCurrentTime(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		
		try
		{
			selectedScheduler.useCurrentTime();
		}
		catch (IOException | AutomationException e)
		{
			return ApiUtils.error("Error while toggling usage of base time in scheduler '"+schedulerName+"': "+ExceptionUtils.getDetailedMessage(e));
		}
		
		return ApiUtils.ok("Successfully toggled usage of base time to 'true' in scheduler '"+schedulerName+"'");
	}
	
	
	/* Failover management routines */
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/setconnectionstoignorefailures")
	@Produces
	public Response setConnectionsToIgnoreFailures(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@QueryParam("ignoreall") boolean ignoreAll, @QueryParam("connections") String connections,
			@Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		if (selectedScheduler.isRunning())
			return ApiUtils.error("Could not apply changes: scheduler '" + schedulerName + "' is currently running");
		
		SchedulerData selectedSchedulerData = selectedScheduler.getSchedulerData();
		try
		{
			selectedSchedulerData.setIgnoreAllConnectionsFailures(ignoreAll);
			selectedSchedulerData.saveIgnoreAllConnectionsFailures();
			
			if (connections != null)
			{
				Set<String> allConnections = ClearThCore.connectionStorage().getConnections().stream()
						.map(ClearThConnection::getName).collect(Collectors.toSet());
				selectedSchedulerData.setConnectionsToIgnoreFailures(Arrays.stream(connections.split(","))
						.map(String::trim).filter(StringUtils::isNotEmpty).filter(allConnections::contains)
						.collect(Collectors.toCollection(LinkedHashSet::new)));
				selectedSchedulerData.saveConnectionsToIgnoreFailures();
			}
			
			return ApiUtils.ok("Specified connections to ignore failures have been applied for scheduler '" + schedulerName + "'");
		}
		catch (Exception e)
		{
			return ApiUtils.error("Could not save connections to ignore failures", ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/getconnectionstoignorefailures")
	@Produces
	public Response getConnectionsToIgnoreFailures(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@Context HttpServletRequest request)
	{
		SchedulerData selectedSchedulerData = findScheduler(schedulerName, getUserName(request)).getSchedulerData();
		if (selectedSchedulerData.isIgnoreAllConnectionsFailures())
			return ApiUtils.ok("All connections failures are ignored");
		
		Set<String> connectionsToIgnoreFailures = selectedSchedulerData.getConnectionsToIgnoreFailures();
		return ApiUtils.ok("Connections failures are " + (connectionsToIgnoreFailures.isEmpty() ? "not ignored"
				: "ignored for " + connectionsToIgnoreFailures));
	}
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/isfailover")
	@Produces(MediaType.TEXT_XML)
	public Response isFailover(@PathParam(RestConstants.PARAM_NAME) String schedulerName, @Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		String failoverReasonString = selectedScheduler.getFailoverReasonString();
		return ApiUtils.ok(failoverReasonString != null ? "Connection failover occurred for '"
						+ selectedScheduler.getFailoverConnectionName() + "' in scheduler '" + schedulerName + "': " + failoverReasonString
				: "Scheduler '" + schedulerName + "' is " + (selectedScheduler.isRunning() ? "running normally" : "not currently executing"));
	}
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/restartfailoveraction")
	@Produces(MediaType.TEXT_XML)
	public Response restartFailoverAction(@PathParam(RestConstants.PARAM_NAME) String schedulerName, @Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		if (!selectedScheduler.isFailover())
			return ApiUtils.error("Scheduler '" + schedulerName + "' is not in failover state");
		
		try
		{
			selectedScheduler.setFailoverRestartAction(true);
			return ApiUtils.ok("Current action in scheduler '" + schedulerName + "' has been restarted");
		}
		catch (AutomationException e)
		{
			return ApiUtils.error(e.getMessage());
		}
	}
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/skipfailoveraction")
	@Produces(MediaType.TEXT_XML)
	public Response skipFailoverAction(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@QueryParam("skipsame") boolean skipSame, @Context HttpServletRequest request)
	{
		Scheduler selectedScheduler = findScheduler(schedulerName, getUserName(request));
		if (!selectedScheduler.isFailover())
			return ApiUtils.error("Scheduler '" + schedulerName + "' is not in failover state");
		
		String connectionName = selectedScheduler.getFailoverConnectionName();
		try
		{
			selectedScheduler.setFailoverSkipAction(true);
			if (skipSame)
				selectedScheduler.addConnectionToIgnoreFailuresByRun(connectionName);
			return ApiUtils.ok("Current action in scheduler '" + schedulerName + "' has been skipped"
					+ (skipSame ? ". All next failures for connection '" + connectionName + "' will be skipped too" : ""));
		}
		catch (AutomationException e)
		{
			return ApiUtils.error(e.getMessage());
		}
	}
	
	
	@GET
	@Path("{" + RestConstants.PARAM_NAME + "}/setreportsconfig")
	@Produces(MediaType.TEXT_XML)
	public Response setReportsConfig(@PathParam(RestConstants.PARAM_NAME) String schedulerName,
			@QueryParam(PARAM_COMPLETE_HTML) String completeHtml,
			@QueryParam(PARAM_FAILED_HTML) String failedHtml,
			@QueryParam(PARAM_COMPLETE_JSON) String completeJson,
			@Context HttpServletRequest request)
	{
		String userName = getUserName(request);
		Scheduler s = findScheduler(schedulerName, userName);
		
		return setReportsConfig(s, completeHtml, failedHtml, completeJson);
	}
	
	private Response setReportsConfig(Scheduler s, String completeHtml, String failedHtml, String completeJson)
	{
		try
		{
			ReportsConfig currCfg = s.getReportsConfig();
			ModifiableReportsConfig cfg = new ModifiableReportsConfig(currCfg);
			boolean html, fHtml, json,
					updateCfg = false;
			
			if (!StringUtils.isEmpty(completeHtml) && currCfg.isCompleteHtmlReport() != (html = getBooleanValue(PARAM_COMPLETE_HTML, completeHtml)))
			{
				cfg.setCompleteHtmlReport(html);
				updateCfg = true;
			}
			
			if (!StringUtils.isEmpty(failedHtml) && currCfg.isFailedHtmlReport() != (fHtml = getBooleanValue(PARAM_FAILED_HTML, failedHtml)))
			{
				cfg.setFailedHtmlReport(fHtml);
				updateCfg = true;
			}
			
			if (!StringUtils.isEmpty(completeJson) && currCfg.isCompleteJsonReport() != (json = getBooleanValue(PARAM_COMPLETE_JSON, completeJson)))
			{
				cfg.setCompleteJsonReport(json);
				updateCfg = true;
			}
			
			if (!updateCfg)
				return ApiUtils.ok("Reports configuration already matches the input parameters");
			
			s.setReportsConfig(new ReportsConfig(cfg));
			return ApiUtils.ok("Reports configuration has been updated");
		}
		catch (IOException | AutomationException e)
		{
			return ApiUtils.error("Error while updating reports configuration", ExceptionUtils.getDetailedMessage(e));
		}
		catch (ParametersException e)
		{
			return ApiUtils.badRequest(e.getMessage());
		}
	}
	
	
	private static java.nio.file.Path getSchedulerReportPath(String matrixName, String executionStartDate, Scheduler scheduler)
	{
		if (StringUtils.isEmpty(matrixName))
			throw new WebApplicationException(ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_NAME)));
		if (StringUtils.isEmpty(executionStartDate))
			throw new WebApplicationException(ApiUtils.badRequest(String.format(PARAMETER_NOT_SPECIFIED_MSG, RestConstants.PARAM_DATE)));
		
		return java.nio.file.Path.of(ClearThCore.appRootRelative(ClearThCore.reportsPath()), scheduler.getForUser(), scheduler.getName(),
				executionStartDate, "completed", matrixName);
	}
	
	private static String getDownloadedReportName(String matrixName, String executionStartDate, String reportFileName)
	{
		return String.format("%s_%s_%s", matrixName, executionStartDate, reportFileName);
	}
}
