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

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.util.List;

public class InstanceOfMethod implements TemplateMethodModelEx {

    @Override
    @SuppressWarnings("unchecked")
    public Object exec(List list) throws TemplateModelException
    {
        if (list.size() != 2) {
            throw new TemplateModelException("Wrong arguments for method 'instanceOf'. Method has two required parameters: object and class");
        } else {
            Object object = ((WrapperTemplateModel) list.get(0)).getWrappedObject();
            Object p2 = ((WrapperTemplateModel) list.get(1)).getWrappedObject();
            if (!(p2 instanceof Class)) {
                throw new TemplateModelException("Wrong type of the second parameter. It should be Class. Found: " + p2.getClass());
            } else {
                Class c = (Class) p2;
                return c.isAssignableFrom(object.getClass());
            }
        }
    }
}
