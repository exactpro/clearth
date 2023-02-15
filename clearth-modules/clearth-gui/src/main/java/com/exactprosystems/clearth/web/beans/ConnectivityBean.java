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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections.*;
import com.exactprosystems.clearth.connectivity.connections.settings.*;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class ConnectivityBean extends ClearThBean
{
	private static final String defaultListenerType = ListenerType.File.getLabel();
	
	private final ClearThConnectionStorage storage = ClearThCore.connectionStorage();
	private ConnectionTypeInfo selectedConnectionType;
	private final List<ClearThConnection> selectedConnections = new ArrayList<ClearThConnection>();
	private List<ClearThConnection> originalSelectedCons = null;
	
	private ListenerConfiguration newListener = createEmptyListener(),
			selectedListener = null;
	private boolean copy = false;
	private boolean copyListners = true;
	private boolean noListenersInfo;
	private boolean listenerInfoVisible = false;
	private FavoriteConnectionManager favoritesManager;
	private Set<String> favoriteConnectionList;
	private String username;
	private List<ClearThConnection> connections;
	protected String selectedType;
	
	private SettingValues settingsToEdit;
	private ColumnsModel columns;
	
	@PostConstruct
	protected void init()
	{
		this.favoritesManager = ClearThCore.getInstance().getFavoriteConnections();
		this.username = UserInfoUtils.getUserName();
		this.favoriteConnectionList = favoritesManager.getUserFavoriteConnectionList(username);
		setSelectedConnectionType(getFirstConnectionType());
	}
	
	
	//*** Connection types ***
	
	public Collection<String> getConnectionTypes()
	{
		return storage.getTypes();
	}
	
	public String getSelectedConnectionType()
	{
		return selectedConnectionType != null ? selectedConnectionType.getName() : null;
	}
	
	public void setSelectedConnectionType(String selectedConnectionType)
	{
		if (StringUtils.isEmpty(selectedConnectionType))
		{
			this.selectedConnectionType = null;
			connections = null;
			columns = null;
		}
		else
		{
			try
			{
				this.selectedConnectionType = storage.getConnectionTypeInfo(selectedConnectionType);
				connections = getConnections();
				columns = storage.getSettingsModel(selectedConnectionType).getColumnsModel();
			}
			catch (Exception e)
			{
				WebUtils.logAndGrowlException("Could not select type '"+selectedConnectionType+"'", e, getLogger());
				return;
			}
		}
		
		resetConsSelection();
	}
	
	
	//*** List of connections ***

	public SelectItem[] getConnectionsList()
	{
		List<ClearThConnection> cons = this.getConnections();
		SelectItem[] result = new SelectItem[cons.size()];
		int i = -1;
		for (ClearThConnection c : cons)
		{
			i++;
			result[i] = new SelectItem(c.getName());
		}
		return result;
	}
	
	public List<ClearThConnection> getConnections()
	{
		if (selectedConnectionType == null)
			return Collections.emptyList();
		
		List<ClearThConnection> res = new ArrayList<>();
		List<ClearThConnection> list = getAllConnections();
		for (ClearThConnection con : list)
			if (selectedConnectionType.equals(con.getTypeInfo()))
				res.add(con);
		return res;
	}
	
	public List<SettingProperties> getColumns()
	{
		return columns == null ? null : columns.getColumns();
	}
	
	public String getColumnValue(SettingProperties columnProps, ClearThConnection con)
	{
		try
		{
			Object result = columnProps.getGetter().invoke(con.getSettings());
			return result == null ? "" : result.toString();
		}
		catch (Exception e)
		{
			getLogger().error("Could not get value of '{}'", columnProps.getFieldName(), e);
			return null;
		}
	}
	
	
	//*** Selection ***
	
	public List<ClearThConnection> getSelectedConnections()
	{
		return selectedConnections;
	}
	
	public void setSelectedConnections(List<ClearThConnection> selectedConnections)
	{
		this.originalSelectedCons = selectedConnections;
		this.selectedConnections.clear();
		
		if (!CollectionUtils.isEmpty(originalSelectedCons))
		{
			for (ClearThConnection c : originalSelectedCons)
			{
				try
				{
					this.selectedConnections.add(c.copy());
				}
				catch (ConnectivityException e)
				{
					WebUtils.logAndGrowlException("Cannot select connection: " + c.getName(), e, getLogger());
				}
			}
			
			refreshSettingsToEdit();
		}
		else
			this.settingsToEdit = null;
	}
	
	
	public ClearThConnection getOneSelectedConnection()
	{
		if (selectedConnections.isEmpty())
			return null;
		return selectedConnections.get(0);
	}
	
	public void setOneSelectedConnection(ClearThConnection selectedCon)
	{
		List<ClearThConnection> cons = new ArrayList<ClearThConnection>(1);
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
	
	public boolean isRunnableConnectionType()
	{
		return isConnectionType(ClearThRunnableConnection.class);
	}
	
	public boolean isMessageConnectionType()
	{
		return isConnectionType(ClearThMessageConnection.class);
	}
	
	
	protected void resetConsSelection()
	{
		originalSelectedCons = null;
		selectedConnections.clear();
	}
	
	private void refreshSettingsToEdit()
	{
		SettingsModel model = storage.getSettingsModel(selectedConnectionType.getName());
		//It is important to create settingsToEdit from selectedConnection, i.e. from copy of original connection
		//When values from GUI are submitted, they go into selectedConnection automatically and we can decide if we need to update the original connection
		this.settingsToEdit = new SettingValues(model, selectedConnections.get(0), isOneConnectionSelected());
	}
	
	
	//*** CRUD ***
	
	public void newConnection()
	{
		resetConsSelection();
		try
		{
			selectedConnections.add(storage.createConnection(selectedConnectionType.getName()));
		}
		catch (ConnectivityException e)
		{
			getLogger().error("Cannot create a new connection", e);
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		refreshSettingsToEdit();
	}
	
	public void saveConnections()
	{
		if (selectedConnections.size() > 1)
		{
			try
			{
				//If multiple connections are edited, JSF changes only the first one. 
				//Need to apply changes to all other selected connections. 
				//The name shouldn't be changed here!
				//We get here only if multiple connections are edited, thus originalSelectedCons can't be null
				ClearThConnection firstCon = getOneSelectedConnection(),
						originalFirstCon = originalSelectedCons.get(0);
				
				//Restoring properties that shouldn't be edited (change flag = false), because JSF has changed all the properties of the first connection
				editConnectionProps(firstCon, firstCon, originalFirstCon);
				boolean first = true;
				for (ClearThConnection c : selectedConnections)
				{
					if (first)
					{
						first = false;
						continue;
					}
					editConnectionProps(c, firstCon, c);
				}
			}
			catch (Exception e)
			{
				getLogger().error("Error while applying changes to connection", e);
				MessageUtils.addErrorMessage("Error", e.getMessage());
				WebUtils.addCanCloseCallback(false);
				return;
			}
		}
		
		boolean canClose = true;
		try
		{
			Logger logger = getLogger();
			if (originalSelectedCons == null || copy)
			{
				ClearThConnection selCon = getOneSelectedConnection();
				if ((selCon instanceof ClearThMessageConnection) && !copyListners)
					((ClearThMessageConnection)selCon).removeAllListeners();

				storage.addConnection(selCon);
				logger.info("created connection '"+selCon.getName()+"'");
			}
			else
			{
				int i = -1;
				for (ClearThConnection c : originalSelectedCons)
				{
					i++;
					storage.modifyConnection(c, selectedConnections.get(i));
					if (c instanceof ClearThRunnableConnection)
					{
						if (((ClearThRunnableConnection)c).isRunning() && !copy)
						{
							stopConnection(c);
							startConnection(c);
						}
					}
				}
				
				if (logger.isInfoEnabled())
				{
					if (selectedConnections.size() == 1)
						logger.info("edited connection '"+selectedConnections.get(0).getName()+"'");
					else
					{
						CommaBuilder cb = new CommaBuilder();
						for (ClearThConnection c : selectedConnections)
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
			getLogger().error("Error while saving connection", e);
			MessageUtils.addErrorMessage("Error", e.getMessage());
			canClose = false;
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
			for (ClearThConnection c : originalSelectedCons)
				storage.removeConnection(c);
			
			Logger logger = getLogger();
			if (logger.isInfoEnabled())
			{
				if (selectedConnections.size() == 1)
					logger.info("removed connection '"+originalSelectedCons.get(0).getName()+"'");
				else
				{
					CommaBuilder cb = new CommaBuilder();
					for (ClearThConnection c : originalSelectedCons)
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
	
	
	public SettingValues getSettingsToEdit()
	{
		return settingsToEdit;
	}
	
	
	//*** Export/import ***
	
	public StreamedContent downloadConnections()
	{
		try
		{
			File resultFile = ClearThCore.getInstance().getConnectionsTransmitter().exportConnections(selectedConnectionType.getName());
			return WebUtils.downloadFile(resultFile);
		}
		catch (IOException | ConnectivityException e)
		{
			WebUtils.logAndGrowlException("Error while exporting connections", e, getLogger());
			return null;
		}
	}
	
	public void uploadConnections(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if ((file == null) || (file.getContent().length == 0))
			return;
		
		try
		{
			File storageDir = new File(ClearThCore.uploadStoragePath());
			File storedConnections = WebUtils.storeUploadedFile(file, storageDir, selectedConnectionType + "_connections_", ".zip");
			
			ClearThCore.getInstance().getConnectionsTransmitter().deployConnections(selectedConnectionType.getName(), storedConnections);
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
	
	
	//*** Start/stop
	
	public void startConnections()
	{
		for (ClearThConnection c : getAllOrSelectedConnections())
			startConnection(c);
	}
	
	public void stopConnections()
	{
		for (ClearThConnection c : getAllOrSelectedConnections())
			stopConnection(c);
	}
	
	public Collection<ConnectionErrorInfo> getStoppedErrors()
	{
		return storage.getStoppedConnectionsErrors();
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
	
	private List<ClearThConnection> getAllConnections()
	{
		if (connections == null || storage.getConnections().size() != connections.size())
		{
			this.connections = new ArrayList<>(storage.getConnections());
			refreshConnectionList();
		}
		return this.connections;
	}
	
	private List<ClearThConnection> getAllOrSelectedConnections()
	{
		return CollectionUtils.isEmpty(originalSelectedCons) ? getConnections() : originalSelectedCons;
	}
	
	private void startConnection(ClearThConnection con)
	{
		if (!(con instanceof ClearThRunnableConnection))
		{
			MessageUtils.addWarningMessage("Info", "Connection '"+con.getName()+"' cannot be run.");
			return;
		}
		ClearThRunnableConnection runnable = (ClearThRunnableConnection)con;
		if (!runnable.isRunning())
		{
			try
			{
				storage.validateConnectionStart(runnable);
				runnable.start();
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
	
	private void stopConnection(ClearThConnection con)
	{
		if (!(con instanceof ClearThRunnableConnection))
		{
			MessageUtils.addWarningMessage("Info", "Connection '"+con.getName()+"' cannot be stopped.");
			return;
		}
		ClearThRunnableConnection runnable = (ClearThRunnableConnection)con;
		if (runnable.isRunning())
		{
			try
			{
				getLogger().trace("Disposing connection");
				runnable.stop();
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
		ClearThMessageConnection con = (ClearThMessageConnection) getOneSelectedConnection();
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
		ClearThConnection c = getOneSelectedConnection();
		if ((c == null) || (!(c instanceof ClearThMessageConnection)))
			return false;

		for (ListenerConfiguration listener : ((ClearThMessageConnection)c).getListeners())
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
		((ClearThMessageConnection)getOneSelectedConnection()).addListener(newListener);
		setSelectedListener(newListener);
		newListener = createEmptyListener();
		noListenersInfo = false;
	}

	public void removeListener()
	{
		ClearThMessageConnection con = (ClearThMessageConnection)getOneSelectedConnection();
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
		if (!isMessageConnectionType())
			return false;
		
		ClearThMessageConnection msgCon = (ClearThMessageConnection) getOneSelectedConnection();
		return msgCon != null && isNotEmpty(msgCon.getListeners());
	}

	public String getListenerDescription()
	{
		ClearThConnection c = getOneSelectedConnection();
		if (!(c instanceof ClearThMessageConnection))
			return null;
		if ((getSelectedListener() == null) || (c == null))
			return null;

		Class<?> descriptionOwner = ((ClearThMessageConnection) c).getListenerClass(getSelectedListener().getType());
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
		copyListners = true;
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
	
	
	public boolean isFavorite(ClearThConnection con)
	{
		return this.favoriteConnectionList.contains(con.getName());
	}

	public void favorite()
	{
		this.favoritesManager.favoriteStateChanged(username,
				getOneSelectedConnection().getName(), true);
		refreshConnectionList();
	}

	public void unfavorite()
	{
		this.favoritesManager.favoriteStateChanged(username,
				getOneSelectedConnection().getName(), false);
		refreshConnectionList();
	}

	public boolean isConnectionRunning()
	{
		List<ClearThConnection> cons = getAllConnections();
		for (ClearThConnection c : cons)
		{
			if (isConnectionRunning(c))
				return true;
		}
		return false;
	}

	public boolean isSelectedConnectionRunning()
	{
		if (originalSelectedCons != null)
		{
			for (ClearThConnection con : originalSelectedCons)
			{
				if (isConnectionRunning(con))
					return true;
			}
		}
		return false;
	}

	private boolean isConnectionRunning(ClearThConnection con)
	{
		return (con instanceof ClearThRunnableConnection) && ((ClearThRunnableConnection)con).isRunning();
	}

	
	protected ListenerConfiguration createEmptyListener()
	{
		return new ListenerConfiguration("", defaultListenerType, "", true, false);
	}
	
	private Class<?> getSelectedListenerClass()
	{
		ClearThConnection c = getOneSelectedConnection();
		if (c == null || !(c instanceof ClearThMessageConnection))
			return null;
		ListenerConfiguration listener = getSelectedListener();
		if (listener == null)
			return null;
		
		return ((ClearThMessageConnection) c).getListenerClass(listener.getType());
	}
	
	private boolean checkSelectedListenerClass(Class<?> expectedClass)
	{
		Class<?> listenerClass = getSelectedListenerClass();
		return listenerClass == null ? false : expectedClass.isAssignableFrom(listenerClass);
	}
	
	private String getFirstConnectionType()
	{
		Iterator<String> it = getConnectionTypes().iterator();
		return it.hasNext() ? it.next() : null;
	}
	
	private void editConnectionProps(ClearThConnection connectionToEdit, ClearThConnection changes, ClearThConnection original) 
			throws Exception
	{
		//This method is called only if multiple connections are selected.
		//Accessor for "Name" (which in member of ClearThConnection, not ClearThConnectionSettings) is not included in settingsToEdit in this case.
		//So it is safe to work with ClearThConnectionSettings only
		ClearThConnectionSettings connectionToEditSettings = connectionToEdit.getSettings(),
				changesSettings = changes.getSettings(),
				originalSettings = original.getSettings();
		for (SettingAccessor accessor : settingsToEdit.getSettings())
		{
			ClearThConnectionSettings copyFrom = accessor.isApplyChange() ? changesSettings : originalSettings;
			SettingAccessor.copyValue(accessor.getProperties(), copyFrom, connectionToEditSettings);
		}
	}
	
	private boolean isConnectionType(Class<? extends ClearThConnection> expectedClass)
	{
		return selectedConnectionType != null && expectedClass.isAssignableFrom(selectedConnectionType.getConnectionClass());
	}
}