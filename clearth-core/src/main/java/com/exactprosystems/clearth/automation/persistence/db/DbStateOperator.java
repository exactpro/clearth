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

package com.exactprosystems.clearth.automation.persistence.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.persistence.ActionState;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateObjects;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateOperator;
import com.exactprosystems.clearth.automation.persistence.ResultState;
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlReportsConfig;

public class DbStateOperator implements ExecutorStateOperator<DbStateContext>
{
	public static final Class[] ALLOWED_CLASSES = {StepContext.class, MatrixContext.class,
			ResultState.class, MvelVariables.class, HashSetValuedHashMap.class, MatrixData.class, 
			ReportsInfo.class, XmlMatrixInfo.class, XmlReportsConfig.class,
			StringTableData.class, TableRow.class};
	public static final String TABLE_MATRICES = "matrices",
			TABLE_STEP_SUCCESS = "step_success",
			TABLE_STEP_STATUS_COMMENTS = "step_status_comments",
			TABLE_ACTIONS = "actions",
			TABLE_STEPS = "steps",
			TABLE_STEP_CONTEXTS = "step_contexts",
			TABLE_FIXED_IDS = "fixedIds",
			TABLE_INFOS = "infos",
			TABLE_REPORTS = "reports",
			COLUMN_INFO_ID = "infoId",
			COLUMN_MATRIX_ID = "matrixId",
			COLUMN_REPORT_ID = "reportId",
			COLUMN_STEP_ID = "stepId",
			COLUMN_RECORD_ID = "recordId",
			COLUMN_ACTION_ID = "actionId",
			STATEINFO_FILENAME = "state.db";
	
	private final Connection con;
	private final QueryHelper helper;
	private final DbStateSaver saver;
	private final DbStateUpdater updater;
	private final DbStateLoader loader;
	
	public DbStateOperator(Connection con, Class[] allowedClasses) throws SQLException
	{
		this.con = con;
		this.helper = createQueryHelper(con);
		this.saver = new DbStateSaver(helper, allowedClasses);
		this.updater = new DbStateUpdater(helper, allowedClasses, saver);
		this.loader = new DbStateLoader(helper, allowedClasses);
	}
	
	public DbStateOperator(Connection con) throws SQLException
	{
		this(con, ALLOWED_CLASSES);
	}
	
	
	@Override
	public void close() throws SQLException
	{
		helper.close();
		con.close();
	}
	
	
	@Override
	public DbStateContext save(ExecutorStateInfo stateInfo, ExecutorStateObjects stateObjects) throws IOException
	{
		try
		{
			return saver.save(stateInfo, stateObjects);
		}
		catch (Exception e)
		{
			throw new IOException("Error while saving state", e);
		}
	}
	
	@Override
	public Pair<ExecutorStateInfo, DbStateContext> loadStateInfo() throws IOException
	{
		try
		{
			return loader.loadStateInfo();
		}
		catch (Exception e)
		{
			throw new IOException("Error while loading state info", e);
		}
	}
	
	@Override
	public ExecutorStateObjects loadStateObjects(DbStateContext context) throws IOException
	{
		try
		{
			return loader.loadStateObjects(context);
		}
		catch (Exception e)
		{
			throw new IOException("Error while loading state objects", e);
		}
	}
	
	@Override
	public void update(ExecutorStateInfo stateInfo, DbStateContext context, Action lastExecutedAction, ActionState actionState) throws IOException
	{
		try
		{
			updater.update(context, lastExecutedAction, actionState);
		}
		catch (Exception e)
		{
			throw new IOException("Error while updating state after action '"+lastExecutedAction.getIdInMatrix()+"' from matrix '"+lastExecutedAction.getMatrix().getName()+"'", e);
		}
	}
	
	@Override
	public void update(ExecutorStateInfo stateInfo, DbStateContext context, Step lastFinishedStep, StepState stepState) throws IOException
	{
		try
		{
			updater.update(context, lastFinishedStep);
		}
		catch (Exception e)
		{
			throw new IOException("Error while updating state after global step '"+lastFinishedStep.getName()+"'", e);
		}
	}
	
	@Override
	public void updateSteps(ExecutorStateInfo stateInfo, DbStateContext context) throws IOException
	{
		try
		{
			updater.updateSteps(stateInfo.getSteps(), context);
		}
		catch (Exception e)
		{
			throw new IOException("Error while updating steps", e);
		}
	}
	
	@Override
	public void updateStateInfo(ExecutorStateInfo stateInfo, DbStateContext context) throws IOException
	{
		try
		{
			updater.updateStateInfo(stateInfo, context);
		}
		catch (Exception e)
		{
			throw new IOException("Error while updating state info", e);
		}
	}
	
	
	protected QueryHelper createQueryHelper(Connection con) throws SQLException
	{
		return new QueryHelper(con);
	}
}
