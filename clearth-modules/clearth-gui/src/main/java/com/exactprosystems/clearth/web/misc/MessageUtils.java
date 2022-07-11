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

package com.exactprosystems.clearth.web.misc;

import com.exactprosystems.clearth.web.beans.PopUpMessagesBean;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

public class MessageUtils
{
	public static final String GENERAL_MESSAGES = "generalMessages";
	
	public static void addMessage(Severity severity, final String id, final String summary, final String details)
	{
		FacesContext context = FacesContext.getCurrentInstance();
		FacesMessage msg = new FacesMessage(severity, summary, details);
		context.addMessage(id, msg);
		
		String severityString = null;
		if (msg.getSeverity().compareTo(FacesMessage.SEVERITY_INFO) == 0) severityString = "INFO";
		else if (msg.getSeverity().compareTo(FacesMessage.SEVERITY_WARN) == 0) severityString = "WARN";
		else if (msg.getSeverity().compareTo(FacesMessage.SEVERITY_ERROR) == 0) severityString = "ERROR";
		
		ExternalContext extContext = context.getExternalContext();
		PopUpMessagesBean bean = (PopUpMessagesBean) extContext.getSessionMap().get("popUpMsgsBean");
		if (bean != null)
			bean.addMessage(new PopUpMessageDesc(msg.getDetail(), severityString, msg.getSummary()));
		
		if (!context.getPartialViewContext().isAjaxRequest())
			extContext.getFlash().setKeepMessages(true);
	}
	
	public static void addMessage(Severity severity, final String summary, final String details)
	{
		addMessage(severity, GENERAL_MESSAGES, summary, details);
	}
	
	public static void addErrorMessage(final String summary, final String details)
	{
		addMessage(FacesMessage.SEVERITY_ERROR, summary, details);
	}
	
	public static void addInfoMessage(final String summary, final String details)
	{
		addMessage(FacesMessage.SEVERITY_INFO, summary, details);
	}
	
	public static void addWarningMessage(final String summary, final String details)
	{
		addMessage(FacesMessage.SEVERITY_WARN, summary, details);
	}
}
