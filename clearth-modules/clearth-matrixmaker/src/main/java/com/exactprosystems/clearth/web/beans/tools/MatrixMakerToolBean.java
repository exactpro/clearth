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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFactory;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFromConfigTool;
import com.exactprosystems.clearth.tools.matrixmaker.MatrixMakerFromFilesTool;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import org.apache.commons.io.FilenameUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.annotation.PreDestroy;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MatrixMakerToolBean extends ClearThBean
{
	private UploadedFile file = null;
	
	protected File mmUploadDir = null;
	public static final String UPLOAD_DIR = "MatrixMakerUploadDir";
	
	protected List<String> mmUploadedFileNames = new ArrayList<String>();
	protected List<String> matrixPatternNames = new ArrayList<String>();
	protected List<String> mmConfigNames = new ArrayList<String>();
	protected boolean mmRandomScenarios = false;
	protected boolean renameMatrices = false;
	
	protected MatrixMakerFromConfigTool generatorGD = null;
	protected List<File> mmUploadConfigs = null;
	protected File generatedMatrices = null;
	
	protected boolean toolWasStopped = false;
	protected final MatrixMakerFactory mmFactory;
	
	protected FutureTask<File> generationTask;
	protected ExecutorService executor = Executors.newSingleThreadExecutor();
	

	public MatrixMakerToolBean()
	{
		mmFactory = createMatrixMakerFactory();
		mmUploadDir = new File(ClearThCore.tempPath() + UPLOAD_DIR);
		mmUploadDir.mkdirs();
		File[] files = mmUploadDir.listFiles();
		if(files != null && files.length > 0)
		{
			for (File file : files)
			{
				file.delete();
			}
		}
	}

	public void handleFileUpload(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if (isFileEmpty(file))
			return;

		mmUploadedFileNames.remove(file.getFileName());
		fileUploader(file, new File(mmUploadDir, file.getFileName()));
		mmUploadedFileNames.add(file.getFileName());
	}
	
	public void handleMatrixPatternUpload(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if (isFileEmpty(file))
			return;

		matrixPatternNames.remove(file.getFileName());
		fileUploader(file, new File(mmUploadDir, file.getFileName()));
		matrixPatternNames.add(file.getFileName());
	}

	public void handleConfigUpload(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if (isFileEmpty(file))
			return;

		mmConfigNames.remove(file.getFileName());
		fileUploader(file, new File(mmUploadDir, file.getFileName()));
		mmConfigNames.add(file.getFileName());
	}
	

	public StreamedContent getZipGeneratedMatrices()
	{
		generatorGD = null;
		mmUploadConfigs = null;
		generatedMatrices = null;
		
		MatrixMakerFromConfigTool generatorMatrices = null;
		try
		{
			List<File> mmUploadConfigs = getUploadConfigs();
			generatorMatrices = createGenerateMatricesGDTool(mmUploadConfigs);
			
			File result = generatorMatrices.generate();
			
			if (generatorMatrices.getMessages().size() > 0)
			{
				for (String mess : generatorMatrices.getMessages())
				{
					MessageUtils.addErrorMessage("Errors while matrix generation", mess);
				}
			}
			
			return getStreamedContent(result);
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Could not generate matrices.", e.getMessage());
			getLogger().debug("Could not generate matrices.", e);
			if (generatorMatrices != null && generatorMatrices.getMessages().size() > 0)
			{
				for (String mess : generatorMatrices.getMessages())
				{
					MessageUtils.addErrorMessage("Errors while matrix generation", mess);
				}
			}
			return null;
		}
	}
	
	protected File getGeneratedMatrices(MatrixMakerFromConfigTool generator) throws Exception
	{
		return generator.generate();
	}
	
	protected List<File> getUploadConfigs() throws Exception
	{
		List<File> uploadConfigs = new ArrayList<File>();
		for (String name : mmConfigNames)
		{
			uploadConfigs.add(new File(mmUploadDir, name));
		}
		
		if(uploadConfigs.size() == 0)
			throw new IllegalArgumentException("There are no upload files for matrix generating");
		return uploadConfigs;
	}
	
	protected StreamedContent getStreamedContent(File result) throws FileNotFoundException
	{
		String fileExtension = FilenameUtils.getExtension(result.getName()).toLowerCase();
		String destFileName = "zip".equals(fileExtension) ? matricesArchiveName() : result.getName();

		return WebUtils.downloadFile(result, destFileName);
	}
	
	public void startTool()
	{
		if (isToolRunning())
		{
			handleErrorMessage("Tool is already running!");
			return;
		}
		
		toolWasStopped = false;
		
		generatedMatrices = null;
		
		mmUploadConfigs = null;
		try
		{
			mmUploadConfigs = getUploadConfigs();
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Could not start tool.", e.getMessage());
			getLogger().error("Could not start tool.", e);
			return;
		}
		generatorGD = createGenerateMatricesGDTool(mmUploadConfigs);
		
		createAndRunGenerationTask();
	}
	
	private void createAndRunGenerationTask()
	{
		generationTask = createGenerationTask();
		executor.submit(generationTask);
	}

	protected FutureTask<File> createGenerationTask()
	{
		return new FutureTask<>(() -> getGeneratedMatrices(generatorGD));
	}

	public void stopTool()
	{
		if (!isToolRunning())
		{
			handleErrorMessage("Tool is not running!");
			return;
		}
		
		handleInfoMessage("Stopping tool...");
		generationTask.cancel(true);
		
		waitForGenerationStop();
		
		generationTask = null;
		generatorGD = null;
		
		toolWasStopped = true;
		handleInfoMessage("Tool has been stopped");
	}
	
	private void waitForGenerationStop()
	{
		Stopwatch stopwatch = Stopwatch.createAndStart(TimeUnit.MINUTES.toMillis(1));
		while (isToolRunning())
		{
			try
			{
				if (stopwatch.isExpired())
				{
					String err = "Could not stop matrix generator";
					handleErrorMessage(err);
				}
				Thread.sleep(100);
			} catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				String err = "Error occurred while waiting for matrices generation to stop";
				handleErrorMessage(err);
			}
		}
	}
	
	public boolean hasResult()
	{
		if (generationTask == null || !generationTask.isDone())
			return false;
		try
		{
			generatedMatrices = generationTask.get();
			return true;
		} catch (InterruptedException e)
		{
			String err = "Generation was interrupted. ";
			handleErrorMessage(err + ExceptionUtils.getDetailedMessage(e.getCause()));
		} catch (ExecutionException e)
		{
			String err = "Error while generating matrices. ";
			handleErrorMessage(err + ExceptionUtils.getDetailedMessage(e.getCause()));
		}
		return false;
	}
	
	@PreDestroy
	private void dispose()
	{
		if (executor != null)
			executor.shutdownNow();
	}

	public StreamedContent getGeneratedMatricesArchive()
	{
		try
		{
			return getStreamedContent(generatedMatrices);
		}
		catch (FileNotFoundException e)
		{
			MessageUtils.addErrorMessage("Could not generate matrices.", e.getMessage());
			getLogger().debug("Could not generate matrices.", e);
			return null;
		}
	}
	
	public String getGeneratedMatrixCount()
	{
		return !isToolRunning() || generatorGD == null ? "" : generatorGD.getGeneratedMatrixCount();
	}
	
	public MatrixMakerFromConfigTool getGeneratorGD()
	{
		return generatorGD;
	}
	
	public File getGeneratedMatrices()
	{
		if (generatedMatrices != null)
		{
			if (toolWasStopped)
				generatedMatrices.delete();
			if (!generatedMatrices.exists())
				generatedMatrices = null;
		}
		return generatedMatrices;
	}
	
	protected String matricesArchiveName()
	{
		return "matrices.zip";
	}
	

	public StreamedContent getGeneratedMatrix()
	{
		try
		{
			if(matrixPatternNames.isEmpty())
			{
				MessageUtils.addErrorMessage("Please upload matrix pattern",
						"You did not upload pattern for generated matrix");
				return null;
			}
			List<File> matrixPatterns = new ArrayList<File>();
			List<File> uploadFiles = new ArrayList<File>();
			
			matrixPatternNames.forEach(name -> matrixPatterns.add(new File(mmUploadDir, name)));
			mmUploadedFileNames.forEach(name -> uploadFiles.add(new File(mmUploadDir, name)));
			
			MatrixMakerFromFilesTool generatorMatrix = createGenerateMatrixTool(matrixPatterns, uploadFiles);
			
			File result = generatorMatrix.generate();
			//generatorMatrix.GenerateAdditionalFiles();
			return WebUtils.downloadFile(result, "new_"+result.getName());

		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Could not download matrix", ExceptionUtils.getDetailedMessage(e));
			getLogger().debug("Could not download matrix", e);
			return null;
		}
		catch (GenerateException e)
		{
			MessageUtils.addErrorMessage("Could not generate matrix.", e.getMessage());
			getLogger().debug("Could not generate matrix.", e);
			return null;
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Could not generate matrix", ExceptionUtils.getDetailedMessage(e));
			getLogger().debug("Could not download matrix", e);
			return null;
		}
	}
	
	
	protected MatrixMakerFactory createMatrixMakerFactory()
	{
		return new MatrixMakerFactory();
	}

	public MatrixMakerFromFilesTool createGenerateMatrixTool(List<File> matrixPatterns, List<File> uploadFiles)
	{
		return mmFactory.createMatrixMakerFromFilesTool(matrixPatterns, uploadFiles, renameMatrices);
	}

	public MatrixMakerFromConfigTool createGenerateMatricesGDTool(List<File> mmUploadFiles)
	{
		MatrixMakerFromConfigTool generator = mmFactory.createMatrixMakerFromConfigTool(mmUploadFiles, mmUploadDir, renameMatrices, mmRandomScenarios);
		generator.setUseOutMessages(true);
		return generator;
	}

	public boolean isMmRandomScenarios()
	{
		return mmRandomScenarios;
	}

	public void setMmRandomScenarios(boolean mmRandomScenarios)
	{
		this.mmRandomScenarios = mmRandomScenarios;
	}

	public boolean isRenameMatrices()
	{
		return renameMatrices;
	}

	public void setRenameMatrices(boolean renameMatrices)
	{
		this.renameMatrices = renameMatrices;
	}
	

	public void removeFile(String fileName)
	{
		new File(mmUploadDir, fileName).delete();
		mmUploadedFileNames.remove(fileName);
	}
	
	public void removeMatrixPattern(String fileName)
	{
		matrixPatternNames.remove(fileName);
	}

	public List<String> getUploadedFileNames()
	{
		return mmUploadedFileNames;
	}

	public List<String> getMatrixPatternNames()
	{
		return matrixPatternNames;
	}

	public void removeConfig(String fileName)
	{
		mmConfigNames.remove(fileName);
	}

	public List<String> getConfigNames()
	{
		return mmConfigNames;
	}

	public UploadedFile getFile()
	{
		return file;
	}

	public void setFile(UploadedFile file)
	{
		this.file = file;
	}


	private void fileUploader(UploadedFile file, File storageDir)
	{
		FileOutputStream output = null;
		try
		{
			output = new FileOutputStream(storageDir);
			output.write(file.getContent());
		}
		catch (IOException e)
		{
			getLogger().error("Error while working with uploaded file '" + file.getFileName() + "'", e);
			MessageUtils.addErrorMessage("Error occurred while working with uploaded file " + file.getFileName(),
					ExceptionUtils.getDetailedMessage(e));
		}
		finally
		{
			if (output != null)
			{
				try
				{
					output.close();
				}
				catch (IOException e)
				{
					getLogger().error("Error while close output.");
				}
			}
		}

		MessageUtils.addInfoMessage("Success", "File "+file.getFileName()+" uploaded.");
	}
	

	private boolean isFileEmpty(UploadedFile file)
	{
		if (file == null || file.getContent().length == 0)
		{
			getLogger().info("Uploaded file is empty");
			MessageUtils.addInfoMessage("Uploaded file is empty", "Uploaded file must not be empty");
			return true;
		}
		return false;
	}


	public boolean isToolRunning()
	{
		tryShowPopupErrorMessages();
		return generationTask != null && !generationTask.isDone();
	}


	protected void handleInfoMessage(String infoMessage)
	{
		getLogger().info(infoMessage);
		MessageUtils.addInfoMessage("Info", infoMessage);
	}
	
	protected void handleErrorMessage(String errorMessage)
	{
		getLogger().error(errorMessage);
		MessageUtils.addErrorMessage("Error", errorMessage);
	}


	private void tryShowPopupErrorMessages()
	{
		if (generatorGD == null || !generatorGD.isUseOutMessages())
			return;

		BlockingQueue<String> outMessages = generatorGD.getOutMessages();
		while (!outMessages.isEmpty())
		{
			String message = outMessages.poll();
			if (message != null)
				handleErrorMessage(message);
		}
	}
}
