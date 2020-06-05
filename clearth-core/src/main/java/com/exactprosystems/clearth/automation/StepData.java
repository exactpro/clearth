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

package com.exactprosystems.clearth.automation;

import java.io.IOException;
import java.util.Date;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import com.exactprosystems.clearth.utils.BinaryConverter;
import com.exactprosystems.clearth.utils.DateTimeUtils;

public class StepData implements CsvDataManager
{
	private String name, kind, startAt;
	private boolean askForContinue, askIfFailed, execute;
	private Date started, finished;
	private ActionsExecutionProgress executionProgress = new ActionsExecutionProgress();


	public StepData()
	{
	}

	public StepData(CsvReader reader) throws IOException
	{
		assignFields(reader);
	}


	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getKind()
	{
		return kind;
	}

	public void setKind(String kind)
	{
		this.kind = kind;
	}

	public String getStartAt()
	{
		return startAt;
	}

	public void setStartAt(String startAt)
	{
		this.startAt = startAt;
	}

	public boolean isAskForContinue()
	{
		return askForContinue;
	}

	public void setAskForContinue(boolean askForContinue)
	{
		this.askForContinue = askForContinue;
	}

	public boolean isAskIfFailed()
	{
		return askIfFailed;
	}

	public void setAskIfFailed(boolean askIfFailed)
	{
		this.askIfFailed = askIfFailed;
	}

	public boolean isExecute()
	{
		return execute;
	}

	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}

	public Date getStarted()
	{
		return started;
	}

	public void setStarted(Date started)
	{
		this.started = started;
	}

	public Date getFinished()
	{
		return finished;
	}

	public void setFinished(Date finished)
	{
		this.finished = finished;
	}

	public ActionsExecutionProgress getExecutionProgress()
	{
		return executionProgress;
	}

	public void setExecutionProgress(ActionsExecutionProgress executionProgress)
	{
		this.executionProgress = executionProgress;
	}

	public void setExecutionProgress(String progressString)
	{
		this.executionProgress = createExecutionProgress(progressString);
	}

	@Override
	public void save(CsvWriter writer) throws IOException
	{
		writer.write(name, true);
		writer.write(kind, true);
		writer.write(startAt, true);
		writer.write(BinaryConverter.getBinaryStringFromBoolean(askForContinue), true);
		writer.write(BinaryConverter.getBinaryStringFromBoolean(askIfFailed), true);
		writer.write(BinaryConverter.getBinaryStringFromBoolean(execute), true);
		writer.write(DateTimeUtils.getMillisecondsFromDate(started), true);
		writer.write(executionProgress.toString(), true);
		writer.write(DateTimeUtils.getMillisecondsFromDate(finished), true);
	}

	@Override
	public void assignFields(CsvReader reader) throws IOException
	{
		assignBasicFields(reader);
		setStarted(DateTimeUtils.getDateFromTimestampOrNull(reader.get(Step.StepParams.STARTED.getValue())));
		setExecutionProgress(reader.get(Step.StepParams.ACTIONS_SUCCESSFUL.getValue()));
		setFinished(DateTimeUtils.getDateFromTimestampOrNull(reader.get(Step.StepParams.FINISHED.getValue())));
	}

	public void assignBasicFields(CsvReader reader) throws IOException
	{
		setKind(reader.get(Step.StepParams.STEP_KIND.getValue()));
		setStartAt(reader.get(Step.StepParams.START_AT.getValue()));
		String stepName = reader.get(Step.StepParams.GLOBAL_STEP.getValue());
		setName(stepName);
		String askForCont = reader.get(Step.StepParams.ASK_FOR_CONTINUE.getValue());
		setAskForContinue(BinaryConverter.getBooleanFromString(askForCont));
		String askIfFld	= reader.get(Step.StepParams.ASK_IF_FAILED.getValue());
		setAskIfFailed(BinaryConverter.getBooleanFromString(askIfFld));
		String exec	= reader.get(Step.StepParams.EXECUTE.getValue());
		setExecute(BinaryConverter.getBooleanFromString(exec));
	}


	private ActionsExecutionProgress createExecutionProgress(String progressString)
	{
		String[] progressNumerals = progressString.split(ActionsExecutionProgress.getDelimiter());
		return progressNumerals.length == 2
				? new ActionsExecutionProgress(Integer.parseInt(progressNumerals[0]), Integer.parseInt(progressNumerals[1]))
				: new ActionsExecutionProgress();
	}


	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		
		StepData that = (StepData) o;
		
		if (askForContinue != that.askForContinue)
			return false;
		if (askIfFailed != that.askIfFailed)
			return false;
		if (execute != that.execute)
			return false;
		if (!name.equals(that.name))
			return false;
		if (!kind.equals(that.kind))
			return false;
		return startAt.equals(that.startAt);
	}

	@Override
	public int hashCode()
	{
		int result = name.hashCode();
		result = 31 * result + kind.hashCode();
		result = 31 * result + startAt.hashCode();
		result = 31 * result + (askForContinue ? 1 : 0);
		result = 31 * result + (askIfFailed ? 1 : 0);
		result = 31 * result + (execute ? 1 : 0);
		return result;
	}
}