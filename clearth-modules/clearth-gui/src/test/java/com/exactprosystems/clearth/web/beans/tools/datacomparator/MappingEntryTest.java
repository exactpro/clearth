/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.tools.datacomparator;

import java.math.BigDecimal;

import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.SourceColumnDesc;

public class MappingEntryTest
{
	@Test
	public void entryToFieldDesc()
	{
		String name = "C1",
				actualName = "ActC1";
		
		MappingEntry entry = new MappingEntry();
		entry.setName(name);
		entry.setKey(true);
		entry.setNumeric(true);
		entry.setPrecision(BigDecimal.TEN);
		entry.setIgnore(true);
		entry.setInfo(true);
		entry.setExpectedName("");
		entry.setActualName(actualName);
		
		FieldDesc desc = entry.toFieldDesc();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(desc.getLocalName(), name, "Name");
		soft.assertTrue(desc.isKey(), "Key");
		soft.assertTrue(desc.isNumeric(), "Numeric");
		soft.assertEquals(desc.getPrecision(), BigDecimal.TEN, "Precision");
		soft.assertTrue(desc.isIgnore(), "Ignore");
		soft.assertTrue(desc.isInfo(), "Info");
		soft.assertNull(desc.getExpected(), "Settings for expected");
		soft.assertEquals(desc.getActual().getName(), actualName, "Actual name");
		soft.assertAll();
	}
	
	@Test
	public void entryFromFieldDesc()
	{
		String name = "Column1",
				actualName = "Col1";
		
		FieldDesc desc = new FieldDesc();
		desc.setLocalName(name);
		desc.setKey(true);
		desc.setNumeric(true);
		desc.setPrecision(BigDecimal.TEN);
		desc.setIgnore(true);
		desc.setInfo(true);
		
		SourceColumnDesc actualColumn = new SourceColumnDesc();
		actualColumn.setName(actualName);
		desc.setActual(actualColumn);
		
		MappingEntry entry = MappingEntry.fromFieldDesc(desc);
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(entry.getName(), name, "Name");
		soft.assertTrue(entry.isKey(), "Key");
		soft.assertTrue(entry.isNumeric(), "Numeric");
		soft.assertEquals(entry.getPrecision(), BigDecimal.TEN, "Precision");
		soft.assertTrue(entry.isIgnore(), "Ignore");
		soft.assertTrue(entry.isInfo(), "Info");
		soft.assertNull(entry.getExpectedName(), "Expected name");
		soft.assertEquals(entry.getActualName(), actualName, "Actual name");
		soft.assertAll();
	}
}
