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

package com.exactprosystems.clearth.automation.actions.compareDataSets;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.TestActionUtils;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

public class CompareDataSetsTest {

    private static final Path TEST_DATA = USER_DIR.resolve("src/test/resources/Action/CompareDataSets");
    private static final Path ACTIONS_MAPPING_PATH = TEST_DATA.resolve("actionsmapping.cfg");
    private static final Path MATRICES_DIR = TEST_DATA.resolve("matrices");
    private static final Path CONFIGS_DIR = TEST_DATA.resolve("configs");
    private static final Path CONFIG = CONFIGS_DIR.resolve("config.cfg");
    private static ApplicationManager clearThManager;
    private static Set<String> extraActions;

    @Test
    public void testSuccessfulExecute() throws ClearThException, IOException, AutomationException {

        Scheduler scheduler = TestActionUtils.runScheduler(clearThManager, ADMIN, ADMIN, CONFIG, MATRICES_DIR, 5000);
        Assert.assertTrue(scheduler.isSuccessful());
    }

    @BeforeClass
    public static void startTestApp() throws ClearThException, SettingsException {
        clearThManager = new ApplicationManager();
        extraActions = TestActionUtils.addCustomActions(ACTIONS_MAPPING_PATH).keySet();
    }

    @After
    public void clearSchedulerData()
    {
        TestActionUtils.resetUserSchedulers(ADMIN);
    }

    @AfterClass
    public static void disposeTestApp() throws IOException
    {
        TestActionUtils.removeCustomActions(extraActions);
        if (clearThManager != null)
          clearThManager.dispose();
    }
}
