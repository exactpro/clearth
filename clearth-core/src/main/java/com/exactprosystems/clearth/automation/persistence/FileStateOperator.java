/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;

@SuppressWarnings("rawtypes")
public class FileStateOperator implements ExecutorStateOperator
{
	private static final Logger logger = LoggerFactory.getLogger(FileStateOperator.class);
	
	public static final Class[] ACTIONSTATE_ANNOTATIONS = new Class[]{ActionState.class, ResultState.class},
			STATEINFO_ANNOTATIONS = new Class[]{ExecutorStateInfo.class, StepState.class, StepContext.class},
			STATEOBJECTS_ANNOTATIONS = new Class[]{ExecutorStateObjects.class, MatrixState.class,
					ActionState.class, ResultState.class},
			STATEMATRIX_ANNOTATIONS = new Class[]{MvelVariables.class};
	public static final Class[] ALLOWED_CLASSES = {ExecutorStateInfo.class,
			StepState.class, StepContext.class, XmlMatrixInfo.class,
			ExecutorStateObjects.class, ResultState.class, MvelVariables.class, HashSetValuedHashMap.class,
			ActionState.class, MatrixData.class, MatrixState.class,
			StringTableData.class, TableRow.class};
	public static final String STATEOBJECTS_FILENAME = "stateobjects.xml", STATEINFO_FILENAME = "stateinfo.xml";
	protected static final int MAXACTIONS = 100;
	
	private final Path storageDir;
	private final Class[] actionStateAnnotations,
			stateInfoAnnotations,
			stateObjectsAnnotations,
			stateMatrixAnnotations,
			allowedClasses;
	
	public FileStateOperator(Path storageDir, Class[] actionStateAnnotations, Class[] stateInfoAnnotations,
			Class[] stateObjectsAnnotations, Class[] stateMatrixAnnotations, Class[] allowedClasses)
	{
		this.storageDir = storageDir;
		this.actionStateAnnotations = actionStateAnnotations;
		this.stateInfoAnnotations = stateInfoAnnotations;
		this.stateObjectsAnnotations = stateObjectsAnnotations;
		this.stateMatrixAnnotations = stateMatrixAnnotations;
		this.allowedClasses = allowedClasses;
	}

	public FileStateOperator(Path storageDir)
	{
		this(storageDir, ACTIONSTATE_ANNOTATIONS, STATEINFO_ANNOTATIONS, STATEOBJECTS_ANNOTATIONS, STATEMATRIX_ANNOTATIONS, ALLOWED_CLASSES);
	}
	
	
	@Override
	public void save(ExecutorState state) throws IOException
	{
		if (Files.exists(storageDir))
			FileUtils.deleteDirectory(storageDir.toFile());
		Files.createDirectories(storageDir);
		
		ExecutorStateObjects stateObjects = state.getStateObjects();
		List<MatrixState> matrices = stateObjects.getMatrices();
		if (matrices != null)
		{
			for (MatrixState matrix : matrices)
			{
				String shortName = new File(matrix.getFileName()).getName();
				
				//If there are too many actions, such XML cannot be unmarshalled due to lack of memory. Writing such large list as few portions in separate files
				if (matrix.getActions() != null)// && (matrix.getActions().size()>MAXACTIONS))
				{
					int index = 1;
					List<ActionState> states = new ArrayList<ActionState>();
					for (ActionState action : matrix.getActions())
					{
						states.add(action);
						if (states.size() >= MAXACTIONS)
						{
							saveToXml(states, storageDir.resolve(actionsFileName(shortName, index)),
									actionStateAnnotations);
							index++;
							states.clear();
						}
					}
					if (states.size() > 0)
						saveToXml(states, storageDir.resolve(actionsFileName(shortName, index)),
								actionStateAnnotations);
//					matrix.getActions().clear();
				}
				if (matrix.getMvelVars() != null)
					saveToXml(matrix.getMvelVars(), storageDir.resolve(varsFileName(shortName)), stateMatrixAnnotations);
			}
		}
		
		saveToXml(state.getStateInfo(), storageDir.resolve(STATEINFO_FILENAME), stateInfoAnnotations);
		saveToXml(stateObjects, storageDir.resolve(STATEOBJECTS_FILENAME), stateObjectsAnnotations);
	}
	
	@Override
	public ExecutorState load() throws IOException
	{
		ExecutorStateInfo stateInfo = (ExecutorStateInfo) loadFromXml(storageDir.resolve(STATEINFO_FILENAME), stateInfoAnnotations);
		ExecutorStateObjects stateObjects = (ExecutorStateObjects) loadFromXml(storageDir.resolve(STATEOBJECTS_FILENAME), stateObjectsAnnotations);
		
		List<MatrixState> matrices = stateObjects.getMatrices();
		if (matrices != null)
		{
			for (MatrixState matrix : matrices)
			{
				String shortName = new File(matrix.getFileName()).getName();
				int index = 1;
				Path actionFile = storageDir.resolve(actionsFileName(shortName, index));
				while (Files.isRegularFile(actionFile))
				{
					List<ActionState> actions = (List<ActionState>) XmlUtils.xmlFileToObject(actionFile.toFile(), actionStateAnnotations, allowedClasses);
					if (matrix.getActions() == null)
						matrix.setActions(new ArrayList<ActionState>());
					matrix.getActions().addAll(actions);
					
					index++;
					actionFile = storageDir.resolve(actionsFileName(shortName, index));
				}
				
				Path varsFile = storageDir.resolve(varsFileName(shortName));
				MvelVariables vars = Files.isRegularFile(varsFile)
						? (MvelVariables) loadFromXml(varsFile, stateMatrixAnnotations)
						: ClearThCore.getInstance().getMvelVariablesFactory().create();
				matrix.setMvelVars(vars);
			}
		}
		
		return new ExecutorState(stateInfo, stateObjects);
	}
	
	@Override
	public void update(ExecutorState state, Action lastExecutedAction) throws IOException
	{
		save(state);
	}
	
	
	protected String actionsFileName(String matrixShortFileName, int fileIndex)
	{
		return matrixShortFileName + "_actions_" + fileIndex + ".xml";
	}
	
	protected String varsFileName(String matrixShortFileName)
	{
		return matrixShortFileName + "_vars.xml";
	}
	
	
	private void saveToXml(Object info, Path file, Class[] annotations) throws IOException
	{
		logger.debug("Save to xml object: {} to file: {},  annotations: {}", info, file, annotations);
		XmlUtils.objectToXmlFile(info, file.toFile(), annotations, allowedClasses);
	}
	
	private Object loadFromXml(Path file, Class[] annotations) throws IOException
	{
		if (logger.isDebugEnabled())
			logger.debug("Load from xml file: {}, annotations: {}", file, Arrays.asList(annotations));
		return XmlUtils.xmlFileToObject(file.toFile(), annotations, allowedClasses);
	}
}
