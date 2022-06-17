/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
import com.exactprosystems.clearth.connectivity.CollectorMessage;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.tools.CollectorScannerTool;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;
import javax.faces.event.AjaxBehaviorEvent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class CollectorScannerToolBean extends ClearThBean
{
	private static final String ELLIPSIS = "...";
	private static final int MAX_MSG_LENGTH = 100000, // 100 kB
							 MAX_LENGTH = MAX_MSG_LENGTH + ELLIPSIS.length();

	protected static final List<String> noCollector = new ArrayList<>();
	protected static final List<CollectorMessage> noMessages;
	protected List<CollectorMessage> correctMessages = null, failedMessages = null;
	protected ClearThMessageConnection<?, ?> selectedConnection;
	protected boolean collectorVerticalAlignment = false;
	protected int collectorScannerMessagesTab = 0;
	protected String textToParse = "";
	
	protected CollectorScannerTool collectorScannerTool;
	
	static
	{
		noCollector.add("No messages");
		noMessages = Collections.singletonList(new CollectorMessage(noCollector.get(0), noCollector.get(0), null));
	}
	
	@PostConstruct
	public void init()
	{
		collectorScannerTool = ClearThCore.getInstance().getToolsFactory().createCollectorScannerTool();
	}
	
	public String getSelectedConnection()
	{
		// If the selected running connection was deleted, prevent scanning its collector
		if (!ClearThCore.connectionStorage().containsConnection(selectedConnection))
			selectedConnection = null;
		return selectedConnection == null ? null : selectedConnection.getName();
	}
	
	public void setSelectedConnection(String selectedConnection)
	{
		if (selectedConnection == null)
		{
			this.selectedConnection = null;
			return;
		}
		
		this.selectedConnection = collectorScannerTool.getConnectionByName(selectedConnection);
		if (this.selectedConnection == null)
		{
			getLogger().warn("Connection '{}' not found", selectedConnection);
		}
	}
	
	
	public void loadMessages(AjaxBehaviorEvent event)
	{
		switch (collectorScannerMessagesTab)
		{
			case 0:
				loadCorrectMessages();
				break;
			case 1:
				loadFailedMessages();
				break;
			default:
				break;
		}
	}
	
	public void loadAllMessages()
	{
		loadCorrectMessages();
		loadFailedMessages();
	}
	
	public void loadCorrectMessages()
	{
		if (selectedConnection != null && selectedConnection.isRunning())
		{
			correctMessages = collectorScannerTool.getCollectorMessages(selectedConnection);
			if (correctMessages == null)
				correctMessages = noMessages;
		}
		else
		{
			correctMessages = noMessages;
		}
	}

	public void loadFailedMessages()
	{
		if (selectedConnection != null && selectedConnection.isRunning())
			failedMessages = collectorScannerTool.getCollectorMessagesFailed(selectedConnection);
		else
			failedMessages = null;
	}


	public List<CollectorMessage> getCollectorMessages()
	{
		return correctMessages;
	}

	public String processMessage(String  message)
	{
		if (message.length() > MAX_LENGTH)
			return message.substring(0, MAX_MSG_LENGTH) + ELLIPSIS;
		return message;
	}

	public StreamedContent downloadParsedMessages()
	{
		return downloadMessages(false, getCollectorMessages(), "parsed");
	}
	public StreamedContent downloadFailedMessages()
	{
		return downloadMessages(true, getCollectorMessagesFailed(), "failed");
	}
	public StreamedContent downloadRawMessages()
	{
		return downloadMessages(true, getCollectorMessages(), "raw");
	}
	public StreamedContent downloadMessages(boolean raw, Collection<CollectorMessage> messages, String prefix)
	{
		String conName = getSelectedConnection();
		if(StringUtils.isEmpty(conName))
			return null;

		try
		{
			Path path = Paths.get(ClearThCore.getInstance().getTempDirPath());
			
			File result = Files.createTempFile(path, prefix + "_messages_", ".zip").toFile();
			File file = Files.createTempFile(path, conName+"_", ".txt").toFile();
			
			writeMessages(raw, messages, file);
			
			FileOperationUtils.zipFiles(result, new File[]{file});
			
			return new DefaultStreamedContent(new FileInputStream(result),
											  new MimetypesFileTypeMap().getContentType(result),
											  result.getName());
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while downloading messages", e, getLogger());
			return null;
		}
	}

	public List<CollectorMessage> getCollectorMessagesFailed()
	{
		return failedMessages;
	}
	
	public List<String> getCollectingConnections()
	{
		return collectorScannerTool.getCollectingConnections();
	}
	
	
	public boolean isCollectorVerticalAlignment()
	{
		return collectorVerticalAlignment;
	}
	
	public void setCollectorVerticalAlignment(boolean collectorVerticalAlignment)
	{
		this.collectorVerticalAlignment = collectorVerticalAlignment;
	}
	
	
	public String getCollectorFilter()
	{
		return collectorScannerTool.getCollectorFilter();
	}
	
	public void setCollectorFilter(String collectorFilter)
	{
		collectorScannerTool.setCollectorFilter(collectorFilter);
	
	}
	
	public int getCollectorScannerMessagesTab()
	{
		return this.collectorScannerMessagesTab;
	}
	
	public void setCollectorScannerMessagesTab(int collectorScannerMessagesTab)
	{
		this.collectorScannerMessagesTab = collectorScannerMessagesTab;
	}

	private void writeMessages(boolean raw, Collection<CollectorMessage> messages, File file)
		throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
		{
			for(CollectorMessage message : messages)
			{
				writer.write(raw ? message.getMessage() : message.getParsedMessage());
				writer.newLine();
				writer.newLine();
			}
		}
	}
}
