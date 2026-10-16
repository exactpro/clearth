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

package com.exactprosystems.clearth.tools.matrixmaker;

import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.exactprosystems.clearth.utils.FileOperationUtils.isZip;

public class MatrixMakerFromConfigTool extends MatrixMakerTool
{
	public static final String RANDOM_RANGE = "RandomRange", RANDOM_CONFIGS_FILE_NAME = "RandomConfigs";
	public static final Logger logger = LoggerFactory.getLogger(MatrixMakerFromConfigTool.class);
	protected List<File> uploadedConfigs;
	protected boolean randomScenarios;
	protected List<Map<String, String>> randomScenarioValues = new ArrayList<>();
	protected List<String> messages = new LinkedList<>();
	protected BlockingQueue<String> outMessages;
	protected int fileCount = 0, generatedCount = 0;
	protected File uploadDir, repositoryPath;
	protected boolean useOutMessages = false;

	public MatrixMakerFromConfigTool(List<File> uploadConfigs, File uploadDir, File tempDir, File cfgRootFile,
			File repositoryPath, boolean renameMatrices, boolean randomScenarios)
	{
		super(tempDir, cfgRootFile, renameMatrices);
		this.uploadedConfigs = uploadConfigs;
		this.randomScenarios = randomScenarios;
		this.uploadDir = uploadDir;
		this.repositoryPath = repositoryPath;
	}


	public File generate() throws Exception
	{
		initMessageContainers();

		List<Map<String,String>> configs = new LinkedList<Map<String,String>>() ;
		for (File f : uploadedConfigs)
			readConfigsFromFile(configs, f);
		File destDir = createUploadDir();
		File resultDir = createMatrixDir();
		
		fileCount = configs.size();
		
		ListIterator<Map<String,String>> it = configs.listIterator();
		
		
		while (it.hasNext())
		{
			Map<String,String> row = it.next();
			processConfigBB(row, it, destDir, resultDir);
		}

		if (randomScenarios)
			writeRandomScenarioValues(resultDir);
		
		if (messages.size() > 0)
			createMessageFile (resultDir.getAbsolutePath());
		
		return prepareMatricesToDownload(resultDir);
	}

	private void initMessageContainers()
	{
		messages = new LinkedList<>();
		if (useOutMessages)
			outMessages = new LinkedBlockingQueue<>();
	}

	protected void processConfigBB(Map<String, String> row, ListIterator<Map<String, String>> it, File destDir,
								   File resultDir) throws GenerateException
	{
		generatedCount++;
		String templateName = row.get(TEMPLATE_NAME);

		if (row.get(TEMPLATE).isEmpty())
			return;

		try
		{
			File template = findTemplate(row, templateName);
			List<File> configArr = new ArrayList<>();
			addConfigFiles(row.get(CONFIG), "config", configArr);
			readAdditionalConfigs(it, destDir, configArr);
			
			String rangesStr = row.get(RANDOM_RANGE);
			if (randomScenarios && !StringUtils.isEmpty(rangesStr))
			{
				List<List<Integer>> randomRanges = parseRandomRange(rangesStr);
				for (List<Integer> range : randomRanges)
				{
					generateMatrices(template, configArr, resultDir, templateName, range);
				}
			}
			else
				generateMatrices(template, configArr, resultDir, templateName, null);
			
		}
		catch (Exception e)
		{
			logger.error("Error while generating matrix. Template: " + templateName, e);
			addMessage(templateName, e.getMessage());
			
		}
	}
	
	protected void generateMatrices(File template, List<File> configArr, File resultDir, String templateName, 
			List<Integer> range) throws GenerateException
	{
		MatrixGenerator gm = createMatrixGenerator(Collections.singletonList(template), configArr, resultDir,
				renameMatrices, randomScenarios, templateName);
		//gm.setGeneratedName(templateName);
		gm.availableConfigRows = range;
		if (!fileMapping.isEmpty())
			gm.setFileMapping(fileMapping);
		if (!randomScenarioValues.isEmpty())
			gm.setRandomScenarioValues(randomScenarioValues);
		gm.initRGParams(readRgParams());
		gm.generate();
		fileMapping = gm.getFileMapping();
		randomScenarioValues = gm.getRandomScenarioValues();
		gm.GenerateAdditionalFiles();
	}
	
	
	protected File findTemplate(Map<String, String> row, String templateName) throws Exception
	{
		String templateLink = replaceVariableInFileName(row.get(TEMPLATE));
		return getFile(templateLink, templateName + "_", uploadDir.getAbsolutePath());
	}
	
