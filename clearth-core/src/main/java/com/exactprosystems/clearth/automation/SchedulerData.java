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
import com.exactprosystems.clearth.automation.persistence.StateConfig;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.DateTimeUtils;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.utils.JsonMarshaller;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;

public abstract class SchedulerData
{
	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(SchedulerData.class);

	public static final String CONFIG_FILENAME = "config.cfg",
			LAUNCHES_FILENAME = "launches.xml",
			HOLIDAYS_FILENAME = "holidays.txt",
			BUSINESSDAY_FILENAME = "businessday.txt",
			BASETIME_FILENAME = "basetime.txt",
			WEEKEND_FILENAME = "weekend.txt",
			IGNORE_ALL_CONNECTIONS_FAILURES_FILENAME = "ignore_all_connections_failures.txt",
			CONNECTIONS_TO_IGNORE_FAILURES_FILENAME = "connections_to_ignore_failures.txt",
			MATRICES_FILENAME = "matrices.csv",
			STEP_INFO_DATA_FILENAME = "executed_steps.csv",
			CONFIGDATA_FILENAME = "configdata.cfg",
			REPORTS_CONFIG_FILENAME = "reports.json",
			STATE_CONFIG_FILENAME = "state.json",
			NAME = "Name",
			MATRIX = "Matrix",
			UPLOADED = "Uploaded",
			EXECUTE = "Execute",
			TRIM_SPACES = "TrimSpaces",
			LINK = "Link",
			TYPE = "Type",
			AUTO_RELOAD = "AutoReload";
	
	public static final String SCH_LAUNCHES = "SchedulerLaunches";

	protected static final DateFormat businessDayFormat = new SimpleDateFormat("yyyyMMdd"),
			baseTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

	private final String forUser, name, matricesDir, launchesName, configName, baseTimeName, weekendHolidayName,
			holidaysName, ignoreAllConnectionsFailuresName, connectionsToIgnoreFailuresName, matricesName, configDataName;

	private final Path businessDayFilePath,
			executedStepsDataFilePath,
			reportsConfigFilePath,
			stateConfigFilePath;

	private final StepFactory stepFactory;
	private final XmlSchedulerLaunches launches;
	private final List<Step> steps;
	private Date businessDay,
			baseTime;
	private boolean useCurrentDate;
	private boolean weekendHoliday;
	private final Map<String, Boolean> holidays;
	private boolean ignoreAllConnectionsFailures;
	private Set<String> connectionsToIgnoreFailures;
	private final List<MatrixData> matrices;
	private final ConfigData configData;
	private final File stateDir;
	private final File repDir;
	private final File schedulerDir;
	private final ExecutedMatricesData executedMatricesData;
	private ReportsConfig reportsConfig;
	private StateConfig stateConfig;
	private List<StepData> executedStepsData;


	public SchedulerData(String name, String configsRoot, String schedulerDirName, String matricesDir,
	                     String lastExecutedDataDir, StepFactory stepFactory) throws Exception
	{
		this.forUser = schedulerDirName;
		this.name = name;
		this.matricesDir = matricesDir;
		
		String cfgDir = configsRoot + schedulerDirName + File.separator;
		schedulerDir = new File(cfgDir, name);
		Files.createDirectories(schedulerDir.toPath());  //Creating parent folder for scheduler data files

		launchesName = getLaunchesName(cfgDir, name);
		configName = getConfigName(cfgDir, name);
		businessDayFilePath = getBusinessDayFilePath(cfgDir, name);
		executedStepsDataFilePath = getExecutedStepsDataFilePath(lastExecutedDataDir);
		baseTimeName = getBaseTimeName(cfgDir, name);
		weekendHolidayName = getWeekendHolidayName(cfgDir, name);
		holidaysName = getHolidaysName(cfgDir, name);
		ignoreAllConnectionsFailuresName = getIgnoreAllConnectionsFailuresName(cfgDir, name);
		connectionsToIgnoreFailuresName = getConnectionsToIgnoreFailuresName(cfgDir, name);
		matricesName = getMatricesName(cfgDir, name);
		configDataName = getConfigDataName(cfgDir, name);
		reportsConfigFilePath = getReportsConfigFilePath(cfgDir, name);
		stateConfigFilePath = getStateConfigFilePath(cfgDir, name);
		this.stepFactory = stepFactory;
		
		File launchesFile = new File(launchesName);
		if (launchesFile.isFile() && launchesFile.length() != 0)
			launches = loadLaunches();
		else {
			launches = ClearThCore.getInstance().getSchedulerFactory().createSchedulerLaunches();
			logger.warn("Launches file for scheduler '" + name + "' is empty or doesn't exist.");
		}
		steps = loadSteps(null); //Ignore step warnings
		executedStepsData = loadExecutedStepsData();
		businessDay = loadBusinessDay();
		baseTime = loadBaseTime();
		weekendHoliday = loadWeekendHoliday();
		holidays = loadHolidays();
		ignoreAllConnectionsFailures = loadIgnoreAllConnectionsFailures();
		connectionsToIgnoreFailures = loadConnectionsToIgnoreFailures();
		matrices = loadMatrices();
		configData = initConfigData();
		stateDir = new File(getStateDirName(cfgDir, name));
		repDir = new File(getReportsDirName(cfgDir, name));

		executedMatricesData = new ExecutedMatricesData(lastExecutedDataDir);
		reportsConfig = loadReportsConfig();
		stateConfig = loadStateConfig();
	}

