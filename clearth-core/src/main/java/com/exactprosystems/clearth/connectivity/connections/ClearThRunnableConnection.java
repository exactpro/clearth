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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.SettingsException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface ClearThRunnableConnection extends ClearThConnection
{
	void start() throws ConnectivityException, SettingsException;
	void stop() throws ConnectivityException;
	void restart() throws ConnectivityException, SettingsException;
	boolean isRunning();
	LocalDateTime getStarted();
	LocalDateTime getStopped();

	boolean isAutoConnect();
	
	List<ConnectionErrorInfo> getErrorInfo();
	void addErrorInfo(String errorMessage, Throwable reason, Instant occurred);
	void clearErrorInfo();
}
