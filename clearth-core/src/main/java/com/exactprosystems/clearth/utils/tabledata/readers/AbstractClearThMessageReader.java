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

package com.exactprosystems.clearth.utils.tabledata.readers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.tabledata.*;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.SUBMSGTYPE;
import static com.exactprosystems.clearth.utils.Utils.nvl;

public abstract class AbstractClearThMessageReader<C extends BasicTableData<String, String>> implements TableDataReader<C>
{
	protected ClearThMessage<?> clearThMessage;
	protected final SubMessageFilter subMessageFilter;
	protected final TableRowFilter<String, String> rowFilter;
	protected RowsListFactory<String, String> rowsListFactory;
	protected Set<String> header;
	protected int rowsLimit;


	public AbstractClearThMessageReader(ClearThMessage<?> message, 
	                                    SubMessageFilter subMessageFilter, 
	                                    TableRowFilter<String, String> rowFilter)
	{
		this.clearThMessage = message;
		this.subMessageFilter = subMessageFilter;
		this.rowFilter = rowFilter;
	}
	
	public AbstractClearThMessageReader(ClearThMessage<?> message, SubMessageFilter subMessageFilter)
	{
		this(message, subMessageFilter, null);
	}

	public AbstractClearThMessageReader(ClearThMessage<?> message, TableRowFilter<String, String> rowFilter)
	{
		this(message, null, rowFilter);
	}

	public AbstractClearThMessageReader(ClearThMessage<?> message)
	{
		this(message, null, null);
	}
	

	public void setRowsListFactory(RowsListFactory<String, String> rowsListFactory)
	{
		this.rowsListFactory = rowsListFactory;
	}

	public void setHeader(Set<String> header)
	{
		this.header = header;
	}

	public void setRowsLimit(int rowsLimit)
	{
		this.rowsLimit = rowsLimit;
	}

	@Override
	public void close()
	{
		this.clearThMessage = null;
	}

	protected Set<String> createHeader(ClearThMessage<?> message)
	{
		Set<String> header = new HashSet<String>();
		fillHeader(header, message);
		return header;
	}
	
	protected void fillHeader(Set<String> header, ClearThMessage<?> message)
	{
		for (String fieldName : message.getFieldNames())
		{
			if (MSGTYPE.equals(fieldName) || SUBMSGTYPE.equals(fieldName))
				continue;
			
			header.add(fieldName);
		}

		for (ClearThMessage<?> subMessage : message.getSubMessages())
		{
			fillHeader(header, subMessage);
		}
	}

	protected abstract C createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory);

	@Override
	public C readAllData() throws IOException
	{
		Set<String> header = (this.header != null) ? this.header : createHeader(clearThMessage);
		RowsListFactory<String, String> rowsListFactory = nvl(this.rowsListFactory,
				RowsListFactories.<String, String>linkedListFactory());
		C tableData = createTableData(header, rowsListFactory);
		collectData(tableData, clearThMessage);
		return tableData;
	}

	protected void collectData(BasicTableData<String, String> tableData, ClearThMessage<?> message) throws IOException
	{
		collectData(tableData, message, new HashMap<String, String>());
	}

	private void collectData(BasicTableData<String, String> tableData, ClearThMessage<?> message, Map<String, String> parentFields) throws IOException
	{
		if ((subMessageFilter != null) && !subMessageFilter.filter(message))
			return;

		Map<String, String> allFields = new HashMap<String, String>(parentFields);
		allFields.putAll(message.getFields());

		if (!message.hasSubMessages())
		{
			TableRow<String, String> row = tableData.createRow();
			fillRow(row, tableData.getHeader(), allFields);
			if ((rowFilter == null) || rowFilter.filter(row))
				tableData.add(row);
		}
		else
		{
			for (ClearThMessage<?> subMessage : message.getSubMessages())
			{
				if((rowsLimit != 0) && (tableData.size() >= rowsLimit))
					return;

				collectData(tableData, subMessage, allFields);
			}
		}
	}

	private void fillRow(TableRow<String, String> row, TableHeader<String> tableHeader, Map<String, String> fields)
	{
		for (String fieldName : tableHeader)
		{
			row.setValue(fieldName, fields.get(fieldName));
		}
	}
}
