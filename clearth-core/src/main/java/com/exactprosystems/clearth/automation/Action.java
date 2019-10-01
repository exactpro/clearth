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

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Pair;

public abstract class Action
{
	private static final Logger staticLogger = LoggerFactory.getLogger(Action.class);
	
	protected static final String PARAM_OUTPUTPARAMS = "OutputParams";

	protected LinkedHashMap<String, String> inputParams; // Matrix and default input parameters
	protected LinkedHashMap<String, String> outputParams;
	protected LinkedHashMap<String, SubActionData> subActionsData;
	protected LinkedHashMap<String, LinkedHashMap<String, String>> subOutputParams;
	protected Set<String> matrixInputParams;
	protected List<String> duplicateParams;
	protected Map<String, String> formulas;

	private List<String> cleanableContext;

	/**
	 * {@link #idInTemplate} field can be used in some template or matrix generator tool if you have one.
	 * Can be helpful to debug matrix.
	 */
	protected String idInMatrix, comment, name, idInTemplate;
	protected boolean executable, inverted, done, passed, suspendIfFailed;
	/**
	 * {@link #formulaIdInTemplate} same as {@link #idInTemplate} but it is set with formula.
	 */
	protected String formulaExecutable, formulaInverted, formulaComment, formulaTimeout,
			formulaAsync, formulaAsyncGroup, formulaWaitAsyncEnd, formulaIdInTemplate;

	protected long timeout;
	
	protected boolean async;
	protected String asyncGroup;
	protected WaitAsyncEnd waitAsyncEnd;
	
	private Result result;
	
	protected Matrix matrix;
	protected Step step;
	
	protected Date started, finished;

	protected Logger logger = null;
	
	protected final String uniqueId = UUID.randomUUID().toString();

	public Action()
	{
		this.inputParams = new LinkedHashMap<String, String>();
		this.cleanableContext = null;
		this.name = "Unnamed_" + Action.class.getSimpleName();
		this.logger = staticLogger;
	}


	public void preInit (Logger logger, String actionName, Map<String, String> mappingValues)
	{
		this.name = actionName;
		this.inputParams.putAll(mappingValues);
		this.logger = (logger != null) ? logger : staticLogger;
	}

	public void init(ActionSettings settings)
	{
		inputParams.putAll(settings.getParams());
		outputParams = null;
		subActionsData = null;
		subOutputParams = null;
		duplicateParams = settings.getDuplicateParams();
		matrixInputParams = settings.getMatrixInputParams();
		formulas = settings.getFormulas();

		idInMatrix = settings.getActionId();
		comment = settings.getComment();
		formulaComment = settings.getFormulaComment();
//		name = settings.getActionName();

		idInTemplate = settings.getIdInTemplate();
		formulaIdInTemplate = settings.getFormulaIdInTemplate();

		async = settings.isAsync();
		formulaAsync = settings.getFormulaAsync();
		asyncGroup = settings.getAsyncGroup();
		formulaAsyncGroup = settings.getFormulaAsyncGroup();
		waitAsyncEnd = settings.getWaitAsyncEnd();
		formulaWaitAsyncEnd = settings.getFormulaWaitAsyncEnd();
		
		step = settings.getStep();
		executable = settings.isExecutable();
		formulaExecutable = settings.getFormulaExecutable();
		inverted = settings.isInverted();
		formulaInverted = settings.getFormulaInverted();
		done = false;
		passed = false;
		timeout = settings.getTimeout();
		formulaTimeout = settings.getFormulaTimeout();
		suspendIfFailed = settings.isSuspendIfFailed();
		
		result = null;
		
		matrix = settings.getMatrix();
		started = null;
		finished = null;
	}
	
	public void dispose()
	{
		outputParams = null;
		subOutputParams = null;
		duplicateParams = null;
		formulas = null;
	}
	
	
	public boolean isSubaction()
	{
		return false;
	}

	
	/**
	 * Implement this method to define action logic
	 * @param stepContext stores data available for other actions in step and same matrix
	 * @param matrixContext stores data available for other actions in matrix
	 * @param globalContext stores data available for all actions in all matrices
	 */
	protected abstract Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException;
	
