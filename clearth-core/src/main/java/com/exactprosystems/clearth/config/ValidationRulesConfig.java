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

package com.exactprosystems.clearth.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlType;

import com.exactprosystems.clearth.connectivity.validation.ClearThConnectionValidationRule;
import com.exactprosystems.clearth.utils.SettingsException;

@XmlType(name = "validationRules")
public class ValidationRulesConfig
{
	private volatile List<String> ruleClass;
	
	public ValidationRulesConfig() {}
	
	@Override
	public String toString()
	{
		return getRuleClass().toString();
	}
	
	
	public List<String> getRuleClass()
	{
		if (ruleClass == null)
			ruleClass = new ArrayList<>();
		return ruleClass;
	}
	
	public void setRuleClass(List<String> ruleClass)
	{
		this.ruleClass = ruleClass;
	}
	
	
	public Set<ClearThConnectionValidationRule> createRules() throws SettingsException
	{
		List<String> rules = getRuleClass();
		if (rules.isEmpty())
			return Collections.emptySet();
		
		Set<ClearThConnectionValidationRule> result = new LinkedHashSet<>();
		for (String r : rules)
		{
			try
			{
				ClearThConnectionValidationRule newRule = Class.forName(r).asSubclass(ClearThConnectionValidationRule.class).newInstance();
				result.add(newRule);
			}
			catch (Exception e)
			{
				throw new SettingsException("Could not instantiate rule of class '"+r+"'", e);
			}
		}
		return result;
	}
}
