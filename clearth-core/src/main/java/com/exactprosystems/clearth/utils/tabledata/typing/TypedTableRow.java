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
package com.exactprosystems.clearth.utils.tabledata.typing;

import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class TypedTableRow extends TableRow<TypedTableHeaderItem, Object>
{
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##########");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

	public TypedTableRow(TypedTableHeader header)
	{
		super(header);
	}

	public TypedTableRow(TypedTableHeader header, Collection<Object> values) throws IllegalArgumentException
	{
		super(header, values);
	}

	@Override
	public TypedTableHeader getHeader()
	{
		return (TypedTableHeader) super.getHeader();
	}

	public String getString(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue == null)
			return null;
		if (rowValue instanceof Float || rowValue instanceof Double)
		{
			return DECIMAL_FORMAT.format(rowValue);
		}
		if (rowValue instanceof LocalDate)
		{
			LocalDate localDate = (LocalDate) rowValue;
			return localDate.format(DATE_FORMAT);
		}
		if (rowValue instanceof BigDecimal)
		{
			return ((BigDecimal) rowValue).toPlainString();
		}
		if (rowValue instanceof Timestamp)
		{
			Timestamp timestamp = (Timestamp) rowValue;
			return DATE_FORMAT.format(timestamp.toLocalDateTime());
		}
		return rowValue.toString();
	}

	public void setString(String columnName, String value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Integer getInteger(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);

		if (rowValue instanceof Integer)
		{
			return (Integer) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Integer");
	}

	public void setInteger(String columnName, Integer value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Boolean getBoolean(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Boolean)
		{
			return (Boolean) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Boolean");
	}

	public void setBoolean(String columnName, Boolean value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Byte getByte(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Byte)
		{
			return (Byte) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Byte");
	}

	public void setByte(String columnName, Byte value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Short getShort(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Short)
		{
			return (Short) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Short");
	}

	public void setShort(String columnName, Short value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Long getLong(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Long)
		{
			return (Long) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Long");
	}

	public void setLong(String columnName, Long value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Float getFloat(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Float)
		{
			return (Float) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Float");
	}

	public void setFloat(String columnName, Float value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public Double getDouble(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Double)
		{
			return (Double) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not Double");
	}

	public void setDouble(String columnName, Double value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public BigDecimal getBigDecimal(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof BigDecimal)
		{
			return (BigDecimal) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not BigDecimal");
	}

	public void setBigDecimal(String columnName, BigDecimal value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public LocalDate getLocalDate(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof LocalDate)
		{
			return (LocalDate) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not LocalDate");
	}

	public void setLocalDate(String columnName, Date value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public LocalTime getLocalTime(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof LocalTime)
		{
			return (LocalTime) rowValue;
		}
		throw new IllegalArgumentException("Specified column value type is not LocalTime");
	}

	public void setLocalTime(String columnName, LocalTime value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, value);
	}

	public LocalDateTime getDateTime(TypedTableHeaderItem columnName)
	{
		Object rowValue = getValue(columnName);
		if (rowValue instanceof Timestamp)
		{
			Timestamp timestamp = (Timestamp) rowValue;
			return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.ofHours(0));
		}
		throw new IllegalArgumentException("Specified column value type is not DateTime");
	}

	public void setDateTime(String columnName, LocalDateTime value)
	{
		int index = getColumnIndex(columnName);
		setValue(index, Timestamp.valueOf(value));
	}

	private int getColumnIndex(String columnName)
	{
		int index = getHeader().getColumnIndex(columnName);
		if (index == -1)
			throw new IllegalArgumentException(String.format("Header doesn't contain specified column %s",
					columnName));
		return index;
	}
}