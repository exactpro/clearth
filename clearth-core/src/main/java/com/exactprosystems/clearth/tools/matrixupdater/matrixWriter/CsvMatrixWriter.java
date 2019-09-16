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

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.utils.StringOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterUtils.headerToArray;

public class CsvMatrixWriter implements MatrixWriter
{
	@Override
	public File writeMatrix(File file, Matrix matrix) throws IOException
	{
		FileUtils.deleteQuietly(file.getCanonicalFile());

		CsvWriter writer = null;
		File result = new File(file.getCanonicalPath());

		try
		{
			writer = new CsvWriter(result.getCanonicalPath());
			writer.setUseTextQualifier(false);

			for (Block block : matrix.getBlocks())
			{
				List<Row> records = block.getActions();

				if (records == null || records.isEmpty()) continue;

				writer.writeRecord(headerToArray(block.getHeader()));

				for (Row record : records)
				{
					if (block.getHeader().size() < 1) {
						for (String value : record.getValues())
							writer.write(StringOperationUtils.quote(value));
						writer.endRecord();
						continue;
					}
					Iterator<String> iterator = block.getHeader().iterator();
					while (iterator.hasNext()) {
						String value = record.getValues().get(block.getHeader().columnIndex(iterator.next()));
						writer.write(StringOperationUtils.quote(value));
					}
					
					writer.endRecord();
				}
			}
		}
		finally
		{
			Utils.closeResource(writer);
		}

		return result;
	}
}
