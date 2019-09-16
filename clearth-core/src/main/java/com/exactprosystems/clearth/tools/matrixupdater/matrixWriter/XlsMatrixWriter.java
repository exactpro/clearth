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

package com.exactprosystems.clearth.tools.matrixupdater.matrixWriter;

import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.XlsMatrix;
import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterUtils;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.List;

public class XlsMatrixWriter implements MatrixWriter
{
	public File writeMatrix(File file, Matrix matrix) throws IOException
	{
		FileUtils.deleteQuietly(file.getCanonicalFile());
		File result = new File(file.getCanonicalPath());

		try (OutputStream out = new FileOutputStream(result, false);
			 Workbook workbook = createWorkbook(result.getName()))
		{
			Sheet sheet = workbook.createSheet(((XlsMatrix) matrix).getSheetName());

			int index = 0;
			int usedColumnsCount = 0;

			for (Block block : matrix.getBlocks())
			{
				writeHeader(sheet, MatrixUpdaterUtils.headerToArray(block.getHeader()), index++);

				for (com.exactprosystems.clearth.tools.matrixupdater.model.Row row : block.getActions())
				{
					Row recordRow = sheet.createRow(index++);
					List<String> values = row.getValues();

					for (int i = 0; i < values.size(); i++)
						recordRow.createCell(i).setCellValue(values.get(i));

					if (values.size() > usedColumnsCount)
						usedColumnsCount = values.size();
				}

				sheet.createRow(index++);
			}

			for (int i = 0; i < usedColumnsCount; i++)
			{
				sheet.autoSizeColumn(i);
			}

			workbook.write(out);
		}

		return result;
	}

	protected void writeHeader(Sheet sheet, String[] header, int index)
	{
		Row headerRow = sheet.createRow(index);

		for (int i = 0; i < header.length; i++)
		{
			Cell headerCell = headerRow.createCell(i);
			headerCell.setCellValue(header[i]);
		}
	}

	protected Workbook createWorkbook(String fileName)
	{
		return fileName.toLowerCase().endsWith("xls") ? new HSSFWorkbook() : new XSSFWorkbook();
	}
}