	public abstract String[] getConfigHeader();
	public abstract String[] getExecutedStepsDataHeader();

	public File getSchedulerDir()
	{
		return schedulerDir;
	}
	
	public static String getConfigName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+CONFIG_FILENAME;
	}
	
	public static String getLaunchesName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+LAUNCHES_FILENAME;
	}
	
	public static String getHolidaysName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+HOLIDAYS_FILENAME;
	}
	
	public static Path getBusinessDayFilePath(String configsRoot, String schedulerName)
	{
		return Paths.get(configsRoot, schedulerName, BUSINESSDAY_FILENAME);
	}

	public static Path getExecutedStepsDataFilePath(String configsRoot)
	{
		return Paths.get(configsRoot, STEP_INFO_DATA_FILENAME);
	}

	public static String getBaseTimeName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+BASETIME_FILENAME;
	}
	
	public static String getWeekendHolidayName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+WEEKEND_FILENAME;
	}
	
	public static String getIgnoreAllConnectionsFailuresName(String configsRoot, String schedulerName)
	{
		return configsRoot + schedulerName + File.separator + IGNORE_ALL_CONNECTIONS_FAILURES_FILENAME;
	}
	
	public static String getConnectionsToIgnoreFailuresName(String configsRoot, String schedulerName)
	{
		return configsRoot + schedulerName + File.separator + CONNECTIONS_TO_IGNORE_FAILURES_FILENAME;
	}
	
	public static String getMatricesName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+MATRICES_FILENAME;
	}

	public static String getConfigDataName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+CONFIGDATA_FILENAME;
	}
	
	public static Path getReportsConfigFilePath(String configsRoot, String schedulerName)
	{
		return Paths.get(configsRoot, schedulerName, REPORTS_CONFIG_FILENAME);
	}
	
	public static Path getStateConfigFilePath(String configsRoot, String schedulerName)
	{
		return Paths.get(configsRoot, schedulerName, STATE_CONFIG_FILENAME);
	}
	
	public static String getStateDirName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+"state";
	}
	
	public static String getReportsDirName(String configsRoot, String schedulerName)
	{
		return configsRoot+schedulerName+File.separator+"reports";
	}
	
	public static void loadSteps(String configCSV, List<Step> stepsContainer, StepFactory stepFactory, List<String> warnings) throws IOException
	{
		stepsContainer.clear();
		File f = new File(configCSV);
		if (!f.isFile())
			return;
		
		try (ClearThCsvReader reader = new ClearThCsvReader(configCSV, createCsvReaderConfig()))
		{
			if (!reader.hasHeader())
				return;
			
			NEXT_RECORD:
			while (reader.hasNext())
			{
				Step newStep = stepFactory.createStep(reader.getRecord());
				if (StringUtils.isEmpty(newStep.getName()))
					continue;
				for (Step addedStep : stepsContainer)
				{
					if (addedStep.getName().equals(newStep.getName()))
					{
						String message = format("Duplicates of %s step are skipped because a step name should be unique.", newStep.getName());
						if (warnings != null && !warnings.contains(message))
							warnings.add(message);
						continue NEXT_RECORD;
					}
				}
				
				if (!stepFactory.validStepKind(newStep.getKind()))
				{
					if (warnings != null)
						warnings.add(format("%s step has incorrect type: %s. Step type is replaced to Default", newStep.getName(), newStep.getKind()));
					newStep.setKind(CoreStepKind.Default.getLabel());
				}
				
				stepsContainer.add(newStep);
			}
		}
	}
	
	public static List<Step> loadSteps(String configCSV, StepFactory stepFactory, List<String> warnings) throws IOException
	{
		List<Step> result = new ArrayList<Step>();
		loadSteps(configCSV, result, stepFactory, warnings);
		return result;
	}

	public static void saveExecutedStepsData(File file, String[] configHeader, List<StepData> stepData) throws IOException
	{
		saveSchedulerData(file, configHeader, stepData);
	}

	public static void saveSteps(File file, String[] configHeader, List<Step> steps) throws IOException
	{
		saveSchedulerData(file, configHeader, steps);
	}

	private static <T extends CsvDataManager> void saveSchedulerData(File file, String[] configHeader, List<T> schedulerData) throws IOException
	{
		try (ClearThCsvWriter writer = new ClearThCsvWriter(new FileWriter(file), createCsvWriterConfig()))
		{
			writer.writeRecord(configHeader);
			for (T data : schedulerData)
			{
				data.save(writer);
				writer.endRecord();
			}
			writer.flush();
		}
	}

	private static ClearThCsvWriterConfig createCsvWriterConfig()
	{
		ClearThCsvWriterConfig config = new ClearThCsvWriterConfig();
		config.setDelimiter(',');
		config.setWithTrim(false);
		return config;
	}

	public List<Step> loadSteps(List<String> warnings) throws IOException
	{
		return loadSteps(configName, stepFactory, warnings);
	}

	public List<StepData> loadExecutedStepsData() throws IOException
	{
		List<StepData> result = new ArrayList<>();
		loadExecutedStepsData(result);
		return result;
	}

	public void loadExecutedStepsData(List<StepData> result) throws IOException
	{
		result.clear();
		File executedStepsDataFile = executedStepsDataFilePath.toFile();
		if (!executedStepsDataFile.isFile())
			return;

		try (ClearThCsvReader reader = new ClearThCsvReader(new FileReader(executedStepsDataFile), createCsvReaderConfig()))
		{
			if (!reader.hasHeader())
				return;

			while (reader.hasNext())
			{
				StepData stepData = new StepData(reader.getRecord());
				result.add(stepData);
			}
		}
	}

	private static ClearThCsvReaderConfig createCsvReaderConfig()
	{
		return ClearThCsvReaderConfig.withFirstLineAsHeader();
	}

	public void loadSteps(List<Step> stepsContainer, List<String> warnings) throws IOException
	{
		loadSteps(configName, stepsContainer, stepFactory, warnings);
	}
	
	public void reloadSteps(List<String> warnings) throws Exception
	{
		loadSteps(configName, steps, stepFactory, warnings);
	}

	public void reloadExecutedStepsData() throws IOException
	{
		loadExecutedStepsData(executedStepsData);
	}

	public void saveSteps() throws IOException
	{
		saveSteps(new File(configName), getConfigHeader(), steps);
	}

	public void saveExecutedStepsData() throws IOException
	{
		saveExecutedStepsData(executedStepsDataFilePath.toFile(), getExecutedStepsDataHeader(), executedStepsData);
	}

	public static void loadHolidays(String holidaysFileName, Map<String, Boolean> holidaysContainer) throws IOException
	{
		holidaysContainer.clear();
		File f = new File(holidaysFileName);
		if (!f.isFile())
			return;
		
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(f));
			String line;
			while ((line = reader.readLine())!=null)
			{
				String[] hol = line.trim().split("=");
				if (!holidaysContainer.containsKey(hol[0]))
				{
					Boolean isHol = hol.length < 2 || !hol[1].equals("0");
					holidaysContainer.put(hol[0], isHol);
				}
			}
		}
		finally
		{
			if (reader!=null)
				reader.close();
		}
	}
	
	public static Map<String, Boolean> loadHolidays(String holidaysFileName) throws IOException
	{
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		loadHolidays(holidaysFileName, result);
		return result;
	}
	
	public static void saveHolidays(String fileName, Map<String, Boolean> holidays) throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(fileName);
			for (String hol : holidays.keySet())
				writer.println(hol+"="+(holidays.get(hol) ? "1" : "0"));
		}
		finally
		{
			if (writer!=null)
			{
				writer.flush();
				writer.close();
			}
		}
	}
	
	public Map<String, Boolean> loadHolidays() throws IOException
	{
		return loadHolidays(holidaysName);
	}
	
	public void loadHolidays(Map<String, Boolean> holidaysContainer) throws IOException
	{
		loadHolidays(holidaysName, holidaysContainer);
	}
	
	public void saveHolidays() throws IOException
	{
		saveHolidays(holidaysName, holidays);
	}

	public static Date loadBusinessDay(Path filePath) throws IOException, ParseException
	{
		File businessDayFile = filePath.toFile();
		if (!businessDayFile.isFile() || businessDayFile.length() == 0)
			return null;

		try (BufferedReader reader = new BufferedReader(new FileReader(businessDayFile)))
		{
			return businessDayFormat.parse(reader.readLine());
		}
	}

	public void saveBusinessDay(Path filePath, Date businessDay) throws IOException
	{
		try (PrintWriter writer = new PrintWriter(filePath.toFile()))
		{
			if (businessDay != null)
				writer.println(businessDayFormat.format(businessDay));
		}
	}

	public Date loadBusinessDay() throws IOException, ParseException
	{
		Date businessDay = loadBusinessDay(businessDayFilePath);

		if (businessDay == null)
		{
			setUseCurrentDate(true);
			return new Date();
		}
		else
		{
			setUseCurrentDate(false);
			return businessDay;
		}
	}


	public static Date loadBaseTime(String fileName) throws IOException, ParseException
	{
		File f = new File(fileName);
		if (!f.isFile())
			return null;
		
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(fileName));
			return baseTimeFormat.parse(reader.readLine());
		}
		finally
		{
			if (reader!=null)
				reader.close();
		}
	}
	
	public static void saveBaseTime(String fileName, Date baseTime) throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(fileName);
			writer.println(baseTimeFormat.format(baseTime));
			writer.flush();
		}
		finally
		{
			if (writer!=null)
				writer.close();
		}
	}
	
	public Date loadBaseTime() throws IOException, ParseException
	{
		return loadBaseTime(baseTimeName);
	}
	
	public void saveBaseTime() throws IOException
	{
		saveBaseTime(baseTimeName, baseTime);
	}
	
	
	public static boolean loadWeekendHoliday(String fileName) throws IOException, ParseException
	{
		File f = new File(fileName);
		if (!f.isFile())
			return false;
		
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(fileName));
			return Boolean.parseBoolean(reader.readLine());
		}
		finally
		{
			if (reader!=null)
				reader.close();
		}
	}
	
	public static void saveWeekendHoliday(String fileName, boolean weekendHoliday) throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(fileName);
			writer.println(Boolean.toString(weekendHoliday));
			writer.flush();
		}
		finally
		{
			if (writer!=null)
				writer.close();
		}
	}
	
	public boolean loadWeekendHoliday() throws IOException, ParseException
	{
		return loadWeekendHoliday(weekendHolidayName);
	}
	
	public void saveWeekendHoliday() throws IOException
	{
		saveWeekendHoliday(weekendHolidayName, weekendHoliday);
	}
	
	
	public boolean loadIgnoreAllConnectionsFailures() throws IOException
	{
		File file = new File(ignoreAllConnectionsFailuresName);
		if (!file.isFile())
			return false;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			return Boolean.parseBoolean(reader.readLine());
		}
	}
	
	public void saveIgnoreAllConnectionsFailures() throws IOException
	{
		try (PrintWriter writer = new PrintWriter(ignoreAllConnectionsFailuresName))
		{
			writer.write(String.valueOf(ignoreAllConnectionsFailures));
		}
	}
	
	public Set<String> loadConnectionsToIgnoreFailures() throws IOException
	{
		Set<String> result = new LinkedHashSet<>();
		File file = new File(connectionsToIgnoreFailuresName);
		if (file.isFile())
		{
			try (BufferedReader reader = new BufferedReader(new FileReader(file)))
			{
				String line;
				while ((line = reader.readLine()) != null)
					result.add(line);
			}
		}
		return result;
	}
	
	public void saveConnectionsToIgnoreFailures() throws IOException
	{
		try (PrintWriter writer = new PrintWriter(connectionsToIgnoreFailuresName))
		{
			connectionsToIgnoreFailures.forEach(writer::println);
		}
	}
	
	
	protected XmlSchedulerLaunches loadLaunches() throws JAXBException, IOException
	{
		try
		{
			return (XmlSchedulerLaunches) XmlUtils.unmarshalObject(XmlSchedulerLaunches.class, launchesName);
		}
		catch (UnmarshalException e)
		{
			logger.warn("Error occured while loading data from '"+launchesName+"' file", e);
			logger.info("Launches info file might be broken. Will use empty list of launches");
			File launchesFile = new File(launchesName),
					backup = new File(launchesName+"_backup");
			try
			{
				FileUtils.copyFile(launchesFile, backup);
				logger.info("Broken file is kept as backup in the same directory");
			}
			catch (Exception e2)
			{
				logger.warn("Could not create backup copy of file '"+launchesFile.getAbsolutePath()+"'", e2);
			}
			return new XmlSchedulerLaunches();
		}
	}
	
	public void saveLaunches() throws JAXBException, ClearThException
	{
		XmlUtils.marshalObject(launches, launchesName);
	}
	
	
	public void loadMatrices(String matricesFileName, String matricesDir, List<MatrixData> matricesContainer) throws IOException
	{
		matricesContainer.clear();
		File f = new File(matricesFileName);
		if (!f.isFile())
			return;

		try (ClearThCsvReader reader = new ClearThCsvReader(matricesFileName, createCsvReaderConfig()))
		{
			if (!reader.hasHeader())
				return;

			while (reader.hasNext())
			{
				String fileName = reader.get(MATRIX);
				if ((fileName==null) || (fileName.isEmpty()))
					continue;
				File matrixFile = new File(matricesDir+fileName);
				if (!matrixFile.exists())
					continue;
				if (matrixFileIndex(matrixFile, matricesContainer)>-1)
					continue;

				String uploadDateNum = reader.get(UPLOADED);
				Date uploadDate = DateTimeUtils.getDateFromTimestampOrNull(uploadDateNum);

				Boolean isExecute = Boolean.parseBoolean(reader.get(EXECUTE));
				Boolean isTrim = (reader.get(TRIM_SPACES) != null && !reader.get(TRIM_SPACES).isEmpty())
						? Boolean.parseBoolean(reader.get(TRIM_SPACES)) : true;
				
				String link = reader.contains(LINK) ? reader.get(LINK) : "";
				String type = reader.contains(TYPE) ? reader.get(TYPE) : "";
				String autoReload = reader.get(AUTO_RELOAD);
				Boolean isAutoReload = !isEmpty(autoReload) && Boolean.parseBoolean(autoReload);

				String name = reader.get(NAME);
				if (StringUtils.isEmpty(name))
					name = StringUtils.isEmpty(link) ? matrixFile.getName() : link;

				MatrixDataFactory mdf = ClearThCore.getInstance().getMatrixDataFactory();
				MatrixData md = mdf.createMatrixData(name, matrixFile, uploadDate, isExecute, isTrim, link, type, isAutoReload);
				readAdditionalMatrixSettings(reader, md);
				matricesContainer.add(md);
			}
		}
	}
	
	@SuppressWarnings("unused")
	protected void readAdditionalMatrixSettings(ClearThCsvReader reader, MatrixData md) throws IOException {}

	public List<MatrixData> loadMatrices(String matricesFileName, String matricesDir) throws IOException
	{
		List<MatrixData> result = new ArrayList<MatrixData>();
		loadMatrices(matricesFileName, matricesDir, result);
		return result;
	}
	
	public void saveMatrices(String fileName, List<MatrixData> matrices) throws IOException
	{
		try (ClearThCsvWriter writer =  new ClearThCsvWriter(fileName, createCsvWriterConfig()))
		{
			writeMatricesTableHeaders(writer);
			for (MatrixData md : matrices)
			{
				writeMatrixData(writer, md);
			}
			writer.flush();
		}
	}
	
	protected void writeMatricesTableHeaders(ClearThCsvWriter writer) throws IOException
	{
		writer.write(NAME);
		writer.write(MATRIX);
		writer.write(UPLOADED);
		writer.write(EXECUTE);
		writer.write(TRIM_SPACES);
		writer.write(LINK);
		writer.write(TYPE);
		writer.write(AUTO_RELOAD);
		List<String> additionalHeaders = getAdditionalMatricesTableHeaders();
		if (isNotEmpty(additionalHeaders))
		{
			for (String header : additionalHeaders)
			{
				writer.write(header);
			}
		}
		writer.endRecord();
	}
	
	protected List<String> getAdditionalMatricesTableHeaders()
	{
		return emptyList();
	}
	
	protected void writeMatrixData(ClearThCsvWriter writer, MatrixData md) throws IOException
	{
		writer.write(md.getName());
		writer.write(md.getFile().getName());
		writer.write(md.getUploadDate() == null ? "" : Long.toString(md.getUploadDate().getTime()));
		writer.write(Boolean.toString(md.isExecute()));
		writer.write(Boolean.toString(md.isTrim()));
		writer.write(md.getLink());
		writer.write(md.getType());
		writer.write(Boolean.toString(md.isAutoReload()));
		writeAdditionalMatrixSettings(writer, md);
		writer.endRecord();
	}
	
	@SuppressWarnings("unused")
	protected void writeAdditionalMatrixSettings(ClearThCsvWriter writer, MatrixData md) throws IOException {	}
	
	public List<MatrixData> loadMatrices() throws IOException
	{
		return loadMatrices(matricesName, matricesDir);
	}
	
	public void loadExecutedMatrices() throws IOException
	{
		executedMatricesData.loadExecutedMatrices();
	}

	public void loadMatrices(List<MatrixData> matricesContainer) throws IOException
	{
		loadMatrices(matricesName, matricesDir, matricesContainer);
	}
	
	public void saveMatrices() throws IOException
	{
		saveMatrices(matricesName, matrices);
	}
	
	
	public static ReportsConfig loadReportsConfig(Path file) throws IOException
	{
		if (!Files.isRegularFile(file))
			return new ReportsConfig(true, true, true);
		
		return new JsonMarshaller<ReportsConfig>().unmarshal(file, ReportsConfig.class);
	}
	
	public static void saveReportsConfig(ReportsConfig config, Path file) throws IOException
	{
		Files.createDirectories(file.getParent());
		new JsonMarshaller<ReportsConfig>().marshal(config, file);
	}
	
	public ReportsConfig loadReportsConfig() throws IOException
	{
		return loadReportsConfig(reportsConfigFilePath);
	}
	
	public void saveReportsConfig() throws IOException
	{
		saveReportsConfig(reportsConfig, reportsConfigFilePath);
	}
	
	
	public static StateConfig loadStateConfig(Path file) throws IOException
	{
		if (!Files.isRegularFile(file))
			return new StateConfig(false);
		
		return new JsonMarshaller<StateConfig>().unmarshal(file, StateConfig.class);
	}
	
	public static void saveStateConfig(StateConfig config, Path file) throws IOException
	{
		Files.createDirectories(file.getParent());
		new JsonMarshaller<StateConfig>().marshal(config, file);
	}
	
	public StateConfig loadStateConfig() throws IOException
	{
		return loadStateConfig(stateConfigFilePath);
	}
	
	public void saveStateConfig() throws IOException
	{
		saveStateConfig(stateConfig, stateConfigFilePath);
	}
	
	
	public static ConfigData loadConfigData(String fileName) throws IOException
	{
		Map<String, String> keyValue = KeyValueUtils.loadKeyValueFile(fileName, true);
		ConfigData result = new ConfigData(keyValue.get("filename"), Boolean.parseBoolean(keyValue.get("changed")));
		return result;
	}
	
	public static void saveConfigData(String fileName, ConfigData configData) throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(fileName);
			if (configData.fileName!=null)
				writer.println("filename="+configData.fileName);
			writer.println("changed="+Boolean.toString(configData.changed));
		}
		finally
		{
			if (writer!=null)
			{
				writer.flush();
				writer.close();
			}
		}
	}
	
	public ConfigData loadConfigData() throws IOException
	{
		return loadConfigData(configDataName);
	}

	private ConfigData initConfigData() throws IOException
	{
		File f = new File(configDataName);
		if (!f.isFile())
			return new ConfigData();
		return loadConfigData();
	}
	
	public void saveConfigData() throws IOException
	{
		saveConfigData(configDataName, configData);
	}
	
	
	public String getForUser()
	{
		return forUser;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getLaunchesName()
	{
		return launchesName;
	}
	
	public String getConfigName()
	{
		return configName;
	}
	
	public String getHolidaysName()
	{
		return holidaysName;
	}
	
	public Path getBusinessDayFilePath()
	{
		return businessDayFilePath;
	}
	
	public String getBaseTimeName()
	{
		return baseTimeName;
	}
	
	public String getMatricesName()
	{
		return matricesName;
	}
	
	
	public XmlSchedulerLaunches getLaunches()
	{
		return launches;
	}
	
	public List<XmlSchedulerLaunchInfo> getLaunches(int first, int numCount)
	{
		List<XmlSchedulerLaunchInfo> ls = launches.getLaunchesInfo();

		if (first >= ls.size())
			return null;
		
		int num = Math.min(first + numCount, ls.size());
		return ls.subList(first, num);
	}
	
	
	public List<Step> getSteps()
	{
		return steps;
	}
	
	
	public Date getBusinessDay()
	{
		return businessDay;
	}

	public void setBusinessDay(Date businessDay) throws IOException
	{
		try
		{
			this.businessDay = businessDay == null ? new Date() : businessDay;
			saveBusinessDay(businessDayFilePath, businessDay);
		}
		catch (IOException e)
		{
			String msg = "Error while saving scheduler business day after setting it";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
	}

	public Date getBaseTime()
	{
		return baseTime;
	}
	
	public void setBaseTime(Date baseTime)
	{
		this.baseTime = baseTime;
	}


	public boolean isUseCurrentDate()
	{
		return useCurrentDate;
	}

	public void setUseCurrentDate(boolean useCurrentDate)
	{
		this.useCurrentDate = useCurrentDate;
	}
	

	public boolean isWeekendHoliday()
	{
		return weekendHoliday;
	}
	
	public void setWeekendHoliday(boolean weekendHoliday)
	{
		this.weekendHoliday = weekendHoliday;
	}
	
	
	public void setIgnoreAllConnectionsFailures(boolean ignoreAllConnectionsFailures)
	{
		this.ignoreAllConnectionsFailures = ignoreAllConnectionsFailures;
	}
	
	public boolean isIgnoreAllConnectionsFailures()
	{
		return ignoreAllConnectionsFailures;
	}
	
	public void setConnectionsToIgnoreFailures(Set<String> connectionsToIgnoreFailures)
	{
		this.connectionsToIgnoreFailures = connectionsToIgnoreFailures;
	}
	
	public Set<String> getConnectionsToIgnoreFailures()
	{
		return connectionsToIgnoreFailures;
	}
	
	
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}
	
	public List<MatrixData> getMatrices()
	{
		return matrices;
	}
	
	public ReportsConfig getReportsConfig()
	{
		return reportsConfig;
	}
	
	public void setReportsConfig(ReportsConfig reportsConfig)
	{
		this.reportsConfig = new ReportsConfig(reportsConfig);
	}
	
	
	public StateConfig getStateConfig()
	{
		return stateConfig;
	}
	
	public void setStateConfig(StateConfig stateConfig)
	{
		this.stateConfig = new StateConfig(stateConfig);
	}
	
	
	public String getConfigFileName()
	{
		return configData.fileName;
	}
	
	public void setConfigFileName(String configFileName)
	{
		this.configData.fileName = configFileName;
	}
	
	
	public boolean isConfigChanged()
	{
		return configData.changed;
	}
	
	public void setConfigChanged(boolean changed)
	{
		this.configData.changed = changed;
	}
	
	
	public boolean isStateSaved()
	{
		return stateDir.isDirectory();
	}
	
	public File getStateDir()
	{
		return stateDir;
	}
	
	public File getRepDir() {
		return repDir;
	}

	public Path getExecutedMatricesDirPath()
	{
		return executedMatricesData.getExecutedMatricesDirPath();
	}

	public static int matrixFileIndex(File matrixFile, List<MatrixData> matricesContainer)
	{
		for (int i=0; i<matricesContainer.size(); i++)
		{
			if (matricesContainer.get(i).getFile().getName().equals(matrixFile.getName()))
				return i;
		}
		return -1;
	}
	
	public int matrixFileIndex(File matrixFile)
	{
		return matrixFileIndex(matrixFile, matrices);
	}

	public void setExecutedMatrices(List<MatrixData> executedMatrices)
	{
		executedMatricesData.setExecutedMatrices(executedMatrices);
	}

	public List<MatrixData> getExecutedMatrices()
	{
		return executedMatricesData.getExecutedMatrices();
	}

	public void setExecutedStepsData(List<StepData> executedStepsData)
	{
		this.executedStepsData = executedStepsData;
	}

	public List<StepData> getExecutedStepsData()
	{
		return executedStepsData;
	}
}