	protected void readAdditionalConfigs(ListIterator<Map<String, String>> it, 
			File destDir, List<File> configArr) throws Exception
	{
		while (it.hasNext())
		{
			Map<String, String> nextRow = it.next();
			if (nextRow.get(TEMPLATE).isEmpty() && !nextRow.get(CONFIG).isEmpty())
				addConfigFiles(nextRow.get(CONFIG), "config", configArr);
			else
			{
				it.previous();
				break;
			}
		}
	}
	
	protected void addConfigFiles(String path, String filePrefix, List<File> files) throws Exception
	{
		path = replaceVariableInFileName(path);
		if (!path.startsWith("http") && (path.contains("*") || path.contains("?")))
		{
			findFilesByFilter(path, filePrefix, uploadDir.getAbsolutePath(), files);
		}
		else if (isZip(new File(path))) {
			Path destDir = Files.createTempDirectory("UnzippedConfigs");
			files.addAll(FileOperationUtils.unzipIfNeed(new File(path), "UnzippedConfigs", destDir.toFile()));
		} else {
			files.add(getFile(path, filePrefix, uploadDir.getAbsolutePath()));
		}
	}
	
	protected void findFilesByFilter(String path, String filePrefix, String dir, List<File> files) throws Exception
	{
		File tmp = new File(path);
		File dir2 = new File(tmp.getParent());
		FileFilter fileFilter = new WildcardFileFilter(tmp.getName());
		File[] files2 = dir2.listFiles(fileFilter);
		
		if (files2 == null)
		{
			logger.error("Cannot get config files by path: "+dir2.getAbsolutePath()+" with filter: "+fileFilter);
			return;
		}
		
		for (int i = 0; i < files2.length; i++) 
		{
			files.add(getFile(files2[i].getAbsolutePath(), filePrefix, dir));
		}
	}

	protected File getFile(String link, String filePrefix, String dir) throws Exception
	{
		return copyFileFromLocalPath(link, filePrefix, dir);
	}
	

	protected File copyFileFromLocalPath(String link, String filePrefix, String dir) throws Exception
	{
		//link = replaceVariableInFileName(link);
		logger.debug("link: "+link);
		File f = new File(link);
		if (!f.exists())
			throw new Exception("File "+ link + " does not exist.");

		File newFile = new File(tempDir, f.getName());
		FileUtils.copyFile(f, newFile);
		if (!newFile.exists())
			throw new Exception("File "+ newFile.getAbsolutePath() + " does not exist. Initial file not copied.");
		return newFile;
	}

	protected String replaceVariableInFileName(String link)
	{
		if (link.startsWith("${"))
		{
			int end = link.indexOf('}');
			String var = link.substring(2, end);
			String path = System.getenv(var);
			if (path == null || path.isEmpty())
				path = repositoryPath.toString();

			link = link.substring(end + 1);
			return fillPath(link, path);
		}
		else if (!link.startsWith("http"))
			return buildFilePath(link, repositoryPath.toPath());

		return link;
	}
	
	protected String fillPath(String link, String path)
	{
		if (link.length() > 1 && (link.startsWith("/") || link.startsWith("\\")) && (path.endsWith("/") || path.endsWith("\\")))
			link = link.substring(1);
		link = path + link;
		return link;
	}

	protected String buildFilePath(String link, Path path)
	{
		Path linkPath = Paths.get(link);
		if(linkPath.isAbsolute())
			return link;

		return path.resolve(linkPath).toString();
	}

	protected void addMessage(String message)
	{
		if (useOutMessages)
			outMessages.add(message);
		messages.add(message);
	}

	protected void addMessage(String templateName, String error)
	{
		addMessage(templateName + ": " + error);
	}


	public BlockingQueue<String> getOutMessages()
	{
		return outMessages;
	}

	public List<String> getMessages()
	{
		return Collections.unmodifiableList(messages);
	}

	public void setUseOutMessages(boolean useOutMessages)
	{
		this.useOutMessages = useOutMessages;
	}

	public boolean isUseOutMessages()
	{
		return useOutMessages;
	}

	public String getMessageStrings()
	{
		StringBuilder sb = new StringBuilder();
		for (String msg : messages)
		{
			sb.append(msg).append(Utils.EOL);
		}
		return sb.toString();
	}


