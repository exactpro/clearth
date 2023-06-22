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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdater;
import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdaterException;
import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Condition;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.tools.matrixupdater.settings.UpdateType;
import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterUtils;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import org.apache.commons.compress.utils.FileNameUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@SuppressWarnings({"unused", "WeakerAccess"})
public class MatrixUpdaterToolBean extends ClearThBean
{
	private final Path uploadsStorage = Paths.get(ClearThCore.uploadStoragePath());
	private MatrixUpdater matrixUpdater;
	private UpdaterThread updateThread;

	private boolean needToUploadChanges = false,
			updateTableEditMode = false;

	private UploadedFile matrixFile = null;

	private String column = null,
			value = null,
			updateName = null,
			conditionName = null;
	
	private Update currentUpdate;
	private Condition currentCondition;
	private UpdateType updateType;
	private List<UpdateType> actionTypesList;

	private Cell newConditionCell = new Cell();
	
	@PostConstruct
	public void init()
	{
		matrixUpdater = new MatrixUpdater(UserInfoUtils.getUserName());

		actionTypesList = Arrays.asList(UpdateType.values());
	}

	public void start()
	{
		updateThread = null;
		matrixUpdater.cleanProgress();
		
		if (matrixFile == null)
		{
			MessageUtils.addWarningMessage("Matrices for update are not uploaded", "");
			return;
		}

		if (isWorkingNow())
		{
			MessageUtils.addWarningMessage("Matrix updater is already working", "");
			return;
		}

		final File matrices;

		try
		{
			matrices = storeFile(matrixFile, matrixUpdater.getConfigDir().resolve(matrixFile.getFileName()));
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while storing matrices", e, getLogger());
			return;
		}

		updateThread = new UpdaterThread(matrices);
		updateThread.start();
	}

	public void cancel()
	{
		if (isRunning()) matrixUpdater.cancel();
	}
	
	
	public StreamedContent downloadResult()
	{
		try
		{
			File f = matrixUpdater.getResult();
			return WebUtils.downloadFile(f);
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Result download error", e, getLogger());
			return null;
		}
	}

	private File storeFile(UploadedFile file, Path destFile) throws IOException
	{
		if (!Files.exists(destFile))
		{
			Files.createDirectories(destFile.getParent());
			Files.createFile(destFile);
		}
		
		return FileOperationUtils.storeToFile(file.getInputStream(), destFile.toFile());
	}

	private Cell createCell(String column, String value)
	{
		return new Cell(column, value);
	}

	/** UPDATES */
	public void createUpdate()
	{
		if (isNotBlank(updateName))
		{
			matrixUpdater.getConfig().addUpdate(updateName, updateType);
			updateName = null;
		}
	}
	
	public void removeUpdate(Update update)
	{
		currentUpdate = null;
		currentCondition = null;
		matrixUpdater.getConfig().removeUpdate(update);
	}
	
	public void switchUpdatesEditMode() { this.updateTableEditMode = !this.updateTableEditMode; }
	
	/** CONDITIONS **/
	public void createCondition()
	{
		if (isNotBlank(conditionName) && currentUpdate != null)
		{
			currentUpdate.getSettings().addCondition(conditionName);
			conditionName = null;
		}
	}
	
	public void removeCondition(Condition condition)
	{
		currentCondition = null;
		currentUpdate.getSettings().removeCondition(condition);
	}

	public void removeConditionCell(Condition condition, Cell cell)
	{
		if (condition.getCells() != null && cell != null)
			condition.getCells().remove(cell);
	}
	
	public void addConditionCell()
	{
		String column = newConditionCell.getColumn();
		String value = newConditionCell.getValue();

		if (isNotBlank(column) && isNotBlank(value) && currentCondition != null)
			currentCondition.addCell(createCell(column, value));

		newConditionCell = new Cell();
		currentCondition = null;
	}
	
	/** CHANGES **/
	public void uploadAdditionFile(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();

		if (file == null || file.getContent().length == 0)
			return;

		try
		{
			File additionFile = storeFile(file, matrixUpdater.getConfigDir().resolve(file.getFileName()));
			currentUpdate.getSettings().getChange().setAddition(additionFile.getName());
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while uploading addition", e, getLogger());
		}
	}

	public void addChangeCell()
	{
		if (isNotBlank(column) && isNotBlank(value) && currentUpdate != null)
		{
			currentUpdate.getSettings().getChange().addCell(createCell(column, value));
			column = value = null;
		}
	}
	
	public void removeChangeCell(Cell change)
	{
		if (currentUpdate.getSettings().getChange().getCells() != null && change != null)
			currentUpdate.getSettings().getChange().getCells().remove(change);
	}
	
	public void switchChangeType()
	{
		needToUploadChanges = currentUpdate != null && currentUpdate.getProcess() == UpdateType.ADD_ACTIONS;
	}

	/** Matrices */
	public void uploadMatrixFile(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();

		if (file == null || file.getContent().length == 0)
			return;

		if (!MatrixUpdaterUtils.isValidExtension(file.getFileName()))
		{
			MessageUtils.addErrorMessage("Invalid matrix file",
					"Wrong file extension. Allowed types: zip or csv/xls/xlsx.");
			return;
		}

		matrixFile = file;
		MessageUtils.addInfoMessage("Matrix file has been uploaded", matrixFile.getFileName());
	}

	/** SETTINGS */
	public void reset()
	{
		this.currentCondition = null;
		this.currentUpdate = null;
		matrixFile = null;
		column = null;
		value = null;
		updateName	= null;
		conditionName = null;
		matrixUpdater.reset();
	}

	public StreamedContent saveSettings()
	{
		try
		{
			return WebUtils.downloadFile(matrixUpdater.saveConfig().toFile());
		}
		catch (JAXBException | IOException e)
		{
			WebUtils.logAndGrowlException("Error while saving config", e, getLogger());
			return null;
		}
	}
	
