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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ConfigFiles;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by alexey.karpukhin on 10/7/15.
 */
public class ActionFactory {

	private static final Logger logger = LoggerFactory.getLogger(ActionFactory.class);

	private Map<String, ActionMetaData> actionsMapping; //map key is action name in lower case
	private Map<String, Logger> loggers;

	public ActionFactory() {
		loggers = new HashMap<String, Logger>();
	}


	public void loadActionsMapping() throws SettingsException {
		this.actionsMapping = new ActionsMapping(true).getDescriptions();
		//Action names will be in lower case so search by containsKey() and get() should be performed by lower case value;
	}

	public void loadActionsMapping(ConfigFiles configData) throws SettingsException {
		this.actionsMapping = new ActionsMapping(
				Paths.get(ClearThCore.rootRelative(
						configData.getActionsMappingFileName())), true).getDescriptions();
		//Action names will be in lower case so search by containsKey() and get() should be performed by lower case value;
	}
	
	public void loadActionsMapping(Path actionsMappingFile) throws SettingsException {
		this.actionsMapping = new ActionsMapping(actionsMappingFile, true).getDescriptions();
		//Action names will be in lower case so search by containsKey() and get() should be performed by lower case value;
	}

	public boolean isDefinedAction(String actionName) {
		try {
			checkActionMapping();
		} catch (AutomationException e) {
			// If actionsmapping is not defined action is not defined as well
			return false;
		}

		if (actionName == null) {
			return false;
		}

		return actionsMapping.containsKey(normaliseActionName(actionName));
	}

	@SuppressWarnings("unchecked")
	public Action createAction(String actionName) throws AutomationException
	{
		checkActionMapping();

		try {
			ActionMetaData metaData = this.actionsMapping.get(normaliseActionName(actionName));
			if (metaData == null) {
				logger.debug("Action with name '" + actionName + "' not found in actionsmapping");
				return null;
			}
			if (metaData.getClazz() == null) {
				throw new AutomationException("Class of action '" + actionName + "' not specified in mapping.");
			}
			Class<?> rawClass = Class.forName(metaData.getClazz());
			Class<? extends Action> actionClass;

			if (Action.class.isAssignableFrom(rawClass)) {
				actionClass = (Class<? extends Action>) rawClass;
			} else {
				throw new AutomationException("Class of action '" + actionName + "' must extends from class 'Action'");
			}

			Action actionInstance = actionClass.newInstance();
			Map<String, String> defaultParams = metaData.getDefaultInputParams();
			if (defaultParams == null) {
				defaultParams = new LinkedHashMap<String, String>();
			}

			Logger actionLogger = this.loggers.get(actionName.toLowerCase().trim());
			if (actionLogger == null) {
				actionLogger = this.createLogger(metaData);
				this.loggers.put(actionName.toLowerCase().trim(), actionLogger);
			}

			actionInstance.preInit(actionLogger, metaData.getName(), defaultParams);

			return actionInstance;
		} catch (ClassNotFoundException cnfe) {
			throw new AutomationException("Class of action '" + actionName + "' not found.", cnfe);
		} catch (InstantiationException e) {
			throw new AutomationException("Error during creating action '" + actionName + "'", e);
		} catch (IllegalAccessException e) {
			throw new AutomationException("Error during creating action '" + actionName + "'", e);
		}

	}

	protected void checkActionMapping() throws AutomationException {
		if (actionsMapping == null || actionsMapping.isEmpty()) {
			throw new AutomationException("Actions mapping not loaded.");
		}
	}

	protected String normaliseActionName(String actionName) {
		return actionName.toLowerCase().trim();
	}

	public ActionSettings createActionSettings()
	{
		return new ActionSettings();
	}

	public Map<String, ActionMetaData> getActionsMapping() {
		return actionsMapping;
	}

	protected Logger createLogger (ActionMetaData actionMetaData) {
		String clazz = actionMetaData.getClazz();
		String actionName = actionMetaData.getName();
		if (clazz == null || clazz.isEmpty()) {
			clazz = Action.class.getName();
		}
		String simpleClassName = clazz.substring(clazz.lastIndexOf('.') + 1);
		String loggerName;
		if (simpleClassName.equals(actionName)) {
			loggerName = clazz;
		} else {
			loggerName = clazz + "(" + actionName + ")";
		}
		return LoggerFactory.getLogger(loggerName);
	}



}
