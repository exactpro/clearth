/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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
import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.generator.ActionReader;
import com.exactprosystems.clearth.automation.generator.CsvActionReader;
import com.exactprosystems.clearth.automation.generator.XlsActionReader;
import com.exactprosystems.clearth.utils.*;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

public abstract class ActionGenerator
{
	// CSV properties
	public static final char DELIMITER = ',', TEXT_QUALIFIER = '"';
	public static final String HEADER_DELIMITER = "#", PREFIX = "_", COMMENT_INDICATOR = "//",
			MATRIX_DESC_INDICATOR = "Description:",
			MATRIX_CONST_INDICATOR = "Constants:";

	// Column names
	public static final String COLUMN_ID = "id", COLUMN_GLOBALSTEP = "globalstep", COLUMN_ACTION = "action",
			COLUMN_EXECUTE = "execute", COLUMN_TIMEOUT = "timeout", COLUMN_COMMENT = "comment",
			COLUMN_INVERT = "invert", COLUMN_SUSPEND_FAILED = "suspendiffailed",
			COLUMN_ASYNC = "async", COLUMN_ASYNCGROUP = "asyncgroup", COLUMN_WAITASYNCEND = "waitasyncend",
			COLUMN_ID_IN_TEMPLATE = "idintemplate", COLUMN_WAITASYNCENDSTEP = "waitasyncendstep";

	// initAction results
	public static final int NO_ERROR = 0, CHECKING_ERROR = 1, INIT_ERROR = 2;

	private static final String SPACES = " \t\r\n";

	private final Map<String, Step> steps;
	private final List<Matrix> matrices;
	protected Map<String, Preparable> preparableActions;
	private String matrixStepName;
	protected final StringCache stringCache = new StringCache(1_000_000, 500);
	

	public ActionGenerator(Map<String, Step> steps, List<Matrix> matrices, Map<String, Preparable> preparableActions)
	{
		this.steps = steps;
		for (String stepName : steps.keySet())
			steps.get(stepName).clearActions();
		this.matrices = matrices;
		this.matrices.clear();
		this.preparableActions = preparableActions;
		
	}
	
	protected abstract Logger getLogger();

	public static Map<String, ActionMetaData> loadActionsMapping(boolean actionNameToLowerCase, Logger logger)
	{
		return loadActionsMapping(ClearThCore.rootRelative(ClearThCore.configFiles().getActionsMappingFileName()),
				actionNameToLowerCase, logger);
	}
	
	public static Map<String, ActionMetaData> loadActionsMapping(String fileName, boolean actionNameToLowerCase, Logger logger)
	{
		Map<String, String> actions = KeyValueUtils.loadKeyValueFile(fileName, false);
		Map<String, ActionMetaData> descriptions = new LinkedHashMap<String, ActionMetaData>();
		ACTION_LOOP:
		for (Map.Entry<String, String> actionEntry: actions.entrySet())
		{
			String name = actionEntry.getKey();
			String data = actionEntry.getValue().trim(); //class + default input parameters
			String clazz = null;
			String defaultInputParamsStr = null;

			// Split action class and default input parameters
			int bracketPos = data.indexOf('(');
			if (bracketPos >= 0)
			{
				clazz = data.substring(0, bracketPos).trim();
				defaultInputParamsStr = data.substring(bracketPos,data.length()).trim();
				if (defaultInputParamsStr.charAt(defaultInputParamsStr.length()-1) != ')')
					defaultInputParamsStr = defaultInputParamsStr.substring(1);
				else
					defaultInputParamsStr = defaultInputParamsStr.substring(1, defaultInputParamsStr.length() - 1);
			}
			else
			{
				clazz = data;
			}

			// Replace references %ref%
			while (clazz.contains("%"))
			{
				int start = clazz.indexOf("%") + 1;
				int end = clazz.indexOf("%", start);
				if (end<0)
				{
					logger.warn("Action mapping contains unclosed reference: '"+clazz+"'. Reference should be closed with %");
					continue ACTION_LOOP;
				}

				String ref = clazz.substring(start, end);
				String refValue = actions.get(ref);
				if (refValue == null)
				{
					logger.warn("Action mapping contains unsolvable reference: '"+ref+"'. Its value should be declared before referencing it");
					continue ACTION_LOOP;
				}

				clazz = clazz.replace("%"+ref+"%", refValue);
			}

			// Build action description
			ActionMetaData metaData = new ActionMetaData(name, clazz);
			LinkedHashMap<String, String> defaultInputParams = parseDefaultInputParams(defaultInputParamsStr, name, logger);
			metaData.addDefaultInputParams(defaultInputParams);
			descriptions.put(actionNameToLowerCase ? name.toLowerCase() : name, metaData);
		}

		return descriptions;
	}

