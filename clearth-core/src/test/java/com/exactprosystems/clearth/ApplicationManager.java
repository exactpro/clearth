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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.config.ClearThConfiguration;
import com.exactprosystems.clearth.config.ConfigurationException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ApplicationManager
{
	public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

	public static final String ADMIN = "admin";
	public static final String TEST_OUTPUT = "testOutput/",
			LOGS_DIR = USER_DIR.getParent().resolve(TEST_OUTPUT).resolve("logs").toString(),
			TEST_DATA_DIR = TEST_OUTPUT + "appRoot/",
			TEST_REPORT_DIR = TEST_OUTPUT + "SchedulerTestData/",
			LOG_PROPERTIES_FILE_PATH = USER_DIR + "/src/test/resources/log.properties",
			APP_ROOT = USER_DIR.getParent().resolve(TEST_DATA_DIR).toString(),
			WEB_APP_DIR = USER_DIR.getParent().resolve("clearth-modules/clearth-gui/src/main/webapp").toString(),
			WEB_UI_RESTRICTED = WEB_APP_DIR + "/ui/restricted/",
			DEFAULT_REPORT_FILES_DIR = WEB_APP_DIR + "/WEB-INF/report_files/",
			REALTIME_REPORT_DIR = APP_ROOT + "/ui/restricted/",
			USER_SETTINGS_DIR = TEST_OUTPUT + "usersettings/",
			USERS_LIST_FILE_PATH = USER_DIR + "/src/test/resources/users.xml";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private static boolean clearAppRoot = true, clearLogs = true;
	private String configFilePath;
	private DeploymentConfig deploymentConfig;

	public ApplicationManager() throws ClearThException
	{
		initClearThInstance();
	}

	public ApplicationManager(String configFilePath) throws ClearThException
	{
		this.configFilePath = configFilePath;
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
					.map(File::new)
					.collect(Collectors.toList());
		}
	}

	public static void waitForSchedulerToStop(Scheduler scheduler, long delay, long timeout)
	{
		try
		{
			Stopwatch s = Stopwatch.createAndStart(timeout);
			while (scheduler.isRunning())
			{
				if (s.isExpired())
					fail("Too long to wait for Scheduler to finish.");

				TimeUnit.MILLISECONDS.sleep(delay);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			fail("Waiting for Scheduler to stop is interrupted.");
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
		if (ClearThCore.getInstance() == null)
		{
			deploymentConfig = getDeploymentConfig();
			ConfigFiles configFiles = getConfigFiles();
			deploymentConfig.init(configFiles);
			ClearThCore application = getCoreInstance();
			application.init(configFiles, deploymentConfig);
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
	public Executor createExecutor(Scheduler scheduler, List<Matrix> matrices, String startedByUser,
			Map<String, Preparable> preparableActions, TestExecutionHandler executionHandler)
	{
		GlobalContext globalContext =
				createGlobalContext(scheduler.getBusinessDay(), scheduler.getBaseTime(), scheduler.isWeekendHoliday(),
						scheduler.getHolidays(), startedByUser, executionHandler);
		if (scheduler.isTestMode())
			globalContext.setLoadedContext(GlobalContext.TEST_MODE, true);
		return new TestingExecutor(scheduler, scheduler.getSteps(), matrices,
				globalContext, createFailoverStatus(), preparableActions);
	}

	@Override
	public Executor createExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices,
			GlobalContext globalContext, Map<String, Preparable> preparableActions)
	{
		return new TestingExecutor(scheduler, steps, matrices, globalContext, createFailoverStatus(), preparableActions);
	}
}

class TestingExecutor extends DefaultExecutor
{
	public TestingExecutor(Scheduler scheduler, List<Step> steps,
			List<Matrix> matrices, GlobalContext globalContext,
			FailoverStatus failoverStatus,
			Map<String, Preparable> preparableActions)
	{
		super(scheduler, steps, matrices, globalContext, failoverStatus, preparableActions);
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
