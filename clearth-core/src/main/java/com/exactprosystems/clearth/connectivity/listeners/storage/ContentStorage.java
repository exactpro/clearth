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

package com.exactprosystems.clearth.connectivity.listeners.storage;

import java.util.Map;

public interface ContentStorage<P, F>
{
	void start();
	
	void dispose();
	
	
	void insertPassed(long id, P item);
	
	void insertFailed(long id, F item);
	
	
	void removePassed(P item);
	
	void removePassed(long itemId);
	
	void removeFailed(F item);
	
	void removeFailed(long itemId);
	
	
	void clearMemory();
	
	void clearPassed();
	
	void clearFailed();
	
	
	Map<Long, P> getContentPassed();
	
	Map<Long, P> getContentPassedAfterId(long id);
	
	Map<Long, F> getContentFailed();
	
	Map<Long, F> getContentFailedAfterId(long id);
}
