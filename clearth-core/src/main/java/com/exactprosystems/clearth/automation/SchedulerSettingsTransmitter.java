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
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerSettingsTransmitter
{
	private static final String SETTINGS_FOLDER = "settings";
	private static final String MATRICES_FOLDER = "matrices";

	private static final List<String> NEEDLESS_SETTINGS_NAMES = Arrays.asList("launches.xml", "configdata.cfg");

	public File exportSettings(SchedulerData schedulerData) throws IOException
	{
		File schedulerDir = schedulerData.getSchedulerDir();
		List<File> settings = getSettings(schedulerDir);
		if (settings == null || settings.isEmpty())
			return null;

		String destDir = ClearThCore.tempPath();

		File exportFolder = FileOperationUtils.createTempDirectory("scheduler_data_", new File(destDir));
		File settingsFolder = new File(exportFolder, SETTINGS_FOLDER);
		File matricesFolder = new File(exportFolder, MATRICES_FOLDER);
		
		Files.createDirectories(settingsFolder.toPath());
		Files.createDirectories(matricesFolder.toPath());
		String settingsPath = settingsFolder.getCanonicalPath();
		String matricesPath = matricesFolder.getCanonicalPath();

		for (File file : settings)
			FileOperationUtils.copyFile(file.getCanonicalPath(), settingsPath+File.separator+file.getName());

		List<MatrixData> matrices = schedulerData.getMatrices();
		for (MatrixData matrixData : matrices)
			FileOperationUtils.copyFile(matrixData.getFile().getCanonicalPath(), matricesPath+File.separator+matrixData.getFile().getName());

		File resultFile = File.createTempFile(schedulerData.getName()+"_data_", ".zip", new File(destDir));
		FileOperationUtils.zipDirectories(resultFile, Arrays.asList(settingsFolder, matricesFolder));
		clearSettingsDir(exportFolder);

		return resultFile;
	}

	protected List<File> getSettings(File schedulerDir) throws IOException
	{
		File[] schedulerFiles = schedulerDir.listFiles();
		if (schedulerFiles == null)
			throw new IOException("Scheduler settings not found");

		List<File> settings = new ArrayList<File>();
		List<String> toExclude = getNeedlessSettingsNames();
		for (File f : schedulerFiles)
			if (!toExclude.contains(f.getName()) && !f.isDirectory())
				settings.add(f);
		return settings;
	}

	protected List<String> getNeedlessSettingsNames()
	{
		return NEEDLESS_SETTINGS_NAMES;
	}

	public synchronized void deploySettings(File zipSettings, Scheduler scheduler) throws Exception
	{
		final File unzippedSettingsDir = unzipSettings(zipSettings);
		copySchedulerSettings(unzippedSettingsDir.listFiles(), getSchedulerPath(scheduler), scheduler);
		scheduler.reloadSchedulerData();
		clearSettingsDir(unzippedSettingsDir);
	}

	private File unzipSettings(File zippedSettings) throws IOException {
		File unzippedSettings = FileOperationUtils.uniqueFileName(new File(ClearThCore.uploadStoragePath()), "imported_settings", "");
		unzippedSettings.mkdir();
		FileOperationUtils.unzipFile(zippedSettings, unzippedSettings);
		if (!checkSettingsStructure(unzippedSettings))
			throw new IOException("Incorrect format of settings");
		return unzippedSettings;
	}

	private String getSchedulerPath(Scheduler scheduler) throws IOException {
		return scheduler.getSchedulerData().getSchedulerDir().getCanonicalPath();
	}

	private void copySchedulerSettings(File[] configSubDirs, String schedulerDirPath, Scheduler scheduler) throws IOException {
		final File settingsDir = getDirWithName(SETTINGS_FOLDER, configSubDirs);
		final File[] settingFiles = settingsDir.listFiles();
		if (settingFiles != null)
			for (File setting : settingFiles)
				FileUtils.copyFile(setting, new File(schedulerDirPath+"/"+setting.getName()));

		final File matricesDir = getDirWithName(MATRICES_FOLDER, configSubDirs);
		if (matricesDir != null) {
			File[] matrices = matricesDir.listFiles();
			if (matrices != null)
				for (File matrix : matrices)
					FileUtils.copyFile(matrix, new File(scheduler.getScriptsDir() + matrix.getName()));
		}
	}

	private File getDirWithName(String name, File[] dirs) {
		for (int i = 0; i < dirs.length; i++) {
			if (dirs[i].getName().equals(name))
				return dirs[i];
		}
		return null;
	}

	private boolean checkSettingsStructure(File settingsDir)
	{
		String[] subDirNames = settingsDir.list();
		if (subDirNames == null)
			return false;
		List<String> dirsList = Arrays.asList(subDirNames);
		return dirsList.contains(SETTINGS_FOLDER);
	}

	private void clearSettingsDir(File settingsDir) throws IOException
	{
		File[] files = settingsDir.listFiles();
		if (files != null)
		for (File f : files)
		{
			if (f.isDirectory())
				FileUtils.deleteDirectory(f);
			else
				f.delete();
		}
		settingsDir.delete();
	}
}
