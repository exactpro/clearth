/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@XmlAccessorType(XmlAccessType.NONE)
public class DaemonActionsBlockDesc
{
	// There are no default actions implementations in Core now


	public List<DaemonActionDesc> getActionsDescs()
	{
		return filterEnabled(getAdditionalActionDescs());
	}
	
	protected List<DaemonActionDesc> getAdditionalActionDescs()
	{
		return emptyList();
	}
	
	private <T extends DaemonActionDesc> List<T> filterEnabled(List<T> actionDescs)
	{
		if (actionDescs == null)
			return emptyList();
		else if (actionDescs.isEmpty())
			return actionDescs;
		else
			return actionDescs.stream()
					.filter(d -> d.isEnabled() && (d.getPart() > 0))
					.collect(toList());
	}

	
	public Set<String> getActionNames()
	{
		Set<String> actionNames = new LinkedHashSet<>();
		getActionsDescs().stream()
				.map(DaemonActionDesc::getActionName)
				.forEach(actionNames::add);
		return actionNames;
	}
}
