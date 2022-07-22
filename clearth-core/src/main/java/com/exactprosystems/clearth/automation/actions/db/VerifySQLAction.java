/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.TimeoutAwaiter;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.MultiDetailedResult;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

public abstract class VerifySQLAction extends SelectSQLAction implements TimeoutAwaiter
{
	protected static final String PARAM_OUTPUTPARAMS = "OutputParams",
			USE_CP_FROM = "UseCPFrom",
			QUERY_WITH_CP = "QueryWithCP",
			QUERY_WITH_CP_FILE = "QueryWithCPFile";

	protected int expectedRecords = -1;
	protected boolean noData = false;
	private boolean needRerun;

	protected long awaitedTimeout;
	
	protected List<DBFieldMapping> loadVerificationMapping(String fileName) throws IOException, NumberFormatException
	{
		return SQLUtils.loadVerificationMapping(fileName);
	}

	protected List<Map<String,String>> extractValues(ResultSet rs, List<DBFieldMapping> mapping) throws ResultException
	{
		//Obtaining all values from DB
		List<Map<String, String>> dbValues = new ArrayList<Map<String, String>>();
		try
		{
			ResultSetMetaData md = rs.getMetaData();
			List<String> foundColumns = new ArrayList<String>();
			
			for (int i = 1; i <= md.getColumnCount(); i++)
				foundColumns.add(md.getColumnName(i));
			
			do
			{
				Map<String, String> row = new HashMap<String, String>();
				for (DBFieldMapping fieldMapping : mapping)
				{
					String destField = fieldMapping.getDestField();
					if (!foundColumns.contains(destField))
					{
						getLogger().warn("Column '" + destField + "' not found in DB");
						continue;
					}
					if (getLogger().isTraceEnabled())
						getLogger().trace("Obtaining value of field '"+fieldMapping.getSrcField()+"' ("+destField+")");
					
					row.put(destField, transformDbValue(SQLUtils.getDbValue(rs, destField)));
				}
				dbValues.add(row);
			}
			while (rs.next());

			return dbValues;
		}
		catch (SQLException e)
		{
			throw ResultException.failed(e);
		}
	}

	protected String prepareValue(String param, DBFieldMapping fieldMapping)
	{
		String before = param;
		
		try{
			Double.valueOf(param);
		}catch(Exception e){
			return param;
		}
		
		if (param.indexOf("0") != 0 || param.indexOf("0.") == 0)
		{
			try
			{
				if (Double.valueOf(param) == 0)
					param = "0";
				else if (param.contains("."))
				{
					int lastPos = param.length()-1;
					while (lastPos >= 0 && param.charAt(lastPos) == '0') {
						lastPos--;
					}
					if (param.charAt(lastPos) == '.'){
						lastPos--;
					}
					param = param.substring(0, lastPos+1); // +1 -> convert position to length
				}

				if (getLogger().isDebugEnabled())
					getLogger().debug("Parameter handled as a number. Before convertion: '"+before+"', after: '"+param+"'");

				return param;
			}
			catch(Exception e)
			{
				getLogger().debug("Error while handling parameter as a number", e);
				return param;
			}
		}

		return param;
	}


	public Result doVerification(ResultSet rs, List<DBFieldMapping> mapping, int expectedRecords)
	{
		List<Map<String, String>> dbValues = null;

		try
		{
			dbValues = this.extractValues(rs, mapping);
		}
		catch (ResultException e)
		{
			needRerun = false;
			return e.getResult();
		}
		
		MultiDetailedResult result = new MultiDetailedResult();
		int matched = 0;
		boolean fl = false;
		Map<String, String> params = getQueryParams();
		for (Map<String, String> row : dbValues)
		{
			if (fl)
				result.startNewBlock(null);
			else
				fl = true;

			boolean allMatched = true;
			for (DBFieldMapping fieldMapping : mapping)
			{
				String paramName = fieldMapping.getSrcField();

				String param = params.get(paramName);
				if (param == null)
					param = ""; //Useful in case of "commented" columns in matrix

				param = transformMatrixParameter(param);

				String dbField = row.get(fieldMapping.getDestField());

				String paramConverted = param;
				boolean converted = false;
				Map<String, String> conversionsMap = fieldMapping.getConversions();
				if (MapUtils.isNotEmpty(conversionsMap))
				{
					for (String oldValue : conversionsMap.keySet())
						if (oldValue.equalsIgnoreCase(param))
						{
							paramConverted = conversionsMap.get(oldValue);
							converted = true;
						}
				}


				ResultDetail rd = new ResultDetail();
				rd.setParam(paramName);

				paramConverted = this.prepareValue(paramConverted,fieldMapping);

				String visualExpected	 = "";
				String visualActual		 = "";
				Map<String, String> visualizationsMap = fieldMapping.getVisualizations();
				if (visualizationsMap !=null)
				{
					if (!paramConverted.isEmpty()) {
						for (String oldValue : visualizationsMap.keySet())
							if (oldValue.equalsIgnoreCase(paramConverted))
								visualExpected = visualizationsMap.get(oldValue);
					}
					if (StringUtils.isNotBlank(dbField))
						for (String oldValue : visualizationsMap.keySet())
							if (oldValue.equalsIgnoreCase(dbField))
								visualActual = visualizationsMap.get(oldValue);
				}

				String expected	 = "",
						actual = dbField;

				if (!converted)
					expected = paramConverted;
				else
					expected = paramConverted+" ("+param+")";
				if (!visualExpected.isEmpty())
					expected = visualExpected + " (" + expected + ")";

				if (!visualActual.isEmpty())
					actual = visualActual + " (" + actual + ")";

				rd.setExpected(expected);
				rd.setActual(actual);


				if (param.isEmpty())
					rd.setIdentical(true);
				else
				{
					try
					{
						rd.setIdentical(getParameterIdentical(param, paramConverted, dbField));
					} catch (ParametersException e)
					{
						rd.setErrorMessage(e.getMessage());
					}
				}
				result.addResultDetail(rd);
				if (!rd.isIdentical())
					allMatched = false;
			}

			if (allMatched)
				matched++;

			if (expectedRecords < 0)
				break;
		}

		if (expectedRecords < 0) {
			boolean success = matched > 0;
			needRerun = !success;
			result.setSuccess(success);
		}
		else
		{
			result.setSuccess(matched == expectedRecords);
			if (matched < expectedRecords)
				result.setComment("Not enough records matched: expected "+expectedRecords+", matched "+matched);
			else if (matched>expectedRecords)
				result.setComment("Too many records matched: expected "+expectedRecords+", matched "+matched);
			needRerun = matched < expectedRecords;
		}

		return result;
	}
	
	
	protected String transformDbValue(String value)
	{
		return value;
	}
	
