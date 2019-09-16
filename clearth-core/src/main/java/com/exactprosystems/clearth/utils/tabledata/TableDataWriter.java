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

package com.exactprosystems.clearth.utils.tabledata;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * Abstract ancestor for classes that write table-like data in various formats.
 * It is bound to TableHeader of data to write. This ensures consistency of written data
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values
 */
public abstract class TableDataWriter<A, B> implements Closeable
{
	protected final TableHeader<A> header;
	
	public TableDataWriter(TableHeader<A> header)
	{
		this.header = header;
	}
	
	
	/**
	 * @return header the writer is bound to
	 */
	public TableHeader<A> getHeader()
	{
		return header;
	}
	
	/**
	 * Writes one row of data, checking if its header is the same as used by writer
	 * @param row to write
	 * @return ID or index of written row, if supported by data destination
	 * @throws IOException if row couldn't be written
	 * @throws IllegalArgumentException if row header doesn't match with writer's header
	 */
	public int write(TableRow<A, B> row) throws IOException, IllegalArgumentException
	{
		checkHeader(row.getHeader());
		return writeRow(row);
	}
	
	/**
	 * Writes multiple rows of data, checking if their headers are the same as used by writer. 
	 * If any header is not the same, no rows are written and exception is thrown.
	 * This method may work faster than writing of rows one by one, but this depends on writer implementation
	 * @param rows to write
	 * @return ID or index of last written row, if supported by data destination
	 * @throws IOException if row couldn't be written
	 * @throws IllegalArgumentException if header of any row doesn't match with writer's header
	 */
	public int write(Collection<TableRow<A, B>> rows) throws IOException, IllegalArgumentException
	{
		for (TableRow<A, B> r : rows)
			checkHeader(r.getHeader());
		return writeRows(rows);
	}
	
	
	/**
	 * Writes one row of data
	 * @param row to write
	 * @return ID or index of written row, if supported by data destination
	 * @throws IOException if row couldn't be written
	 */
	protected abstract int writeRow(TableRow<A, B> row) throws IOException;
	/**
	 * Writes multiple rows of data 
	 * @param rows to write
	 * @return ID or index of last written row, if supported by data destination
	 * @throws IOException if row couldn't be written
	 */
	protected abstract int writeRows(Collection<TableRow<A, B>> rows) throws IOException;
	
	/**
	 * Compares writer's header with given header to ensure they match
	 * @param header to verify
	 * @throws IllegalArgumentException if header doesn't match with writer's header
	 */
	protected void checkHeader(TableHeader<A> header) throws IllegalArgumentException
	{
		if (header != this.header)
			throw new IllegalArgumentException("Unexpected header in row to write");
	}
}
