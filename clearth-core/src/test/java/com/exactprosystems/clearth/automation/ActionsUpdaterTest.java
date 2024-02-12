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

package com.exactprosystems.clearth.automation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.automation.exceptions.ActionUpdateException;
import com.exactprosystems.clearth.config.MatrixFatalErrors;
import com.exactprosystems.clearth.config.SpecialActionParameters;
import com.exactprosystems.clearth.utils.CollectionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;

public class ActionsUpdaterTest
{
	private Path resourcesPath;
	private ActionGeneratorResources generatorResources;
	
	@BeforeClass
	public void init() throws FileNotFoundException, SettingsException
	{
		resourcesPath = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath("ActionsUpdaterTest"));
		
		ActionFactory actionFactory = new ActionFactory();
		actionFactory.loadActionsMapping(resourcesPath.resolve("actionsmapping.cfg"));
		
		MvelVariablesFactory varFactory = new MvelVariablesFactory(null, null);
		MatrixFunctions mf = new MatrixFunctions(null, null, null, false, null);
		
		MatrixFatalErrors fatalErrors = new MatrixFatalErrors();
		fatalErrors.setDuplicateActionId(true);
		
		generatorResources = new ActionGeneratorResources(new SpecialActionParameters(), actionFactory, varFactory, mf, fatalErrors);
	}
	
	@Test(expectedExceptions = ActionUpdateException.class, expectedExceptionsMessageRegExp = "Given current action is not from running matrices")
	public void unknownCurrentAction() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("unknownCurrentAction");
		String stepName = "Step1";
		
		Map<String, Step> originalSteps = createSteps(stepName);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
				updatedMatrices = buildMatrices(createSteps(stepName), createUpdatedMatrixData(matricesPath));
		
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, toList(originalSteps));
		updater.updateFrom(updatedMatrices.get(0).getActions().get(0), updatedMatrices);  //Current action is taken not from original matrix
	}
	
	@Test(expectedExceptions = ActionUpdateException.class,
			expectedExceptionsMessageRegExp = "Current action \\(ID=id2, matrix 'simpleMatrix.csv'\\) differs in updated matrix from action in running matrix")
	public void corruptedCurrentAction() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("corruptedCurrentAction");
		String[] stepNames = {"Step1", "NewStep"};
		
		Map<String, Step> originalSteps = createSteps(stepNames);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
				updatedMatrices = buildMatrices(createSteps(stepNames), createUpdatedMatrixData(matricesPath));
		
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, toList(originalSteps));
		updater.updateFrom(originalMatrices.get(0).getActions().get(1), updatedMatrices);
	}
	
	@Test(expectedExceptions = ActionUpdateException.class, expectedExceptionsMessageRegExp = "Matrix 'matrix2.csv' is not in list of running matrices")
	public void unknownMatrix() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("unknownMatrix");
		String stepName = "Step1";
		
		Map<String, Step> originalSteps = createSteps(stepName);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
					updatedMatrices = buildMatrices(createSteps(stepName), createUpdatedMatrixData(matricesPath));
		
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, toList(originalSteps));
		updater.updateFrom(originalMatrices.get(0).getActions().get(0), updatedMatrices);
	}
	
	@Test
	public void oneMatrixUpdate() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("oneMatrix");
		String initStep = "Init",
				uploadStep = "Upload",
				verifyStep = "Verify",
				setStaticAction = "SetStatic";
		String[] stepNames = {initStep, uploadStep, verifyStep};
		
		Map<String, Step> originalSteps = createSteps(stepNames);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
				updatedMatrices = buildMatrices(createSteps(stepNames), createUpdatedMatrixData(matricesPath));
		
		Matrix originalMatrix = originalMatrices.get(0);
		List<Step> originalStepsList = toList(originalSteps),
				stepsListCopy = toList(originalSteps);
		List<Action> matrixActions = originalMatrix.getActions();
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, originalStepsList);
		updater.updateFrom(matrixActions.get(1), updatedMatrices);  //Current action - id2
		
		Assert.assertEquals(originalStepsList, stepsListCopy, "Steps list");  //Updater should not change the given list of steps
		
		Step originalInitStep = originalSteps.get(initStep);
		List<ActionData> expectedInitStepActions = List.of(
				new ActionData("id1", setStaticAction, CollectionUtils.map("Param1", "Value1"), originalMatrix, originalInitStep),  //I.e. action remained as in original matrix
				new ActionData("id2", setStaticAction, CollectionUtils.map("ParamX", "1", "ParamY", "2"), originalMatrix, originalInitStep),
				new ActionData("id3", setStaticAction, CollectionUtils.map("ParamX", "2"), originalMatrix, originalInitStep));
		List<ActionData> expectedUploadStepActions = List.of(new ActionData("id4", setStaticAction, CollectionUtils.map("FileName", "my.csv"), originalMatrix, originalSteps.get(uploadStep))),
				expectedVerifyStepActions = List.of(new ActionData("id5", "Compare2Values", CollectionUtils.map("Expected", "2", "Actual", "3"), originalMatrix, originalSteps.get(verifyStep)));
				
		List<ActionData> expectedMatrixActions = new ArrayList<>(expectedInitStepActions);
		expectedMatrixActions.addAll(expectedUploadStepActions);
		expectedMatrixActions.addAll(expectedVerifyStepActions);
		
		Assert.assertEquals(toActionData(matrixActions), expectedMatrixActions, "Matrix actions");
		
		Iterator<Step> stepIt = originalStepsList.iterator();
		Assert.assertEquals(toActionData(stepIt.next().getActions()), expectedInitStepActions, "'Init' step actions");
		Assert.assertEquals(toActionData(stepIt.next().getActions()), expectedUploadStepActions, "'Upload' step actions");
		Assert.assertEquals(toActionData(stepIt.next().getActions()), expectedVerifyStepActions, "'Verify' step actions");
	}
	
	@Test
	public void twoMatricesUpdate() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("twoMatrices");
		String initStep = "Init",
				uploadStep = "Upload",
				setStaticAction = "SetStatic";
		String[] stepNames = {initStep, uploadStep};
		
		Map<String, Step> originalSteps = createSteps(stepNames);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
				updatedMatrices = buildMatrices(createSteps(stepNames), createUpdatedMatrixData(matricesPath));
		
		Matrix matrix1 = originalMatrices.get(0),
				matrix2 = originalMatrices.get(1);
		List<Step> originalStepsList = toList(originalSteps);
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, originalStepsList);
		updater.updateFrom(matrix1.getActions().get(0), updatedMatrices);  //Current action - id1_1
		
		Step originalInitStep = originalSteps.get(initStep),
				originalUploadStep = originalSteps.get(uploadStep);
		
		
		List<ActionData> expectedMatrix1_InitStepActions = List.of(new ActionData("id1_1", setStaticAction, CollectionUtils.map("Param1", "Value1.1"), matrix1, originalInitStep)),
				expectedMatrix1_UploadStepActions = List.of(new ActionData("id1_2", setStaticAction, CollectionUtils.map("NewParam", "New1"), matrix1, originalUploadStep));
		
		List<ActionData> expectedMatrix1_Actions = new ArrayList<>(expectedMatrix1_InitStepActions);
		expectedMatrix1_Actions.addAll(expectedMatrix1_UploadStepActions);
		Assert.assertEquals(toActionData(matrix1.getActions()), expectedMatrix1_Actions, "matrix1 actions");
		
		
		List<ActionData> expectedMatrix2_InitStepActions = List.of(new ActionData("id2_1", setStaticAction, CollectionUtils.map("Param1", "Value2.1", "Param2", "Value2.2"), matrix2, originalInitStep)),
				expectedMatrix2_UploadStepActions = List.of(new ActionData("id2_2", setStaticAction, CollectionUtils.map("FileName", "dummy.txt"), matrix2, originalUploadStep));
		
		List<ActionData> expectedMatrix2_Actions = new ArrayList<>(expectedMatrix2_InitStepActions);
		expectedMatrix2_Actions.addAll(expectedMatrix2_UploadStepActions);
		Assert.assertEquals(toActionData(matrix2.getActions()), expectedMatrix2_Actions, "matrix2 actions");
		
		
		Iterator<Step> stepIt = originalStepsList.iterator();
		
		List<ActionData> expectedInitStepActions = new ArrayList<>(expectedMatrix1_InitStepActions);
		expectedInitStepActions.addAll(expectedMatrix2_InitStepActions);
		Assert.assertEquals(toActionData(stepIt.next().getActions()), expectedInitStepActions, "'Init' step actions");
		
		List<ActionData> expectedUploadStepActions = new ArrayList<>(expectedMatrix1_UploadStepActions);
		expectedUploadStepActions.addAll(expectedMatrix2_UploadStepActions);
		Assert.assertEquals(toActionData(stepIt.next().getActions()), expectedUploadStepActions, "'Upload' step actions");
	}
	
	@Test(description = "Order of steps in matrix differs from order of steps in scheduler")
	public void differentStepsOrder() throws IOException, ActionUpdateException
	{
		Path matricesPath = resourcesPath.resolve("stepsOrder");
		String uploadStep = "Upload",
				verifyStep = "Verify",
				setStaticAction = "SetStatic";
		String[] stepNames = {uploadStep, verifyStep};
		
		Map<String, Step> originalSteps = createSteps(stepNames);
		List<Matrix> originalMatrices = buildMatrices(originalSteps, createOriginalMatrixData(matricesPath)),
				updatedMatrices = buildMatrices(createSteps(stepNames), createUpdatedMatrixData(matricesPath));
		
		Matrix originalMatrix = originalMatrices.get(0);
		List<Step> originalStepsList = toList(originalSteps);
		List<Action> matrixActions = originalMatrix.getActions();
		ActionsUpdater updater = new ActionsUpdater(originalMatrices, originalStepsList);
		updater.updateFrom(originalStepsList.get(0).getActions().get(0), updatedMatrices);  //Current action - id1
		
		Step originalVerifyStep = originalSteps.get(verifyStep);
		ActionData id1 = new ActionData("id1", setStaticAction, CollectionUtils.map("PX", "V"), originalMatrix, originalSteps.get(uploadStep)),
				id2 = new ActionData("id2", setStaticAction, CollectionUtils.map("ParamX", "ValueX"), originalMatrix, originalVerifyStep),
				id3 = new ActionData("id3", setStaticAction, CollectionUtils.map("ParamY", "ValueY"), originalMatrix, originalVerifyStep);
		
		List<ActionData> expectedMatrixActions = List.of(id1, id2, id3);  //i.e. ActionsUpdater has re-ordered actions in matrix object according to steps order
		Assert.assertEquals(toActionData(matrixActions), expectedMatrixActions, "Matrix actions");
		
		Iterator<Step> stepIt = originalStepsList.iterator();
		Assert.assertEquals(toActionData(stepIt.next().getActions()), List.of(id1), "'Upload' step actions");
		Assert.assertEquals(toActionData(stepIt.next().getActions()), List.of(id2, id3), "'Verify' step actions");
	}
	
	
	private Map<String, Step> createSteps(String... names)
	{
		Map<String, Step> result = new LinkedHashMap<>(names.length);
		for (String name : names)
		{
			Step step = new DefaultStep(name, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT, false, "", false, false, true, "");
			result.put(name, step);
		}
		return result;
	}
	
	private List<MatrixData> createOriginalMatrixData(Path dir) throws IOException
	{
		return createMatrixData(dir.resolve("original"));
	}
	
	private List<MatrixData> createUpdatedMatrixData(Path dir) throws IOException
	{
		return createMatrixData(dir.resolve("updated"));
	}
	
	private List<MatrixData> createMatrixData(Path matricesDir) throws IOException
	{
		try (Stream<Path> matrices = Files.list(matricesDir)
				.filter(m -> Files.isRegularFile(m))
				.sorted())
		{
			return matrices
					.map(m -> {
						MatrixData md = new MatrixData();
						md.setName(m.getFileName().toString());
						md.setFile(m.toFile());
						md.setExecute(true);
						return md;
					})
					.collect(Collectors.toList());
		}
	}
	
	private List<Matrix> buildMatrices(Map<String, Step> steps, List<MatrixData> matrixData) throws IOException
	{
		List<Matrix> result = new ArrayList<>(matrixData.size());
		
		ActionGenerator generator = new DefaultActionGenerator(steps, result, Collections.emptyMap(), generatorResources);
		try
		{
			for (MatrixData md : matrixData)
				generator.build(md, false);
			return result;
		}
		finally
		{
			generator.dispose();
		}
	}
	
	private List<Step> toList(Map<String, Step> steps)
	{
		return new ArrayList<>(steps.values());
	}
	
	private List<ActionData> toActionData(List<Action> actions)
	{
		return actions.stream()
				.map(a -> new ActionData(a.getIdInMatrix(), a.getName(), a.getInputParams(), a.getMatrix(), a.getStep()))
				.collect(Collectors.toList());
	}
	
	
	private static class ActionData
	{
		private final String id,
				actionName;
		private final Map<String, String> inputParams;
		private final Matrix matrix;
		private final Step step;
		
		public ActionData(String id, String actionName, Map<String, String> inputParams, Matrix matrix, Step step)
		{
			this.id = id;
			this.actionName = actionName;
			this.inputParams = inputParams;
			this.matrix = matrix;
			this.step = step;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(id, actionName, inputParams, matrix, step);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActionData other = (ActionData) obj;
			return Objects.equals(actionName, other.actionName) && Objects.equals(id, other.id)
					&& Objects.equals(inputParams, other.inputParams) && Objects.equals(matrix, other.matrix)
					&& Objects.equals(step, other.step);
		}
		
		@Override
		public String toString()
		{
			return "[id=" + id +
					", actionName=" + actionName +
					", inputParams=" + inputParams +
					", matrix=" + (matrix != null ? matrix.getName()+":"+matrix.hashCode() : "null") +
					", step=" + (step != null ? step.getName()+":"+step.hashCode() : "null") + "]";
		}
	}
}