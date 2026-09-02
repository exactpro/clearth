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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import com.exactprosystems.clearth.woodpecker.configuration.start.*;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;

import java.nio.file.Path;
import java.util.*;

import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile.START_FILE;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException.whenInvalidContent;
import static java.lang.String.format;

public class ActionDaemonStartScriptReader extends StartScriptReader<ActionDaemonStartScript>
{
	
	public ActionDaemonStartScriptReader(Path replicaBaseDir, 
	                                     Class<? extends ActionDaemonStartScript> extendedStartScriptClass, 
	                                     Class[] otherExtendedClasses)
	{
		super(replicaBaseDir, extendedStartScriptClass, otherExtendedClasses);
	}


	public List<DaemonActionDesc> getActionsDescs()
	{
		return startScript.getActionsBlockDesc().getActionsDescs();
	}
	

	@Override
	protected void checkContent(ActionDaemonStartScript startScript, Path path) throws WoodpeckerConfigException
	{
		if (startScript.getActionsBlockDesc() == null)
			throw whenInvalidContent(path, START_FILE.description(),
					format("Required %s is absent.", StartFileElement.ACTIONS_BLOCK));
	}

	
	@Override
	protected Set<String> findExecutableOperationNames(ActionDaemonStartScript startScript)
			throws WoodpeckerConfigException
	{
		Set<String> actionNames = startScript.getActionsBlockDesc().getActionNames();
		if (actionNames.isEmpty())
			throw new WoodpeckerConfigException("Nothing to execute. " +
					"No executable actions are present in Start script.");
		return actionNames;
	}
}
