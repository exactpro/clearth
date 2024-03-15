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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ActionUpdateException;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.FatalAutomationException;
import com.exactprosystems.clearth.automation.exceptions.NothingToStartException;
import com.exactprosystems.clearth.automation.exceptions.SchedulerUpdateException;
import com.exactprosystems.clearth.automation.matrix.linked.LocalMatrixProvider;
import com.exactprosystems.clearth.automation.matrix.linked.MatrixProvider;
import com.exactprosystems.clearth.automation.matrix.linked.MatrixProviderHolder;
import com.exactprosystems.clearth.automation.persistence.ExecutorState;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.automation.steps.Default;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.exactprosystems.clearth.automation.MatrixFileExtensions.isExtensionSupported;
import static com.exactprosystems.clearth.automation.matrix.linked.MatrixProvider.STORED_MATRIX_PREFIX;
import static java.lang.String.format;

public abstract class Scheduler
{
	protected static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	protected final String scriptsDir, lastExecutionDataDir;
	protected final ExecutorFactory executorFactory;
	protected final StepFactory stepFactory;
	protected final MatrixDataFactory matrixDataFactory;
	protected final ActionGeneratorResources generatorResources;
	protected final SchedulerData schedulerData;
	protected List<Step> steps = new ArrayList<Step>();
	protected List<Matrix> matrices = new ArrayList<Matrix>();
	protected SchedulerStatus status = new SchedulerStatus();
	protected boolean sequentialRun = false;
	protected IExecutor executor = null;
	protected Map<String, List<ActionGeneratorMessage>> matricesErrors = null;
	protected Date businessDay = new Date(),
			baseTime = null;
	protected boolean weekendHoliday = false;
	protected Map<String, Boolean> holidays = new HashMap<String, Boolean>();
	protected Set<String> connectionsToIgnoreFailuresByRun = new HashSet<>();
	protected ExecutorStateInfo stateInfo;
	protected boolean testMode;
	private Date executorStartedTime;
	private final AtomicBoolean stoppedByUser = new AtomicBoolean(false);

	protected MatrixProviderHolder matrixProviderHolder;

//	protected Map<String, ActionMetaData> actionsMapping;

	public Scheduler(String name, String configsRoot, String schedulerDirName, 
			ExecutorFactory executorFactory, StepFactory stepFactory, ActionGeneratorResources generatorResources) throws Exception
	{
		scriptsDir = ClearThCore.scriptsPath() + schedulerDirName + File.separator + name + File.separator;
		lastExecutionDataDir = ClearThCore.lastExecutionPath() + schedulerDirName + File.separator + name + File.separator;
		
		this.executorFactory = executorFactory;
		this.stepFactory = stepFactory;
		this.matrixDataFactory = ClearThCore.getInstance().getMatrixDataFactory();
		this.generatorResources = generatorResources;
		schedulerData = createSchedulerData(name, configsRoot, schedulerDirName, lastExecutionDataDir, scriptsDir);
		
		//If some matrices files were added before scheduler construction - let's add them to schedulerData
		File mdFile = new File(scriptsDir);
		File[] files = mdFile.listFiles(file -> isExtensionSupported(FilenameUtils.getExtension(file.getName())));
		if (files != null)
		{
			boolean added = false;
			for (File f : files)
				if (schedulerData.matrixFileIndex(f)<0)
				{
					schedulerData.getMatrices().add(matrixDataFactory.createMatrixData(f, null, true,true));
					added = true;
				}
			if (added)
				saveMatrices();
		}
		
		try
		{
			stateInfo = loadStateInfo(schedulerData.getStateDir());
		}
		catch (IOException e)
		{
			logger.error("Error while loading state info", e);
			stateInfo = null;
		}
	}
	
	public abstract SchedulerData createSchedulerData(String name, String configsRoot, String schedulerDirName, String lastExecutedDataDir, String matricesDir) throws Exception;
	public abstract void initEx() throws Exception;
	public abstract ActionGenerator createActionGenerator(Map<String, Step> stepsMap, List<Matrix> matricesContainer, Map<String, Preparable> preparableActions);
	public abstract SequentialExecutor createSequentialExecutor(Scheduler scheduler, String userName, Map<String, Preparable> preparableActions);
	
	protected abstract ExecutorStateInfo loadStateInfo(File sourceDir) throws IOException;
	protected abstract void saveStateInfo(File destDir, ExecutorStateInfo stateInfo) throws IOException;
	protected abstract ExecutorState createExecutorState(File sourceDir) throws IOException;
	protected abstract ExecutorState createExecutorState(SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo);
	
	protected abstract void initExecutor(SimpleExecutor executor);
	protected abstract void initSequentialExecutor(SequentialExecutor executor);
	protected abstract void initSchedulerOnRestore(SimpleExecutor executor);
	
	
	/* Properties control routines */
	
