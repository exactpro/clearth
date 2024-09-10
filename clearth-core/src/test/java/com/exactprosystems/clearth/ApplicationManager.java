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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.config.ClearThConfiguration;
import com.exactprosystems.clearth.config.ConfigurationException;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.generators.IncrementingValueGenerators;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Stopwatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mockito.Matchers;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ApplicationManager
{
	public static final Path USER_DIR = Paths.get(System.getProperty("user.dir")),
			PROJ_DIR = USER_DIR.getParent(),
			TEST_RES_DIR = PROJ_DIR.resolve("clearth-core").resolve("src").resolve("test").resolve("resources");

	public static final String ADMIN = "admin";
	public static final String TEST_OUTPUT = "testOutput/",
			LOGS_DIR = PROJ_DIR.resolve(TEST_OUTPUT).resolve("logs").toString(),
			TEST_DATA_DIR = TEST_OUTPUT + "appRoot/",
			TEST_REPORT_DIR = TEST_OUTPUT + "SchedulerTestData/",
			LOG_PROPERTIES_FILE_PATH = TEST_RES_DIR.resolve("log.properties").toString(),
			APP_ROOT = PROJ_DIR.resolve(TEST_DATA_DIR).toString(),
			WEB_APP_DIR = PROJ_DIR.resolve("clearth-modules/clearth-gui/src/main/webapp").toString(),
			WEB_UI_RESTRICTED = WEB_APP_DIR + "/ui/restricted/",
			DEFAULT_REPORT_FILES_DIR = WEB_APP_DIR + "/WEB-INF/report_files/",
			REALTIME_REPORT_DIR = APP_ROOT + "/ui/restricted/",
			USER_SETTINGS_DIR = TEST_OUTPUT + "usersettings/",
			USERS_LIST_FILE_PATH = TEST_RES_DIR.resolve("users.xml").toString(),
			GLOB_CONST_FILENAME = TEST_RES_DIR.resolve("global_constants.cfg").toString(),
			ENV_VARS_FILENAME = TEST_RES_DIR.resolve("env_variables.cfg").toString(),
			HTML_TEMPLATE_DIR = PROJ_DIR.resolve("cfg").resolve("templates").toString(),
			ACTIONS_MAPPING_FILENAME = "cfg/actionsmapping.cfg";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private static boolean clearAppRoot = true, clearLogs = true;
	private String configFilePath,
			envVarsFilePath = ENV_VARS_FILENAME,
			globalConstFilePath = GLOB_CONST_FILENAME,
			actionsMappingFile = ACTIONS_MAPPING_FILENAME;
	private DeploymentConfig deploymentConfig;
	private DataHandlersFactory dataHandlersFactory;
	private ReportTemplatesProcessor templatesProcessor;
	
	public ApplicationManager() throws ClearThException
	{
		initClearThInstance();
	}
	
	private ApplicationManager(Builder builder) throws ClearThException
	{
		dataHandlersFactory = builder.dataHandlersFactory;
		configFilePath = builder.configFilePath;
		envVarsFilePath = builder.envVarsFilePath;
		globalConstFilePath = builder.globalConstFilePath;
		templatesProcessor = builder.templatesProcessor;
		actionsMappingFile = builder.actionsMappingFile;
		
		initClearThInstance();
	}

	public void dispose() throws IOException
	{
		ClearThCore coreInstance = ClearThCore.getInstance();
		if (coreInstance != null)
		{
			coreInstance.beforeShutdown();
			if (clearAppRoot)
			{
				File appRoot = new File(coreInstance.getAppRoot());
				if (appRoot.exists())
					FileUtils.cleanDirectory(appRoot);
			}
		}

		if (clearLogs)
		{
			File logs = new File(LOGS_DIR);
			if (logs.exists())
				FileUtils.cleanDirectory(logs);
		}
	}

	protected void resetClearThInstance()
	{
		if (ClearThCore.getInstance() == null)
			return;
		
		try
		{
			Field field = ClearThCore.class.getDeclaredField("instance");
			field.setAccessible(true);
			field.set(ClearThCore.class, null);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public Scheduler getScheduler(String schedulerName, String userName) throws ClearThException
	{
		SchedulersManager schedulersManager = ClearThCore.getInstance().getSchedulersManager();
		Scheduler scheduler = schedulersManager.getSchedulerByName(schedulerName, userName);
		if (scheduler == null)
		{
			try
			{
				scheduler = schedulersManager.addScheduler(userName, schedulerName);
			}
			catch (Exception e)
			{
				String msg = String.format("An error occurred while creating scheduler '%s' for user '%s'",
						schedulerName, userName);
				throw new ClearThException(msg, e);
			}
		}

		return scheduler;
	}

	public List<String> loadSteps(Scheduler scheduler, File stepCfg) throws ClearThException
	{
		List<String> warnings = new ArrayList<>();
		try
		{
			scheduler.uploadSteps(stepCfg, stepCfg.getName(), warnings, false);
		}
		catch (Exception e)
		{
			String msg = String.format("An error occurred while uploading scheduler configuration from file '%s'",
					stepCfg.getName());
			throw new ClearThException(msg, e);
		}

		return warnings;
	}

	public void loadMatrices(Scheduler scheduler, File matricesDir) throws ClearThException
	{
		List<File> matrixFiles;
		try
		{
			matrixFiles = getMatricesFiles(matricesDir.toPath());
		}
		catch (IOException e)
		{
			throw new ClearThException("Error on matrix files getting", e);
		}

		matrixFiles.forEach(scheduler::addMatrix);
	}

	public File getSchedulerConfig(Path pathCfg) throws ClearThException
	{
		File schedulerConfig = pathCfg.toFile();
		if (!schedulerConfig.exists())
			throw new ClearThException("File 'config.cfg' with scheduler parameters is not found");

		return schedulerConfig;
	}

	public List<File> getMatricesFiles(Path matrixDir) throws IOException
	{
		try (Stream<Path> list = Files.list(matrixDir))
		{
			return list
					.map(Path::toString)
					.filter(s -> "csv".equalsIgnoreCase(FilenameUtils.getExtension(s)))
					.sorted()
					.map(File::new)
					.collect(Collectors.toList());
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	public static void waitForSchedulerToStop(Scheduler scheduler, long delay, long timeout)
	{
		waitForSchedulerState(scheduler, delay, timeout, s -> !s.isRunning(), "stop");
	}
	
	public static void waitForSchedulerToSuspend(Scheduler scheduler, long delay, long timeout)
	{
		waitForSchedulerState(scheduler, delay, timeout, s -> s.isSuspended(), "suspension");
	}
	
	
	private static void waitForSchedulerState(Scheduler scheduler, long delay, long timeout, Function<Scheduler, Boolean> stateChecker, String stateName)
	{
		try
		{
			Stopwatch s = Stopwatch.createAndStart(timeout);
			while (!stateChecker.apply(scheduler))
			{
				if (s.isExpired())
					fail("Too long waiting for Scheduler "+stateName);

				TimeUnit.MILLISECONDS.sleep(delay);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			fail("Waiting for Scheduler "+stateName+" interrupted");
		}
	}

	protected ConfigFiles getConfigFiles()
	{
		ConfigFiles cfg = new ConfigFiles("clearth.cfg");
		ConfigFiles spy = spy(cfg);

		when(spy.getReportFilesDir()).thenReturn(DEFAULT_REPORT_FILES_DIR);
		when(spy.getReportsDir()).thenReturn(TEST_REPORT_DIR + cfg.getReportsDir());
		when(spy.getLastExecutionDir()).thenReturn(TEST_REPORT_DIR + cfg.getLastExecutionDir());
		when(spy.getAutomationStorageDir()).thenReturn(TEST_DATA_DIR + cfg.getAutomationStorageDir());
		when(spy.getScriptsDir()).thenReturn(TEST_DATA_DIR + cfg.getScriptsDir());
		when(spy.getSchedulersDir()).thenReturn(TEST_DATA_DIR + cfg.getSchedulersDir());
		when(spy.getSchedulersFileName()).thenReturn(TEST_DATA_DIR + cfg.getSchedulersFileName());
		when(spy.getRealTimeReportDir()).thenReturn(REALTIME_REPORT_DIR);
		when(spy.getConnectionsDir()).thenReturn(TEST_DATA_DIR + cfg.getConnectionsDir());
		when(spy.getLogsDir()).thenReturn(TEST_DATA_DIR + cfg.getLogsDir());
		when(spy.getLogCfgFileName()).thenReturn(LOG_PROPERTIES_FILE_PATH);
		when(spy.getUserSettingsDir()).thenReturn(USER_SETTINGS_DIR);
		when(spy.getUsersFileName()).thenReturn(USERS_LIST_FILE_PATH);
		when(spy.getGlobalConstantsFilename()).thenReturn(globalConstFilePath);
		when(spy.getEnvVarsFilename()).thenReturn(envVarsFilePath);
		when(spy.getHtmlTemplatesDir()).thenReturn(HTML_TEMPLATE_DIR);
		when(spy.getActionsMappingFileName()).thenReturn(actionsMappingFile);

		return spy;
	}

	protected DeploymentConfig getDeploymentConfig()
	{
		DeploymentConfig dc = mock(DeploymentConfig.class, CALLS_REAL_METHODS);
		ClearThVersion testRelease = new ClearThVersion("TestRelease", DATE_FORMAT.format(new Date()));
		when(dc.getCTHVersion()).thenReturn(testRelease);
		doAnswer((Answer<Void>) invocation ->
		{
			Whitebox.setInternalState(dc, "appRoot", APP_ROOT);
			return null;
		}).when(dc).configureAppRoot();

		return dc;
	}

	protected ClearThCore getCoreInstance() throws ClearThException
	{
		Logger coreLogger = LoggerFactory.getLogger(ClearThCore.class);
		ClearThCore core = spy(ClearThCore.class);
		when(core.getAdditionalTemplateParams()).thenReturn(null);
		when(core.getLogger()).thenReturn(coreLogger);
		when(core.getValueGenerators()).thenReturn(new IncrementingValueGenerators());
		when(core.createStepFactory()).thenReturn(createStepFactory());
		doReturn(createExecutorFactory(core.getValueGenerators().getCommonGenerator()))
				.when(core).createExecutorFactory(any(ValueGenerators.class));
		if (dataHandlersFactory != null)
			doReturn(dataHandlersFactory).when(core).createDataHandlersFactory();
		
		//initOtherEntities() implementation
		try
		{
			doAnswer((Answer<Void>) invocation ->
			{
				File src = new File(WEB_UI_RESTRICTED);
				File dst = new File(core.getRootRelative(core.getRealTimeReportPath()));
				FileUtils.copyDirectory(src, dst);
				return null;
			}).when(core).initOtherEntities(Matchers.<Object>anyVararg());
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		//createDirs() implementation
		try
		{
			doAnswer((Answer<Void>) invocation ->
			{
				Files.createDirectories(Paths.get(core.getRealTimeReportPath()));
				return null;
			}).when(core).createDirs();
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		try
		{
			if(configFilePath != null)
				when(core.loadConfig(deploymentConfig.getConfigFileName())).thenAnswer(invocation -> loadConfig(configFilePath));
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}

		try
		{
			if (templatesProcessor != null)
				doReturn(templatesProcessor).when(core).createReportTemplatesProcessor();
		}
		catch (Exception e)
		{
			throw new ClearThException(e);
		}
		
		return core;
	}

	protected ClearThConfiguration loadConfig(String configFile) throws ConfigurationException
	{
		return ClearThConfiguration.create(new File(configFile));
	}

	private ExecutorFactory createExecutorFactory(ValueGenerator valueGenerator)
	{
		return new TestingExecutorFactory(valueGenerator);
	}

	protected StepFactory createStepFactory()
	{
		return new DefaultStepFactory(){
			@Override
			protected boolean validStepKindEx(String stepKind)
			{
				return "Passed".equals(stepKind);
			}
		};
	}

	protected void initClearThInstance() throws ClearThException
	{
		if (ClearThCore.getInstance() != null)
			resetClearThInstance();
		
		deploymentConfig = getDeploymentConfig();
		ConfigFiles configFiles = getConfigFiles();
		deploymentConfig.init(configFiles);
		ClearThCore application = getCoreInstance();
		application.init(configFiles, deploymentConfig);
	}
	
	
	public static class Builder
	{
		private DataHandlersFactory dataHandlersFactory = null;
		private String configFilePath = null;
		private String envVarsFilePath = ENV_VARS_FILENAME;
		private String globalConstFilePath = GLOB_CONST_FILENAME;
		private String actionsMappingFile = ACTIONS_MAPPING_FILENAME;
		private ReportTemplatesProcessor templatesProcessor;
		
		public ApplicationManager build() throws ClearThException
		{
			return new ApplicationManager(this);
		}
		
		public Builder dataHandlersFactory(DataHandlersFactory dataHandlersFactory)
		{
			this.dataHandlersFactory = dataHandlersFactory;
			return this;
		}
		
		public Builder configFilePath(String configFilePath)
		{
			this.configFilePath = configFilePath;
			return this;
		}
		
		public Builder envVarsFilePath(String envVarsFilePath)
		{
			this.envVarsFilePath = envVarsFilePath;
			return this;
		}
		
		public Builder globalConstFilePath(String globalConstFilePath)
		{
			this.globalConstFilePath = globalConstFilePath;
			return this;
		}
		
		public Builder templatesProcessor(ReportTemplatesProcessor templatesProcessor)
		{
			this.templatesProcessor = templatesProcessor;
			return this;
		}
		
		public Builder actionsMappingFile(String actionsMappingFile)
		{
			this.actionsMappingFile = actionsMappingFile;
			return this;
		}
	}
}

class TestingExecutorFactory extends DefaultExecutorFactory
{
	public TestingExecutorFactory(ValueGenerator valueGenerator)
	{
		super(valueGenerator);
	}

	@Override
	public SimpleExecutor createExecutor(Scheduler scheduler, List<Matrix> matrices, String startedByUser,
			Map<String, Preparable> preparableActions, TestExecutionHandler executionHandler)
	{
		GlobalContext globalContext =
				createGlobalContext(scheduler.getBusinessDay(), scheduler.getBaseTime(), scheduler.isWeekendHoliday(),
						scheduler.getHolidays(), startedByUser, executionHandler);
		if (scheduler.isTestMode())
			globalContext.setLoadedContext(GlobalContext.TEST_MODE, true);
		return new TestingExecutor(scheduler, scheduler.getSteps(), matrices,
				globalContext, createFailoverStatus(), preparableActions, scheduler.getCurrentReportsConfig());
	}

	@Override
	public SimpleExecutor createExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices,
			GlobalContext globalContext, Map<String, Preparable> preparableActions, ReportsConfig reportsConfig)
	{
		return new TestingExecutor(scheduler, steps, matrices, globalContext, createFailoverStatus(), preparableActions, reportsConfig);
	}
}

class TestingExecutor extends DefaultSimpleExecutor
{
	public TestingExecutor(Scheduler scheduler, List<Step> steps,
			List<Matrix> matrices, GlobalContext globalContext,
			FailoverStatus failoverStatus,
			Map<String, Preparable> preparableActions, ReportsConfig reportsConfig)
	{
		super(scheduler, steps, matrices, globalContext, failoverStatus, preparableActions, reportsConfig);
	}

	@Override
	protected StepImpl createStepImpl(String stepKind, String stepParameter)
	{
		StepImpl testStepImpl = spy(StepImpl.class);
		when(testStepImpl.execute(anyMapOf(Matrix.class, StepContext.class),any(GlobalContext.class)))
				.thenReturn(DefaultResult.passed("Passed"));

		return testStepImpl;
	}
}
