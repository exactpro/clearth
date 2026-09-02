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

package com.exactprosystems.clearth.woodpecker.daemons;

/**
 * 17 October 2018
 */
public enum DaemonStatus
{
	INACTIVE,

	/**
	 * Optional. 
	 * Used for daemons that have initialization step that takes time. 
	 * F.e. loading of initial data.
	 */
	INITIALIZING,
	
	RUNNING,

	/**
	 * Optional.
	 * Used in cases when stopping takes time.
	 * F.e. if daemon is stopped by user and trying to complete current tasks.
	 */
	STOPPING
}
