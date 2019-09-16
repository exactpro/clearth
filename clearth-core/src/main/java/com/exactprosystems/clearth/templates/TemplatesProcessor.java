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
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by victor.klochkov on 5/5/17.
 */
public abstract class TemplatesProcessor
{
	protected Configuration configuration;

	protected static final Map<String, Object> COMMON_PARAMETERS = new HashMap<String, Object>()
	{{
		BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_23).build();
		TemplateHashModel staticModels = wrapper.getStaticModels();
		put("statics", staticModels);
	}};

	public TemplatesProcessor() throws IOException, TemplateModelException
	{
		configuration = createConfiguration();
	}

	public void processTemplate(Writer out, Map<String, Object> parameters, String fileName) throws IOException, TemplateException
	{
		Template template = configuration.getTemplate(fileName);
		parameters.putAll(getParameters());
		template.process(parameters, out);
	}

	protected Configuration createConfiguration() throws IOException, TemplateModelException
	{
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
		configuration.setDirectoryForTemplateLoading(new File(ClearThCore.htmlTemplatesPath()));
		configuration.setDefaultEncoding("UTF-8");
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		configuration.setSharedVariable("includeFile", new IncludeFileDirective());
		configuration.setSharedVariable("instanceOf", new InstanceOfMethod());
		return configuration;
	}

	protected Map<String, Object> getParameters()
	{
		return COMMON_PARAMETERS;
	}

	public abstract void addParameter(String key, Object value);
}
