/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.matrixmaker;

import com.exactprosystems.clearth.ClearThCore;

import java.io.File;
import java.util.List;

public class MatrixMakerFactory
{
	private static final String CFG_ROOT_FILE = "cfg/matrixMaker/rg_config.cfg";
	private final File tempDir, cfgRootFile, repositoryPath;
	
	public MatrixMakerFactory()
	{
		tempDir = new File(ClearThCore.tempPath());
		cfgRootFile = new File(ClearThCore.rootRelative(CFG_ROOT_FILE));
		repositoryPath = new File(ClearThCore.getInstance().getRepositoryPath());
	}
	
	public MatrixMakerFactory(File tempDir, File cfgRootFile, File repositoryPath)
	{
		this.tempDir = tempDir;
		this.cfgRootFile = cfgRootFile;
		this.repositoryPath = repositoryPath;
	}

	public MatrixMakerFromFilesTool createMatrixMakerFromFilesTool(List<File> templates, List<File> dataFiles, boolean renameMatrices)
	{
		return new MatrixMakerFromFilesTool(templates, dataFiles, tempDir, cfgRootFile, renameMatrices);
	}

	public MatrixMakerFromConfigTool createMatrixMakerFromConfigTool(List<File> uploadConfigs, File uploadDir,
			boolean renameMatrices, boolean randomScenarios)
	{
		return new MatrixMakerFromConfigTool(uploadConfigs, uploadDir, tempDir, cfgRootFile, repositoryPath, renameMatrices, randomScenarios);
	}
}
