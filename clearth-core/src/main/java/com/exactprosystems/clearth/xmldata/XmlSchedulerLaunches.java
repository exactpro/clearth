/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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


package com.exactprosystems.clearth.xmldata;

import org.apache.commons.collections4.list.UnmodifiableList;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "launchesInfo"
})
@XmlRootElement(name = "SchedulerLaunches")
public class XmlSchedulerLaunches
    implements Serializable
{

    private static final long serialVersionUID = 7110082469808678461L;

    @XmlElement(name = "LaunchInfo")
    protected List<XmlSchedulerLaunchInfo> launchesInfo;

    public XmlSchedulerLaunches()
    {
        this.launchesInfo = new CopyOnWriteArrayList<>();
    }

    public List<XmlSchedulerLaunchInfo> getLaunchesInfo()
    {
        return UnmodifiableList.unmodifiableList(launchesInfo);
    }

    public void addLaunchInfo(int num, XmlSchedulerLaunchInfo launchInfo)
    {
        this.launchesInfo.add(num, launchInfo);
    }

    public void addLaunchInfo(XmlSchedulerLaunchInfo launchInfo)
    {
        this.launchesInfo.add(launchInfo);
    }

    public void clearLaunchInfoList()
    {
        this.launchesInfo.clear();
    }

    public void removeLaunchInfo(XmlSchedulerLaunchInfo launchInfo)
    {
        this.launchesInfo.remove(launchInfo);
    }

}