	static LinkedHashMap<String, String> parseDefaultInputParams(String line, String actionName, Logger logger)
	{
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		if (StringUtils.isEmpty(line))
			return params;
		line = line.trim();
		int start = 0, cur = 0;
		try
		{
			while (cur != line.length())
			{
				// Skip whitespaces
				while (SPACES.indexOf(line.charAt(cur)) >= 0)
					cur++;
				start = cur;
				
				// Parse parameter name
				while (SPACES.indexOf(line.charAt(cur)) < 0 && line.charAt(cur) != '=')
					cur++;
				String name = line.substring(start, cur);
				
				// Skip whitespaces and =
				while (SPACES.indexOf(line.charAt(cur)) >= 0)
					cur++;
				if (line.charAt(cur) != '=')
				{
					logger.warn("Error while parsing default input parameters for '" + actionName + "' action: " + line);
					return params;
				}
				cur++;
				while (SPACES.indexOf(line.charAt(cur)) >= 0)
					cur++;
				start = cur;
				
				// Parse parameter value
				String value;
				if (line.charAt(cur) == '\"') // When value is quoted:
																			// name="Quoted name"
				{
					cur++;
					while (line.charAt(cur) != '\"')
					{
						if (line.charAt(cur) == '\\') // Skip 2 characters for escape
																					// characters
							cur++;
						cur++;
					}
					value = line.substring(start + 1, cur);
					
					// Replace escape characters
					if (value.contains("\\"))
					{
						value = value.replace("\\\\", "\\").
								replace("\\\"", "\"").
								replace("\\t", "\t").
								replace("\\n", "\n").
								replace("\\r", "\r");
					}
					
					while (cur != line.length() && line.charAt(cur) != ',')
						cur++;
					if (cur != line.length())
						cur++;
				}
				else
				// Simple value: name=SimpleName
				{
					while (cur != line.length() && line.charAt(cur) != ',')
						cur++;
					value = line.substring(start, cur).trim();
					if (cur != line.length())
						cur++;
				}
				
				params.put(name, value);
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			logger.warn("Error while loading '" + actionName + "' action: unexpected end of parameter's list: " + line);
			return params;
		}
		return params;
	}

	protected abstract boolean customSetting(String name, String value, ActionSettings settings, int headerLineNumber, int lineNumber);
	protected abstract int initAction(Action action, ActionSettings settings, int headerLineNumber, int lineNumber);
	protected abstract boolean checkAction(Action action, int headerLineNumber, int lineNumber);

	/**
	 * @param onlyCheck if true action won't be generated, only validation will be performed
	 * @return true if no issues were found
	 */
	private boolean generateAction(Matrix matrix,
								   List<String> header,
								   List<String> values,
								   int headerLineNumber,
								   int lineNumber,
								   Set<String> usedIDs,
								   boolean onlyCheck)
	{
		ActionSettings actionSettings = ClearThCore.getInstance().getActionFactory().createActionSettings();
		actionSettings.setMatrix(matrix);

		boolean missingValues = false, allSuccessful = true;

		if (hasExcessiveValues(matrix, header, values, lineNumber)) {
			allSuccessful = false;
			missingValues = true;
		}

		allSuccessful &= initActionSettings(actionSettings, matrix, lineNumber, header, values, headerLineNumber,
				missingValues, onlyCheck);
		allSuccessful &= checkActionSettings(actionSettings, matrix, lineNumber, usedIDs);

		if (!onlyCheck) {
			allSuccessful &= createActionInstance(actionSettings, matrix, lineNumber, headerLineNumber);
		}

		return allSuccessful;
	}

	private boolean hasExcessiveValues(Matrix matrix, List<String> header, List<String> values, int lineNumber) {
		if (values.size() > header.size())
		{
			String message = "Action (line "+lineNumber+"): "+header.size()+" columns in header, "+values.size()+" columns in values. Excessive values ignored.";
			getLogger().warn(message);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.EXCESSIVE_VALUES, message);
			return true;
		}
		return false;
	}