	protected String transformMatrixParameter(String param)
	{
		return this.valueTransformer.transform(param);
	}
	
	protected boolean getParameterIdentical(String param, String paramConverted, String dbField) throws ParametersException
	{
		return (param.equalsIgnoreCase("null")) ? (dbField == null)
			: comparisonUtils().compareValuesIgnoreCase(paramConverted, dbField);
	}

	protected void saveFieldToMatrix(ResultSet rs, List<Pair<String, String>> params) throws SQLException
	{
		if (params == null)
			return;
		List<String> columnNames = new ArrayList<String>();
		for (int i=0;i<rs.getMetaData().getColumnCount();i++)
			columnNames.add(rs.getMetaData().getColumnName(i+1));
		for (Pair<String, String> pair : params)
		{
			if (getLogger().isTraceEnabled())
				getLogger().trace("Setting output param value for field '" + pair.getFirst() + "' (" + pair.getSecond() + ")");
			if (!columnNames.contains(pair.getSecond()))
			{
				getLogger().error("Column '" + pair.getFirst() + "' (" + pair.getSecond() + ") is missing in result.");
				continue;
			}
			String field = transformDbValue(SQLUtils.getDbValue(rs, pair.getSecond()));
			if (field != null && field.length() > 0)
				addOutputParam(pair.getFirst(), field);
		}
	}
	

	@Override
	protected List<Pair<String, String>> getOutputFields() throws ResultException

	{
		String outputParamsList = getInputParam(PARAM_OUTPUTPARAMS);
				
		List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
		List<DBFieldMapping> mapping = getVerificationMapping();
		List<String> addedFields = new ArrayList<String>();		
		
		if (!StringUtils.isEmpty(outputParamsList))
		{
			String[] params = outputParamsList.split(",");
			for (String n : params)
			{
				String[] names = n.split("\\|");
				String paramName = names[0];
				boolean found = false;
				if (names.length>1)
				{
					found = true;
					result.add(new Pair<String, String>(paramName, names[1]));
				}
				else
				{
					for (DBFieldMapping fm : mapping)
						if (fm.getSrcField().equals(n))
						{
							found = true;
							result.add(new Pair<String, String>(n, fm.getDestField()));
							break;
						}
				}
				if (!found)
				{
					getLogger().warn("Field for output '" + n + "' is not present in mapping.");  
					throw ResultException.failed("Field for output '" + n + "' is not present in mapping.");
				}
				else
					addedFields.add(paramName);
			}
		}
		
		for (DBFieldMapping fm : mapping)
			if (!addedFields.contains(fm.getSrcField()))
			{
				result.add(new Pair<String, String>(fm.getSrcField(), fm.getDestField()));
			}
		
		return result;
	}


	protected String keysToString(String[] keys)
	{
		String result = "";
		List<String> usedKeys = new ArrayList<String>();
		Map<String, String> params = getQueryParams();
		for (String key : keys)
		{
			if (usedKeys.indexOf(key)>-1)
				continue;

			String value = params.get(key);
			if (value==null)
				continue;

			if (result.length()>0)
				result += ", ";
			result += key+"="+value;

			usedKeys.add(key);
		}
		return result;
	}

