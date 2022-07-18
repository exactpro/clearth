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

package com.exactprosystems.clearth.automation.actions.db.checkers;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultRecordChecker implements RecordChecker
{
	@Override
	public void checkRecord(Result result, Set<String> columnNames, List<DBFieldMapping> mapping)
	{
		List<DBFieldMapping> absentFields = null;
		for (DBFieldMapping fm : mapping)
		{
			if (!columnNames.contains(fm.getSrcField()))
			{
				if (absentFields == null)
					absentFields = new ArrayList<>();
				absentFields.add(fm);
			}
		}
		if (absentFields != null)
		{
			result.setSuccess(false);
			CommaBuilder cb = new CommaBuilder();
			for (DBFieldMapping fm : absentFields)
			{
				cb.append(fm.getSrcField()).add(" (").add(fm.getDestField()).add(')');
			}
			result.appendComment(String.format("Query result doesn't contain the following fields: %s.%s", cb, Utils.EOL));
		}
	}
}