	protected boolean initActionSettings(ActionSettings actionSettings,
									   Matrix matrix,
									   int lineNumber,
									   List<String> header,
									   List<String> values,
									   int headerLineNumber,
									   boolean missingValues,
	                                   boolean onlyCheck)
	{
		boolean allSuccessful = true;
		Logger logger = getLogger();

		for (int i = 0; i < header.size(); i++)
		{
			String head = header.get(i).trim(), 
					value = i < values.size() ? values.get(i) : null, 
					headLow = head.toLowerCase(), 
					valueLow;
			
			if (value==null)
			{
				value = "";
				if (!missingValues)
				{
					missingValues = true;
					allSuccessful = false;
					String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+"): "+header.size()+" columns in header, "+values.size()+" columns in values. Considering missing values empty by default";
					logger.warn(message);
					matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.MISSING_VALUES, message);
				}
			}
			
			if (headLow.isEmpty() && !value.isEmpty())
			{
				missingValues = true;
				allSuccessful = false;
				String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+"): value '"+ value +"' doesn't have header in "+ (i+1) +" column. Excessive value ignored.";
				logger.warn(message);
				matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.MISSING_HEAD, message);
			}
			
			if (headLow.isEmpty())
				continue;
			
			valueLow = value.toLowerCase().trim();
			
			if (!onlyCheck)
			{
				head = stringCache.get(head);
				value = stringCache.get(value);
			}
			
