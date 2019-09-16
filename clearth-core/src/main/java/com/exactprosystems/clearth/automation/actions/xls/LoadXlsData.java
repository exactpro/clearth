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

package com.exactprosystems.clearth.automation.actions.xls;

import java.io.FileInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.multidata.MultiRowStringData;
import com.exactprosystems.clearth.utils.multidata.MultiStringData;

public class LoadXlsData extends Action
{
	public static final String PARAM_FILENAME = "FileName";
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String fileName = getFileName();
		
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(fileName);
			Workbook workbook = WorkbookFactory.create(fis);
			
			DataFormatter df = createDataFormatter();
			
			MultiRowStringData loadedData = createDataStorage(workbook);
			loadData(loadedData, workbook, fileName, df);
			storeLoadedData(loadedData, stepContext, matrixContext, globalContext);
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while loading data from file '"+fileName+"'", e);
		}
		finally
		{
			Utils.closeResource(fis);
		}
		return null;
	}
	
	protected String getFileName() throws ResultException
	{
		String fileName = InputParamsUtils.getRequiredString(getInputParams(), PARAM_FILENAME);
		return ClearThCore.rootRelative(fileName);
	}
	
	protected DataFormatter createDataFormatter()
	{
		return new DataFormatter();  //In order to obtain values of date and other formatted fields we need to use DataFormatter
	}
	
	protected MultiRowStringData createDataStorage(Workbook workbook)
	{
		return new MultiRowStringData(workbook.getSheetAt(0).getFirstRowNum());
	}
	
	protected MultiStringData createDataRowStorage(Row row)
	{
		return new MultiStringData(row.getFirstCellNum());
	}
	
	
	protected String getCellValue(Cell cell, DataFormatter formatter)
	{
		if (cell == null)
			return null;
		
		return formatter.formatCellValue(cell);
	}
	
	protected void loadDataRow(MultiStringData result, Row row, DataFormatter formatter) throws ResultException
	{
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++)
		{
			String value = getCellValue(row.getCell(i), formatter);
			result.addData(value);
		}
	}
	
	protected void loadData(MultiRowStringData result, Workbook workbook, String fileName, DataFormatter formatter) throws ResultException
	{
		Sheet sheet = workbook.getSheetAt(0);
		for (int lineIndex = sheet.getFirstRowNum(); lineIndex <= sheet.getLastRowNum(); lineIndex++)
		{
			if (Thread.interrupted())
				throw new ResultException("Action was interrupted while loading data from file '"+fileName+"'");
			
			Row xlsRow = sheet.getRow(lineIndex);
			MultiStringData dataRow = createDataRowStorage(xlsRow);
			loadDataRow(dataRow, xlsRow, formatter);
			result.addData(dataRow);
		}
	}
	
	protected void storeLoadedData(MultiRowStringData result, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		XlsUtils.storeRowsData(result, matrixContext);
	}
}
