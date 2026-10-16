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

import com.exactprosystems.clearth.automation.ActionExecutor;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitializer;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.ListInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.SelfInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.ValueInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData.CsvInitialRow;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.InitialData;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatrixGenerator
{
	public static final String START_CYCLE = "start", END_CYCLE = "end", NESTED_CYCLE = "NESTED_CYCLE";
	public static final String START_INIT = "init start", END_INIT = "init end";
	public static final String START_BLOCK = "block start", END_BLOCK = "block end", 
			BLOCK_NAME = "name",  BLOCK = "#{block", START_MATRIX_FORMULA = "@{",
			START_GROUP = "start group", END_GROUP = "end group";
	
	public static final String START_COMMENT = "//";
	public static final String NEW_FILE = "#{splitMatrix";
	public static final String RGS = "#RepeatingGroups", ID = "#id";
	//public static final String ACTION_ADD_RG = "AddRepeatingGroup".toUpperCase(), ACTION_ADD_TE = "AddTableEntry".toUpperCase(),
	//		ACTION_TAKE_SNAPSHOT_MR = "TakeSnapshotMR".toUpperCase(), ACTION_TAKE_SNAPSHOT_RRM = "TakeSnapshotRRM".toUpperCase();
	//public static final List<String> addRgActions = Arrays.asList(ACTION_ADD_RG, ACTION_ADD_TE, ACTION_TAKE_SNAPSHOT_MR, ACTION_TAKE_SNAPSHOT_RRM);
	public static final Set<String> keyNames = new HashSet<String>(Arrays.asList(START_CYCLE, END_CYCLE, START_INIT, END_INIT,START_BLOCK,END_BLOCK,
			START_GROUP, END_GROUP, "start subgroup", "end subgroup"));
	public static final String AUTO_ID_INDEX = "AutoIdIndex";
	public static final String CSV_EXTENSION = ".csv";
	protected static final String ADD_NAME_DELIMITER = "_";
	public static final List<String> MATRIX_JAVA_CLASSES = Arrays.asList("Math", "Long", "Integer", "Double", "System","java","util","Arrays");
	//protected static final Pattern pattern = Pattern.compile("\\@\\{(([A-Za-z0-9_])*)\\.(.)*\\}");
	private static final Set<String> STRING_METHODS;
	
	protected Pattern idPattern = Pattern.compile("(?<![\\.\\w])([A-Za-z]\\w+)\\.([A-Za-z]\\w+)");
	
	static
	{
		STRING_METHODS = new HashSet<>();
		for (Method method : String.class.getMethods())
			STRING_METHODS.add(method.getName());
	}
	
	protected Set<String> csvConfigs = new HashSet<String>(Arrays.asList("csvFile","csvLink"));
	protected Collection<String> notChangedActionIds = getNotChangedActionIds();
	
	protected String currentTemplate = "";
	
	protected Functions functions;
	protected Map<String, InitialData> initialData = new HashMap<String, InitialData>();
	protected List<File> dataFiles;
	protected List<File> templateFiles;
	
	MMTemplateWriter writer = null;
	protected Map<String,String> outputParams = null;
	protected Map<String,String> fileMapping = new LinkedHashMap<String,String>();
	protected boolean renameMatrices = false;
	protected File resultDir = null;
	protected boolean randomScenarios = false;
	protected List<Map<String, String>> randomScenarioValues = new LinkedList<Map<String, String>>();
	
	protected List<String> headers;
	protected List<Integer> availableConfigRows = null;
	protected String generatedName = "";
	protected Set<String> rgParams = new HashSet<>();

	public static final Logger logger = LoggerFactory.getLogger(MatrixGenerator.class);

	public MatrixGenerator(List<File> templateFiles, List<File> dataFiles, File resultDir, boolean renameMatrices, 
			boolean randomScenarios, String matrixName)
	{
		this.templateFiles = templateFiles;
		this.resultDir = resultDir;
		this.dataFiles = dataFiles;
		this.renameMatrices = renameMatrices;
		this.randomScenarios = randomScenarios;
		this.generatedName = matrixName;
		outputParams = new HashMap<String,String>();
		currentTemplate = "";
		
	}
	
	
	public void setGeneratedName(String generatedName)
	{
		this.generatedName = generatedName;
	}
	
	

	public Map<String, String> getFileMapping()
	{
		return fileMapping;
	}


	public void setFileMapping(Map<String, String> fileMapping)
	{
		this.fileMapping = fileMapping;
	}

	

	public List<Map<String, String>> getRandomScenarioValues()
	{
		return randomScenarioValues;
	}


	public void setRandomScenarioValues(List<Map<String, String>> randomScenarioValues)
	{
		this.randomScenarioValues = randomScenarioValues;
	}


	protected Map<String,Block> retrieveBlocksAndTemplates(List<File> matrixPatterns, List<MMTemplate> templates) 
			throws GenerateException
	{
		Map<String,Block> blocks = new HashMap<String,Block>();
		for(File matrixPattern : matrixPatterns)
		{
			MMTemplateReader templateReader = new MMTemplateReader(matrixPattern);
			templateReader.read(templates, blocks, initialData);
			
		}
		return blocks;
		
	}
	
	protected List<TemplateRow> processFile(String fName, List<TemplateRow> template, Map<String,Block> blocks) throws GenerateException
	{
		List<TemplateRow> processedRows = new LinkedList<TemplateRow>();
		
		
		try
		{
			processFileRecords( template, fName, blocks,  processedRows,"", null,"");
			logger.debug("OutputParams: "+outputParams);
			
		} catch (IOException e)
		{
			throw new GenerateException(e.getMessage(), e);
		} 
		return processedRows;
	}
	
	protected void processComment(TemplateRow row, MutableBoolean isConstant) throws GenerateException
	{
		String val0 = row.get(0);
		if (val0.equals(START_COMMENT + ActionGenerator.MATRIX_CONST_INDICATOR) 
				|| val0.equals(START_COMMENT + ActionGenerator.MATRIX_DESC_INDICATOR))
		{
			isConstant.setValue(true);
		}
		/*else if (line[0].equals(MATRIX_DESC_INDICATOR))
		{
			isConstant = false;
		}*/
		else
		{
			if (isConstant.booleanValue())
			{
				writeLine(writer, row, currentTemplate);
				return;
			}
		}
		writer.writeRow(row);
	}
	
	protected List<TemplateRow>  processFileRecords(List<TemplateRow> template, String templateName, Map<String,Block> blocks, List<TemplateRow> processedRows, String preIndex, CalledBlock curBlock,String extName) throws IOException, GenerateException
	{
		//List<String[]> result = new LinkedList<String[]> ();
		boolean insideInit = false;
		boolean hasRGColumn = false;
		//String rgColName = RGS;
		//int rgColumn = 0;
		TemplateRow headerRow = null;
		
		for (TemplateRow row : template)
		{
			try
			{
			List<String> valuesList = row.getRow();
			if (row == null || row.size() == 0)
			{
				processedRows.add(row);
				continue;
			}

			String col0 = row.get(0);
			if (StringUtils.isEmpty(col0))
			{
				processedRows.add(row);
				continue;
			}
			
			if (col0.equals(START_INIT))
				insideInit = true;
			else if (col0.equals(END_INIT))
				insideInit = false;
			
			else if (col0.startsWith(BLOCK))
			{
				Collection<CalledBlock> blockList = findBlocks(col0, blocks);
				for (CalledBlock block : blockList)
				{
					if (col0.endsWith("}"))
						col0 = col0.substring(0, col0.length()-1);
					String[] params = Functions.parsParams(col0);
					String externalName = params[1];
					block.setExternalName(externalName);
					processFileRecords(block.getRows(), templateName, blocks, processedRows, block.getName()+"_", block,externalName);
				}
				continue;
			}
			
			
			
			
			if (!insideInit && !keyNames.contains(col0) && !col0.startsWith(START_COMMENT))
			{
				if (col0.equalsIgnoreCase(ID))
				{
					processedRows.add(row);
					headerRow = row;
					hasRGColumn = hasRgColumn(valuesList);
					continue;
					//started means the pointer is inside matrix
				}
				if (!col0.startsWith("#"))
				{
					replaceIdColumn(valuesList, preIndex, curBlock,  col0);
				}
				
				if (hasRGColumn)						
				{
					//hasRGColumn = false;
					for (int i = 0; i < headerRow.size(); i++)
					{
						if (rgParams.contains(headerRow.get(i)))
							replaceValuesInRg(valuesList, i, preIndex);
					}
				}
				
				for (int i = 1; i < row.size(); i++) 
				{
					if (valuesList.get(i).contains(START_MATRIX_FORMULA))
					{
						String pref = preIndex;
						valuesList.set(i, processMatrixFieldWithFormula(valuesList.get(i), pref, curBlock != null));
					}
					
				}
				row.setHeader(headerRow);
			}
			processedRows.add(row);
			}
			catch(Exception e)
			{
				throw new GenerateException("Error while processing row #" + row.getRowNumber() + ", Temlate: " + templateName + ": " + e.getMessage(), e);
			}
		}
		return processedRows;
	}
	
	protected boolean hasRgColumn(List<String> headers)
	{
		for (String h : headers)
			if (rgParams.contains(h))
				return true;
		return false;
			
	}

	protected void replaceIdColumn(List<String> valuesList, String preIndex, CalledBlock curBlock, String col0)
	{
		String pref = preIndex;

		
		String newId = pref+col0;
		//if (addRgActions.contains(valuesList.get(ACTION_COLUMN).toUpperCase()))
			valuesList.set(0, "#{saveThisN,"+newId+",id#{"+AUTO_ID_INDEX+",++}}");
		//else
		//	valuesList.set(0, "#{saveThis,"+newId+",id_#{"+AUTO_ID_INDEX+",++}}");//#{saveThis,id_7,id_#{idIndex,++}}
		
		if (curBlock != null && curBlock.block.outputParams.containsValue(col0))
		{
			Entry<String,String> output = null;
			for (Entry<String,String> e : curBlock.block.outputParams.entrySet())
				if (e.getValue().equals(col0))
				{
					output = e;
					break;
				}
			curBlock.outputParams.put(output.getKey(), newId);
			outputParams.put(curBlock.externalName + "." + output.getKey(), newId);
		}
	}
	
	protected void replaceValuesInRg(List<String> valuesList, int rgColumn, String preIndex) throws GenerateException
	{
		if (valuesList.size() <= rgColumn || StringUtils.isEmpty(valuesList.get(rgColumn)))
			return;
		
		String rgValue = valuesList.get(rgColumn);
		
		if (rgValue.startsWith(START_MATRIX_FORMULA))
			return;
		
		if (!rgValue.startsWith("#") && rgValue.length()>0)
		{
			rgValue = rgValue.replace("'", "");
			String[] rgValues = rgValue.split(",");
			for (int i = 0; i<rgValues.length; i++)
			{
				String pref = preIndex;

				if (!notChangedActionIds.contains(rgValues[i]))
					rgValues[i] = pref+rgValues[i];
			}
			rgValue = StringUtils.join(rgValues,",");
			String newRgValue = rgValue.startsWith("'") ? rgValue : "'"+rgValue+"'";
			valuesList.set(rgColumn, "#{getSavedN,"+newRgValue+",','}");
		}
	}
	
	protected Collection<String> getNotChangedActionIds()
	{
		Set<String> set = new HashSet<>(MATRIX_JAVA_CLASSES);
		set.add(ActionExecutor.PARAMS_PREV_ACTION);
		set.add(ActionExecutor.PARAMS_THIS_ACTION);
		set.add(ActionExecutor.PARAMS_OUT);

		return set;
	}
	protected void preProcessTemplate(List<TemplateRow> processedFile) throws GenerateException, IOException
	{
		
	}
	
	protected Functions createFunctions()
	{
		return new Functions(initialData);
	}
	
	public void generate() throws GenerateException
	{
		functions = createFunctions();
		ListIterator<TemplateRow> it = null ;
		try
		{
			String matrixPatternName = templateFiles.get(0).getName();
			
			writer = new MMTemplateWriter(matrixPatternName, renameMatrices, resultDir, fileMapping);//createNewWriter(writer, resultFile);
			List<MMTemplate> templates = new ArrayList<MMTemplate>();
			Map<String,Block> blocks = retrieveBlocksAndTemplates(templateFiles, templates);
			
			initialData.put(AUTO_ID_INDEX, new ValueInitialData(AUTO_ID_INDEX,"0"));
						
			for(MMTemplate mmTemplate : templates)
			{
				try
				{
					processTemplate(mmTemplate, it, blocks, matrixPatternName, writer.getCurrentFile());
				}catch(GenerateException e)
				{
					throw e;
				}catch(Exception e)
				{
					throw new GenerateException(GenerateException.createTemplateExceptionMessage(
							mmTemplate.getTemplateName(), null, null, e));
				}
			}
			
		} catch (Exception e)
		{
			logger.error(e.getMessage(),e);
			throw new GenerateException(e.getMessage(),e);
		} finally
		{
			writer.close();
		}
	}
	
	protected void processTemplate(MMTemplate mmTemplate, ListIterator<TemplateRow> it, Map<String, Block> blocks,
			String matrixPatternName, File resultFile) throws GenerateException, IOException
	{
		currentTemplate = mmTemplate.getTemplateName();
		List<TemplateRow> processedFile = processFile(currentTemplate, mmTemplate.getTemplateRows(), blocks);// processFile(matrixPattern.getAbsolutePath(),
																												// blocks);
		MutableBoolean isConstant = new MutableBoolean(false);

		preProcessTemplate(processedFile);

		it = processedFile.listIterator();

		while (it.hasNext())
		{
			TemplateRow row = it.next();
			try
			{
				checkInterruption();
				processTemplateRow(mmTemplate, row, it, matrixPatternName, resultFile, isConstant);
			} catch (IOException e)
			{
				throw new GenerateException(GenerateException
						.createTemplateExceptionMessage(mmTemplate.getTemplateName(), row.getRowNumber(), null, e), e);
			} catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new GenerateException(e);
			}
		}
	}
	
	private void checkInterruption() throws InterruptedException
	{
		if (Thread.interrupted())
		{
			String msg = "Generation was interrupted";
			logger.debug(msg);
			throw new InterruptedException(msg);
		}
	}

	protected void afterWriteCycle(Cycle cycle, String matrixPatternName)
	{
		if (!randomScenarios)
			return;

		CsvInitialData data = (CsvInitialData) cycle.getCycleInitData();
		CsvInitialRow row = data.getCsvTable().get(0);
		int ind = matrixPatternName.lastIndexOf(ADD_NAME_DELIMITER);
		if (matrixPatternName.matches("(.)*_(\\d+)") && ind > 0)
			matrixPatternName = matrixPatternName.substring(0, matrixPatternName.lastIndexOf(ADD_NAME_DELIMITER) - 1);
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put(MatrixMakerFromConfigTool.TEMPLATE_NAME, matrixPatternName);
		List<String> randomScenarioParams = getRandomScenarioParams();
		if (randomScenarioParams != null)
		{
			for (String param : getRandomScenarioParams())
				values.put(param, row.containsKey(param) ? row.get(param) : "");
		}
		else
		{
			values.putAll(row.getRow());
		}

		randomScenarioValues.add(values);
	}
	

	protected List<String> getRandomScenarioParams()
	{
		return null;
	}


	public void GenerateAdditionalFiles() throws GenerateException
	{

		/*if (renameMatrices)
		{
			writeFileMapping(resultDir);
			//resultFilesCount++;
		}*/
		
		
		
	}
	
	protected void processTemplateRow(MMTemplate mmTemplate, TemplateRow row, ListIterator<TemplateRow> it, String matrixPatternName,
			File resultFile,MutableBoolean isConstant) throws IOException, GenerateException
	{
		List<String> values = row.getRow();
		
		if(values.size() == 0)
		{
			writer.writeRow((TemplateRow)null);
			return;
		}
		String val0 = values.get(0);
		
		if(val0 == null)
		{
			return;
		}
		if(val0.startsWith(START_COMMENT))
		{
			processComment( row,  isConstant);
			return;
		}
		if(StringUtils.equals(val0, START_INIT))
		{
			initialData.putAll(parsInitBlock(it, null, false));
			return;
		}
		if(StringUtils.equals(val0, START_CYCLE))
		{
			Cycle cycle = parsCycleBlock(it, false, mmTemplate);
			writeCycle(cycle, false, resultDir, matrixPatternName);
			afterWriteCycle(cycle, StringUtils.isEmpty(generatedName) ? generatedName : matrixPatternName);
			return;
		}
		
		if (StringUtils.startsWith(val0,NEW_FILE))
		{
				writer.startNewFile(generatedName, getAddName(val0));
				//resultFilesCount++;
			
			return;
		}

		if (StringUtils.equals(val0,"#{MatrixMakerTemplate}"))
		{
			return;
		}
		else if (StringUtils.startsWith(val0,BLOCK))
		{
			//write block
			return;
		}

		writeLine(writer, row, currentTemplate);
	}
	
	protected Cycle parsCycleBlock(ListIterator<TemplateRow> it, boolean nested, MMTemplate mmTemplate) throws  GenerateException
	{
		
		List<TemplateRow> lines = new ArrayList<TemplateRow>();
		Cycle cycle = new Cycle(mmTemplate);
		while(it.hasNext())
		{
			TemplateRow row = it.next();
			//String[] values = row.getRow();
			String val0 = row.get(0);
			if(val0 == null)
				continue;
			
			if (val0.startsWith(START_COMMENT))
			{
				lines.add(row);
				continue;
			}

			if(StringUtils.equals(val0, START_CYCLE))
			{
				Cycle nestedCycle = parsCycleBlock(it, true, mmTemplate);
				cycle.addNestedCycle(nestedCycle);
				TemplateRow r = new TemplateRow(new String[] {NESTED_CYCLE,String.valueOf(cycle.getNestedCycles().size()-1)}, mmTemplate.getTemplateName(), row.getRowNumber());
				lines.add(r);
				continue;
			}
			
			if (StringUtils.equals(val0, START_INIT))
			{
				parsInitBlock(it, cycle, nested);
				continue;
			}
			
			if (StringUtils.equals(val0, END_CYCLE))
				break;
			
			if (StringUtils.startsWith(val0,"#{saveThis"))
			{
				String[] pars = Functions.parsParams(val0);
				if (pars != null && pars.length > 1 && pars[1] != null)
					cycle.addCycleId(pars[1]);
			}
			
			lines.add(row);
		}
		cycle.setTemplateRows(lines);
		if (cycle.getCycleInitData() == null && !nested)
			throw new GenerateException("Cycle block does not contains init block");
		
		
		return cycle;
	}
	
	protected void clearIdLists(Set<String> cycleIds)
	{
		for (String id : cycleIds)
		{
			try
			{
				functions.clearId("", new String[] {id});
			} catch (GenerateException e)
			{
				logger.error("Cannot clear id=" + id);
			}
		}
	}

	protected void writeCycle(Cycle cycle, boolean nested, File resultDir, String matrixPatternName) throws IOException, GenerateException
	{
		clearIdLists(cycle.getCycleIds());
		MutableBoolean isConstant = new MutableBoolean(false);
		if(nested)
		{
			InitialData initialData = parsInitValue(cycle.getInitDataName(), cycle.getInitFormula());
			cycle.setCycleInitData(initialData);
			this.initialData.put(cycle.getInitDataName(), initialData);
		}
		else
			initialData.put(cycle.getInitDataName(), cycle.getCycleInitData());
		
		while(cycle.getCycleInitData().hasNext())
		{
			cycle.getCycleInitData().next();
			for(TemplateRow row : cycle.getTemplateRows())
			{
				try
				{
					if (row.size() >= 2 && StringUtils.equals(row.get(0), NESTED_CYCLE))
					{
						int cycleNumber = Integer.valueOf(row.get(1));
						writeCycle(cycle.getNestedCycles().get(cycleNumber), true, resultDir, matrixPatternName);
					}
					else
						processCycleRow(row, matrixPatternName, isConstant);
				}catch(GenerateException e)
				{
					throw e;
				}catch(Exception e)
				{
					throw new GenerateException(GenerateException.createTemplateExceptionMessage(
							cycle.getTemplate().getTemplateName(), row.getRowNumber(), null, e));
				}
			}
		}

		initialData.remove(cycle.getInitDataName());
	}
	
	protected void processCycleRow(TemplateRow row, String matrixPatternName, MutableBoolean isConstant) throws GenerateException
	{
		String val0 = row.get(0);
		if (StringUtils.startsWith(val0,START_COMMENT))
		{
			processComment( row,  isConstant);
			return;
		}
		if (StringUtils.startsWith(val0,NEW_FILE))
		{
			try
			{
				writer.startNewFile(generatedName, getAddName(val0));
			}
			catch (IOException e)
			{
				throw new GenerateException(e);
			}
			//resultFilesCount++;
		}
		else if (StringUtils.equals(val0,"#{MatrixMakerTemplate}"))
		{
			//ignore
			return;
		}
		else if (StringUtils.startsWith(val0,BLOCK))
		{
			//write block
			return;
		}
		else if (StringUtils.equals(val0,ID))
		{
			headers = row.getRow();
			writeLine(writer, row, matrixPatternName);
		}
		else
		writeLine(writer, row, matrixPatternName);
	}
	
	
	protected void writeLine(MMTemplateWriter writer, TemplateRow row, String templateName) throws  GenerateException
	{
		List<String> values = new ArrayList<String>();
		for(int i = 0; i < row.size(); i++)
		{
			try
			{
				values.add(processValue(row, i));
			}
			catch(Exception e)
			{
				throw new GenerateException(GenerateException.createTemplateExceptionMessage(templateName, row.getRowNumber(), i+1, e));
			}
		}
		if (!values.isEmpty() && !values.get(0).isEmpty())
		{
			writer.writeRow(values);
		}
	}
	
	protected String processValue(TemplateRow row, int colNumber) throws GenerateException
	{
		
			String value = row.get(colNumber);
			if(value.contains(Functions.FORMULA_START))
			{
				value = functions.generateValue(value);
			}
	
			return value;
		
	}

	protected  Map<String, InitialData> parsInitBlock(ListIterator<TemplateRow> it, Cycle cycle, boolean nestedCycle) throws  GenerateException
	{
		Map<String, InitialData> initData = new HashMap<String, InitialData>();
		while(it.hasNext())
		{
			TemplateRow row = it.next();
			List<String> values = row.getRow();
			if(values.size() == 0 || values.get(0) == null || values.get(0).startsWith(START_COMMENT))
				continue;
			String name = values.get(0);
			
			if(name.startsWith(END_INIT))
				return initData;
			if(values.size() == 1)
				throw new GenerateException("Init block must have 2 columns");
			
			String value = values.get(1);
			
			//if(StringUtils.isEmpty(name) || StringUtils.isEmpty(value))
			//	continue;
			if  (StringUtils.isEmpty(value))
				value = "";

			if(cycle == null || !nestedCycle)
			{
				InitialData init = parsInitValue(name, value);
				
				if(cycle != null)
				{
					cycle.setCycleInitData(init);
					cycle.setInitDataName(name);
				}
				initData.put(name, init);
			}else
			{
				cycle.setInitDataName(name);
				cycle.setInitFormula(value);
			}
		}
		throw new GenerateException("Did not find " + END_INIT);
	}
	
	protected InitialData parsInitValue(String name, String value) throws GenerateException
	{
		
		if ("NumberOfRepeats".equals(name))
			return new SelfInitialData(name,value);
		else if(!value.startsWith(Functions.FORMULA_START))
			return new ValueInitialData(name,value);
		
		String formula = value.substring(2, value.length()-1);
		if(formula.contains(Functions.FORMULA_START))
			formula = functions.generateValue(formula);
		String[] f = MMCsvUtils.splitLine(formula);
		String function = f[0];
		String[] params = new String[f.length-1];
		System.arraycopy( f, 1, params, 0, params.length);
		
		if(csvConfigs.contains(function) || CsvInitializer.isMapping(function))
		{
			return new CsvInitializer().initializeCsvConfig(name, function, params, dataFiles.toArray(new File[dataFiles.size()]), randomScenarios, availableConfigRows);
		}
		else if (StringUtils.equals(function, "list")) {
			return new ListInitialData(value, name, params);
		}
		
		else
		{
			try
			{
				value = functions.generateValue(value);
				return new ValueInitialData(name,value);
			}
			catch(GenerateException e)
			{
				throw new GenerateException(function + " is not a function or a variable",e);
			}
		}
	}
	
	protected Collection<CalledBlock> findBlocks(String expression, Map<String,Block> blocks) throws GenerateException
	{
		String[] params = Functions.parsParams(expression.substring(0,expression.length()-1));
		if (params.length < 3)
			throw new GenerateException("Expression "+ expression+ " has invalid parameter names");
		//String extName = params[1];
		String name = params[2];
		String[] nameArr = name.split("\\.");
		Collection<Block> blockList = Block.findBlocksByName(blocks, nameArr, 0);
		List<CalledBlock> result = new LinkedList<CalledBlock>();
		
		//Map<String, InitialData> initData = new HashMap<String, InitialData>();
		Map<String,String> pars = new HashMap<String,String>();
		
		if (params.length > 3)
			for (int i = 3; i<params.length; i++)
			{
				String param = params[i];
				String[] paramArr = param.split("=");
				if (paramArr.length < 2)
					throw new GenerateException("Parameter "+ param+ " doesn't have a value");
				String parVal = paramArr[1].trim();
				
				if (outputParams.containsKey(parVal))
					parVal = outputParams.get(parVal);
				pars.put(paramArr[0].trim(),parVal);
				
			}

		for (Block block : blockList){
			CalledBlock b = new CalledBlock(block);
			b.parseInit(functions, pars, 0);
			result.add(b);
		}
		
		return result;
	}
	
	protected String getAddName(String val) throws GenerateException
	{
		if (val.equals(NEW_FILE+"}"))
			return null;
		else return functions.generateValue(val);
	}
	
	public String processMatrixFieldWithFormula(String text, String pref,  boolean insideBlock)
	{
		String result = text;
		// TODO use StringBuilder for 'result'
		int startFormulaInd = result.indexOf(START_MATRIX_FORMULA);
		int endFormulaInd = startFormulaInd;
		List<Integer> quoteIndexes = new ArrayList<Integer>();
		
		while(true)
		{
			endFormulaInd = findFormulaEnd(result, startFormulaInd, endFormulaInd, quoteIndexes);
			
			if (endFormulaInd > result.length() || endFormulaInd <= 0 || startFormulaInd == endFormulaInd)
			{
				logger.warn("Expression {} is incorrect", text);
				return text;
			}
			
			String formula = result.substring(startFormulaInd, endFormulaInd);
			String formulaEdited = replaceIdInFormula(formula, quoteIndexes, startFormulaInd, pref, insideBlock);
			
			result = result.substring(0, startFormulaInd) + formulaEdited + result.substring(endFormulaInd);
			
			startFormulaInd = result.indexOf(START_MATRIX_FORMULA, startFormulaInd + formulaEdited.length());
			if (startFormulaInd < 0)
				break;
			endFormulaInd = startFormulaInd;
		}
		return result;
	}
	
	protected int findFormulaEnd(String text, int startFormulaInd, int endFormulaInd, List<Integer> quoteIndexes)
	{
		int openBrace = 0, closeBrace = 0;
		char[] chars = text.toCharArray();
		for (int i = startFormulaInd + 1; i < text.length(); i++)
		{
			if (chars[i] == '{')
				openBrace++;
			else if (chars[i] == '}')
				closeBrace++;
			else if (chars[i] == '\'')
				quoteIndexes.add(i);
			if (openBrace == closeBrace)
			{
				endFormulaInd = i + 1;
				break;
			}
		}
		return endFormulaInd;
	}
	
	protected String replaceIdInFormula(String formula, List<Integer> quoteIndexes, int startFormulaInd, 
			String pref,  boolean insideBlock)
	{
		StringBuilder formulaEdited = new StringBuilder();
		int startGroup = 0;
		
		Matcher m = idPattern.matcher(formula);
		
		
		while (m.find())
		{
			String group = m.group();
			int groupStart = m.start();
			if (groupStart < startGroup)
				continue;

			if(isStringMethod(m.group(2)))
				continue;
			boolean insideText = false;
			
			for (int i = 0; i < quoteIndexes.size(); i+=2)
				if (groupStart > quoteIndexes.get(i) - startFormulaInd && groupStart < quoteIndexes.get(i+1) - startFormulaInd)
				{
					insideText = true;
					break;
				}
			
			if (insideText)
				continue;

			String id = m.group(1);
			if (notChangedActionIds.contains(id))
				continue;
			String curPref = pref;
			if (insideBlock && outputParams.containsValue(id))
				curPref = "";
			String stringForReplace = "#{getSaved,"+curPref+ id + "}";
			group = group.replaceFirst(id, stringForReplace);
			formulaEdited.append(formula, startGroup, m.start()).append(group);
			startGroup = m.end();
		}
		
		formulaEdited.append(formula.substring(startGroup));
		return formulaEdited.toString();
	}
	

	protected static boolean isStringMethod(String text)
	{
		return STRING_METHODS.contains(text);
	}
	
	
	public void initRGParams(Set<String> rgActions)
	{
		this.rgParams = new HashSet<>();
		this.rgParams.add(RGS);
		this.rgParams.addAll(rgActions);
	}
}