	/**
	 * Executes action and returns its result. Result IS NOT STORED within action
	 * @param stepContext stores data available for other actions in step and same matrix
	 * @param matrixContext stores data available for other actions in matrix
	 * @param globalContext stores data available for all actions in all matrices
	 * @return execution result
	 * @throws FailoverException
	 */
	public final Result executeForResult(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws FailoverException
	{
		Result result = null;
		try
		{
			result = run(stepContext, matrixContext, globalContext);
			if (result != null && result.getError() != null) {
				logger.debug("Result with cause: " + result.getComment(), result.getError());
			}
		}
		catch (ResultException e)
		{
			result = e.getResult();
			if (result.getError() != null) {
				logger.warn("ResultException with cause: " + result.getComment(), result.getError());
			} else {
				logger.warn("ResultException without cause: " + result.getComment());
			}
		}
		catch (Exception e)
		{
			if (e instanceof FailoverException)
				throw (FailoverException)e;
			result = DefaultResult.failed(e);
			logger.error("Action failed with exception", e);

		}
		return result;
	}
	
	/**
	 * Executes action and returns its result. Result is stored within action
	 * @param stepContext stores data available for other actions in step and same matrix
	 * @param matrixContext stores data available for other actions in matrix
	 * @param globalContext stores data available for all actions in all matrices
	 * @return execution result
	 * @throws FailoverException
	 */
	public final Result execute(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws FailoverException
	{
		result = executeForResult(stepContext, matrixContext, globalContext);
		return result;
	}
	
	public Result getResult()
	{
		return this.result;
	}
	
	public void setResult(Result result)
	{
		this.result = result;
	}
	
	
	/**
	 * Returns map of input parameters as <Key, Value>
	 * @return map of input parameters
	 */
	public LinkedHashMap<String, String> getInputParams()
	{
		return inputParams;
	}
	
	public String getInputParam(String name)
	{
		if (getInputParams()==null)
			return null;
		return getInputParams().get(name);
	}
	
	public String getInputParam(String name, String defaultValue)
	{
		String result = getInputParam(name);
		if (result==null)
			return defaultValue;
		return result;
	}

	public void setMatrixInputParams(Set<String> matrixInputParams)
	{
		this.matrixInputParams = matrixInputParams;
	}

	public Set<String> getMatrixInputParams()
	{
		return matrixInputParams;
	}
	
	public Map<String, String> copyInputParams()
	{
		return inputParams == null ? null : new LinkedHashMap<String, String>(inputParams);
	}
	
	
	/**
	 * Returns map of output parameters as <Key, Value>
	 * @return map of output parameters
	 */
	public LinkedHashMap<String, String> getOutputParams()
	{
		return outputParams;
	}
	
	public void setOutputParams(LinkedHashMap<String, String> outputParams)
	{
		this.outputParams = outputParams;
	}
	
	
	public LinkedHashMap<String, SubActionData> getSubActionData()
	{
		return subActionsData;
	}
	
	public void setSubActionsData(LinkedHashMap<String, SubActionData> subActionsData)
	{
		this.subActionsData = subActionsData;
	}
	
	
	public LinkedHashMap<String, LinkedHashMap<String, String>> getSubOutputParams()
	{
		return subOutputParams;
	}
	
	public void setSubOutputParams(LinkedHashMap<String, LinkedHashMap<String, String>> subOutputParams)
	{
		this.subOutputParams = subOutputParams;
	}
	
	
	public List<String> getDuplicateParams()
	{
		return duplicateParams;
	}
	
	public Map<String, String> getFormulas()
	{
		return formulas;
	}
	
	public String getIdInMatrix()
	{
		return idInMatrix;
	}
	
	public String getUniqueId()
	{
		return uniqueId;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	public String getComment()
	{
		return this.comment;
	}
	
	public String getFormulaComment()
	{
		return formulaComment;
	}
	
	
	public void setAsync(boolean async)
	{
		this.async = async;
	}
	
	public boolean isAsync()
	{
		return async;
	}
	
	public String getFormulaAsync()
	{
		return formulaAsync;
	}

	
	public void setAsyncGroup(String asyncGroup)
	{
		this.asyncGroup = asyncGroup;
	}
	
	public String getAsyncGroup()
	{
		return asyncGroup;
	}
	
	public String getFormulaAsyncGroup()
	{
		return formulaAsyncGroup;
	}
	

	public void setWaitAsyncEnd(WaitAsyncEnd waitAsyncEnd)
	{
		this.waitAsyncEnd = waitAsyncEnd;
	}

	public WaitAsyncEnd getWaitAsyncEnd()
	{
		return waitAsyncEnd;
	}
	
	public String getFormulaWaitAsyncEnd()
	{
		return formulaWaitAsyncEnd;
	}

	
	public String getName()
	{
		return name;
	}
	
	public String getStepName()
	{
		return step != null ? step.getName() : null;
	}
	
	public String getStepKind()
	{
		return step != null ? step.getKind() : null;
	}
	
	public void setExecutable(boolean executable)
	{
		this.executable = executable;
	}
	
	public boolean isExecutable()
	{
		return executable;
	}

	public String getFormulaExecutable()
	{
		return formulaExecutable;
	}

	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
	}
	
	public boolean isInverted()
	{
		return inverted;
	}

	public String getFormulaInverted()
	{
		return formulaInverted;
	}
	
	public boolean isDone()
	{
		return done;
	}
	
	public void setDone(boolean done)
	{
		this.done = done;
	}

	public String getIdInTemplate()
	{
		return idInTemplate;
	}

	public void setIdInTemplate(String idInTemplate)
	{
		this.idInTemplate = idInTemplate;
	}

	public String getFormulaIdInTemplate()
	{
		return formulaIdInTemplate;
	}

	public void setFormulaIdInTemplate(String formulaIdInTemplate)
	{
		this.formulaIdInTemplate = formulaIdInTemplate;
	}

	public boolean isPassed()
	{
		return passed;
	}
	
	public void setPassed(boolean passed)
	{
		this.passed = passed;
	}
	
	public void setTimeOut(long timeout)
	{
		this.timeout = timeout;
	}

	public String getFormulaTimeout()
	{
		return formulaTimeout;
	}
	
	public long getTimeOut()
	{
		return this.timeout;
	}

	/**
	 * Defines step kinds expected for this class of action, null means "any step kind".
	 */
	public Set<String> getExpectedStepKinds()
	{
		return null;
	}

	public Step getStep()
	{
		return step;
	}
	
	public Matrix getMatrix()
	{
		return matrix;
	}

	public void setMatrix(Matrix matrix)
	{
		this.matrix = matrix;
	}
	
	
	public Date getStarted()
	{
		return started;
	}

	public void setStarted(Date started)
	{
		this.started = started;
		if ((matrix!=null) && (matrix.getStarted()==null))
			matrix.setStarted(started);
	}
	

	public Date getFinished()
	{
		return finished;
	}

	public void setFinished(Date finished)
	{
		this.finished = finished;
	}

	public boolean isSuspendIfFailed() {
		return suspendIfFailed;
	}

	public void setSuspendIfFailed(boolean suspendIfFailed) {
		this.suspendIfFailed = suspendIfFailed;
	}

	public void addOutputParam(String name, String value)
	{
		if (outputParams==null)
			outputParams = new LinkedHashMap<String, String>();
		outputParams.put(name, value);
	}
	
	public void addSubActionData(String subActionId, SubActionData subActionData)
	{
		if (this.subActionsData==null)
			this.subActionsData = new LinkedHashMap<String, SubActionData>();
		this.subActionsData.put(subActionId, subActionData);
	}
	
	public void addSubOutputParams(String subActionId, LinkedHashMap<String, String> subParams)
	{
		if (this.subOutputParams==null)
			this.subOutputParams = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		this.subOutputParams.put(subActionId, subParams);
	}
	
	
	public int getActionType()
	{
		return ActionType.NONE;
	}
	
	
	@Override
	public String toString()
	{
		LineBuilder lb = new LineBuilder();
		lb.append("Id in matrix: " + this.idInMatrix);
		lb.append("Unique id: " + this.uniqueId);
		lb.append("Name: " + this.name);
		lb.append("Step name: " + getStepName());
		lb.append("Step kind: " + getStepKind());
		lb.append("Comment: " + this.comment);
		lb.append("Timeout: " + this.timeout);
		lb.append("Matrix: " + this.matrix.getName());
		lb.append("Parameters: ");
		for (String key : inputParams.keySet())
			lb.append("  " + key + "=" + inputParams.get(key));
		lb.append("Result: ");
		if (result == null)
			lb.append("null");
		else
			result.toLineBuilder(lb, " ");
		return lb.toString();
	}
	
	public boolean duplicateParamsDisabled()
	{
		return true;
	}

	protected final Logger getLogger() {
		return logger;
	}

	public List<String> getCleanableContext() {
		return cleanableContext;
	}

	public void addCleanableContext(String name) {
		if (cleanableContext == null) {
			cleanableContext = new ArrayList<String>();
		}
		cleanableContext.add(name);
	}
	
	protected List<Pair<String, String>> getOutputFields() throws ResultException 
	{
		String outputParamsList = getInputParam(PARAM_OUTPUTPARAMS);
		if (StringUtils.isEmpty(outputParamsList))
			return null;
		
		List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
		String[] params = outputParamsList.split(",");
		for (String n : params)
		{
			String[] names = n.split("\\|");
			result.add(new Pair<String, String>(names[0], names.length>1 ? names[1] : names[0]));
		}
		return result;
	}

	/**
	 * Tests whether scheduler execution has been interrupted by user 
	 * and throws ResultException with specified error message.
	 * 
	 * If scheduler is stopped, the interrupted flag of the thread is set.
	 * The interrupted flag is unaffected by this method.
	 * 
	 * This method can be used in long-running actions during processing of big amount of data.
	 * 
	 * @param errorMessage message for reports.
	 * @throws ResultException If scheduler has been interrupted.
	 */
	protected void checkInterruption(String errorMessage) throws ResultException
	{
		if (Thread.currentThread().isInterrupted())
			throw ResultException.failed(errorMessage);
	}
	
	/**
	 * Tests whether scheduler execution has been interrupted by user and throws ResultException.
	 *
	 * If scheduler is stopped, the interrupted flag of the thread is set.
	 * The interrupted flag is unaffected by this method.
	 *
	 * This method can be used in long-running actions during processing of big amount of data.
	 *
	 * @throws ResultException If scheduler has been interrupted.
	 */
	protected void checkInterruption() throws ResultException
	{
		checkInterruption("Action execution has been interrupted.");
	}

	public String getDescForLog(String prefix)
	{
		String actionType = isAsync()?" asynchronous":"";
		String matrixName = getMatrix().getShortFileName();
		String actionComment = getComment() == null ? "" : ", comment: " + getComment();
		
		return String.format(prefix + "%s action '%s' with ID '%s' from matrix '%s'%s",
						actionType, getName(), getIdInMatrix(), matrixName, actionComment);
	}
}
