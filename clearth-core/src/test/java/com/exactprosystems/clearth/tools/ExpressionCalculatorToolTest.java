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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.utils.ClearThException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.testng.Assert.*;

public class ExpressionCalculatorToolTest {

    private static final String TEST_EXPRESSION = "@{format(time(0),'yyyy-MM-dd')}";
    private static final String PREFIX = "trade_";
    private static final String POSTFIX = "_01";
    private String current_date;
    private static ApplicationManager clearThManager;
    private final ExpressionCalculatorTool calculatorTool = new ExpressionCalculatorTool();

    @DataProvider(name="test-data")
    public Object[][] dataProviderMethod() {

        return new Object[][] {
                {
                    TEST_EXPRESSION,
                    current_date
                },
                {
                    PREFIX + TEST_EXPRESSION,
                    PREFIX + current_date
                },
                {
                    TEST_EXPRESSION + POSTFIX,
                    current_date + POSTFIX
                },
                {
                    PREFIX + TEST_EXPRESSION + POSTFIX,
                    PREFIX + current_date + POSTFIX
                },
                {
                        TEST_EXPRESSION + TEST_EXPRESSION,
                        current_date + current_date
                },
                {
                        TEST_EXPRESSION + PREFIX + TEST_EXPRESSION,
                        current_date + PREFIX + current_date
                },
                {
                        PREFIX + TEST_EXPRESSION + PREFIX + TEST_EXPRESSION,
                        PREFIX + current_date + PREFIX + current_date
                },
                {
                        PREFIX + TEST_EXPRESSION + PREFIX + TEST_EXPRESSION + POSTFIX,
                        PREFIX + current_date + PREFIX + current_date + POSTFIX
                }
        };
    }


    @BeforeClass
    public void startTestApplication() throws ClearThException
    {
        clearThManager = new ApplicationManager();
        current_date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    }

    @Test(dataProvider = "test-data")
    public void test(String expression, String expected) throws Exception {
        String actual = calculatorTool.calculate(expression, null);
        assertEquals(actual, expected);
    }


    @AfterClass
    public static void disposeTestApplication() throws IOException
    {
        if (clearThManager != null) clearThManager.dispose();
    }

}