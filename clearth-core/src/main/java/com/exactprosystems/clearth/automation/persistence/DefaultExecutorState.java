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

package com.exactprosystems.clearth.automation.persistence;

import java.io.File;
import java.io.IOException;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

public class DefaultExecutorState extends ExecutorState
{
	public static final Class[] ACTIONSTATE_ANNOTATIONS = new Class[]{DefaultActionState.class, DefaultResultState.class},
			STATEINFO_ANNOTATIONS = new Class[]{ExecutorStateInfo.class, DefaultStepState.class, StepContext.class},
			STATEOBJECTS_ANNOTATIONS = new Class[]{ExecutorStateObjects.class, DefaultMatrixState.class,
					DefaultActionState.class, DefaultResultState.class},
			STATEMATRIX_ANNOTATIONS = new Class[]{MvelVariables.class};
	public static final Class[] ALLOWED_CLASSES = {ExecutorStateInfo.class,
			DefaultStepState.class, StepContext.class, XmlMatrixInfo.class,
			ExecutorStateObjects.class, DefaultResultState.class, MvelVariables.class, HashSetValuedHashMap.class,
			ExecutorState.class, ActionState.class, MatrixData.class, MatrixState.class, DefaultMatrixState.class,
			DefaultActionState.class,
			StringTableData.class, TableRow.class};


	public DefaultExecutorState(SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		super(executor, stepFactory, reportsInfo);
	}
	
	public DefaultExecutorState(File sourceDir) throws IOException
	{
		super(sourceDir);
	}
	
	
	@Override
	protected MatrixState createMatrixState(Matrix matrix)
	{
		return new DefaultMatrixState(matrix);
	}
	
	@Override
	protected void initExecutor(SimpleExecutor executor)
	{
		//Nothing to do here
	}
	

	@Override
	protected ExecutorStateInfo createStateInfo()
	{
		return new ExecutorStateInfo();
	}

	@Override
	protected void initStateInfo(ExecutorStateInfo stateInfo)
	{
		//Nothing to do here
	}
	
	@Override
	protected void initFromStateInfo(ExecutorStateInfo stateInfo)
	{
		//Nothing to do here
	}
	
	
	@Override
	protected ExecutorStateObjects createStateObjects()
	{
		return new ExecutorStateObjects();
	}
	
	@Override
	protected void initStateObjects(ExecutorStateObjects stateObjects)
	{
		//Nothing to do here
	}

	@Override
	protected Class[] getActionStateAnnotations()
	{
		return ACTIONSTATE_ANNOTATIONS;
	}

	@Override
	protected Class[] getStateInfoAnnotations()
	{
		return STATEINFO_ANNOTATIONS;
	}

	@Override
	protected Class[] getStateObjectsAnnotations()
	{
		return STATEOBJECTS_ANNOTATIONS;
	}

	protected  Class[] getStateMatrixAnnotations()
	{
		return STATEMATRIX_ANNOTATIONS;
	}

	@Override
	protected Class[] getAllowedClasses()
	{
		return ALLOWED_CLASSES;
	}
}