			if (headLow.equals(COLUMN_ID))
			{
				actionSettings.setActionId(value);
				if (value.isEmpty())
				{
					allSuccessful = false;
					String message = "Action on line "+lineNumber+" has empty value in '"+COLUMN_ID+"' column, it's parameters can't be referenced in other actions";
					logger.warn(message);
					matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.MISSING_ID, message);
				} else if (ActionExecutor.PARAMS_PREV_ACTION.equals(value) || ActionExecutor.PARAMS_THIS_ACTION.equals(value)) {
					allSuccessful = false;
					String message = String.format("Action on line %s has reserved value '%s' in '%s' column, action won't be executed",
					                               lineNumber,
					                               ActionExecutor.PARAMS_PREV_ACTION.equals(value) ? ActionExecutor.PARAMS_PREV_ACTION : ActionExecutor.PARAMS_THIS_ACTION,
					                               COLUMN_ID);
					logger.warn(message);
					matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.INVALID_ACTION_ID, message);
					return allSuccessful;
				}
			}
			else if (headLow.equals(COLUMN_GLOBALSTEP))
			{
				if (StringUtils.isEmpty(value))
				{
					allSuccessful = false;
					String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+") doesn't have '"+COLUMN_GLOBALSTEP+"' field, it won't be executed";
					logger.warn(message);
					matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.MISSING_GLOBALSTEP, message);
				}
				else
				{
					actionSettings.setStep(steps.get(value));
					actionSettings.setStepName(value);
					matrixStepName = value;
				}
			}
			else if (headLow.equals(COLUMN_EXECUTE))
			{
				if (InputParamsUtils.NO.contains(valueLow))
				{
					logger.trace("Action '"+actionSettings.getActionId()+"' won't be executed");
					actionSettings.setExecutable(false);
					matrix.getNonExecutableActions().add(actionSettings.getActionId());
				}
				else if (value.contains(MatrixFunctions.FORMULA_START))
				{
					actionSettings.setFormulaExecutable(value);
				}
			}
			else if (headLow.equals(COLUMN_INVERT))
			{
				if (InputParamsUtils.YES.contains(valueLow))
				{
					logger.trace("Result of action '"+actionSettings.getActionId()+"' will be inverted");
					actionSettings.setInverted(true);
				}
				else if (value.contains(MatrixFunctions.FORMULA_START))
				{
					actionSettings.setFormulaInverted(value);
				}
			}
			else if (headLow.equals(COLUMN_TIMEOUT))
			{
				try
				{
					actionSettings.setTimeout(Integer.parseInt(value));
				}
				catch (Exception e)
				{
					if (value.contains(MatrixFunctions.FORMULA_START))
					{
						actionSettings.setFormulaTimeout(value);
					}
					else
					{
						actionSettings.setTimeout(0);
						allSuccessful = false;
						String message = "Action '" + actionSettings.getActionId() + "' (line " + lineNumber + "): invalid timeout value - '" + value + "'. Set to " + actionSettings.getTimeout() + " by default";
						logger.warn(message);
						matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.INVALID_TIMEOUT, message);
					}
				}
			}
			else if (headLow.equals(COLUMN_ACTION) && actionSettings.getActionName() == null)
			{
				actionSettings.setActionName(value);
			}
			else if (headLow.equals(COLUMN_COMMENT))
			{
				if(value.contains(MatrixFunctions.FORMULA_START))
				{
					actionSettings.setFormulaComment(value);
				}
				else
					actionSettings.setComment(value);
			}
			else if (headLow.equals(COLUMN_SUSPEND_FAILED))
			{
				actionSettings.setSuspendIfFailed(InputParamsUtils.YES.contains(valueLow));
			}
			else if (headLow.equals(COLUMN_ASYNC))
			{
				if (value.contains(MatrixFunctions.FORMULA_START))
					actionSettings.setFormulaAsync(value);
				else
					actionSettings.setAsync(InputParamsUtils.YES.contains(valueLow));
			}
			else if (headLow.equals(COLUMN_ASYNCGROUP))
			{
				if (value.contains(MatrixFunctions.FORMULA_START))
					actionSettings.setFormulaAsyncGroup(value);
				else
					actionSettings.setAsyncGroup(value);
			}
			else if (headLow.equals(COLUMN_WAITASYNCEND))
			{
				if (value.contains(MatrixFunctions.FORMULA_START))
					actionSettings.setFormulaWaitAsyncEnd(value);
				else
					actionSettings.setWaitAsyncEnd(WaitAsyncEnd.byLabel(value));
			}
			else if (headLow.equals(COLUMN_WAITASYNCENDSTEP))
			{
				if(StringUtils.isNotBlank(value))
				{
					if (steps.containsKey(value))
						actionSettings.setWaitAsyncEndStep(value);
					else
					{
						allSuccessful = false;
						String message = "Action '" + actionSettings.getActionId() + "' (line " + lineNumber + ") has nonexistent step '" + value + "' in '" + COLUMN_WAITASYNCENDSTEP + "' field, it will be ignored";
						logger.warn(message);
						matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.NONEXISTENT_GLOBALSTEP, message);
					}
				}
			}
			else if (headLow.equals(COLUMN_ID_IN_TEMPLATE))
			{
				if (value.contains(MatrixFunctions.FORMULA_START))
					actionSettings.setFormulaIdInTemplate(value);
				else
					actionSettings.setIdInTemplate(value);
			}
			else if (!customSetting(headLow, value, actionSettings, headerLineNumber, lineNumber))
			{
				if ((actionSettings.getParams()!=null) && (actionSettings.getParams().containsKey(head)))
					actionSettings.addDuplicateParam(head);
				actionSettings.addParam(head, value);
			}
		}

		return allSuccessful;
	}

	private boolean checkActionSettings(ActionSettings actionSettings,
										Matrix matrix,
										int lineNumber,
										Set<String> usedIDs)
	{
		boolean allSuccessful = true;
		Logger logger = getLogger();

		if (actionSettings.getDuplicateParams()!=null)
		{
			String message = String.format("Action '%s' (line %d) contains duplicate parameters: %s.",
					actionSettings.getActionId(), lineNumber, StringUtils.join(actionSettings.getDuplicateParams(), ", "));
			logger.warn(message);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.DUPLICATE_PARAMS, message);
		}

		if (actionSettings.getActionId() == null)
		{
			allSuccessful = false;
			String message = "Action from line "+lineNumber+" doesn't have '"+COLUMN_ID+"' field, it can't be referenced from other actions";
			logger.warn(message);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.MISSING_ID, message);

			actionSettings.setActionId("");
		}
		else
		{
			if (usedIDs.contains(actionSettings.getActionId()))
			{
				allSuccessful = false;
				String message = "Id of action '" + actionSettings.getActionId() +"' (line "+lineNumber+") duplicates the existing Id";
				logger.warn(message);
				matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.DUPLICATE_ID, message);
			}
			else
				usedIDs.add(actionSettings.getActionId());
		}

		if (actionSettings.getStep() == null) {
			allSuccessful = false;
			String message = "Action '" + actionSettings.getActionId() + "' (line " + lineNumber + ") is included in " + "nonexistent step '" + actionSettings.getStepName() + "', it won't be executed";
			logger.warn(message);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.NONEXISTENT_GLOBALSTEP, message);
		}

		String actionName = actionSettings.getActionName();
		if (!ClearThCore.getInstance().getActionFactory().isDefinedAction(actionName)) {
			allSuccessful = false;
			String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+") has unknown name '"+ actionName +"', it won't be executed";
			logger.warn(message);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.UNKNOWN_ACTION_TYPE, message);
		}

		return allSuccessful;
	}

	private boolean createActionInstance(ActionSettings actionSettings, Matrix matrix, int lineNumber, int headerLineNumber)
	{
		Logger logger = getLogger();
		boolean allSuccessful = true;

		Action action = null;
		try {
			String actionName = actionSettings.getActionName();
			if (actionName != null) {
				action = ClearThCore.getInstance().getActionFactory().createAction(actionName);
			}
		} catch (AutomationException e) {
			allSuccessful = false;
			String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+") cannot be instantiated. "
					+ "Probably, wrong class name is specified for this action. See log for details.";
			logger.warn(message, e);
			matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.ACTION_INSTANCE_ERROR, message);
		}

		if (action != null) {
			// Init default input parameters
			Map<String, String> matrixInputParams = actionSettings.getParams();
			LinkedHashMap<String, String> allParams = new LinkedHashMap<String, String>(matrixInputParams);
			actionSettings.setParams(allParams);
			actionSettings.setMatrixInputParams(matrixInputParams.keySet());

			int initActionResult = initAction(action, actionSettings, headerLineNumber, lineNumber);

			if (initActionResult == CHECKING_ERROR)
				allSuccessful = false;

			if (initActionResult == INIT_ERROR)
				logger.trace("Action '"+actionSettings.getActionId()+"' (line "+lineNumber+") rejected on initialization, it won't be executed");
			else
			{
				matrix.getActions().add(action);
				
				Step step = actionSettings.getStep();
				if (step!=null)
				{
					Set<String> kinds = action.getExpectedStepKinds();
					if (kinds != null && !kinds.contains(step.getKind()))
					{
						allSuccessful = false;
						String message = "Action '"+actionSettings.getActionId()+"' (line "+lineNumber+") is included in step " +
								"'"+matrixStepName+"' of unexpected kind '"+step.getKind()+"'. '"+action.getName()+
								"' action must be used within the following step kind(s): "+StringUtils.join(action.getExpectedStepKinds(), ", ");
						logger.warn(message);
						matrix.addGeneratorMessage(ActionGeneratorMessageType.WARNING, ActionGeneratorMessageKind.UNEXPECTED_STEP_KIND, message);
					}
					step.addAction(action);
					if (step.isExecutable()
							&& action.isExecutable()
							&& action instanceof Preparable
							&& preparableActions != null
							&& !preparableActions.containsKey(action.getName())
					)
						preparableActions.put(action.getName(), (Preparable)action);
				}
				logger.trace("Finished adding new action " + action.getClass());
				if (logger.isTraceEnabled() && action.getStep() != null) // this check reduces time for loading very big matrices
					logger.trace(action.toString());
			}
		}

		return allSuccessful;
	}

	/**
	 * Builds Matrix instance according to given data
	 * @param matrixData matrix settings including file path
	 * @return true if matrix has been generated without any errors or warnings
	 * @throws IOException if error occurred while reading matrix file
	 */
	public boolean build(MatrixData matrixData, boolean onlyCheck) throws IOException
	{
		Matrix matrix = createMatrix(matrixData);
		
		boolean allSuccessful = generateActions(matrix.getFileName(), matrixData.isTrim(), matrix, onlyCheck);
		
		MvelVariables vars = matrix.getMvelVars();
		MvelVarsCleaningTableBuilder cleaningTableBuilder = new MvelVarsCleaningTableBuilder();
		vars.setCleaningTable(cleaningTableBuilder.build(matrix, steps.keySet()));
		
		matrices.add(matrix);
		getLogger().debug(String.format("Matrix '%s' %s", matrixData.getFile().getCanonicalPath(), onlyCheck ? "checked" : "compiled"));
		return allSuccessful;
	}

	protected Matrix createMatrix(MatrixData matrixData) throws IOException {
		Matrix matrix = new Matrix();
		matrix.setName(matrixData.getName());
		String fileName = matrixData.getFile().getCanonicalPath();
		matrix.setFileName(fileName);
		matrix.setMatrixData(matrixData);

		return matrix;
	}

	/**
	 * @param onlyCheck if true action won't be generated, only validation will be performed
	 * @return true if no issues were found
	 */
	protected boolean generateActions(String fileName, boolean trim, Matrix matrix, boolean onlyCheck) throws IOException
	{
		String fileExtension = FilenameUtils.getExtension(fileName).toLowerCase();
		ActionReader reader;
		if (fileExtension.equals("csv"))
			reader = new CsvActionReader(fileName, trim);
		else if ((fileExtension.equals("xls")) || (fileExtension.equals("xlsx")))
			reader = new XlsActionReader(fileName, trim);
		else
		{
			matrix.addGeneratorMessage(ActionGeneratorMessageType.ERROR, ActionGeneratorMessageKind.UNSUPPORTED_FILE_EXTENSION,
					"Unsupported file extension '" + fileExtension + "'");
			getLogger().warn("Matrix file with unsupported extension '" + fileExtension + "' has been ignored");
			return false;
		}
		
		return generateActions(reader, matrix, onlyCheck);
	}

	/**
	 * @param onlyCheck if true action won't be generated, only validation will be performed
	 * @return true if no issues were found
	 */
	protected boolean generateActions(ActionReader reader, Matrix matrix, boolean onlyCheck) throws IOException
	{
		Logger logger = getLogger();
		logger.info("Compiling matrix '"+matrix.getFileName()+"'");
		boolean allSuccessful = true;
		List<String> header = null;
		Set<String> usedIDs = new HashSet<String>();
		StringBuilder description = new StringBuilder();
		try
		{
			int lineNumber = 0;
			int headerLineNumber = 0;
			boolean commentDesc = false;
			boolean commentConstant = false;
			while (reader.readNextLine())
			{
				lineNumber++;
				if (header == null)
				{
					if (reader.isCommentLine())
					{
						String commentLine = reader.getRawLine();
						commentLine = StringUtils.stripEnd(commentLine, String.valueOf(DELIMITER));
						commentLine = commentLine.substring(commentLine.indexOf(COMMENT_INDICATOR)+COMMENT_INDICATOR.length());
						if (commentLine.startsWith(MATRIX_DESC_INDICATOR))
						{
							commentDesc = true;
							commentConstant = false;
							continue;
						}
						if (commentLine.startsWith(MATRIX_CONST_INDICATOR))
						{
							commentConstant = true;
							commentDesc = false;
							continue;
						}
						
						if (commentDesc)
							description.append(commentLine).append(Utils.EOL);
						if (commentConstant)
						{
							Pair<String, String> constantKv = KeyValueUtils.parseKeyValueString(commentLine);
							if (StringUtils.isNotEmpty(constantKv.getFirst()))
								matrix.addConstant(constantKv.getFirst(), constantKv.getSecond());
						}
						continue;
					}
				}

				if (reader.isCommentLine())
					continue;
				
				if (reader.isHeaderLine())
				{
					commentDesc = false;
					commentConstant = false;
					
					header = reader.parseLine(true);
					headerLineNumber = lineNumber;
				}
				else
				{
					if (reader.isEmptyLine())
						continue;
					
					if (header!=null)
					{
						try
						{
							List<String> values = reader.parseLine(false);
							if (!generateAction(matrix, header, values, headerLineNumber, lineNumber, usedIDs, onlyCheck))
								allSuccessful = false;
						}
						catch (Exception e)
						{
							allSuccessful = false;
							String message = "Unexpected error occurred while generating action for line "+lineNumber;
							logger.warn(message, e);
							matrix.addGeneratorMessage(ActionGeneratorMessageType.ERROR, 
									ActionGeneratorMessageKind.UNEXPECTED_GENERATING_ERROR,
									message + ": " + ExceptionUtils.getDetailedMessage(e));
						}
					}
					else
					{
						allSuccessful = false;
						String message = "Header not defined for action in line "+lineNumber;
						logger.error(message);
						matrix.addGeneratorMessage(ActionGeneratorMessageType.ERROR, ActionGeneratorMessageKind.HEADER_NOT_DEFINED_FOR_ACTION, message);
					}
				}
			}
		}
		finally
		{
			Utils.closeResource(reader);
		}
		matrix.setDescription(description.toString());
		return allSuccessful;
	}

	public void createContextCleanData() {

		Logger logger = getLogger();

		Map<Matrix, Map<String, Action>> matrContextAction = new HashMap<Matrix, Map<String, Action>>();

		for (Step step : steps.values()) {
			logger.debug("Processing step: " + step.getName());
			if (!step.isExecute()) {
				logger.debug("Step isnt' executable. Skipping.");
			}

			for (Action action : step.getActions()) {
				if (!action.executable) {
					continue;
				}

				Map<String, Action> contextAction = matrContextAction.get(action.getMatrix());
				if (contextAction == null) {
					contextAction = new HashMap<String, Action>();
					matrContextAction.put(action.getMatrix(), contextAction);
				}
				if (action instanceof ContextWriter) {
					String[] names = ((ContextWriter) action).writtenContextNames();
					if (names != null) {
						for (String cname : names) {
							Action lastUsAction = contextAction.get(cname);
							if (lastUsAction != null) {
								logger.debug("Id: " + lastUsAction.getIdInMatrix() + " to clean: " + cname);
								lastUsAction.addCleanableContext(cname);
							}
							contextAction.put(cname, action);
						}
					}
				}
				if (action instanceof ContextReader) {
					String[] names = ((ContextReader) action).readContextNames();
					if (names != null) {
						for (String cname : names) {
							contextAction.put(cname, action);
						}
					}
				}
			}
		}
		logger.debug("Clean remained contexts");
		for (Map<String, Action> contextAction : matrContextAction.values()) {
			for (Map.Entry<String, Action> remainedContext : contextAction.entrySet()) {
				if (remainedContext.getValue() != null) {
					logger.debug("Id: " + remainedContext.getValue().getIdInMatrix() + " to clean: " + remainedContext.getKey());
					remainedContext.getValue().addCleanableContext(remainedContext.getKey());
				}
			}
		}
	}

	
	public void dispose()
	{
		stringCache.clear();
	}
}
