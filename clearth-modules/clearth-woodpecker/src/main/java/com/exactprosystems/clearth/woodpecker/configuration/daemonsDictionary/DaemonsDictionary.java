/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary;

import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.daemons.action.ActionDaemonDesc;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException.fromIOException;
import static java.lang.String.format;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "daemons")
public class DaemonsDictionary
{
	@XmlElement(name = "messageDaemon")
	private List<MessageDaemonDesc> messageDaemons;
	
	@XmlElement(name = "actionDaemon")
	private List<ActionDaemonDesc> actionDaemons;


	public List<DaemonDesc> getDaemons()
	{
		List<DaemonDesc> daemons = new ArrayList<>();
		if (messageDaemons != null)
			daemons.addAll(messageDaemons);
		if (actionDaemons != null)
			daemons.addAll(actionDaemons);
		return daemons;
	}


	public List<MessageDaemonDesc> getMessageDaemons()
	{
		return messageDaemons;
	}

	public void setMessageDaemons(List<MessageDaemonDesc> messageDaemons)
	{
		this.messageDaemons = messageDaemons;
	}
	
	
	public List<ActionDaemonDesc> getActionDaemons()
	{
		return actionDaemons;
	}

	public void setActionDaemons(List<ActionDaemonDesc> actionDaemons)
	{
		this.actionDaemons = actionDaemons;
	}
	

	public static DaemonsDictionary load(String fileName) throws WoodpeckerDaemonException
	{
		WoodpeckerObjectsFactory factory = woodpecker().getObjectsFactory();
		
		Class<? extends DaemonsDictionary> dictionaryClass = factory.getDaemonsDictionaryClass();
		Class[] extendedClasses = factory.getExtendedDaemonsDictionaryClasses();
		
		int extCount = extendedClasses.length;
		Class[] allClasses = new Class[extCount + 1];
		allClasses[0] = dictionaryClass;
		if (extCount > 0)
			System.arraycopy(extendedClasses, 0, allClasses, 1, extCount);
		
		try (FileInputStream is = new FileInputStream(fileName))
		{
			Unmarshaller u = JAXBContext.newInstance(allClasses).createUnmarshaller();
			JAXBElement<? extends DaemonsDictionary> e = u.unmarshal(new StreamSource(is), dictionaryClass);
			return e.getValue();
		}
		catch (JAXBException e)
		{
			throw new WoodpeckerDaemonException(format("Error while loading '%s': %s", fileName,
					(e.getLinkedException() != null) ?
							e.getLinkedException().getMessage() :
							e.getMessage()), e);
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Error while loading '%s'", fileName);
		}
	}
}
