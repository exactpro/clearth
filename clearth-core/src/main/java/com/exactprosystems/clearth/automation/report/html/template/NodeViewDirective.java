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

package com.exactprosystems.clearth.automation.report.html.template;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class NodeViewDirective implements TemplateDirectiveModel
{
	@Override
	public void execute(Environment environment, Map map, TemplateModel[] templateModels, TemplateDirectiveBody body) throws TemplateException, IOException
	{
		if (body != null)
		{
			body.render(new NodeViewFilterWriter(environment.getOut()));
		}
	}

	private static class NodeViewFilterWriter extends Writer
	{
		private final Writer out;

		NodeViewFilterWriter(Writer out)
		{
			this.out = out;
		}

		public void write(char[] buf, int off, int len) throws IOException
		{
			out.write("<span class=\"node\">");
			out.write(buf);
			out.write("</span>");
		}

		public void flush() throws IOException
		{
			out.flush();
		}

		public void close() throws IOException
		{
			out.close();
		}
	}
}
