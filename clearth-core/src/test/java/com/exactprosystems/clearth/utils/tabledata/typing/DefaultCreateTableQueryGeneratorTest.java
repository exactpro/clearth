package com.exactprosystems.clearth.utils.tabledata.typing;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.exactprosystems.clearth.utils.tabledata.typing.TableDataType.values;

public class DefaultCreateTableQueryGeneratorTest extends Assert
{
	private TypedTableHeader testHeader;
	private String headerRepresent;
	private static final String TEST_TABLE = "TestTable";
	private String sqliteExpected, oracleExpected;
	private SqlSyntax sqliteSyntax, oracleSyntax;

	@BeforeClass
	void init()
	{
		testHeader = getTestHeader();
		headerRepresent = getHeaderRepresent(testHeader);

		sqliteExpected =
				"CREATE TABLE TestTable (A INTEGER, B TEXT, C CHAR(5), D INTEGER, E INTEGER, F INTEGER, G REAL, H REAL, I " +
						"NUMERIC, J NUMERIC, K NUMERIC, L REAL, M BLOB)"; 
		sqliteSyntax = tdType ->
		{
			switch (tdType)
			{
				case STRING:
					return "TEXT";
				case BOOLEAN:
					return "CHAR(5)";
				case INTEGER:
				case BYTE:
				case SHORT:
				case LONG:
					return "INTEGER";
				case LOCALDATE:
				case LOCALTIME:
					return "NUMERIC";
				case LOCALDATETIME:
				case FLOAT:
				case DOUBLE:
					return "REAL";
				case BIGDECIMAL:
					return "NUMERIC";
				case OBJECT:
				default:
					return "BLOB";
			}
		};

		oracleExpected = "CREATE TABLE TestTable (A INTEGER, B VARCHAR2(4000), C CHAR(5), D " +
			"SHORTINTEGER, E SHORTINTEGER, F LONGINTEGER, G NUMBER, H NUMBER, I NUMBER, J DATE, K DATE, L " +
			"TIMESTAMP, M BLOB)";
		oracleSyntax = tdType ->
		{
			switch (tdType)
			{
				case STRING:
					return "VARCHAR2(4000)";
				case BOOLEAN:
					return "CHAR(5)";
				case INTEGER:
					return "INTEGER";
				case BYTE:
				case SHORT:
					return "SHORTINTEGER";
				case LONG:
					return "LONGINTEGER";
				case FLOAT:
				case DOUBLE:
				case BIGDECIMAL:
					return "NUMBER";
				case LOCALDATE:
				case LOCALTIME:
					return "DATE";
				case LOCALDATETIME:
					return "TIMESTAMP";
				case OBJECT:
				default:
					return "BLOB";
			}
		};
	}
	
	@DataProvider
	public Object[][] testData()
	{
		return new Object[][]{
				{"Sqlite", sqliteExpected, sqliteSyntax},
				{"Oracle", oracleExpected, oracleSyntax}
		};
	}
	

	@Test(dataProvider = "testData")
	public void generateTableCreateQuery(String dbms, String expected, SqlSyntax sqlSyntax)
	{
		String actual = new DefaultCreateTableQueryGenerator(sqlSyntax).generateQuery(testHeader, TEST_TABLE);
		System.out.println(
				String.format("DBMS = '%s';\nHEADER = '%s'\nEXPEXTED_QUERY = '%s'\nACTUAL_QUERY = '%s'", dbms,
						headerRepresent, expected, actual));
		assertEquals(actual, expected);
	}

	private String getHeaderRepresent(TypedTableHeader header)
	{
		return StringUtils.join(header.getColumnNames().stream().map(h -> h + "=" + header.getColumnType(h)).toArray(), ';');
	}

	private TypedTableHeader getTestHeader()
	{
		int codeOfA = 'A';
		Set<TypedTableHeaderItem> headerItems = new LinkedHashSet<>();
		int i = 0;
		for (TableDataType tdType : values())
		{
			headerItems.add(new TypedTableHeaderItem(String.valueOf(((char) (codeOfA + i++))), tdType));
		}

		return new TypedTableHeader(headerItems);
	}
}