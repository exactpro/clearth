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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "InitBlockDesc")
public class InitialDataBlockDesc
{
    @XmlElement(name = "blacklists")
    private BlacklistsBlockDesc blacklistsBlock;
    @XmlElement(name = "collections")
    private CollectionsBlockDesc collectionsBlock;
    @XmlElement(name = "tables")
    private TablesBlockDesc tablesBlock;
    @XmlElement(name = "queries")
    private QueriesBlockDesc queriesBlock;


    public BlacklistsBlockDesc getBlacklistsBlock()
    {
        return blacklistsBlock;
    }

    public void setBlacklistsBlock(BlacklistsBlockDesc blacklistsBlock)
    {
        this.blacklistsBlock = blacklistsBlock;
    }

    
    public CollectionsBlockDesc getCollectionsBlock()
    {
        return collectionsBlock;
    }

    public void setCollectionsBlock(CollectionsBlockDesc collectionsBlock)
    {
        this.collectionsBlock = collectionsBlock;
    }

    
    public TablesBlockDesc getTablesBlock()
    {
        return tablesBlock;
    }

    public void setTablesBlock(TablesBlockDesc tablesBlock)
    {
        this.tablesBlock = tablesBlock;
    }

    
    public QueriesBlockDesc getQueriesBlock()
    {
        return queriesBlock;
    }

    public void setQueriesBlock(QueriesBlockDesc queriesBlock)
    {
        this.queriesBlock = queriesBlock;
    }
}
