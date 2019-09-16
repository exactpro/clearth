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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.html.ReportParser;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.primefaces.model.UploadedFile;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.IOException;

import static com.exactprosystems.clearth.ClearThCore.configFiles;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class MatrixFromReportToolBean extends ClearThBean
{
	protected final File uploadStorage = new File(ClearThCore.uploadStoragePath());
	private final File destDir = new File(ClearThCore.tempPath());
	protected UploadedFile file = null;
	
	public void makeMatrix()
	{
		if ((file == null) || (file.getContents().length == 0))
		{
			MessageUtils.addWarningMessage("No report selected", "Please select a report to create matrix from");
			return;
		}
		
		File matrix = null;
		try
		{
			File storedFile = WebUtils.storeUploadedFile(file, uploadStorage, "reportformatrix_", ".htm");
			ReportParser rp = ClearThCore.getInstance().getToolsFactory().createReportParser();
			matrix = rp.writeMatrix(storedFile, destDir.getCanonicalPath());
		}
		catch (IOException e)
		{
			String errMsg = "Error while making matrix";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
			return;
		}
		
		ExternalContext ex = FacesContext.getCurrentInstance().getExternalContext();
		String context = WebUtils.getContext();
		try
		{
			ex.redirect(context + "/download?file=" + configFiles().getTempDir() + matrix.getName());
		}
		catch (IOException e)
		{
			getLogger().error("error while redirecting to matrix", e);
			MessageUtils.addErrorMessage("Error occurred while redirecting to matrix", ExceptionUtils.getDetailedMessage(e));
			return;
		}
	}
	
	public UploadedFile getFile()
	{
		return this.file;
	}
	
	public void setFile(UploadedFile file)
	{
		this.file = file;
	}
}
