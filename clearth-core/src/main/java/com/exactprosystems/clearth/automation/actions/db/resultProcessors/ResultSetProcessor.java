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

package com.exactprosystems.clearth.automation.actions.db.resultProcessors;

import com.exactprosystems.clearth.automation.actions.db.checkers.RecordChecker;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.ResultSetProcessorSettings;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.ObjectToStringTransformer;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.exactprosystems.clearth.automation.actions.db.SQLAction.OUT_ROWS_COUNT;

public abstract class ResultSetProcessor implements AutoCloseable
{
	protected final TableResult result;
	protected final ObjectToStringTransformer objectToStringTransformer;
	protected final IValueTransformer valueTransformer;
	protected final ConversionSettings conversionSettings;
	protected final int maxDisplayedRows;
	protected final Map<String, String> outputParams;
	protected final RecordChecker recordChecker;

	public ResultSetProcessor(ResultSetProcessorSettings settings)
	{
		this.result = Validate.notNull(settings.getResult(), "Result can't be null");
		this.conversionSettings = settings.getConversionSettings();
		this.objectToStringTransformer = settings.getObjectToStringTransformer();
		this.valueTransformer = settings.getValueTransformer();
		this.maxDisplayedRows = settings.getMaxDisplayedRows();
		this.outputParams = new HashMap<>();
		this.recordChecker = settings.getRecordChecker();
	}

	public void processHeader(ResultSet resultSet, List<DBFieldMapping> mapping) throws SQLException, IOException
	{
		List<String> columns = SQLUtils.getColumnNames(resultSet.getMetaData());
		if (conversionSettings != null)
		{
			for (int i = 0; i < columns.size(); i++)
				columns.set(i, conversionSettings.getTableHeader(columns.get(i)));
		}

		result.setColumns(columns);
		processValues(columns);
		checkHeader(result, new HashSet<>(columns), mapping);
	}

	public int processRecords(ResultSet resultSet, int limit) throws SQLException, IOException
	{
		if (resultSet.getRow() < 1)
			return 0;

		int rowsCount = 0;
		do
		{
			rowsCount++;
			List<String> columns = result.getColumns();
			Map<String, String> row = new LinkedHashMap<>(columns.size());
			for (String column : columns)
			{
				String value = getDbValue(resultSet, conversionSettings != null ? conversionSettings.getDBHeader(column) : column);
				row.put(column, value);
			}
			processRecord(row);

			if (rowsCount <= maxDisplayedRows)
				result.addDetail(new DefaultTableResultDetail(new ArrayList<>(row.values())));

		}
		while (rowsCount != limit && resultSet.next());

		outputParams.put(OUT_ROWS_COUNT, String.valueOf(rowsCount));
		return rowsCount;
	}

	protected String getDbValue(ResultSet resultSet, String column) throws SQLException, IOException
	{
		String value = objectToStringTransformer != null ?
				SQLUtils.getDbValue(resultSet, column, objectToStringTransformer) : SQLUtils.getDbValue(resultSet, column);
		value = valueTransformer != null ? valueTransformer.transform(value) : value;
		return conversionSettings != null ? conversionSettings.getConvertedDBValue(column, value) : value;
	}

	protected abstract void processRecord(Map<String, String> record) throws IOException;
	protected abstract void processValues(Collection<String> values) throws IOException;

	protected void checkHeader(Result result, Set<String> columnNames, List<DBFieldMapping> mapping)
	{
		if (recordChecker != null)
			recordChecker.checkRecord(result, columnNames, mapping);
	}

	public Map<String, String> getOutputParams()
	{
		return outputParams;
	}
}
