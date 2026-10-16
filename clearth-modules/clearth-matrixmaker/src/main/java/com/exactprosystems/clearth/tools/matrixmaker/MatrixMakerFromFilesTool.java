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

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MatrixMakerFromFilesTool extends MatrixMakerTool
{
	protected List<File> templates, dataFiles;
	
	public MatrixMakerFromFilesTool(List<File> templates, List<File> dataFiles, File tempDir, File cfgRootFile, boolean renameMatrices)
	{
		super(tempDir, cfgRootFile, renameMatrices);
		this.templates = templates;
		this.dataFiles = dataFiles;
	}
	
	public File generate() throws GenerateException, IOException
	{
		File resultDir = createMatrixDir();
		MatrixGenerator gm = createMatrixGenerator(templates, dataFiles, resultDir, renameMatrices, false, null);
		gm.initRGParams(readRgParams());
		gm.generate();
		gm.GenerateAdditionalFiles();
		
		return prepareMatricesToDownload(resultDir);
	}
}
