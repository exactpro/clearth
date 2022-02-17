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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.ThreadDumpGenerator;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

public class LogsBean extends ClearThBean
{
	private static final String USER_LOG = "User activity log";
	
	private final File logsDir, outputDir;
	private List<String> selectedLogsList = new ArrayList<String>();
	
	private String currentLogger = null;
	private final Map<String, Logger[]> loggers;

	private Set<String> keysForAdmin;
	private Set<String> keysForPowerUser;
	private final Pattern logBackupPattern = Pattern.compile("\\D+\\.log\\.\\d");
	
	public LogsBean()
	{
		File dir = WebUtils.getLogsDir();
		if (!dir.exists())
			getLogger().warn("Logs directory '"+dir.getAbsolutePath()+"' doesn't exist");
		
		logsDir = dir;
		outputDir = new File(logsDir, "output/");
		
		String pathToLogConfig = ClearThCore.rootRelative(ClearThCore.configFiles().getCfgDir()+"loggers.cfg");
		Map<String, String> definitions = KeyValueUtils.loadKeyValueFile(pathToLogConfig, false);
		
		loggers = buildLoggersInfo(definitions, pathToLogConfig);
		this.keysForAdmin = new LinkedHashSet<>(this.loggers.keySet());
		this.keysForPowerUser = new LinkedHashSet<>(this.loggers.keySet());
		this.keysForPowerUser.remove(USER_LOG);
	}
	
	
	//*** Logs manipulation routines ***
	
	public List<String> getAllLogsList()
	{
		List<String> result = new ArrayList<>();
		if (logsDir.exists())
		{
			File[] logsFiles = logsDir.listFiles();
			if (logsFiles!=null)
				for (File file : logsFiles)
				{
					if ((!file.isDirectory()) && (!file.getName().equals(".donotdelete")))
						result.add(file.getName());
				}
		}

		Collections.sort(result);

		return result;
	}

	
	public List<String> getSelectedLogsList()
	{
		return selectedLogsList;
	}

	public void setSelectedLogsList(List<String> selectedLogsList)
	{
		this.selectedLogsList = selectedLogsList;
	}

	public void selectAllLogs()
	{
		this.selectedLogsList = getAllLogsList();
	}

	public void deselectAllLogs()
	{
		this.selectedLogsList = Collections.emptyList();
	}

