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

import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

public abstract class MatrixMakerTool
{
	private static final Logger logger = LoggerFactory.getLogger(MatrixMakerTool.class);
	public static final String TEMPLATE = "Template", CONFIG = "Config", TEMPLATE_NAME = "TemplateName";
	public static final String DIR_FOR_MATRICES = "matrixDir", DIR_FOR_UPLOAD = "uploadDir";
	protected boolean renameMatrices;
	protected File tempDir, cfgRootFile;
	protected Map<String, String> fileMapping = new HashMap<>();
	
	public MatrixMakerTool(File tempDir, File cfgRootFile, boolean renameMatrices)
	{
		super();
		this.renameMatrices = renameMatrices;
		this.tempDir = tempDir;
		this.cfgRootFile = cfgRootFile;
	}
	
	protected File prepareMatricesToDownload(File resultDir) throws IOException, GenerateException
	{
		File[] files = resultDir.listFiles();
		if (!fileMapping.isEmpty())
			writeFileMapping(resultDir, fileMapping);
		if (files.length == 0)
			throw new GenerateException("No matrix generated.");
		else if (files.length == 1)
			return files[0];
		else
		{
			File result = new File(resultDir, getArchiveName());
			FileOperationUtils.zipFiles(result, resultDir.listFiles());
			return result;
		}
	}
	
	protected Set<String> readRgParams()
	{
		File file = cfgRootFile;
		if (file == null || !file.exists())
			return new HashSet<String>();
		Properties props = new Properties();
		try
		{
			props.load(new FileReader(file));
			String rgParamStr = props.getProperty("RG_PARAM");
			if (!StringUtils.isEmpty(rgParamStr))
				return new HashSet<>(Arrays.asList(rgParamStr.split(",")));
		}
		catch (Exception e)
		{
			logger.warn("Cannot read properties from file " + file, e);
		}
		return new HashSet<String>();
	}
	
	protected File createDestinationDir(String dirName)
	{
		File tempGenMatrices = new File(tempDir, "GeneratedMatrices");
		if (!tempGenMatrices.exists())
			tempGenMatrices.mkdirs();
		File resultDir = FileOperationUtils.uniqueFileName(tempGenMatrices, dirName, "");
		resultDir.mkdirs();
		return resultDir;
	}

	protected File createUploadDir()
	{
		return createDestinationDir(DIR_FOR_UPLOAD);
	}

	protected File createMatrixDir()
	{
		return createDestinationDir(DIR_FOR_MATRICES);
	}

	protected String getArchiveName()
	{
		return "matrices.zip";
	}
	
	protected MatrixGenerator createMatrixGenerator(List<File> templateFiles, List<File> dataFiles, File resultDir,
			boolean renameMatrices, boolean randomScenarios, String matrixName)
	{
		return new MatrixGenerator(templateFiles, dataFiles, resultDir, renameMatrices, randomScenarios, matrixName);
	}
	

	protected void writeFileMapping(File resultDir, Map<String,String> fileMapping) throws GenerateException 
	{
		StringBuilder sb = new StringBuilder("FileName,FullName").append(Utils.EOL);
		for (Entry<String,String> map : fileMapping.entrySet())
		{
			sb.append(map.getKey()).append(",").append(map.getValue()).append(Utils.EOL);
		}
		try
		{
			FileUtils.writeStringToFile(new File(resultDir,"FileMapping.csv"), sb.toString(), Charset.defaultCharset());
		} catch (IOException e)
		{
			throw new GenerateException("Cannot write file mapping for renamed matrices.", e);
		}
	}
}
