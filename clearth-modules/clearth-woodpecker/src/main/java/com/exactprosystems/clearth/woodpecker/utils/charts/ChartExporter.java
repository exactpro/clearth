/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.utils.charts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;

import static com.exactprosystems.clearth.ClearThCore.tempPath;
import static com.exactprosystems.clearth.utils.FileOperationUtils.zipFiles;
import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedReader;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.readFileToString;

public class ChartExporter 
{
	private static final Path TEMPLATES_DIR_PATH = Paths.get(ClearThCore.htmlTemplatesPath(), "chart");
	private static final Path CHART_TEMPLATE_PATH = TEMPLATES_DIR_PATH.resolve("chart.html");
	
	private static final Map<String, String> FILE_NAME_BY_PLACEHOLDER = new HashMap<>();
	static 
	{
		FILE_NAME_BY_PLACEHOLDER.put("{css}", "chartist.min.css");
		FILE_NAME_BY_PLACEHOLDER.put("{chartist}", "chartist.min.js");
		FILE_NAME_BY_PLACEHOLDER.put("{zoom}", "chartist-plugin-zoom.min.js");
		FILE_NAME_BY_PLACEHOLDER.put("{legend}", "chartist-plugin-legend.min.js");
		FILE_NAME_BY_PLACEHOLDER.put("{tooltip}", "chartist-plugin-tooltip.min.js");
		FILE_NAME_BY_PLACEHOLDER.put("{model}", "chart-model.js");
	}
	private static final String DATA_PLACEHOLDER = "{data}";

	
	public File exportChartToTempDir(LoadingChart chart) throws WoodpeckerDaemonException
	{
		File tmpDir = new File(tempPath(), "tmp_chart/");
		try
		{
			createDirectories(tmpDir.toPath());
		}
		catch (IOException e)
		{
			throw new WoodpeckerDaemonException(e, "Unable to create directory '%s' to export chart.", tmpDir);
		}

		File tmpFile = new File(tmpDir, "/chart.html");

		exportChartToFile(chart, tmpFile);

		File zipFile = zipChartFiles(tmpDir);

		deleteQuietly(tmpDir);

		return zipFile;
	}

	public void exportChartToFile(LoadingChart chart, File filePath) throws WoodpeckerDaemonException
	{
		ObjectMapper mapper = new ObjectMapper();
		try (BufferedReader reader = newBufferedReader(CHART_TEMPLATE_PATH, UTF_8);
		     BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
		     JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(writer))
		{
			String currentLine;
			while ((currentLine = reader.readLine()) != null) 
			{
				String fileNameToInline = FILE_NAME_BY_PLACEHOLDER.get(currentLine);
				if (fileNameToInline != null)
				{
					Path path = TEMPLATES_DIR_PATH.resolve(fileNameToInline);
					writer.write(readFileToString(path.toFile(), UTF_8));
					writer.newLine();
				}
				else if (DATA_PLACEHOLDER.equals(currentLine))
				{
					writer.write("var data = ");
					mapper.writeValue(jsonGenerator, chart);
					writer.write(";");
					writer.newLine();
				}
				else
					writer.write(currentLine);
			}
		}
		catch (IOException e)
		{
			throw new WoodpeckerDaemonException(e, "Unable to export chart to file.");
		}
	}

	private File zipChartFiles(File exportFolder) throws WoodpeckerDaemonException
	{
		try
		{
			File[] filesToZip = exportFolder.listFiles();
			if (filesToZip == null)
				throw new WoodpeckerDaemonException("Files to zip aren't found in '%s'.", exportFolder);

			File resultFile = createTempFile("chart_report_", ".zip", new File(tempPath()));
			zipFiles(resultFile, filesToZip);
			return resultFile;
		}
		catch (IOException e)
		{
			throw new WoodpeckerDaemonException("Unable to zip chart export.", e);
		}
	}
}
