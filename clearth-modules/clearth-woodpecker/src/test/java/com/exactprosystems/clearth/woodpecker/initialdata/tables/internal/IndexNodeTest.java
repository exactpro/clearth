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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class IndexNodeTest {

    @Test
    public void checkSubNodeKeys() {

        List<String> keysExpected = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            keysExpected.add(String.valueOf(i + 1));
        }

        IndexNode root = new RootIndexNode(6);

        IndexNode node1 = new NonRootIndexNode();
        IndexNode node2 = new NonRootIndexNode();
        IndexNode node3 = new NonRootIndexNode();

        root.addSubNode("1", node1);
        root.addSubNode("2", node2);
        root.addSubNode("3", node3);

        List<String> keysActual = root.getSubNodeKeys();

        root.addSubNode("4", node1);
        root.addSubNode("5", node2);
        root.addSubNode("6", node3);

        keysActual = root.getSubNodeKeys();

        Assert.assertEquals(keysExpected,keysActual);
    }

    @Test
    public void checkKeysListNotNull(){
        IndexNode root = new RootIndexNode(6);

        List<String> keysActual = root.getSubNodeKeys();

        Assert.assertNotEquals(keysActual,null);
    }

}