	public StreamedContent getLogsZip()
	{
		if (selectedLogsList.size()==0)
			return null;
		
		if (!logsDir.exists())
			return null;
		
		try
		{
			if (!outputDir.exists())
				outputDir.mkdir();
			
			List<File> filesToZip = new ArrayList<File>();
			for (String logName : selectedLogsList)
				filesToZip.add(new File(logsDir, logName));
			File result = new File(outputDir, UserInfoUtils.getUserName()+"_logs.zip");
			FileOperationUtils.zipFiles(result, filesToZip);
			result.deleteOnExit();
			StreamedContent file = new DefaultStreamedContent(new FileInputStream(result), new MimetypesFileTypeMap().getContentType(result), "logs.zip");
			//selectedLogsList.clear();
			return file;
		}
		catch (Exception e)
		{
			String msg = "Could not download logs";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, ExceptionUtils.getDetailedMessage(e));
			return null;
		}
	}

	public void clearLogs()
	{
		if (selectedLogsList.size()==0)
			return;

		if (!logsDir.exists())
			return;

		for (String logFileName : selectedLogsList)
		{
			if (isLogBackup(logFileName))
				removeLogBackup(logFileName);
			else
				clearLogFile(logFileName);
		}
	}
	
	
	protected void removeLogBackup(String logFileName)
	{
		Path logPath = Paths.get(logsDir.getAbsolutePath(), logFileName);
		try
		{
			Files.delete(logPath);
			getLogger().info("deleted log file '{}'", logPath);
		}
		catch (IOException e)
		{
			String msg = "Could not delete log file '"+logFileName+"'";
			getLogger().warn(msg, e);
			MessageUtils.addWarningMessage(msg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	protected void clearLogFile(String logFileName)
	{
		File logFile = new File(logsDir, logFileName);
		try
		{
			FileOperationUtils.clearFileContent(logFile);
			getLogger().info("cleared log file '{}'", logFile);
		}
		catch (IOException e)
		{
			String msg = "Could not clear contents of file '"+logFile.getPath()+"'";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	
	protected List<Logger> getLoggersForPackages(String loggerName, String loggerPackages, String pathOfConfig)
	{
		List<Logger> result = new ArrayList<>();
		String[] packages = loggerPackages.split(",");
		for (String loggerPackage : packages)
		{
			Logger loggerObject = LogManager.getLogger(loggerPackage);
			if (loggerObject.getLevel() != null)
				result.add(loggerObject);
			else
				getLogger().error(String.format("Could not get level for logger '%s'. "
								+ "Mapping to non-existing package '%s' or other misconfiguration in file '%s'.", 
						loggerName, loggerObject.getName(), pathOfConfig));
		}
		return result;
	}
	
	protected Map<String, Logger[]> buildLoggersInfo(Map<String, String> definitions, String pathOfConfig)
	{
		Map<String, Logger[]> result = new LinkedHashMap<String, Logger[]>();
		for (Entry<String, String> entry : definitions.entrySet())
		{
			List<Logger> loggerObjects = getLoggersForPackages(entry.getKey(), entry.getValue(), pathOfConfig);
			if (!loggerObjects.isEmpty())
				result.put(entry.getKey(), loggerObjects.toArray(new Logger[0]));
		}
		return result;
	}

	private boolean isLogBackup(String logFileName)
	{
		return logBackupPattern.matcher(logFileName).find();
	}
	
	
	//*** Logging level routines ***

	@Deprecated
	public Set<String> getLoggers()
	{
		return this.keysForAdmin;
	}
	
	public Set<String> getLoggers(boolean isAdmin)
	{
		return isAdmin ? this.keysForAdmin : this.keysForPowerUser;
	}
	
	
	public String getCurrentLogger()
	{
		return currentLogger == null ? "None" : currentLogger;
	}

	public void setCurrentLogger(String currentLogger)
	{
		if (USER_LOG.equals(currentLogger) && !UserInfoUtils.isAdmin()) {
			MessageUtils.addErrorMessage("Denied", "You don't have sufficient permissions to change this logger. This incident will be reported.");
			return;
		}
		this.currentLogger = currentLogger;
	}
	
	
	public String getLoggingLevel()
	{
		if (!StringUtils.isEmpty(currentLogger))
			return loggers.get(currentLogger)[0].getLevel().toString();
		else 
			return "<Choose logger>";
	}
	
	public String getAllLoggingLevel()
	{
		int count = 0;
		String lvl = "no loggers";
		for (String key : loggers.keySet()) {
			if (count == 0) {
				lvl = loggers.get(key)[0].getLevel().toString();
			}
			if (!loggers.get(key)[0].getLevel().toString().equals(lvl))
				return "various levels";
			count++;
		}
		return lvl;
	}
	
	public void setLoggingLevel(String lvl)
	{
		if (currentLogger == null) {
			MessageUtils.addWarningMessage("No logger selected", "Choose logger before setting logger level");
			return;
		}
		
		Level level = Level.toLevel(lvl);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		for (Logger loggerObject : loggers.get(currentLogger))
		{
			LoggerConfig loggerConfig = config.getLoggerConfig(loggerObject.getName());
			loggerConfig.setLevel(level);
		}
		ctx.updateLoggers();
		
		MessageUtils.addInfoMessage("Success", "Level " + lvl + " is now set for '" + currentLogger + "'");
	}
	
	public void setAllLoggingLevels(String lvl)
	{
		Set<String> loggerKeys;
		if (UserInfoUtils.isAdmin()) {
			loggerKeys = this.keysForAdmin;
		} else if (UserInfoUtils.isPowerUser()) {
			loggerKeys = this.keysForPowerUser;
		} else {
			loggerKeys = Collections.emptySet();
		}
		
		Level level = Level.toLevel(lvl);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		for (String loggerKey : loggerKeys)
		{
			Logger[] logger = loggers.get(loggerKey);
			for (Logger loggerObject : logger)
			{
				LoggerConfig loggerConfig = config.getLoggerConfig(loggerObject.getName());
				loggerConfig.setLevel(level);
			}
		}
		ctx.updateLoggers();
		
		MessageUtils.addInfoMessage("Success", "Level " + lvl + " is now set for all loggers");
	}
	
	public void resetLoggingLevel()
	{
		ClearThCore.getInstance().configureLogging();
	}
	
	public void testLogging()
	{
		Logger[] selectedLoggers;
		if (currentLogger == null || (selectedLoggers = loggers.get(currentLogger)) == null)
			return;
		
		for (Logger loggerObject : selectedLoggers)
		{
			org.apache.logging.log4j.Logger lg = LogManager.getLogger(loggerObject.getName()+".Test");
			lg.trace("Logging test at TRACE level");
			lg.debug("Logging test at DEBUG level");
			lg.info ("Logging test at INFO  level");
			lg.warn ("Logging test at WARN  level");
			lg.error("Logging test at ERROR level");
		}
	}

	public File getLogsDir()
	{
		return logsDir;
	}
	
	
	//*** Thread dumps routines ***

	public StreamedContent getThreadDumps()
	{
		if (!logsDir.exists())
			return null;
		try
		{
			if (!outputDir.exists())
				outputDir.mkdir();
			ThreadDumpGenerator threadDumpGenerator = new ThreadDumpGenerator();
			File result = threadDumpGenerator.writeThreadDump(outputDir, UserInfoUtils.getUserName()+"_thread_dump.txt");
			result.deleteOnExit();
			return new DefaultStreamedContent(new FileInputStream(result), new MimetypesFileTypeMap().getContentType(result), "thread_dump.txt");
		}
		catch (Exception e)
		{
			String msg = "Could not generate thread dumps";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, ExceptionUtils.getDetailedMessage(e));
			return null;
		}
	}
}
