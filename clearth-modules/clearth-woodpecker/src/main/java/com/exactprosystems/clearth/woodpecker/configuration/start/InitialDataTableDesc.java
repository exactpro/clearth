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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import com.exactprosystems.clearth.utils.xml.CommaSeparatedListAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.concurrent.TimeUnit;


@XmlAccessorType(XmlAccessType.FIELD)
public abstract class InitialDataTableDesc
{
    @XmlAttribute(required = true)
    private String name;
    
    @XmlAttribute
    private String blacklist;
    
    @XmlAttribute(name = "indexBy")
    @XmlJavaTypeAdapter(CommaSeparatedListAdapter.class)
    private List<String> indexKeys;
    
    @XmlAttribute
    private int updatePeriod;
    @XmlAttribute
    @XmlJavaTypeAdapter(TimeUnitAdapter.class)
    private TimeUnit updatePeriodUnit = TimeUnit.MINUTES;
    
    @XmlAttribute
    private boolean writeDebugFile;
    
    
    public abstract String getTableType();


    public String getName()
    {
        return name;
    }

    public void setName(String value)
    {
        this.name = value;
    }


    public String getBlacklist()
    {
        return blacklist;
    }

    public void setBlacklist(String value)
    {
        this.blacklist = value;
    }


    public List<String> getIndexKeys()
    {
        return indexKeys;
    }

    public void setIndexKeys(List<String> indexKeys)
    {
        this.indexKeys = indexKeys;
    }


    public int getUpdatePeriod()
    {
        return updatePeriod;
    }

    public void setUpdatePeriod(int updatePeriod)
    {
        this.updatePeriod = updatePeriod;
    }
    

    public TimeUnit getUpdatePeriodUnit()
    {
        return updatePeriodUnit;
    }

    public void setUpdatePeriodUnit(TimeUnit updatePeriodUnit)
    {
        this.updatePeriodUnit = updatePeriodUnit;
    }


    public boolean isWriteDebugFile()
    {
        return writeDebugFile;
    }

    public void setWriteDebugFile(boolean writeDebugFile)
    {
        this.writeDebugFile = writeDebugFile;
    }
}