	@Override
	protected Result processResultSet(ResultSet rs, String[] keys) throws SQLException,ResultException
	{

		if (!noData)
		{
			if (!rs.next()) //No data found
			{
				String message;
				if ((keys != null) && (keys.length > 0))
					message = "No records in database for key: "+keysToString(keys);
				else
					message = "No records in database";
				needRerun = true;
				return DefaultResult.failed(message);
			}
			else
			{
				saveFieldToMatrix(rs, getOutputFields());
				return doVerification(rs, getVerificationMapping(), expectedRecords);  //Compare parameters from matrix with DB fields according to the mapping
			}
		}
		else  //Checking whether there are no more records for the key
		{
			DefaultResult result = new DefaultResult();
			result.setSuccess(!rs.next());
			if (!result.isSuccess())
			{
				String message;
				if ((keys!=null) && (keys.length>0))
					message = "Found records in database for key: "+keysToString(keys);
				else
					message = "Found records in database";
//				needRerun = false;

				result.setComment(message);
			}
			needRerun = false;
			return result;
		}
	}

	protected void useCheckPointer() throws Exception
	{
		Map<String, String> params = getQueryParams();
		String cpActionId = params.get(USE_CP_FROM);
		getLogger().debug("cpActionId="+cpActionId);
		if ((cpActionId!=null) && (!cpActionId.isEmpty()))
		{
			Map<?, ?> cp = (Map<?, ?>)getMatrixContext().getContext(SQLCheckPointer.getCPName(cpActionId));
			if (cp!=null)
			{
				for (Object key : cp.keySet())
					params.put((String)key, (String)cp.get(key));

				getGlobalContext().setLoadedContext(getQueryFileName(), SQLUtils.loadQuery(ClearThCore.rootRelative(getQueryCPFileName())));
			}
			else 
				getLogger().debug("CheckPointer has not been set by action '" + cpActionId + "'. Using default query");
		}
	}

	protected String getQueryCPFileName()
	{
		return getInputParam(QUERY_WITH_CP_FILE);
	}

	protected String getQueryCPName()
	{
		return getName() + QUERY_WITH_CP;
	}
	
	@Override
	protected String getQuery() throws Exception
	{
		String cpActionId = inputParams.get("UseCPFrom");
		String queryNameKey;
		if (cpActionId != null && !cpActionId.isEmpty())
		{
			useCheckPointer();
			queryNameKey = getQueryCPName();
		}
		else
			queryNameKey = getQueryFileName();
		
		String query = (String) getGlobalContext().getLoadedContext(queryNameKey);
		
		if (query == null)
		{
			useCheckPointer();
			query = (String) getGlobalContext().getLoadedContext(queryNameKey);
		}
		
		logger.debug("Used query: " + queryNameKey);
		return query;
	}

	@Override
	protected Result executeQuery() throws Exception {

		long timeout = getTimeOut();
		long sleepDelta = getWaitPeriod();

		String query = getQuery();
		String[] keys = SQLUtils.getKeysFromQuery(query);

		beforeQuery();

		needRerun = false;

		Connection con = getDBConnection();
		PreparedStatement parametrizedQuery = null;
		Stopwatch sw = new Stopwatch();
		try
		{
			parametrizedQuery = this.prepareStatement(query, keys, con);

			sw.start(timeout);

			ResultSet rs = null;
			int iteration = 0;

			Result result = null;
			do {
				try {
					iteration++;
					if (iteration != 1) {
						Thread.sleep(sleepDelta);
					}

					logger.debug("Iteration #" + iteration + " started.");

					rs = parametrizedQuery.executeQuery();

					result = processResultSet(rs, keys);

				} finally {
					try {
						if (rs != null) {
							rs.close();
						}
					} catch (SQLException e) {
						logger.error("Exception during closing ResultSet", e);
						if (result != null) {
							if (result.isSuccess()) {
								result.setError(e);
								result.setSuccess(false);
							} else {
								result.appendComment("Exception during closing ResultSet: " + e.getMessage());
							}
						} else {
							result = DefaultResult.failed("Exception during closing ResultSet", e);
						}
						needRerun = false;
					}
				}

				logger.debug("Awaiting next iteration: " + needRerun);

			} while (needRerun && !sw.isExpired());

			logger.debug("Search stopped. Reason: " + (needRerun ? "Timed out" : "End of search"));

			if (result == null) {
				result = DefaultResult.failed("Result of action is not generated!");
			}

			return result;
		}
		finally
		{
			awaitedTimeout = sw.stop();
			Utils.closeStatement(parametrizedQuery);
			if (isNeedCloseDbConnection())
				Utils.closeResource(con);
		}
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}


	protected void beforeQuery(){
		String expR = inputParams.get("ExpectedRecords");
		//inputParams.remove("ExpectedRecords");
		if (expR != null) {
			try  {
				expectedRecords = Integer.parseInt(expR);
				expectedRecords = expectedRecords == 0 ? -1 : expectedRecords;
			}
			catch (NumberFormatException e) {
				getLogger().warn("Could not parse value in parameter 'ExpectedRecords'", e);
				expectedRecords = -1;
			}
		}

		String noDataIP = inputParams.get("NoData");
		this.noData = !((noDataIP == null) || (noDataIP.isEmpty())
				|| (noDataIP.equalsIgnoreCase("false")) || (noDataIP.equalsIgnoreCase("n")));


	}


	protected long getWaitPeriod() {
		return 200;
	}
}
