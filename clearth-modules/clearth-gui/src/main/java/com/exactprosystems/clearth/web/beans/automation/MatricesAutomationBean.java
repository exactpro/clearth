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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.matrix.linked.LocalMatrixProvider;
import com.exactprosystems.clearth.tools.ConfigMakerTool;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.MatrixUploadHandler;
import com.exactprosystems.clearth.utils.exception.MatrixUploadHandlerException;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.beans.tools.ConfigMakerToolBean;
import com.exactprosystems.clearth.web.misc.MatrixIssue;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.io.FilenameUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import javax.activation.MimetypesFileTypeMap;

import static com.exactprosystems.clearth.ClearThCore.configFiles;
import static com.exactprosystems.clearth.automation.MatrixFileExtensions.isExtensionSupported;
import static java.lang.String.format;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MatricesAutomationBean extends ClearThBean {

	protected AutomationBean automationBean;

	protected final List<MatrixData> selectedMatrices;

	protected boolean matricesExecuteAll = true;
	protected boolean matricesTrimAll = true;

	protected MatrixUploadHandler matrixUploadHandler;

	protected MatrixData selectedLinkedMatrix = null;
	
	protected boolean googleSpreadsheetsAvailable;
	


	public MatricesAutomationBean()
	{
		this.selectedMatrices = new ArrayList<>();
		this.matrixUploadHandler = createMatrixUploadHandler();
		this.createLinkedMatrix();
		
		this.googleSpreadsheetsAvailable = ClearThCore.getInstance().getGoogleMatricesConfiguration().isEnabled();
		
	}

	protected Scheduler selectedScheduler() {
		return this.automationBean.selectedScheduler;
	}

	public void setAutomationBean(AutomationBean automationBean) {
		this.automationBean = automationBean;
	}

	protected MatrixUploadHandler createMatrixUploadHandler()
	{
		return new MatrixUploadHandler(new File(ClearThCore.automationStoragePath()));
	}


	/* Matrices management */

	public List<MatrixData> getSelectedMatrices()
	{
		return selectedMatrices;
	}

	public void setSelectedMatrices(List<MatrixData> selectedMatrices)
	{
		this.selectedMatrices.clear();
		this.selectedMatrices.addAll(selectedMatrices);
	}


	public MatrixData getOneSelectedMatrix()
	{
		if (selectedMatrices.isEmpty())
			return null;
		return selectedMatrices.get(0);
	}

	public void setOneSelectedMatrix(MatrixData selectedMatrix)
	{
		setSelectedMatrices(Collections.singletonList(selectedMatrix));
	}

	public boolean isOneMatrixSelected()
	{
		return selectedMatrices.size() == 1;
	}


	public List<MatrixData> getMatrices()
	{
		return selectedScheduler().getMatricesData();
	}
	
	public StreamedContent downloadMatrix(MatrixData matrix)
	{
		try
		{
			File f = matrix.getFile();
			return new DefaultStreamedContent(new FileInputStream(f), new MimetypesFileTypeMap().getContentType(f), f.getName());
		}
		catch (IOException e)
		{
			String errMsg = "Could not download matrix";
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
			getLogger().debug(errMsg, e);
			return null;
		}
	}

	public void uploadMatrix(FileUploadEvent event)
	{
		UploadedFile uploadedFile = event.getFile();

		if (uploadedFile.getContents().length == 0)
		{
			MessageUtils.addErrorMessage("Invalid file content", "Uploaded file is empty");
			return;
		}

		String uploadedFileName = uploadedFile.getFileName();

		try
		{
			matrixUploadHandler.handleUploadedFile(uploadedFile.getInputstream(), uploadedFileName, selectedScheduler());
			String details = format("File '%s' is uploaded", uploadedFileName);
			MessageUtils.addInfoMessage("Success", details);
		}
		catch (MatrixUploadHandlerException e)
		{
			MessageUtils.addErrorMessage(e.getMessage(), e.getDetails());
		}
		catch (IOException e)
		{
			String errorMessage = MessageFormat.format("Unexpected error occurred. File '{0}' cannot be opened",
					uploadedFileName);
			WebUtils.logAndGrowlException(errorMessage, e, getLogger());
		}
	}


	protected void resetMatricesSelection()
	{
		selectedMatrices.clear();
		Logger logger = getLogger();
		if (!logger.isInfoEnabled())
			return;

		if (selectedMatrices.size() == 1)
			logger.info("removed matrix '" + selectedMatrices.get(0).getName()+"'");
		else
		{
			CommaBuilder cb = new CommaBuilder();
			for (MatrixData m : selectedMatrices)
				cb.append("'"+m.getName()+"'");
			logger.info("removed matrices "+cb.toString());
		}
	}

	public void removeMatrices()
	{
		selectedScheduler().removeMatrices(selectedMatrices);

		resetMatricesSelection();;
	}

	public void removeAllMatrices()
	{
		selectedScheduler().removeAllMatrices();

		resetMatricesSelection();
	}

	public void saveMatricesPositions()
	{
		synchronized (selectedScheduler())
		{
			selectedScheduler().saveMatricesPositions();
		}
	}

	public void toggleMatrixExecute(MatrixData md)
	{
		selectedScheduler().toggleMatrixExecute(md);
	}

	public void toggleAllMatricesExecute()
	{
		matricesExecuteAll = !matricesExecuteAll;
		selectedScheduler().setMatricesExecute(matricesExecuteAll);
	}

	public void toggleMatrixTrim(MatrixData md)
	{
		selectedScheduler().toggleMatrixTrim(md);
	}

	public void toggleAllMatricesTrim()
	{
		matricesTrimAll = !matricesTrimAll;
		selectedScheduler().setMatricesTrim(matricesTrimAll);
	}

	private List<MatrixIssue> matrixIssuesToList(String header, Map<ActionGeneratorMessageKind, List<String>> issuesMap)
	{
		List<MatrixIssue> result = new ArrayList<MatrixIssue>();

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

	private List<MatrixIssue> matricesIssuesToList(Map<String, List<ActionGeneratorMessage>> mes, Set<ActionGeneratorMessageKind> issuesFilter)
	{
		List<MatrixIssue> result = new ArrayList<MatrixIssue>();

		if ((mes==null) || (mes.size()==0))
			return result;

		LinkedHashMap<ActionGeneratorMessageKind, List<String>> errors = new LinkedHashMap<ActionGeneratorMessageKind, List<String>>(),
				warnings = new LinkedHashMap<ActionGeneratorMessageKind, List<String>>();

		for (Map.Entry<String, List<ActionGeneratorMessage>> entry : mes.entrySet())
		{
			errors.clear();
			warnings.clear();

			for (ActionGeneratorMessage m : entry.getValue())
			{
				if (!issuesFilter.contains(m.kind))
					continue;

				LinkedHashMap<ActionGeneratorMessageKind, List<String>> map = null;
				switch (m.type)
				{
					case ERROR : map = errors; break;
					case WARNING : map = warnings; break;
					default:
						break;
				}

				if (map != null)
				{
					List<String> list = map.get(m.kind);
					if (list == null)
					{
						list = new ArrayList<String>();
						map.put(m.kind, list);
					}
					list.add(m.message);
				}
			}

			if (!errors.isEmpty() || !warnings.isEmpty())
			{
				result.add(MatrixIssue.matrixName(entry.getKey()));
				result.addAll(matrixIssuesToList("ERRORS",errors));
				result.addAll(matrixIssuesToList("WARNINGS",warnings));
			}
		}
		return  result;
	}
	
	public void makeStepsAndApply()
	{
		if (!isOneMatrixSelected())
			return;

		MatrixData selectedMatrix = getOneSelectedMatrix();
		if (selectedMatrix.isAutoReload())
			reloadLinkedMatrix(selectedMatrix);

		File selectedMatrixFile = selectedMatrix.getFile();
		File destDir = new File(ClearThCore.tempPath());
		ConfigMakerTool configMakerTool = ClearThCore.getInstance().getToolsFactory().createConfigMakerTool();
		try
		{
			List<String> warnings = configMakerTool.makeConfigAndApply(selectedScheduler(), selectedMatrixFile, destDir);
			ConfigMakerToolBean.handleWarnings(warnings);
		}
		catch (ClearThException e)
		{
			ConfigMakerToolBean.handleException(e);
		}
	}


	public void reloadLinkedMatrix(MatrixData matrix)
	{
		try {
			selectedScheduler().addLinkedMatrix(matrix);
			MessageUtils.addInfoMessage("Success", "'" + matrix.getName() + "' linked matrix is updated successfully");
			selectedScheduler().checkDuplicatedMatrixNames();
			WebUtils.addCanCloseCallback(true);
		}
		catch (Exception e)
		{
			String msg = "Error while updating '"+matrix.getLink()+"' linked matrix";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, e.getMessage());
		}
	}

	public boolean isMatricesExecuteAll()
	{
		return matricesExecuteAll;
	}

	public void setMatricesExecuteAll(boolean matricesExecuteAll)
	{
		this.matricesExecuteAll = matricesExecuteAll;
	}

	public boolean isMatricesTrimAll()
	{
		return matricesTrimAll;
	}

	public void setMatricesTrimAll(boolean matricesTrimAll)
	{
		this.matricesTrimAll = matricesTrimAll;
	}

	public MatrixData getSelectedLinkedMatrix() {
		return selectedLinkedMatrix;
	}

	public void createLinkedMatrix()
	{
		MatrixDataFactory mdf = ClearThCore.getInstance().getMatrixDataFactory();
		this.selectedLinkedMatrix = mdf.createMatrixData();
		this.selectedLinkedMatrix.setType(LocalMatrixProvider.TYPE);
	}

	public void saveLinkedMatrix()
	{
		String extension = FilenameUtils.getExtension(selectedLinkedMatrix.getLink());
		if (isExtensionSupported(extension))
		{
			try
			{
				selectedLinkedMatrix.setExecute(true);
				selectedScheduler().addLinkedMatrix(selectedLinkedMatrix);
				MatrixDataFactory mdf = ClearThCore.getInstance().getMatrixDataFactory();
				selectedLinkedMatrix = mdf.createMatrixData();
				WebUtils.addCanCloseCallback(true);
			}
			catch (Exception e)
			{
				String msg = "Error while uploading '" + selectedLinkedMatrix.getLink() + "' linked matrix";
				getLogger().error(msg, e);
				MessageUtils.addErrorMessage(msg, e.getMessage());
			}
		}
		else
		{
			String msg = format("Matrix file format must be one of these: %s.",
					MatrixFileExtensions.supportedExtensionsAsString);
			MessageUtils.addErrorMessage("Unsupported matrix file format", msg);
			return;
		}
	}

	public void editLinkedMatrix(MatrixData matrix)
	{
		selectedLinkedMatrix = matrix.clone();
	}

	public String getGoogleSpreadsheetsHelp()
	{
		return "Please share the document for\n" + ClearThCore.getInstance()
				.getGoogleMatricesConfiguration().getServiceEmail();
	}

	public boolean isGoogleSpreadsheetsAvailable() {
		return googleSpreadsheetsAvailable;
	}
}
