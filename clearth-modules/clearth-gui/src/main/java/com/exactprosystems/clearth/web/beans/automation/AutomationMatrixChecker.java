/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.automation.ActionGeneratorMessage;
import com.exactprosystems.clearth.automation.ActionGeneratorMessageKind;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.web.misc.MatrixIssue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AutomationMatrixChecker {

	public static final String NO_ERRORS = "No errors in matrices";

	protected Scheduler selectedScheduler = null;
	protected List<MatrixIssue> checkedMatricesIssues;
	protected Map<String, List<ActionGeneratorMessage>> matricesIssuesMap;
	protected Set<ActionGeneratorMessageKind> issuesFilter;
	protected String checkedMatricesErrors;

	public AutomationMatrixChecker() {
		this.checkedMatricesIssues = new ArrayList<>();
		this.issuesFilter = new HashSet<>(Arrays.asList(ActionGeneratorMessageKind.values()));
		
		this.checkedMatricesErrors = NO_ERRORS;
	}

	public void setSelectedScheduler(Scheduler selectedScheduler) {
		this.selectedScheduler = selectedScheduler;
	}

	private List<MatrixIssue> matricesIssuesToList(Map<String, List<ActionGeneratorMessage>> mes, Set<ActionGeneratorMessageKind> issuesFilter)
	{
		List<MatrixIssue> result = new ArrayList<>();

		if ((mes==null) || (mes.size()==0))
			return result;

		LinkedHashMap<ActionGeneratorMessageKind, List<String>> fatalErrors = new LinkedHashMap<>(),
				errors = new LinkedHashMap<>(), warnings = new LinkedHashMap<>();

		for (Map.Entry<String, List<ActionGeneratorMessage>> entry : mes.entrySet())
		{
			fatalErrors.clear();
			errors.clear();
			warnings.clear();

			for (ActionGeneratorMessage m : entry.getValue())
			{
				if (!issuesFilter.contains(m.kind))
					continue;

				LinkedHashMap<ActionGeneratorMessageKind, List<String>> map = null;
				switch (m.type)
				{
					case FATAL_ERROR : map = fatalErrors; break;
					case ERROR : map = errors; break;
					case WARNING : map = warnings; break;
					default:
						break;
				}

				if (map != null)
				{
					List<String> list = map.computeIfAbsent(m.kind, k -> new ArrayList<>());
					list.add(m.message);
				}
			}

			if (!fatalErrors.isEmpty() || !errors.isEmpty() || !warnings.isEmpty())
			{
				result.add(MatrixIssue.matrixName(entry.getKey()));
				result.addAll(matrixIssuesToList("FATAL ERRORS, scheduler will not be started until they are fixed", fatalErrors));
				result.addAll(matrixIssuesToList("ERRORS", errors));
				result.addAll(matrixIssuesToList("WARNINGS", warnings));
			}
		}
		return result;
	}

	private List<MatrixIssue> matrixIssuesToList(String header, Map<ActionGeneratorMessageKind, List<String>> issuesMap)
	{
		List<MatrixIssue> result = new ArrayList<>();

		if (!issuesMap.isEmpty())
		{
			result.add(MatrixIssue.messageType(header + ":"));
			for (ActionGeneratorMessageKind kind : issuesMap.keySet())
			{
				result.add(MatrixIssue.messageKind(kind.getDescription()));
				for (String text : issuesMap.get(kind))
					result.add(MatrixIssue.issue(text));
			}
		}
		return result;
	}

	public void prepareIssues()
	{
		checkedMatricesIssues = matricesIssuesToList(matricesIssuesMap, issuesFilter);
	}

	public void checkMatrices() {
		checkMatrices(true);
	}

	public void refreshMatricesChecking()
	{
		checkMatrices(false);
	}

	private void checkMatrices(boolean fullCheck)
	{
		setIssuesFilter(ActionGeneratorMessageKind.values());
		try
		{
			matricesIssuesMap = fullCheck ?
					selectedScheduler.checkMatrices(selectedScheduler.getMatricesData()) :
					selectedScheduler.getMatricesErrors();  //This will return updated errors list because happens after selectedScheduler.start(), see refreshMatricesChecking() and start()
		}
		catch (Exception e)
		{
			List<MatrixIssue> result = new ArrayList<>();
			result.add(MatrixIssue.matrixName("Could not check matrices. " + e.getMessage()));
			checkedMatricesIssues = result;
		}
	}
	
	public void resetCheckedMatricesErrors() {
		this.checkedMatricesErrors = NO_ERRORS;
	}

	public List<MatrixIssue> getCheckedMatricesIssues()
	{
		if (checkedMatricesIssues.isEmpty())
			checkedMatricesIssues.add(MatrixIssue.matrixName(NO_ERRORS));
		return checkedMatricesIssues;
	}

	public ActionGeneratorMessageKind[] getIssuesKinds()
	{
		return ActionGeneratorMessageKind.values();
	}

	public ActionGeneratorMessageKind[] getIssuesFilter()
	{
		return issuesFilter.toArray(new ActionGeneratorMessageKind[0]);
	}

	public void setIssuesFilter(ActionGeneratorMessageKind[] selectedIssues)
	{
		issuesFilter.clear();
		issuesFilter.addAll(Arrays.asList(selectedIssues));
	}

	public boolean isMatricesHaveErrors()
	{
		return selectedScheduler.getMatricesErrors() != null;
	}
	
	
}
