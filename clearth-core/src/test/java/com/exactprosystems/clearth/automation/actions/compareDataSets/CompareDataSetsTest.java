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
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.LoggerStub;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.automation.ActionMetaData;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.exactprosystems.clearth.ApplicationManager.*;

public class CompareDataSetsTest {

    private static final Path TEST_DATA = USER_DIR.resolve("src/test/resources/Action/CompareDataSets");
    private static final Path ACTIONS_MAPPING_PATH = TEST_DATA.resolve("actionsmapping.cfg");
    private static final Path MATRICES_DIR = TEST_DATA.resolve("matrices");
    private static final Path CONFIGS_DIR = TEST_DATA.resolve("configs");
    private static final Path CONFIG = CONFIGS_DIR.resolve("config.cfg");
    private static ApplicationManager clearThManager;
    private static final Logger logger = new LoggerStub();
    private static Map<String, ActionMetaData> extraActionsMapping;
    private static Map<String, ActionMetaData> originActionsMapping;

    @Test
    public void testSourceParamAsOptional() throws ClearThException, IOException, AutomationException {

        Scheduler scheduler = clearThManager.getScheduler(ADMIN, ADMIN);
        scheduler.clearSteps();

        clearThManager.loadSteps(scheduler, CONFIG.toFile());
        clearThManager.loadMatrices(scheduler, MATRICES_DIR.toFile());

        scheduler.start(ADMIN);
        waitForSchedulerToStop(scheduler, 100, 5000);
        Assert.assertTrue(scheduler.isSuccessful());
    }

    @BeforeClass
    public static void startTestApp() throws ClearThException
    {
        clearThManager = new ApplicationManager();
        addExtraActionsMapping();
    }

    private static void addExtraActionsMapping()
    {
        originActionsMapping = ClearThCore.getInstance().getActionFactory().getActionsMapping();
        extraActionsMapping = ActionGenerator.loadActionsMapping(ACTIONS_MAPPING_PATH.toString(), true, logger);
        originActionsMapping.putAll(extraActionsMapping);
    }

    @AfterClass
    public static void disposeTestApp() throws IOException
    {
        removeExtraActionsMapping();
        if (clearThManager != null) clearThManager.dispose();
    }

    private static void removeExtraActionsMapping()
    {
        for (String actionName : extraActionsMapping.keySet())
        {
            originActionsMapping.remove(actionName);
        }
    }
}