	public List<Map<String, String>> readConfigsFromFile(List<Map<String, String>> config, File conf) throws Exception
	{
		try (ClearThCsvReader reader = new ClearThCsvReader(conf.getAbsolutePath(), createCsvReaderConfig()))
		{
			List<String> columnNames = new ArrayList<>();
			
			while (reader.hasNext())
			{
				String[] record = reader.getValues();
				if (record.length == 0 || record[0].startsWith("//"))
					continue;

				if (record[0].startsWith("#"))
				{
					readColumnNames(columnNames, reader);
					continue;
				}
				if (columnNames.isEmpty())
				{
					String mess = "Config file " + conf.getName() + " doesn't contain a header starting from #.";
					throw new GenerateException(mess);
				}

				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size(); i++)
					row.put(columnNames.get(i), record[i]);
				config.add(row);
			}
			return config;
		}
		catch (IOException e)
		{
			throw new Exception("Error occurred while reading config: " + conf.getName(), e);
		}
	}

	protected ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setFirstLineAsHeader(false);
		return config;
	}
	
	protected void checkColumnNames(List<String> columnNames) throws Exception
	{
		if (!columnNames.contains(TEMPLATE) || !columnNames.contains(CONFIG) || !columnNames.contains(TEMPLATE_NAME))
		{
			String errorMsg = "Column names should contain: #"+TEMPLATE+",#"+CONFIG+",#"+TEMPLATE_NAME;
			addMessage(errorMsg);
			throw new Exception(errorMsg);
		}
	}

	protected void readColumnNames(List<String> columnNames, ClearThCsvReader reader) throws Exception
	{
		columnNames.clear();
		for (String name : reader.getValues())
		{
			columnNames.add(name.replace("#", ""));
		}
		checkColumnNames(columnNames);
	}

	private void writeRandomScenarioValues(File resultDir) throws GenerateException
	{
		String fileName = RANDOM_CONFIGS_FILE_NAME;
		List<String> params = getRandomScenarioParams();
		String[] matrixNames = resultDir.list((current, name) -> new File(current, name).isFile());
		
		if (matrixNames == null)
			return;

		try (ClearThCsvWriter writer = new ClearThCsvWriter(new File(resultDir, fileName).getAbsolutePath()))
		{
			if (params != null)
				writer.writeRecord(params.toArray(new String[params.size()]));
			
			for (Map <String, String> row : randomScenarioValues)
			{
				Collection<String> paramList = (params != null) ? params : row.keySet();
				if (params != null)
				{
					for (String paramName : paramList)
					{
						writer.write(row.get(paramName));
					}
				}
				else
				{
					writer.writeRecord(paramList.toArray(new String[row.size()]));
					writer.writeRecord(row.values().toArray(new String[row.size()]));
					writer.endRecord();
				}
				writer.endRecord();
			}
		}
		catch (IOException e)
		{
			throw new GenerateException("Cannot create file: " + fileName, e);
		}
	}

	private List<String> getRandomScenarioParams()
	{
		return null;
	}

	public File createMessageFile (String repDir) throws IOException
	{
		File file = new File(repDir,"errorMsgs.txt");
		try
		{
		if (!file.exists())
			file.createNewFile();
		
			FileUtils.writeLines(file, messages);
			return file;
		} catch (IOException e)
		{
			throw e;
		}
	}
	

	protected List<List<Integer>> parseRandomRange(String randomRange) throws GenerateException
	{
		List<List<Integer>> ranges = new ArrayList<List<Integer>> ();
		String[] rangesStr = randomRange.split(";");
		for (String rangeStr : rangesStr)
		{
			List<Integer> range = new ArrayList<Integer>();
			String[] points = rangeStr.split("-");
			if (points.length != 2 || points[0] == null)
				throw new GenerateException("Invalid range: " + rangeStr);
			Integer point1, point2;
			try
			{
				point1 = Integer.valueOf(points[0]);
				if (points[1] == null)
					range.add(point1);
				else
				{
					point2 = Integer.valueOf(points[1]);
					for (int i = point1; i <= point2; i++)
						range.add(i);
				}
			}catch(NumberFormatException e)
			{
				throw new GenerateException("Invalid range: " + rangeStr, e);
			}
			if (!range.isEmpty())
				ranges.add(range);
		}
		
		return ranges;
	}
	

	public String getGeneratedMatrixCount()
	{
		return generatedCount+"/"+fileCount;
	}
	
}
