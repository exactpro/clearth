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

package com.exactprosystems.clearth.web.beans;

import static com.exactprosystems.clearth.connectivity.connections.ClearThConnectionStorage.MQ;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionErrorInfo;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.web.misc.*;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConnectivityBean extends ClearThBean
{
	private static final String defaultListenerType = ListenerType.File.getLabel();
	
	private final List<ClearThConnection<?, ?>> selectedConnections = new ArrayList<ClearThConnection<?, ?>>();
	private List<ClearThConnection<?, ?>> originalSelectedCons = null;
	private final MqConPropsToEdit mqConProps;
	
	private ListenerConfiguration newListener = createEmptyListener(),
			selectedListener = null;
	private boolean copy = false;
	private boolean copyListners = true;
	private boolean noListenersInfo;
	private boolean listenerInfoVisible = false;
	private FavoriteConnectionManager favoriteConnection;
	private Set<String> favoriteConnectionList;
	private String username;
	private List<ClearThConnection<?, ?>> connections;
	protected String selectedType;
	
	public ConnectivityBean()
	{
		mqConProps = createMqConPropsToEdit();
	}

	@PostConstruct
	protected void init()
	{
		this.favoriteConnection = ClearThCore.getInstance().getFavoriteConnections();
		this.username = UserInfoUtils.getUserName();
		this.favoriteConnectionList = favoriteConnection.getUserFavoriteConnectionList(username);
	}


	public SelectItem[] getConnectionsList()
	{
		List<ClearThConnection<?, ?>> cons = this.getConnections();
		SelectItem[] result = new SelectItem[cons.size()];
		int i = -1;
		for (ClearThConnection<?, ?> c : cons)
		{
			i++;
			result[i] = new SelectItem(c.getName());
		}
		return result;
	}

	public List<ClearThConnection<?, ?>> getSelectedConnections()
	{
		return selectedConnections;
	}

	public void setSelectedConnections(List<ClearThConnection<?, ?>> selectedConnections)
	{
		this.originalSelectedCons = selectedConnections;
		this.selectedConnections.clear();
		for (ClearThConnection<?, ?> c : originalSelectedCons)
			this.selectedConnections.add(c.copy());
	}

	public List<ClearThConnection<?, ?>> getSelectedXConnections(String type)
	{
		ClearThConnection<?, ?> con = getOneSelectedConnection();
		if ((con == null) || (!type.equals(con.getType())))
			return new ArrayList<>();
		return getSelectedConnections();
	}

	public ClearThConnection<?, ?> getOneSelectedConnection()
	{
		if (selectedConnections.isEmpty())
			return null;
		return selectedConnections.get(0);
	}

	protected ClearThConnection<?, ?> getOneSelectedXConnection(String type)
	{
		List<ClearThConnection<?, ?>> cons = getSelectedXConnections(type);
		if (cons.isEmpty())
			return null;
		return cons.get(0);
	}

	protected void setOneSelectedXConnection(ClearThConnection<?, ?> selectedCon, String type)
	{
		List<ClearThConnection<?, ?>> cons = new ArrayList<ClearThConnection<?, ?>>();
		cons.add(selectedCon);
		setSelectedConnections(cons);
	}

	public boolean isOneConnectionSelected()
	{
		return selectedConnections.size() == 1;
	}

	public boolean isOneOrMoreConsSelected()
	{
		return selectedConnections.size() > 0;
	}

	protected MqConPropsToEdit createMqConPropsToEdit()
	{
		return new MqConPropsToEdit();
	}

	public MqConPropsToEdit getMqConProps()
	{
		return mqConProps;
	}

	protected void resetConsSelection()
	{
		originalSelectedCons = null;
		selectedConnections.clear();
	}

	protected void newXConnection(String type)
	{
		resetConsSelection();
		selectedConnections.add(ClearThCore.getInstance().getConnectionStorage().getConnectionFactory(type).createConnection());
	}

	protected boolean editXConnectionProps(ClearThConnection<?, ?> connectionToEdit, ClearThConnection<?, ?> changes, ClearThConnection<?, ?> original)
	{
		if (connectionToEdit instanceof MQConnection)
		{
			editMqConnectionProps((MQConnection)connectionToEdit, (MQConnection)changes, (MQConnection)original, mqConProps);
			return true;
		}
		return false;
	}

	public void saveConnections()
	{
		Logger logger = getLogger();

		if (selectedConnections.size() > 1)  //If multiple connections are edited, JSF changes only the first one. Need to apply changes to all other selected connections. The name shouldn't be changed here!
		{
			//We get here only if multiple connections are edited, thus originalSelectedCons can't be null
			ClearThConnection<?, ?> firstCon = getOneSelectedConnection(),
					originalFirstCon = originalSelectedCons.get(0);
			editXConnectionProps(firstCon, firstCon, originalFirstCon);  //Restoring properties that shouldn't be edited, because JSF has changed all the properties of the first connection
			boolean first = true;
			for (ClearThConnection<?, ?> c : selectedConnections)
			{
				if (first)
				{
					first = false;
					continue;
				}
				editXConnectionProps(c, firstCon, c);
			}
		}

		boolean canClose = true;
		try
		{
			if (originalSelectedCons == null || copy)
			{
				ClearThConnection<?, ?> selCon = getOneSelectedConnection();
				if ((ClearThMessageConnection.isMessageConnection(selCon)) && (!copyListners))
					((ClearThMessageConnection<?, ?>)selCon).setListeners(new ArrayList<ListenerConfiguration>());
				ClearThCore.connectionStorage().addConnection(selCon);
				logger.info("created connection '"+selCon.getName()+"'");
			}
			else
			{
				int i = -1;
				for (ClearThConnection<?, ?> c : originalSelectedCons)
				{
					i++;
					ClearThCore.connectionStorage().modifyConnection(c, selectedConnections.get(i));
					if (c.isRunning() && !copy)
					{
						stopConnection(c);
						startConnection(c);
					}
				}

				if (logger.isInfoEnabled())
				{
					if (selectedConnections.size() == 1)
						logger.info("edited connection '"+selectedConnections.get(0).getName()+"'");
					else
					{
						CommaBuilder cb = new CommaBuilder();
						for (ClearThConnection<?, ?> c : selectedConnections)
							cb.append("'"+c.getName()+"'");
						logger.info("edited connections "+cb.toString());
					}
				}
			}

		}
		catch (SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
			canClose = false;
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		WebUtils.addCanCloseCallback(canClose);
	}

	public void saveConnectionsAfterCopy()
	{
		if(isCopy())
			saveConnections();
	}

	public void removeConnections()
	{
		try
		{
			for (ClearThConnection<?, ?> c : originalSelectedCons)
				ClearThCore.connectionStorage().removeConnection(c);

			Logger logger = getLogger();
			if (logger.isInfoEnabled())
			{
				if (selectedConnections.size() == 1)
					logger.info("removed connection '"+originalSelectedCons.get(0).getName()+"'");
				else
				{
					CommaBuilder cb = new CommaBuilder();
					for (ClearThConnection<?, ?> c : originalSelectedCons)
						cb.append("'"+c.getName()+"'");
					logger.info("removed connections "+cb.toString());
				}
			}
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		finally
		{
			resetConsSelection();
		}
	}

	protected StreamedContent downloadXConnections(String type)
	{
		try
		{
			File resultFile = ClearThCore.getInstance().getConnectionsTransmitter().exportConnections(type);
			return WebUtils.downloadFile(resultFile);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while exporting connections", e, getLogger());
			return null;
		}
	}
	
	protected void uploadXConnections(String type, FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if ((file == null) || (file.getContent().length == 0))
			return;

		try
		{
			File storageDir = new File(ClearThCore.uploadStoragePath());
			File storedConnections = WebUtils.storeUploadedFile(file, storageDir, type + "_connections_", ".zip");

			ClearThCore.getInstance().getConnectionsTransmitter().deployConnections(type, storedConnections);
			MessageUtils.addInfoMessage("Success", "Connections successfully uploaded");
			getLogger().info("Connections uploaded from file '" + file.getFileName() + "'");
		}
		catch (Exception e)
		{
			String msg = "Error while working with connections from file '" + file.getFileName() + "'";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage("Error", msg + ": " + e.getMessage());
		}
	}

	private void refreshConnectionList()
	{
		connections.sort((o1, o2) -> {
			boolean isC1Fav = isFavorite(o1);
			boolean isC2Fav = isFavorite(o2);
			if (isC1Fav == isC2Fav) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			} else {
				return (isC1Fav) ? -1 : 1;
			}
		});
	}

	private List<ClearThConnection<?, ?>> getConnections()
	{
		if (connections == null || ClearThCore.connectionStorage().getConnections().size() != connections.size())
		{
			this.connections = new ArrayList<>(ClearThCore.connectionStorage().getConnections());
			refreshConnectionList();
		}
		return this.connections;
	}

	protected List<ClearThConnection<?, ?>> getXConnections(String type)
	{
		List<ClearThConnection<?, ?>> res = new ArrayList<>();
		List<ClearThConnection<?, ?>> list = getConnections();
		for (ClearThConnection<?, ?> con : list)
			if (type.equals(con.getType()))
				res.add(con);
		return res;
	}

	private void startConnection(ClearThConnection<?, ?> con)
	{
		if (!con.isRunning())
		{
			try
			{
				con.start();
				MessageUtils.addInfoMessage("Info", "Connection '"+con.getName()+"' is now running");
				getLogger().info("started connection '"+con.getName()+"'");
			}
			catch (Exception e)
			{
				MessageUtils.addErrorMessage("Error occurred while starting connection", printExceptionMessage(e));
			}
		}
		else
			MessageUtils.addInfoMessage("Info", "Connection '"+con.getName()+"' is already running");
	}

	protected void startXConnections(String type)
	{
		for (ClearThConnection<?, ?> c : getAllOrSelectedXConnections(type))
			startConnection(c);
	}

	private void stopConnection(ClearThConnection<?, ?> con)
	{
		if (con.isRunning())
		{
			try
			{
				getLogger().trace("Disposing connection");
				con.stop();
				MessageUtils.addInfoMessage("Info", "Connection '"+con.getName()+"' stopped");
				getLogger().info("stopped connection '"+con.getName()+"'");
			}
			catch (Exception e)
			{
				String msg = "Error occurred while stopping connection";
				getLogger().error(msg, e);
				MessageUtils.addErrorMessage(msg, printExceptionMessage(e));
			}
		}
		else
			MessageUtils.addInfoMessage("Info", "Connection '"+con.getName()+"' is already stopped");
	}

	protected void stopXConnections(String type)
	{
		for (ClearThConnection<?, ?> c : getAllOrSelectedXConnections(type))
			stopConnection(c);
	}

	public Collection<ConnectionErrorInfo> getStoppedErrors()
	{
		return ClearThCore.connectionStorage().getStoppedConnectionsErrors();
	}

	private String printExceptionMessage(Throwable t) {
		String result = t.getClass().getName();
		if (t.getMessage()!=null)
			result += " - " + t.getMessage();

		if (t instanceof ConnectionException) {
			if (t.getCause() != null) {
				result += ", " + t.getCause().getClass().getName() + " - " + t.getCause().getMessage();
			}
		}
		return result;
	}

	protected List<ClearThConnection<?, ?>> getAllOrSelectedXConnections(String type)
	{
		return originalSelectedCons.isEmpty() ? getXConnections(type) : originalSelectedCons;
	}


	// Listeners
	public ListenerConfiguration getSelectedListener()
	{
		return selectedListener;
	}

	public void setSelectedListener(ListenerConfiguration selectedListener)
	{
		this.selectedListener = selectedListener;
		if (selectedListener != null)
			selectedType = selectedListener.getType();
	}

	public void selectFirstListener()
	{
		ClearThMessageConnection<?, ?> con = (ClearThMessageConnection<?, ?>) getOneSelectedConnection();
		List<ListenerConfiguration> listeners = con.getListeners();
		if (isNotEmpty(listeners))
			setSelectedListener(listeners.get(0));
		else
			setSelectedListener(null);
	}

	public ListenerConfiguration getNewListener()
	{
		return newListener;
	}

	public boolean isCollectorPresent()
	{
		ClearThConnection<?, ?> c = getOneSelectedConnection();
		if ((c == null) || (!ClearThMessageConnection.isMessageConnection(c)))
			return false;

		for (ListenerConfiguration listener : ((ClearThMessageConnection<?,?>)c).getListeners())
		{
			if (listener.getType().equals(ListenerType.Collector.getLabel()))
				return true;
		}
		return false;
	}

	public List<String> getListenersTypes()
	{
		if (isCollectorPresent())
			return Arrays.asList(ListenerType.File.getLabel(), ListenerType.Proxy.getLabel());
		else
			return Arrays.asList(ListenerType.File.getLabel(), ListenerType.Proxy.getLabel(), ListenerType.Collector.getLabel());
	}

	public void addListener()
	{
		((ClearThMessageConnection<?, ?>)getOneSelectedConnection()).addListener(newListener);
		setSelectedListener(newListener);
		newListener = createEmptyListener();
		noListenersInfo = false;
	}

	public void removeListener()
	{
		ClearThMessageConnection<?, ?> con = (ClearThMessageConnection<?, ?>)getOneSelectedConnection();
		List<ListenerConfiguration> listeners = con.getListeners();
		int index = listeners.indexOf(selectedListener);
		con.removeListener(selectedListener);
		getLogger().info("removed listener '"+selectedListener.getName()+"' from connection '"+con.getName()+"'");
		if (isNotEmpty(listeners))
		{
			if (index <= listeners.size() - 1)
				setSelectedListener(listeners.get(index));
			else
				setSelectedListener(listeners.get(index - 1));
		}
		else
			setSelectedListener(null);
	}

	public boolean selectedConHasListeners()
	{
		ClearThMessageConnection<?, ?> msgCon = (ClearThMessageConnection<?, ?>) getOneSelectedConnection();
		return msgCon != null && isNotEmpty(msgCon.getListeners());
	}

	public String getListenerDescription()
	{
		ClearThConnection<?, ?> c = getOneSelectedConnection();
		if (!ClearThMessageConnection.isMessageConnection(c))
			return null;
		if ((getSelectedListener() == null) || (c == null))
			return null;

		Class<?> descriptionOwner = ((ClearThMessageConnection<?, ?>) c).getListenerClass(getSelectedListener().getType());
		if (descriptionOwner == null)
			return "Error: no class found for this listener type";
		ListenerDescription ann = descriptionOwner.getAnnotation(ListenerDescription.class);
		if (ann == null)
			return "No description available";
		return ann.description();
	}

	public String getListenerSettingsDetails()
	{
		Class<?> detailsOwner = getSelectedListenerClass();
		if (detailsOwner == null)
			return "Error: no class found for this listener type";
		SettingsDetails ann = detailsOwner.getAnnotation(SettingsDetails.class);
		if (ann == null)
			return "No description available";
		return ann.details();
	}
	
	public boolean isReceiveListener()
	{
		return checkSelectedListenerClass(ReceiveListener.class);
	}
	
	public boolean isSendListener()
	{
		return checkSelectedListenerClass(SendListener.class);
	}

	public boolean isListenerInfoVisible()
	{
		return listenerInfoVisible;
	}

	public void setListenerInfoVisible(boolean listenerInfoVisible)
	{
		this.listenerInfoVisible = listenerInfoVisible;
	}

	public boolean isCopy()
	{
		return copy;
	}

	public void trueCopy()
	{
		copy = true;
	}

	public void trueEdit()
	{
		copy = false;
		copyListners = true;
	}

	public void trueListners()
	{
		selectFirstListener();
		copy = false;
		copyListners = false;
		noListenersInfo = !selectedConHasListeners();
	}

	public boolean isCopyListners() {
		return copyListners;
	}

	public void setCopyListners(boolean copyListeners) {
		this.copyListners = copyListeners;
	}

	public boolean isNoListenersInfo()
	{
		return noListenersInfo;
	}

	public String getSelectedType()
	{
		return selectedType;
	}
	
	public void setSelectedType(String selectedType)
	{
		this.selectedType = selectedType;
	}
	
	public void changeListenerType()
	{
		getSelectedListener().setType(selectedType);
	}
	
	
	public boolean isFavorite(ClearThConnection<?, ?> con)
	{
		return this.favoriteConnectionList.contains(con.getName());
	}

	public void favorite()
	{
		this.favoriteConnection.favoriteStateChanged(username,
				getOneSelectedConnection().getName(), true);
		refreshConnectionList();
	}

	public void unfavorite()
	{
		this.favoriteConnection.favoriteStateChanged(username,
				getOneSelectedConnection().getName(), false);
		refreshConnectionList();
	}

	public boolean isConnectionRunning()
	{
		List<ClearThConnection<?, ?>> cons = getConnections();
		for (ClearThConnection<?, ?> c : cons)
		{
			if (c.isRunning())
				return true;
		}
		return false;
	}

	public boolean isSelectedConnectionRunning()
	{
		if (originalSelectedCons != null)
		{
			for (ClearThConnection<?, ?> con : originalSelectedCons)
			{
				if (con.isRunning())
					return true;
			}
		}
		return false;
	}

	// MQ-specific methods to use on page. Implement these methods for each new connection type

	public void newMqConnection()
	{
		newXConnection(MQ);
	}

	public List<ClearThConnection<?, ?>> getMqConnections()
	{
		return getXConnections(MQ);
	}
	
	public List<ClearThConnection<?, ?>> getSelectedMqConnections()
	{
		return getSelectedXConnections(MQ);
	}
	
	public void setSelectedMqConnections(List<ClearThConnection<?, ?>> selection)
	{
		setSelectedConnections(selection);
	}
	
	public ClearThConnection<?, ?> getOneSelectedMqConnection()
	{
		return getOneSelectedXConnection(MQ);
	}
	
	public void setOneSelectedMqConnection(ClearThConnection<?, ?> selectedCon)
	{
		setOneSelectedXConnection(selectedCon, MQ);
	}

	public StreamedContent downloadMqConnections()
	{
		return downloadXConnections(MQ);
	}

	public void uploadMqConnections(FileUploadEvent fileUploadEvent)
	{
		uploadXConnections(MQ, fileUploadEvent);
	}

	public void startMqConnections()
	{
		startXConnections(MQ);
	}

	public void stopMqConnections()
	{
		stopXConnections(MQ);
	}

	protected void editMqConnectionProps(MQConnection connectionToEdit, MQConnection changes, MQConnection original, MqConPropsToEdit propsToEdit)
	{
		connectionToEdit.setHostname(propsToEdit.isHost() ? changes.getHostname() : original.getHostname());
		connectionToEdit.setPort(propsToEdit.isPort() ? changes.getPort() : original.getPort());
		connectionToEdit.setQueueManager(propsToEdit.isQueueManager() ? changes.getQueueManager() : original.getQueueManager());
		connectionToEdit.setChannel(propsToEdit.isChannel() ? changes.getChannel() : original.getChannel());
		connectionToEdit.setUseReceiveQueue(propsToEdit.isReceiveQueue() ? changes.isUseReceiveQueue() : original.isUseReceiveQueue());
		connectionToEdit.setReceiveQueue(propsToEdit.isReceiveQueue() ? changes.getReceiveQueue() : original.getReceiveQueue());
		connectionToEdit.setSendQueue(propsToEdit.isSendQueue() ? changes.getSendQueue() : original.getSendQueue());
		connectionToEdit.setReadDelay(propsToEdit.isReadDelay() ? changes.getReadDelay() : original.getReadDelay());
		connectionToEdit.setAutoConnect(propsToEdit.isAutoConnect() ? changes.isAutoConnect() : original.isAutoConnect());
		connectionToEdit.setAutoReconnect(propsToEdit.isAutoReconnect() ? changes.isAutoReconnect() : original.isAutoReconnect());
		connectionToEdit.setRetryAttemptCount(propsToEdit.isRetryAttemptCount() ? changes.getRetryAttemptCount() : original.getRetryAttemptCount());
		connectionToEdit.setRetryTimeout(propsToEdit.isRetryTimeout() ? changes.getRetryTimeout() : original.getRetryTimeout());
	}
	
	protected ListenerConfiguration createEmptyListener()
	{
		return new ListenerConfiguration("", defaultListenerType, "", true, false);
	}
	
	private Class<?> getSelectedListenerClass()
	{
		ClearThConnection<?, ?> c = getOneSelectedConnection();
		if (c == null || !ClearThMessageConnection.isMessageConnection(c))
			return null;
		ListenerConfiguration listener = getSelectedListener();
		if (listener == null)
			return null;
		
		return ((ClearThMessageConnection<?, ?>) c).getListenerClass(listener.getType());
	}
	
	private boolean checkSelectedListenerClass(Class<?> expectedClass)
	{
		Class<?> listenerClass = getSelectedListenerClass();
		return listenerClass == null ? false : expectedClass.isAssignableFrom(listenerClass);
	}
}