	public void uploadConfigFile(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();

		if (file == null || file.getContent().length == 0)
			return;

		currentUpdate = null;
		switchChangeType();
		
		try
		{
			String fileName = file.getFileName();
			Path storedConfigPath = Files.createTempFile(uploadsStorage, 
					FileNameUtils.getBaseName(fileName)+"_", 
					"."+FileNameUtils.getExtension(fileName));
			File storedConfig = storeFile(file, storedConfigPath);
			matrixUpdater.setConfig(storedConfig);
		}
		catch (MatrixUpdaterException | IOException | JAXBException e)
		{
			WebUtils.logAndGrowlException("Error while loading config", e, getLogger());
		}
	}

	/** GETTERS */
	public boolean isWorkingNow()
	{
		return updateThread != null && updateThread.isAlive();
	}

	public boolean isNeedToUploadChanges()
	{
		return currentUpdate != null && currentUpdate.getProcess() == UpdateType.ADD_ACTIONS;
	}

	public boolean isBefore()
	{
		return currentUpdate == null || currentUpdate.getSettings().getChange().isBefore();
	}

	public boolean isUpdateIDs()
	{
		return needToUploadChanges && currentUpdate != null ? currentUpdate.getSettings().getChange().isUpdateIDs() : false;
	}

	public boolean isDisableChanges()
	{
		return currentUpdate == null || currentUpdate.getProcess() == UpdateType.REMOVE_ACTIONS;
	}

	public boolean getUpdateTableEditMode() { return updateTableEditMode; }

	public boolean isRunning()
	{
		return updateThread != null && matrixUpdater != null && matrixUpdater.isRunning();
	}

	public boolean isResultAvailable()
	{
		return matrixUpdater != null && !isRunning() && matrixUpdater.getResult() != null;
	}

	public boolean isCanceled() { return matrixUpdater.isCanceled(); }

	public String getColumn(){ return column; }

	public String getValue(){ return value; }

	public List<Cell> getConditions()
	{
		return currentCondition == null ? null : currentCondition.getCells();
	}

	public List<Cell> getChanges()
	{
		return currentUpdate == null? null : currentUpdate.getSettings().getChange().getCells();
	}

	public List<UpdateType> getActionTypesList() { return  actionTypesList; }

	public List<Update> getUpdates(){ return matrixUpdater.getConfig().getUpdates(); }

	public List<Condition> getConditionsList()
	{
		return currentUpdate == null? null: currentUpdate.getSettings().getConditions();
	}

	public Update getCurrentUpdate(){ return currentUpdate; }

	public Condition getCurrentCondition(){ return currentCondition; }

	public UpdateType getUpdateType(){ return updateType; }

	public String getConditionsActiveIndexes()
	{
		int size;

		if (currentUpdate == null
				|| (size = currentUpdate.getSettings().getConditions().size()) == 0) return "0";

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < size; i++)
			sb.append(i).append(",");

		sb.deleteCharAt(sb.length()-1);

		return sb.toString();
	}

	public String getUpdateName(){ return updateName; }

	public String getConditionName() { return conditionName; }

	public Cell getNewConditionCell() { return newConditionCell; }

	public String getCurrentMatrixFileName()
	{
		return matrixFile == null ? null : matrixFile.getFileName();
	}

	public int getProgress()
	{
		return matrixUpdater == null ? 0 : matrixUpdater.getProgress();
	}

	public String getAdditionFile()
	{
		if (currentUpdate != null && currentUpdate.getSettings() !=null && currentUpdate.getSettings().getChange() !=null
				&& currentUpdate.getSettings().getChange().getAddition() != null)
		{
			return currentUpdate.getSettings().getChange().getAddition();
		}
		return null;
	}

	public String getError()
	{
		return updateThread == null ? "" : updateThread.getError();
	}

	public List<String> getWarning()
	{
		return updateThread == null ? null : updateThread.getWarning();
	}
	
	/** SETTER */
	public void setColumn(String column) { this.column = column; }

	public void setValue(String value) { this.value = value; }

	public void setCurrentUpdate(Update currentUpdate)
	{
		currentCondition = null;
		this.currentUpdate = currentUpdate;
		switchChangeType();
	}

	public void setCurrentCondition(Condition currentCondition) { this.currentCondition = currentCondition; }

	public void setUpdateName(String updateName) { this.updateName = updateName; }

	public void setUpdateType(UpdateType updateType) { this.updateType = updateType; }

	public void setConditionName(String conditionName) { this.conditionName = conditionName; }
	
	public void setBefore(boolean before)
	{
		if (currentUpdate != null)
			currentUpdate.getSettings().getChange().setBefore(before);
	}

	public void setUpdateIDs(boolean updateIDs)
	{
		if (needToUploadChanges && currentUpdate != null)
			currentUpdate.getSettings().getChange().setUpdateIDs(updateIDs);
	}
	
	/** Internal class **/
	class UpdaterThread extends Thread
	{
		private File result;
		private String error;
		private File matrices;
		private List<String> warning;
		
		public UpdaterThread(File matrices)
		{
			this.matrices = matrices;
		}
		
		@Override
		public void run()
		{
			try
			{
				matrixUpdater.update(matrices);
				warning = matrixUpdater.getDuplicatedHeaderFields();
				getLogger().info("Matrix update completed");
			}
			catch (Exception e)
			{
				getLogger().error("Could not update matrices", e);
				error = ExceptionUtils.getDetailedMessage(e);  
			}
		}
		
		public File getResult()
		{
			return result;
		}
		
		public String getError()
		{
			return error;
		}

		public List<String> getWarning()
		{
			return warning;
		}

	}
}
