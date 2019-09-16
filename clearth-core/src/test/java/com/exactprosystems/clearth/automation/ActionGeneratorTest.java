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

package com.exactprosystems.clearth.automation;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ActionGeneratorTest {
    Logger logger = mock(Logger.class);
    private static final String ACTION_NAME = "ACTION_NAME";
    private static final String INCORRECT_PARSING_RESULT_MSG = "Incorrect parsing result. Expected: %s, Actual: %s";

    @Test
    public void parseDefaultInputParams_Simple() {
        Map<String, String> expected = buildMap("param1", "One");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams("param1=One", ACTION_NAME, logger);
        verify(logger, times(0)).warn(anyString());
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    @Test
    public void parseDefaultInputParams_Simple2() {
        Map<String, String> expected = buildMap("param1", "One", "param2", "Two", "param3", "Three");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams("param1=One, param2 = Two ,param3=Three", ACTION_NAME, logger);
        verify(logger, times(0)).warn(anyString());
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    @Test
    public void parseDefaultInputParams_Quoted() {
        String line = "param1=\"One\"";
        Map<String, String> expected = buildMap("param1", "One");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams(line, ACTION_NAME, logger);
        verify(logger, times(0)).warn(anyString());
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    @Test
    public void parseDefaultInputParams_Quoted2() {
        String line = "param1=\"One\", param2=\"T\\\"w\\\"o\" , OtherParam = \",=\\t!@$%^&*[]{}()\\\\\"";
        Map<String, String> expected = buildMap("param1", "One", "param2", "T\"w\"o", "OtherParam", ",=\t!@$%^&*[]{}()\\");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams(line, ACTION_NAME, logger);
        verify(logger, times(0)).warn(anyString());
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    @Test
    public void parseDefaultInputParams_FailWithoutValue() {
        String line = "param1=One, param2";
        Map<String, String> expected = buildMap("param1", "One");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams(line, ACTION_NAME, logger);
        verify(logger, times(1)).warn(contains("unexpected end of parameter's list"));
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    @Test
    public void parseDefaultInputParams_FailUnclosedQuote() {
        String line = "param1=One, param2=\"Two";
        Map<String, String> expected = buildMap("param1", "One");
        Map<String, String> actual = ActionGenerator.parseDefaultInputParams(line, ACTION_NAME, logger);
        verify(logger, times(1)).warn(contains("unexpected end of parameter's list"));
        verify(logger, times(0)).error(anyString());
        assertTrue(format(INCORRECT_PARSING_RESULT_MSG, expected, actual), mapIsEquals(expected, actual));
    }

    private static boolean mapIsEquals(Map<String, String> map1, Map<String, String> map2)
    {
        if (map1 == null && map2 == null)
            return true;
        if (map1 == null || map2 == null)
            return false;
        if (map1.size() != map2.size())
            return false;
        for (Map.Entry<String, String> entry1: map1.entrySet())
        {
            if (!StringUtils.equals(entry1.getValue(), map2.get(entry1.getKey())))
                return false;
        }
        return true;
    }

    private static Map<String, String> buildMap(String... keysAndValues) {
        if (keysAndValues.length % 2 != 0)
            throw new RuntimeException("Please use even number of arguments for buildMap() method");
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 1; i < keysAndValues.length; i+=2)
        {
            map.put(keysAndValues[i-1], keysAndValues[i]);
        }
        return map;
    }
}
