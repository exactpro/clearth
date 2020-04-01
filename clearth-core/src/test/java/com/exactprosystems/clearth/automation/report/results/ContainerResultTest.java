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
package com.exactprosystems.clearth.automation.report.results;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;


public class ContainerResultTest extends BasicTestNgTest
{
	@DataProvider
	private Object[] invertedStatuses()
	{
		return new Object[] { true, false };
	}


	@Test(dataProvider = "invertedStatuses")
	public void containerWithInnerContainer(boolean isInverted)
	{
		ContainerResult externalContainerResult = ContainerResult.createPlainResult("External container");
		ContainerResult innerContainer = ContainerResult.createBlockResult("Inner container");
		DetailedResult detailedResult = new DetailedResult();

		detailedResult.addResultDetail(createResultDetail("Param1", "1", "1", true));
		detailedResult.addResultDetail(createResultDetail("Param2", "1", "2", false));
		innerContainer.addDetail(detailedResult);
		externalContainerResult.addDetail(innerContainer);
		innerContainer.setInverted(isInverted);

		checkContainers(externalContainerResult, isInverted);
	}

	@Test(dataProvider = "invertedStatuses")
	public void updatedContainerWithInnerContainers(boolean isInverted)
	{
		ContainerResult externalContainer = ContainerResult.createPlainResult("External container");
		ContainerResult secondInnerContainer = ContainerResult.createBlockResult("Inner container 2");
		ContainerResult firstInnerContainer = ContainerResult.createBlockResult("Inner container 1",
				Collections.singletonList(secondInnerContainer));
		DetailedResult detailsInInnerContainer = new DetailedResult();

		externalContainer.addDetail(firstInnerContainer);
		secondInnerContainer.addDetail(detailsInInnerContainer);
		detailsInInnerContainer.addResultDetail(createResultDetail("Param1", "1", "1", true));
		detailsInInnerContainer.addResultDetail(createResultDetail("Param2", "1", "2", false));
		secondInnerContainer.setInverted(isInverted);

		checkContainers(externalContainer, isInverted);
	}


	private void checkContainers(ContainerResult result, boolean shouldBeSuccessfully)
	{
		if (shouldBeSuccessfully)
			assertTrue(result.isSuccess(), "Container result should be true");
		else
			assertFalse(result.isSuccess(), "Container result should be false");

		for (Result detail : result.details)
		{
			if (detail instanceof ContainerResult)
				checkContainers((ContainerResult) detail, shouldBeSuccessfully);
		}
	}

	private ResultDetail createResultDetail(String param, String expected, String actual, boolean identical)
	{
		return new ResultDetail(param, expected, actual, identical);
	}
}
