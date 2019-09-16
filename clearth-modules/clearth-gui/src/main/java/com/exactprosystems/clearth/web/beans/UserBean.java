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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.utils.DateTimeUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

public class UserBean extends ClearThBean
{
	public String getUserName()
	{
		return UserInfoUtils.getUserName();
	}
	
	public String getRole()
	{
		return UserInfoUtils.getUserRole();
	}
	
	public boolean isLoggedIn()
	{
		return UserInfoUtils.isLoggedIn();
	}
	
	public boolean isAdmin()
	{
		return UserInfoUtils.isAdmin();
	}
	
	public boolean isPowerUser()
	{
		return UserInfoUtils.isPowerUser();
	}
	
	public boolean isRegularUser()
	{
		return UserInfoUtils.isRegularUser();
	}
	
	public boolean isNewYear()
	{
		return DateTimeUtils.isNewYear();
	}
}