	private void saveConfigData() throws IOException
	{
		try
		{
			schedulerData.saveConfigData();
		}
		catch (IOException e)
		{
			String msg = "Error while saving scheduler config data";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
	}
	
	public void saveStepsAndInit(String errorMsg) throws IOException
	{
		try
		{
			schedulerData.saveSteps();
			schedulerData.setConfigChanged(true);
			saveConfigData();
		}
		catch (IOException e)
		{
			logger.error(errorMsg, e);
			throw new IOException(errorMsg, e);
		}

		init();
	}

	@Deprecated
	synchronized public void uploadStepsOld(File uploadedConfig, String originalFileName, List<String> warnings) throws Exception
	{
		FileOperationUtils.copyFile(uploadedConfig.getCanonicalPath(), schedulerData.getConfigName());
		schedulerData.reloadSteps(warnings);
		schedulerData.setConfigFileName(originalFileName);
		schedulerData.setConfigChanged(false);
		saveConfigData();
		init();
	}
	

	synchronized public void uploadSteps(File uploadedConfig, String originalFileName,
			List<String> warnings, boolean append) throws Exception
	{
		if (!append)
		{
			FileOperationUtils.copyFile(uploadedConfig.getCanonicalPath(), schedulerData.getConfigName());
			schedulerData.reloadSteps(warnings);
			schedulerData.setConfigFileName(originalFileName);
			schedulerData.setConfigChanged(false);
			saveConfigData();
			init();
			logger.info("Steps in scheduler replaced with new from " + uploadedConfig.toString());
			return;
		}

		Set<String> stepNames = new HashSet<String>();
		List<Step> stepContainer = new ArrayList<Step>();
		List<Step> newStepContainer = new ArrayList<Step>();
		List<String> warnings0 = new ArrayList<String>();
		
		SchedulerData.loadSteps(schedulerData.getConfigName(), stepContainer, stepFactory, warnings0);
		
		for (Step step : stepContainer) 
		{
			stepNames.add(step.getName());
		}
		
		SchedulerData.loadSteps(uploadedConfig.getAbsolutePath(), newStepContainer, stepFactory, warnings);
		
		for (Step step : newStepContainer) {
			String currentName = step.getName();
			if (!stepNames.contains(currentName)) 
			{
				stepContainer.add(step);
			}
			else
			{
				warnings.add("Skipped step with name " + currentName + " as it is already present in existing configuration.");
			}
		}
		
		SchedulerData.saveSteps(new File(schedulerData.getConfigName()), schedulerData.getConfigHeader(), stepContainer);
		schedulerData.reloadSteps(warnings);
		schedulerData.setConfigChanged(true);
		saveConfigData();
		init();
		logger.info("New scheduler steps added from " + uploadedConfig.toString());
	}
	
	synchronized public void setExecute(boolean execute) throws IOException
	{
		for (Step step : schedulerData.getSteps())
			step.setExecute(execute);
		
		saveStepsAndInit("Error while saving steps after setting 'Execute' flag");
	}

	synchronized public void setAskForContinue(boolean askForContinue) throws IOException
	{
		for (Step step : schedulerData.getSteps())
			step.setAskForContinue(askForContinue);

		saveStepsAndInit("Error while saving steps after setting 'Ask for continue' flag");
	}

	synchronized public void setAskIfFailed(boolean askIfFailed) throws IOException
	{
		for (Step step : schedulerData.getSteps())
			step.setAskIfFailed(askIfFailed);

		saveStepsAndInit("Error while saving steps after setting 'Ask if failed' flag");
	}

	synchronized public void setMatricesExecute(boolean execute)
	{
		for (MatrixData md : schedulerData.getMatrices())
			md.setExecute(execute);
		
		saveMatrices();
	}
	
	synchronized public void setMatricesTrim(boolean trim)
	{
		for (MatrixData md : schedulerData.getMatrices())
			md.setTrim(trim);
		
		saveMatrices();
	}
	
	synchronized public void toggleHoliday(Date hol) throws IOException
	{
		Map<String, Boolean> hols = schedulerData.getHolidays();
		String holString = MatrixFunctions.getHolidayDF().format(hol);
		if (hols.containsKey(holString))
		{
			if (hols.get(holString))
				hols.put(holString, false);
			else
				hols.remove(holString);
		}
		else
			hols.put(holString, true);
		
		try
		{
			schedulerData.saveHolidays();
		}
		catch (IOException e)
		{
			String msg = "Error while saving scheduler holidays after adding/removing a holiday";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
		
		init();
	}
	
	synchronized public void setBusinessDay(Date businessDay) throws IOException
	{
		schedulerData.setBusinessDay(businessDay);
		init();
	}

	synchronized public void setBaseTime(Date baseTime) throws IOException
	{
		schedulerData.setBaseTime(baseTime);
		
		try
		{
			schedulerData.saveBaseTime();
		}
		catch (IOException e)
		{
			String msg = "Error while saving scheduler base time after setting it";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
		
		init();
	}
	
	synchronized public void useCurrentTime() throws IOException
	{
		new File(schedulerData.getBaseTimeName()).delete();
		try
		{
			schedulerData.setBaseTime(schedulerData.loadBaseTime());
		}
		catch (Exception e)
		{
			logger.warn("Error while loading base time when using current time");
		}
		
		init();
	}
	
	synchronized public void setWeekendHoliday(boolean weekendHoliday) throws IOException
	{
		schedulerData.setWeekendHoliday(weekendHoliday);
		
		try
		{
			schedulerData.saveWeekendHoliday();
		}
		catch (IOException e)
		{
			String msg = "Error while saving 'weekend is holiday' setting after setting it";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
		
		init();
	}
	
	
	/* Steps management routines */
	
	protected void validateStep(Step step, Step skipStep) throws SettingsException
	{
		if (step.getName().isEmpty())
			throw new SettingsException("Step should have a unique name");
		
		for (Step sdStep : schedulerData.getSteps())
			if ((sdStep != skipStep) && (sdStep.getName().equals(step.getName())))
				throw new SettingsException("Step with name '"+step.getName()+"' already exists in this scheduler");
		
		if (!stepFactory.validStepKind(step.getKind()))
			step.setKind(CoreStepKind.Default.getLabel());
	}
	
	synchronized public void addStep(Step newStep) throws IOException, SettingsException
	{
		validateStep(newStep, null);
		
		schedulerData.getSteps().add(newStep);
		saveStepsAndInit("Error while saving steps after adding new one");
	}
	
	protected void doModifyStep(Step originalStep, Step newStep) throws IOException, SettingsException
	{
		List<Step> steps = schedulerData.getSteps();
		
		int stepIndex = steps.indexOf(originalStep);
		if (stepIndex < 0)
		{
			logger.error("Cannot find step to modify: " + originalStep);
			throw new IOException("Cannot find step to modify");
		}
		
		validateStep(newStep, originalStep);
		
		steps.set(stepIndex, newStep);
	}
	
	synchronized public void modifyStep(Step originalStep, Step newStep) throws IOException, SettingsException
	{
		doModifyStep(originalStep, newStep);
		saveStepsAndInit("Error while saving steps after modifying one of them");
	}
	
	synchronized public void modifySteps(List<Step> originalSteps, List<Step> newSteps) throws IOException, SettingsException
	{
		for (int i = 0; i < newSteps.size(); i++)
			doModifyStep(originalSteps.get(i), newSteps.get(i));
		saveStepsAndInit("Error while saving steps after modifying");
	}
	
	synchronized public void removeStep(Step stepToRemove) throws IOException
	{
		schedulerData.getSteps().remove(stepToRemove);
		saveStepsAndInit("Error while saving steps after removing one of them");
	}
	
	synchronized public void removeSteps(List<Step> stepsToRemove) throws IOException
	{
		schedulerData.getSteps().removeAll(stepsToRemove);
		saveStepsAndInit("Error while saving steps after removing multiple steps");
	}
	
	synchronized public void clearSteps() throws IOException
	{
		schedulerData.getSteps().clear();
		saveStepsAndInit("Error while saving steps after removing all of them");
	}
	
	synchronized public void downStep(Step step) throws IOException
	{
		List<Step> steps = schedulerData.getSteps();
		int index = steps.indexOf(step);
		if (index < 0)
			return;
		
		steps.remove(index);
		if (index == steps.size())
			steps.add(0, step);
		else
			steps.add(index + 1, step);
		saveStepsAndInit("Error while saving steps after moving one of them down");
	}
	
	synchronized public void updateRunningSteps(List<StepData> updatedStepData) throws AutomationException, SchedulerUpdateException
	{
		if (!isSuspended())
			throw new AutomationException("Scheduler is not in suspended state. Steps cannot be updated");
		
		SchedulerUpdater updater = new SchedulerUpdater(this);
		updater.updateSteps(updatedStepData);
	}
	
	
	/* Matrices compilation and check */
	
	public Map<String, List<ActionGeneratorMessage>> prepare(List<Step> stepsContainer,
															 List<Matrix> matricesContainer,
															 List<MatrixData> matricesData,
															 Map<String, Preparable> preparableActions) throws Exception
	{
		return buildMatrices(stepsContainer, matricesContainer, preparableActions, matricesData, false);
	}

	public Map<String, List<ActionGeneratorMessage>> checkMatrices(List<Step> stepsContainer,
															 List<Matrix> matricesContainer,
															 List<MatrixData> matricesData,
															 Map<String, Preparable> preparableActions) throws Exception
	{
		return buildMatrices(stepsContainer, matricesContainer, preparableActions, matricesData, true);
	}

	public boolean isStoppedByUser()
	{
		return stoppedByUser.get();
	}

	protected Map<String, List<ActionGeneratorMessage>> buildMatrices(List<Step> stepsContainer,
																	  List<Matrix> matricesContainer,
																	  Map<String, Preparable> preparableActions,
																	  List<MatrixData> matricesData,
																	  boolean onlyCheck) throws IOException {
		Map<String, Step> stepsMap = toMap(stepsContainer);
		ActionGenerator generator = createActionGenerator(stepsMap, matricesContainer, preparableActions);
		boolean allSuccessful = true;
		for (MatrixData matrixData : matricesData)
		{
			if ((!matrixData.isExecute()) || (!matrixData.getFile().isFile()))
				continue;
			if (!generator.build(matrixData, onlyCheck))
				allSuccessful = false;
		}

		if (!onlyCheck) {
			generator.createContextCleanData();
		}

		generator.dispose();

		if (!allSuccessful) return collectIssues(matricesContainer);
		else return null;
	}

	
	synchronized public Map<String, List<ActionGeneratorMessage>> checkMatrices(List<MatrixData> matrices) throws Exception
	{
		List<Step> steps;
		try
		{
			updateLinkedMatrices();
			checkDuplicatedMatrixNames();
			
			steps = schedulerData.loadSteps(null); //Ignore step warnings
		}
		catch (IOException e)
		{
			steps = new ArrayList<Step>();
		}

		return buildMatrices(steps, new ArrayList<Matrix>(), null, matrices, true);
	}

	protected Map<String, Step> toMap(Collection<Step> stepsCollection) {
		Map<String, Step> stepsMap = new LinkedHashMap<String, Step>();
		for (Step step : stepsCollection)
			stepsMap.put(step.getName(), step);
		return stepsMap;
	}

	protected Map<String, List<ActionGeneratorMessage>> collectIssues(List<Matrix> matricesContainer) {
		Map<String, List<ActionGeneratorMessage>> result = new HashMap<String, List<ActionGeneratorMessage>>();
		for (Matrix m : matricesContainer)
			if ((m.getGeneratorMessages()!=null) && (m.getGeneratorMessages().size()>0))
				result.put(m.getName(), m.getGeneratorMessages());
		return result;
	}
	
	
	/* Matrices routines */
	
	private void checkMatricesExistance()
	{
		List<MatrixData> matrices = schedulerData.getMatrices();
		for (int i=matrices.size()-1; i>=0; i--)
			if (!matrices.get(i).getFile().exists())
				matrices.remove(i);
	}
	
	protected boolean saveMatrices()
	{
		try
		{
			schedulerData.saveMatrices();
		}
		catch (IOException e)
		{
			logger.error("Error while saving matrices data", e);
			return false;
		}
		return true;
	}
	
	public List<MatrixData> getMatricesData()
	{
		return schedulerData.getMatrices();
	}

	public List<MatrixData> getExecutedMatricesData()
	{
		return schedulerData.getExecutedMatrices();
	}
	
	protected void doRemoveMatrix(MatrixData matrix)
	{
		checkMatricesExistance();
		
		schedulerData.getMatrices().remove(matrix);
		matrix.getFile().delete();
	}
	
	synchronized public void removeMatrix(MatrixData matrix)
	{
		doRemoveMatrix(matrix);
		saveMatrices();
	}
	
	synchronized public void removeMatrices(List<MatrixData> matrices)
	{
		for (MatrixData m : matrices)
			doRemoveMatrix(m);
		saveMatrices();
	}
	
	synchronized public void removeAllMatrices()
	{
		for (MatrixData md : schedulerData.getMatrices())
			md.getFile().delete();
		schedulerData.getMatrices().clear();
		saveMatrices();
	}
	
	synchronized private void updateLinkedMatrices() throws AutomationException
	{
		try
		{
			for (int i = 0; i < schedulerData.getMatrices().size(); i++)
			{
				MatrixData matrix = schedulerData.getMatrices().get(i);
				if (matrix.isLinked() && matrix.isAutoReload() && matrix.isExecute())
					addLinkedMatrix(matrix);
			}
		}
		catch (Exception e)
		{
			logger.error("Error while updating linked matrices", e);
			throw new AutomationException("Error while updating linked matrices: " + e.getMessage());
		}
	}

	public MatrixProviderHolder getMatrixProviderHolder() {
		//default value
		return matrixProviderHolder == null ? MatrixProviderHolder.getInstance() : matrixProviderHolder;
	}

	public void setMatrixProviderHolder(MatrixProviderHolder matrixProviderHolder) {
		this.matrixProviderHolder = matrixProviderHolder;
	}

	synchronized public void addLinkedMatrix(MatrixData matrix) throws Exception
	{
		File matrixFile = matrix.getFile();
		boolean isNewMatrix = matrixFile == null; //Add matrix or Update matrix
		String link = matrix.getLink();
		String type = matrix.getType();
		String name = matrix.getName();

		String matrixFileExtension;
		if(StringUtils.equals(type,LocalMatrixProvider.TYPE))
			matrixFileExtension = "." +  FilenameUtils.getExtension(link);
		else
			matrixFileExtension = "." + FilenameUtils.getExtension(name);

		File tempFile;

		InputStream input = null;
		try
		{
			Map<String, Object> params = new HashMap<String, Object>();
			MatrixProvider provider = getMatrixProviderHolder().getMatrixProvider(link, name, type, params);

			name = provider.getName();
			matrix.setName(name);

			for (MatrixData m : schedulerData.getMatrices())
			{
				if (StringUtils.equals(m.getName(), name) && (isNewMatrix || !matrixFile.equals(m.getFile())))
					throw new ClearThException("Matrix with the same name already exists");
			}

			input = provider.getMatrix();
			FileOutputStream output = null;
			try
			{
				File tempDir = new File(ClearThCore.automationStoragePath());
				if (isExtensionSupported(matrixFileExtension))
					tempFile = File.createTempFile("matrix_", matrixFileExtension, tempDir);
				else
				{
					throw new ClearThException(format("Matrix file '%s' has unexpected type. Only %s are"
							+ " supported.", matrix.getName(), MatrixFileExtensions.supportedExtensionsAsString));
				}
				output = new FileOutputStream(tempFile);
				output.write(IOUtils.toByteArray(input));
			}
			finally
			{
				IOUtils.closeQuietly(output);
			}
		}
		finally
		{
			IOUtils.closeQuietly(input);
		}
		
		Date currentDate = new Date();
		matrix.setUploadDate(currentDate);
		
		File file;
		if (matrix.getFile() == null)
		{
			String matrixName = STORED_MATRIX_PREFIX + currentDate.getTime() + matrixFileExtension;
			matrix.setFile(new File(scriptsDir + matrixName));
		}
		file = matrix.getFile();
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		if (file.exists())
			file.delete();
		FileOperationUtils.copyFile(tempFile.getCanonicalPath(), file.getCanonicalPath());
		
		checkMatricesExistance();
		int index = schedulerData.matrixFileIndex(file);
		if (index < 0)
			schedulerData.getMatrices().add(matrix);
		else
			schedulerData.getMatrices().set(index, matrix);
		saveMatrices();
	}
	
	public void checkDuplicatedMatrixNames() throws ClearThException
	{
		List<MatrixData> matrices = getMatricesData();
		for (int i = 0; i < matrices.size(); i++)
		{
			for (int j = i + 1; j < matrices.size(); j++)
			{
				if (StringUtils.equals(matrices.get(i).getName(), matrices.get(j).getName()))
				{
					throw new ClearThException(format("Some matrices contain '%s' duplicated names. Please use unique matrix names.",
							matrices.get(i).getName()));
				}
			}
		}
	}
	
	synchronized public boolean addMatrix(File newMatrix)
	{
		int index = schedulerData.matrixFileIndex(newMatrix);
		if (index < 0)
			schedulerData.getMatrices().add(matrixDataFactory.createMatrixData(newMatrix, new Date(), true, true));
		return true;
	}
	
	synchronized public void addMatrix(File newMatrix, String matrixName) throws ClearThException
	{
		try
		{
			for (MatrixData m: getMatricesData()) // check duplicated matrix names for linked matrices
			{
				if (m.isLinked() && StringUtils.equals(matrixName, m.getName()))
					throw new ClearThException(
							format("'%s' matrix already exists. Please use unique matrix names.", matrixName));
			}
			
			File matrix = new File(scriptsDir+matrixName);
			if (!matrix.getParentFile().exists())
				matrix.getParentFile().mkdirs();
			if (matrix.exists())
				matrix.delete();
			FileOperationUtils.copyFile(newMatrix.getCanonicalPath(), matrix.getCanonicalPath());
			
			checkMatricesExistance();
			int index = schedulerData.matrixFileIndex(matrix);
			if (index < 0)
				schedulerData.getMatrices().add(matrixDataFactory.createMatrixData(matrix, new Date(), true, true));
			else
			{
				MatrixData matrixData = schedulerData.getMatrices().get(index);
				matrixData.setUploadDate(new Date());
				matrixData.setExecute(true);
			}
			saveMatrices();
		}
		catch (IOException e)
		{
			throw new ClearThException("Error while adding new file with matrix", e);
		}
	}
	
	synchronized public void saveMatricesPositions()
	{
		checkMatricesExistance();
		saveMatrices();
	}
	
	synchronized public void toggleMatrixExecute(MatrixData md)
	{
		md.setExecute(!md.isExecute());
		saveMatrices();
	}
	
	synchronized public void toggleMatrixTrim(MatrixData md)
	{
		md.setTrim(!md.isTrim());
		saveMatrices();
	}
	
	synchronized public void updateRunningMatrices(List<MatrixData> updatedMatrixData) throws AutomationException, SchedulerUpdateException, ActionUpdateException
	{
		if (!isSuspended())
			throw new AutomationException("Scheduler is not in suspended state. Matrices cannot be updated");
		
		SchedulerUpdater updater = new SchedulerUpdater(this);
		updater.updateMatrices(updatedMatrixData);
	}
	
	
	/* Scheduler execution methods */
	
	synchronized public boolean isRunning()
	{
		return (executor != null && !executor.isTerminated());
	}
	
	synchronized public boolean isSuspended()
	{
		if (!isRunning())
			return false;
		return executor.isSuspended();
	}
	
	public boolean isReplayEnabled()
	{
		if (!isRunning())
			return false;
		return executor.isReplayEnabled();
	}
	
	public boolean isSequentialRun()
	{
		return sequentialRun;
	}
	
	public boolean isInterrupted()
	{
		if (!isRunning())
			return true;
		return executor.isExecutionInterrupted();
	}

	public boolean isSuccessful()
	{
		// If any Step has at least one failed action then the Scheduler is not successful.
		for (Step step : getSteps())
		{
			if (step.isAnyActionFailed() || step.isFailedDueToError())
				return false;
			// Supposed that current step is taken from step list.
			if (step == getCurrentStep())
				return !step.isAnyActionFailed() && !step.isFailedDueToError();
		}
		return true;
	}

	public boolean init()
	{
		if (isRunning())
			return false;
		
		try
		{
			schedulerData.loadSteps(steps, null); //Ignore step warnings
			schedulerData.loadHolidays(holidays);
			businessDay = schedulerData.loadBusinessDay();
			baseTime = schedulerData.loadBaseTime();
			weekendHoliday = schedulerData.loadWeekendHoliday();
			stoppedByUser.set(false);
			initEx();
		}
		catch (Exception e)
		{
			logger.error("Error while initializing scheduler", e);
			return false;
		}
		
		return true;
	}
	
	
	synchronized public void start(String userName) throws AutomationException
	{
		if (isRunning())
			throw new AutomationException("Scheduler is already running");
		
		if (!init())
			throw new AutomationException("Error while initiating scheduler");
		
		sequentialRun = false;
		try
		{
			status.clear();

			updateLinkedMatrices();

			Map<String, Preparable> preparableActions = new HashMap<String, Preparable>();
			matricesErrors = prepare(steps, matrices, getMatricesData(), preparableActions);

			checkMatrixFatalErrors();

			checkIfNothingToExecute();
			
			doBeforeStartExecution();
			
			TestExecutionHandler executionHandler = ClearThCore.getInstance().getDataHandlersFactory().createTestExecutionHandler(getName());
			SimpleExecutor simpleExecutor = executorFactory.createExecutor(this, matrices, userName, preparableActions, executionHandler);
//			executor.globalContext.setLoadedContext(GlobalContext.ACTIONS_MAPPING, actionsMapping);
			simpleExecutor.setOnFinish((x) -> this.executorFinished());
			initExecutor(simpleExecutor);
			executor = simpleExecutor;
			executor.start();
			
			waitAfterExecutionStarted();
		}
		catch (AutomationException e)
		{
			status.add(e.getMessage());
			throw e;
		}
		catch (DataHandlingException e)
		{
			status.add(e.getMessage());
			throw new AutomationException("Could not start scheduler", e);
		}
		catch (Exception e)
		{
			final String msg = "Unknown error while starting scheduler";
			logger.error(msg, e);
			throw new AutomationException(msg, e);
		}
	}
	
	private void waitAfterExecutionStarted() throws AutomationException
	{
		try
		{
			Thread.sleep(100);
		}
		catch (InterruptedException e)
		{
			final String msg = "Wait after scheduler start interrupted";
			logger.error(msg);
			throw new AutomationException(msg, e);
		}
	}

	private void checkMatrixFatalErrors() throws FatalAutomationException
	{
		for(Matrix matrix: getMatrices())
		{
			if(!matrix.isHasFatalErrors())
				continue;

			throw new FatalAutomationException("Scheduler will not start, because matrix '" + matrix.getName() +
					"' has fatal errors");
		}
	}

	private void checkIfNothingToExecute() throws NothingToStartException
	{
		for (Step step : steps)
		{
			if (!step.isExecute())
				continue;
			
			for (Action action : step.getActions())
			{
				if (action.isExecutable())
					return;
			}
		}
		
		throw new NothingToStartException("Scheduler will not start, because no actions would be executed");
	}
	
	synchronized public void startSequential(String userName) throws AutomationException
	{
		if (isRunning())
			throw new AutomationException("Scheduler is already running");

		if (!init())
			throw new AutomationException("Error while initiating scheduler");
		
		sequentialRun = false;
		try
		{
			status.clear();

			updateLinkedMatrices();

			Map<String, Preparable> preparableActions = new HashMap<String, Preparable>();
			SequentialExecutor sequentialExecutor = createSequentialExecutor(this, userName, preparableActions);
			sequentialExecutor.setOnFinish((x) -> this.executorFinished());
			initSequentialExecutor(sequentialExecutor);
			executor = sequentialExecutor;
			executor.start();
			sequentialRun = true;
		}
		catch (AutomationException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AutomationException("Unknown error while starting scheduler", e);
		}
	}
	
	protected void doBeforeStartExecution() throws AutomationException
	{
		if (schedulerData.isIgnoreAllConnectionsFailures())
			logger.info("All connections failures will be ignored and corresponding actions failed in current run");
		else
		{
			connectionsToIgnoreFailuresByRun = new HashSet<>(schedulerData.getConnectionsToIgnoreFailures());
			if (!connectionsToIgnoreFailuresByRun.isEmpty())
				logger.info("Failures will be ignored for the following connections: {}", connectionsToIgnoreFailuresByRun);
		}
	}
	
	synchronized public boolean restoreState(String userName) 
			throws IOException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, 
			InvocationTargetException, NoSuchMethodException, AutomationException, DataHandlingException
	{
		if (isRunning())
			return false;
		
		if (!schedulerData.isStateSaved())  //Checking existence of saved state and not stateInfo==null because state will be restored from state directory, not from stateInfo object
			return false;
		
		sequentialRun = false;
		status.clear();
		ExecutorState es = createExecutorState(schedulerData.getStateDir());
		SimpleExecutor simpleExecutor = es.executorFromState(this, executorFactory, businessDay, baseTime, userName);
		simpleExecutor.setRestored(true);
		simpleExecutor.setStoredActionReports(schedulerData.getRepDir());
		simpleExecutor.setOnFinish((x) -> this.executorFinished());
		
		steps = simpleExecutor.getSteps();
		matrices = simpleExecutor.getMatrices();
		holidays = simpleExecutor.getHolidays();
		weekendHoliday = simpleExecutor.isWeekendHoliday();
		initSchedulerOnRestore(simpleExecutor);
		executor = simpleExecutor;
		executor.start();
		
		return true;
	}
	
	synchronized public void stop() throws AutomationException
	{
		if (!isRunning() || isInterrupted())
			return;
		
		if (!sequentialRun)
		{
			SimpleExecutor simpleExecutor = (SimpleExecutor) executor;
			interruptSqlStatements(simpleExecutor.globalContext);
			executor.interruptExecution();
		}
		else
		{
			SequentialExecutor seqExec = (SequentialExecutor) executor;
			if (seqExec.currentExecutor != null)
				interruptSqlStatements(seqExec.currentExecutor.globalContext);
			
			if (seqExec.getCurrentMatrix() != null)
				seqExec.interruptExecution();
			else
				seqExec.interruptWholeExecution();
		}
		stoppedByUser.set(true);
//		matrices.clear();
	}

	protected void interruptSqlStatements(GlobalContext globalContext)
	{
		Set<Statement> statements = globalContext.getSqlStatements();
		Iterator<Statement> it = statements.iterator();
		while (it.hasNext())
		{
			try (Statement statement = it.next())
			{
				if (statement != null && !statement.isClosed())
					statement.cancel();
			}
			catch (SQLException e)
			{
				logger.warn("Error occurred while closing statement", e);
			}
		}
	}
	
	synchronized public void pause() throws AutomationException
	{
		if (!isRunning() || isInterrupted())
			return;
		executor.pauseExecution();
	}
	
	synchronized public void continueExecution()
	{
		if (!isSuspended())
			return;
		executor.clearLastReportsInfo();
		executor.continueExecution();
	}
	
	synchronized public void replayStep()
	{
		if (!isSuspended())
			return;
		executor.replayStep();
	}
	
	
	/* Failover management routines */
	
	public boolean isFailover()
	{
		if (!isRunning())
			return false;
		return executor.isFailover();
	}
	
	public void tryAgainMain()
	{
		if (isFailover())
			executor.tryAgainMain();
	}
	
	public void tryAgainAlt()
	{
		if (isFailover())
			executor.tryAgainAlt();
	}
	
	public int getFailoverActionType()
	{
		if (!isFailover())
			return ActionType.NONE;
		return executor.getFailoverActionType();
	}
	
	public int getFailoverReason()
	{
		if (!isFailover())
			return FailoverReason.NONE;
		return executor.getFailoverReason();
	}
	
	public String getFailoverReasonString()
	{
		if (!isFailover())
			return null;
		return executor.getFailoverReasonString();
	}
	
	public String getFailoverConnectionName()
	{
		if (!isFailover())
			return null;
		return executor.getFailoverConnectionName();
	}
	
	public void setFailoverRestartAction(boolean needRestart)
	{
		if (!isFailover())
			return;
		executor.setFailoverRestartAction(needRestart);
	}
	
	public void setFailoverSkipAction(boolean needSkipAction)
	{
		if (!isFailover())
			return;
		executor.setFailoverSkipAction(needSkipAction);
	}
	
	public void addConnectionToIgnoreFailuresByRun(String connectionName)
	{
		connectionsToIgnoreFailuresByRun.add(connectionName);
		logger.info("All next failures during current run will be ignored for connection '{}'", connectionName);
	}
	
	public Set<String> getConnectionsToIgnoreFailuresByRun()
	{
		return Collections.unmodifiableSet(connectionsToIgnoreFailuresByRun);
	}
	
	
	/* State management routines */
	
	synchronized public boolean saveState() throws IOException
	{
		if ((!isSuspended()) || sequentialRun)
			return false;
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String repDir = getReportsDir()+"current_"+df.format(new Date())+"/";
		ReportsInfo repInfo = makeCurrentReports(repDir, true);
		ExecutorState es = createExecutorState((SimpleExecutor) executor, stepFactory, repInfo);
		es.save(schedulerData.getStateDir());
		copyActionReport(schedulerData.getRepDir());
		
		try
		{
			stateInfo = loadStateInfo(schedulerData.getStateDir());
		}
		catch (IOException e)
		{
			stateInfo = null;
		}
		return true;
	}
	
	
	protected void saveStateInfo() throws IOException
	{
		try
		{
			saveStateInfo(schedulerData.getStateDir(), stateInfo);
		}
		catch (IOException e)
		{
			String msg = "Error while saving state info";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
	}
	
	public void removeSavedState()
	{
		
		try
		{
			File stateDir = schedulerData.getStateDir();
			if(stateDir.isDirectory())
				FileUtils.deleteDirectory(stateDir);
			
			File reportsStateDir = schedulerData.getRepDir();
			if(reportsStateDir.isDirectory())
				FileUtils.deleteDirectory(reportsStateDir);

			stateInfo=null;
			
		} catch (IOException e)
		{
			String msg = "Error while removing saved state";
			logger.warn(msg,e);
		}
	}
	
	synchronized public void setStateExecute(boolean execute) throws IOException
	{
		if ((stateInfo==null) || (stateInfo.getSteps()==null))
			return;
		
		for (StepState step : stateInfo.getSteps())
			if (step.getFinished()==null)  //Can change only not finished steps
				step.setExecute(execute);
		
		saveStateInfo();
	}
	
	synchronized public void setStateAskForContinue(boolean askForContinue) throws IOException
	{
		if ((stateInfo==null) || (stateInfo.getSteps()==null))
			return;
		
		for (StepState step : stateInfo.getSteps())
			if (step.getFinished()==null)  //Can change only not finished steps
				step.setAskForContinue(askForContinue);
		
		saveStateInfo();
	}

	synchronized public void setStateAskIfFailed(boolean stateAskIfFailed) throws IOException
	{
		if ((stateInfo==null) || (stateInfo.getSteps()==null))
			return;

		for (StepState step : stateInfo.getSteps())
			if (step.getFinished()==null)  //Can change only not finished steps
				step.setAskIfFailed(stateAskIfFailed);

		saveStateInfo();
	}
	
	synchronized public void modifyStepState(StepState originalStepState, StepState newStepState) throws IOException
	{
		if ((stateInfo==null) || (stateInfo.getSteps()==null) || (originalStepState.getFinished()!=null))
			return;
		
		List<StepState> steps = stateInfo.getSteps();
		
		int stepIndex = steps.indexOf(originalStepState);
		if (stepIndex < 0)
		{
			logger.error("Cannot find step to modify: " + originalStepState.getName());
			throw new IOException("Cannot find step to modify");
		}
		
		steps.set(stepIndex, newStepState);
		saveStateInfo();
	}
	
	
	/* Various getters */
	
	public String getForUser()
	{
		return schedulerData.getForUser();
	}
	
	public String getName()
	{
		return schedulerData.getName();
	}
	
	public SchedulerData getSchedulerData()
	{
		return schedulerData;
	}
	
	public synchronized Step getCurrentStep()
	{
		if (!isRunning())
			return null;
		return executor.getCurrentStep();
	}
	
	public boolean isCurrentStepIdle()
	{
		if (!isRunning())
			return false;
		return executor.isCurrentStepIdle();
	}
	
	public String getCurrentMatrix()
	{
		if (!sequentialRun)
			return null;
		return ((SequentialExecutor)executor).getCurrentMatrix();
	}
	
	public List<Step> getSteps()
	{
		return steps;
	}

	public List<StepData> getExecutedStepsData()
	{
		return schedulerData.getExecutedStepsData();
	}

	public void setExecutedStepsData(List<StepData> executedStepsData)
	{
		schedulerData.setExecutedStepsData(executedStepsData);
	}

	public void saveExecutedStepsData() throws IOException
	{
		schedulerData.saveExecutedStepsData();
	}

	public List<Matrix> getMatrices()
	{
		return matrices;
	}
	
	public SchedulerStatus getStatus()
	{
		return status;
	}
	
	public Map<String, List<ActionGeneratorMessage>> getMatricesErrors()
	{
		return matricesErrors;
	}
	
	public Date getBusinessDay()
	{
		return businessDay;
	}
	
	public Date getBaseTime()
	{
		return baseTime;
	}
	
	public boolean isWeekendHoliday()
	{
		return weekendHoliday;
	}
	
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}
	
	public String getConfigFileName()
	{
		return schedulerData.getConfigFileName();
	}
	
	public boolean isConfigChanged()
	{
		return schedulerData.isConfigChanged();
	}
	
	public String getReportsDir()
	{
		return executor.getReportsDir();
	}
	
	public String getCompletedReportsDir()
	{
		return executor.getCompletedReportsDir();
	}
	
	public ExecutorStateInfo getStateInfo()
	{
		return stateInfo;
	}

	public boolean isTestMode()
	{
		return testMode;
	}

	public void setTestMode(boolean testMode)
	{
		this.testMode = testMode;
	}
	
	synchronized public void addLaunch(XmlSchedulerLaunchInfo launchInfo) throws JAXBException, ClearThException
	{
		XmlSchedulerLaunches launches = schedulerData.getLaunches();
		launches.addLaunchInfo(0, launchInfo);
		schedulerData.saveLaunches();
	}
	
	synchronized public void copyActionReport(File pathToStoreReports)
	{
		executor.copyActionReports(pathToStoreReports);
	}
	
	synchronized public ReportsInfo makeCurrentReports(String pathToStoreReports, boolean reuseReports)
	{
		ReportsInfo reportsInfo = executor.getLastReportsInfo();
		if (!reuseReports || reportsInfo == null || !Files.isDirectory(Path.of(reportsInfo.getPath())))
			executor.makeCurrentReports(pathToStoreReports);
		return executor.getLastReportsInfo();
	}
	
	synchronized protected void executorFinished()
	{
		executor = null;
	}

	public StepFactory getStepFactory()
	{
		return stepFactory;
	}
	
	public MatrixDataFactory getMatrixDataFactory()
	{
		return matrixDataFactory;
	}
	
	
	public Date getStartTime()
	{
		return executorStartedTime;
		//TODO: need to implement getter for current executor in seqExec. And to obtain start time from it.
	}

	public void setLastExecutorStartedTime(Date startedTime)
	{
		this.executorStartedTime = startedTime;
	}

	public String getActionReportsDir()
	{
		if (executor != null && !sequentialRun)
			return ((SimpleExecutor)executor).getActionsReportsDir();
		return "";
	}

	public String getScriptsDir()
	{
		return scriptsDir;
	}

	synchronized public void reloadSchedulerData() throws Exception
	{
		schedulerData.loadMatrices(schedulerData.getMatrices());
		schedulerData.loadExecutedMatrices();
		schedulerData.reloadSteps(null);
		schedulerData.reloadExecutedStepsData();
		schedulerData.setBusinessDay(SchedulerData.loadBusinessDay(schedulerData.getBusinessDayFilePath()));
		schedulerData.setWeekendHoliday(schedulerData.loadWeekendHoliday());
		schedulerData.loadHolidays(schedulerData.getHolidays());
		schedulerData.setIgnoreAllConnectionsFailures(schedulerData.loadIgnoreAllConnectionsFailures());
		schedulerData.setConnectionsToIgnoreFailures(schedulerData.loadConnectionsToIgnoreFailures());
		init();
	}

	public IExecutor getExecutor()
	{
		return executor;
	}
	
	
	public Class<? extends StepImpl> getStepImplClass(String label) {
		switch (CoreStepKind.stepKindByLabel(label)) {
			case Default: return Default.class;
			default: return getStepImplClassEx(label);
		}
	}

	protected Class<? extends StepImpl> getStepImplClassEx(String label) {
		return null;
	}

	public void setExecutedMatrices(List<MatrixData> matrices)
	{
		schedulerData.setExecutedMatrices(matrices);
	}

	public Path getExecutedMatricesPath()
	{
		return schedulerData.getExecutedMatricesDirPath();
	}
}
