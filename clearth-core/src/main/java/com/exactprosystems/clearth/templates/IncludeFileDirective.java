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

package com.exactprosystems.clearth.templates;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.Utils;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class IncludeFileDirective implements TemplateDirectiveModel {

	private static final String NAME_PARAM = "name";

	@Override
	public void execute(Environment environment, Map params, TemplateModel[] templateModels,
			TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException
	{
		Object nameParam = params.get(NAME_PARAM);
		if (nameParam == null)
		{
			throw new TemplateException(String.format("The required parameter '%s' is missing", NAME_PARAM), environment);
		}
		else if (!(nameParam instanceof SimpleScalar))
		{
			throw new TemplateException(String.format("The parameter '%s' should be String", NAME_PARAM), environment);
		}
		else
		{
			Writer out = environment.getOut();

			String fileName = ClearThCore.rootRelative(((SimpleScalar)nameParam).getAsString());
			BufferedReader in = null;
			try
			{
				in = new BufferedReader(new FileReader(fileName));

				String line;
				while ((line = in.readLine()) != null)
				{
					out.write(line + Utils.EOL);
				}
			}
			finally
			{
				Utils.closeResource(in);
			}
		}
	}
}
