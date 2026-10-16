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

package com.exactprosystems.clearth.web.api;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFactory;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFromConfigTool;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFromFilesTool;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.beans.tools.MatrixMakerToolBean;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static com.exactprosystems.clearth.web.api.RestConstants.*;

@Path("tools/matrixmaker")
public class MatrixMakerTool
{
	protected static final Logger logger = LoggerFactory.getLogger(MatrixMakerTool.class);

	protected final File uploadDir = new File(ClearThCore.tempPath() + MatrixMakerToolBean.UPLOAD_DIR);
	protected final MatrixMakerFactory mmFactory = createMatrixMakerFactory();

	@POST
	@Path("/genmatrix")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response generateMatrix(@FormDataParam(PARAM_FILE) InputStream fileStream,
									   @FormDataParam(PARAM_FILE) FormDataContentDisposition fileDetail,
									   @FormDataParam(PARAM_TEMPLATE) InputStream templateStream,
									   @FormDataParam(PARAM_TEMPLATE) FormDataContentDisposition templateDetail,
									   @QueryParam(PARAM_RENAME_MATRICES) boolean renameMatrices,
									   @Context HttpServletRequest request)
	{
		File files;
		try
		{
			files = generateMatrix(fileStream, fileDetail, templateStream, templateDetail, renameMatrices);
		}
		catch (GenerateException e)
		{
			return ApiUtils.errorWithLog("Could not generate matrix.", e, logger);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while making files", e, logger);
		}

		return ApiUtils.fileResponse(files.getAbsolutePath(), files.getName());
	}

	@POST
	@Path("/genfromconfig")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_OCTET_STREAM})
	public Response generateMatrixFromConfig(@FormDataParam(PARAM_FILE) InputStream fileStream,
												 @FormDataParam(PARAM_FILE) FormDataContentDisposition fileDetail,
												 @QueryParam(PARAM_RENAME_MATRICES) boolean renameMatrices,
												 @QueryParam(PARAM_RANDOM_SCENARIOS) boolean randomScenarios,
												 @Context HttpServletRequest request)
	{
		File files;
		try
		{
			files = generateMatricesFromConfig(fileStream, fileDetail,
					renameMatrices, randomScenarios);
		}
		catch (GenerateException e)
		{
			return ApiUtils.errorWithLog("Could not generate matrix.", e, logger);
		}
		catch (Exception e)
		{
			return ApiUtils.errorWithLog("Error while making files", e, logger);
		}

		return ApiUtils.fileResponse(files.getAbsolutePath(), files.getName());
	}

	private File generateMatrix(InputStream fileStream, FormDataContentDisposition fileDetail, InputStream templateStream, FormDataContentDisposition templateDetail, boolean renameMatrices) throws IOException, GenerateException
	{
		if (!uploadDir.exists())
			uploadDir.mkdirs();
		File file = FileOperationUtils.storeToFile(fileStream, uploadDir, fileDetail.getFileName());
		List<File> fileList = FileOperationUtils.unzipIfNeed(file, "unzipped_input");
		
		File template = FileOperationUtils.storeToFile(templateStream, uploadDir, templateDetail.getFileName());
		List<File> templates = FileOperationUtils.unzipIfNeed(template, "unzipped_input");

		MatrixMakerFromFilesTool tool = mmFactory.createMatrixMakerFromFilesTool(templates, fileList, renameMatrices);
		File result = tool.generate();
		return zipIfUnzipped(result);
	}

	private File generateMatricesFromConfig(InputStream fileStream, FormDataContentDisposition fileDetail,
												boolean renameMatrices, boolean randomScenarios) throws Exception
	{
		if (!uploadDir.exists())
			uploadDir.mkdirs();
		File file = FileOperationUtils.storeToFile(fileStream, uploadDir, fileDetail.getFileName());
		List<File> fileList = FileOperationUtils.unzipIfNeed(file, "unzipped_input");
		
		MatrixMakerFromConfigTool tool = mmFactory.createMatrixMakerFromConfigTool(fileList, uploadDir, renameMatrices, randomScenarios);
		File result = tool.generate();
		if (tool.getMessages().size() > 0)
			logger.error("Errors while matrix generation: {}", tool.getMessages());
		return zipIfUnzipped(result);
	}
	
	//use packaging to have unified way of getting tool result
	private static File zipIfUnzipped(File file) throws IOException
	{
		if (FileOperationUtils.isZip(file))
			return file;
		
		File zipFile = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".zip");
		FileOperationUtils.zipFiles(zipFile, Collections.singletonList(file));
		
		return zipFile;
	}
	
	protected MatrixMakerFactory createMatrixMakerFactory()
	{
		return new MatrixMakerFactory();
	}
}
