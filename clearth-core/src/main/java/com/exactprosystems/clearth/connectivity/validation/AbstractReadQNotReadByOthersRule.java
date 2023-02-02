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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.ibmmq.ClearThBasicMqConnectionSettings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Rule to check that there are no started connections
 * reading from queues that are read by the connection being checked.
 */
abstract public class AbstractReadQNotReadByOthersRule
		implements ClearThConnectionValidationRule
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractReadQNotReadByOthersRule.class);

	protected abstract List<ClearThMessageConnection> getOtherConnections(ClearThMessageConnection connection);

	protected abstract boolean useSameReceiveQueue(ClearThBasicMqConnectionSettings settingsToCheck,
	                                               ClearThBasicMqConnectionSettings anotherSettings);

	@Override
	public String check(ClearThConnection connection)
	{
		if (!isConnectionSuitable(connection))
			return null;

		ClearThMessageConnection connectionToCheck = (ClearThMessageConnection) connection;

		ClearThBasicMqConnectionSettings settings = (ClearThBasicMqConnectionSettings) connectionToCheck.getSettings();

		if (!settings.isUseReceiveQueue())
			return null;

		List<ClearThMessageConnection> otherMqConnections = getOtherConnections(connectionToCheck);

		for (ClearThMessageConnection otherConnection : otherMqConnections)
		{
			 ClearThBasicMqConnectionSettings otherConnectionSettings = (ClearThBasicMqConnectionSettings) otherConnection.getSettings();

			if (!otherConnectionSettings.isUseReceiveQueue())
				continue;

			if (useSameReceiveQueue(settings, otherConnectionSettings))
			{
				return String.format(
						"Can't start connection '%s' that reads the same queue as '%s' (receiveQueue = '%s').",
						connectionToCheck.getName(),
						otherConnection.getName(),
						settings.getReceiveQueue());
			}
		}

		return null;
	}

	/**
	 * Checks if two host names are related to the same host.
	 * True is returned if corresponding IP addresses are equal or if they are both local.
	 */
	protected boolean hostsEquals(String hostnameToCheck, String anotherHostname)
	{
		// Don't compare invalid settings.
		if (isBlank(hostnameToCheck) || isBlank(anotherHostname))
			return false;
		
		if (StringUtils.equals(hostnameToCheck, anotherHostname))
			return true;

		String ipToCheck, anotherIp;
		try
		{
			ipToCheck = InetAddress.getByName(hostnameToCheck).getHostAddress();
			logger.trace("IP address by Host of checking connection: {}", ipToCheck);
			anotherIp = InetAddress.getByName(anotherHostname).getHostAddress();
			logger.trace("IP address by Host of another connection: {}", anotherIp);
			if (StringUtils.equals(ipToCheck, anotherIp))
				return true;
		}
		catch (UnknownHostException e)
		{
			return false;
		}

		boolean isHostnameToCheckLocalhost = false, isAnotherHostnameLocalhost = false;
		try
		{
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			logger.trace("List of local addresses: ");
			while (networkInterfaces.hasMoreElements())
			{
				for (InterfaceAddress interfaceAddress : networkInterfaces.nextElement().getInterfaceAddresses())
				{
					String localAddress = interfaceAddress.getAddress().getHostAddress();
					logger.trace("Local address: {}", localAddress);
					if (StringUtils.equals(localAddress, ipToCheck))
						isHostnameToCheckLocalhost = true;
					if (StringUtils.equals(localAddress, anotherIp))
						isAnotherHostnameLocalhost = true;

					if (isHostnameToCheckLocalhost && isAnotherHostnameLocalhost)
						return true;
				}
			}
		}
		catch (SocketException e)
		{
			return false;
		}

		return false;
	}
}
