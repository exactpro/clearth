/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth;

public class ConfigFiles
{
	private static final String XMLDATANMS = "com.exactprosystems.clearth.xmldata";
	
	private static final String CFG_DIR = "cfg/";
	private static final String LOGCFG_FILENAME = CFG_DIR+"log.properties";
	private static final String USERS_FILENAME = CFG_DIR+"users.xml";
	private static final String SCHEDULERS_FILENAME = CFG_DIR+"schedulers.cfg";
	private static final String SCHEDULERS_DIR = CFG_DIR+"schedulers/";
	private static final String USER_SETTINGS_DIR = CFG_DIR+"usersettings/";
	private static final String ACTIONS_MAPPING_FILENAME = CFG_DIR+"actionsmapping.cfg";
	private static final String DICTS_DIR = CFG_DIR+"dicts/";
	private static final String CODECS_FILENAME = CFG_DIR+"codecs.xml";
	private static final String MESSAGE_CONVERTERS_FILENAME = CFG_DIR+"message_converters.xml";
	private static final String SCRIPT_CONVERTERS_FILENAME = CFG_DIR+"script_converters.xml";
	private static final String MESSAGE_HELPERS_FILENAME = CFG_DIR+"message_helpers.xml";
	private static final String GUI_CONFIG_FILENAME = "gui_config.cfg";

	private static final String LOGS_DIR = "logs/";
	private static final String CONNECTIONS_DIR = "connections/";
	private static final String FILE_CONNECTIONS_DIR = "file_connections/";
	private static final String DB_CONNECTIONS_DIR = "db_connections/";
	private static final String FTP_CONNECTIONS_DIR = "ftp_connections/";

	private static final String HTML_TEMPLATES_DIR = CFG_DIR+"templates/";

	private static final String TEMPLATES_DIR = "templates/";
	
	private static final String AUTOMATION_STORAGE_DIR = "automation/storage/";
	private static final String SCRIPTS_DIR = "automation/scripts/";
	private static final String REPORTS_DIR = "automation/reports/";
	private static final String LAST_EXECUTION_DIR = "automation/last_execution/";
	private static final String REALTIME_REPORT_DIR = "../ui/restricted/realtime/";

	private static final String UPLOAD_STORAGE_DIR = "uploads/storage/";
	private static final String TEMP_DIR = "temp/";
	private static final String DATA_DIR = "data/";

	private static final String DEFAULT_REPORT_FILES_DIR = "report_files/";
	private static final String DEFAULT_SCHEDULER_INFO_FILES_DIR = "scheduler_info_files/";

	
	
	private final String configFileName;
	
	public ConfigFiles(String configFileName)
	{
		this.configFileName = configFileName;
	}
	
	
	public String getCfgDir()
	{
		return CFG_DIR;
	}
	
	public String getLogCfgFileName()
	{
		return LOGCFG_FILENAME;
	}
	
	public String getXmlDataNms()
	{
		return XMLDATANMS;
	}
	
	public String getUsersFileName()
	{
		return USERS_FILENAME;
	}
	
	public String getSchedulersFileName()
	{
		return SCHEDULERS_FILENAME;
	}
	
	public String getActionsMappingFileName()
	{
		return ACTIONS_MAPPING_FILENAME;
	}
	
	public String getLogsDir()
	{
		return LOGS_DIR;
	}
	
	public String getConnectionsDir()
	{
		return CONNECTIONS_DIR;
	}

	public String getFileConnectionsDir()
	{
		return FILE_CONNECTIONS_DIR;
	}

	public String getFtpConnectionsDir()
	{
		return FTP_CONNECTIONS_DIR;
	}

	public String getDbConnectionsDir()
	{
		return DB_CONNECTIONS_DIR;
	}

	public String getHtmlTemplatesDir()
	{
		return HTML_TEMPLATES_DIR;
	}

	public String getTemplatesDir()
	{
		return TEMPLATES_DIR;
	}
	
	public String getUploadStorageDir()
	{
		return UPLOAD_STORAGE_DIR;
	}

	public String getDataDir()
	{
		return DATA_DIR;
	}

	public String getTempDir()
	{
		return TEMP_DIR;
	}
	
	public String getSchedulersDir()
	{
		return SCHEDULERS_DIR;
	}
	
	public String getUserSettingsDir()
	{
		return USER_SETTINGS_DIR;
	}
	
	public String getDictsDir()
	{
		return DICTS_DIR;
	}
	
	public String getAutomationStorageDir()
	{
		return AUTOMATION_STORAGE_DIR;
	}
	
	public String getScriptsDir()
	{
		return SCRIPTS_DIR;
	}
	
	public String getCodecsFileName()
	{
		return CODECS_FILENAME;
	}
	
	public String getMessageConvertersFileName()
	{
		return MESSAGE_CONVERTERS_FILENAME;
	}
	
	public String getScriptConvertersFileName()
	{
		return SCRIPT_CONVERTERS_FILENAME;
	}
	
	public String getMessageHelpersFileName()
	{
		return MESSAGE_HELPERS_FILENAME;
	}
	
	public String getGUIConfigFileName()
	{
		return GUI_CONFIG_FILENAME;
	}

	public String getConfigFileName()
	{
		return configFileName;
	}
	
	public String getConfigFilePath()
	{
		return getCfgDir()+getConfigFileName();
	}
	
	public String getReportFilesDir()
	{
		return DEFAULT_REPORT_FILES_DIR;
	}
	
	public String getReportsDir()
	{
		return REPORTS_DIR;
	}

	public String getLastExecutionDir()
	{
		return LAST_EXECUTION_DIR;
	}

	public String getRealTimeReportDir()
	{
		return REALTIME_REPORT_DIR;
	}

	public String getSchedulerInfoFilesDir()
	{
		return DEFAULT_SCHEDULER_INFO_FILES_DIR;
	}

	public String getRootPath()
	{
		return "../"+getCfgDir();
	}
	
	public String getAlternativeRootPath()
	{
		return getCfgDir();
	}
}
