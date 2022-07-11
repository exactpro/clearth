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

package com.exactprosystems.clearth.automation.matrix.linked;

import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.KeyValueUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by alexander.magomedov on 4/26/17.
 */
public class RemoteMatrixProvider implements MatrixProvider
{
	private static Logger logger = LoggerFactory.getLogger(RemoteMatrixProvider.class);

	public static final String TYPE = "Remote";
	private String link;
	private String name;

	public RemoteMatrixProvider(String link, String name) {
		this.link = link;
		this.name = name;
	}

	@Override
	public InputStream getMatrix() throws ClearThException {
		try {
			final URLConnection urlConnection = new URL(link).openConnection();
			urlConnection.setConnectTimeout(5000);
			return urlConnection.getInputStream();
		} catch (SocketTimeoutException e) {
			logger.warn("Could not get matrix from remote host", e);
			throw new ClearThException("Could not get matrix from remote host: connection timeout.", e);
		} catch ( IOException e) {
			logger.warn("Could not get matrix from remote host", e);
			throw new ClearThException("Could not get matrix from remote host. Cause: " + ExceptionUtils.getDetailedMessage(e));
		}
	}

	@Override
	public String getName() throws Exception {
		return name;
	}
}
