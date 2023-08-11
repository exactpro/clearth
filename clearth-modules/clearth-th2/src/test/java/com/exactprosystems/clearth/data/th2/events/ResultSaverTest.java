/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data.th2.events;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.EventStatus;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.AttachedFilesResult;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.MultiDetailedResult;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.automation.report.results.TableResultDetail;
import com.exactprosystems.clearth.data.TestExecutionHandlingException;
import com.exactprosystems.clearth.data.th2.CollectingRouter;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.StringValueParser;
import com.google.protobuf.Timestamp;

public class ResultSaverTest
{
	private CollectingRouter<EventBatch> router;
	private ResultSaver saver;
	private Th2EventMetadata parentMetadata;
	
	@BeforeClass
	public void init()
	{
		ResultSavingConfig config = new ResultSavingConfig();
		config.setMaxBatchSize(2);
		
		router = new CollectingRouter<>();
		saver = new ResultSaver(router, config);
		
		Instant now = Instant.now();
		Timestamp ts = Timestamp.newBuilder()
				.setSeconds(now.getEpochSecond())
				.setNanos(now.getNano())
				.build();
		
		EventID parentId = EventID.newBuilder()
				.setBookName("Test book")
				.setScope("Test scope")
				.setStartTimestamp(ts)
				.setId("RootEventID")
				.build();
		
		parentMetadata = new Th2EventMetadata(parentId, now, null);
	}
	
	@BeforeMethod
	public void reset()
	{
		router.clearSent();
	}
	
	
//	Checks if basic features of results (status, comment, error) are stored in the event.
//	The check is done on example of DefaultResult.
//	All result classes have these features. Tests for other result classes won't include these checks.
//	Linked messages are expected to be attached to action event, not to event of its result, so attached messages not verified here
	@Test
	public void basicResultTest() throws UnsupportedResultException, TestExecutionHandlingException
	{
		String errorMessage = "Test error";
		Throwable error = new ClearThException(errorMessage);
		String errorClass = error.getClass().getCanonicalName();
		
		String name = "DefaultResultTest",
				comment = "Test comment";
		
		DefaultResult r = new DefaultResult();
		r.setSuccess(false);
		r.setComment(comment);
		r.setError(error);
		
		String expectedBody = String.format("["
				+ "{\"data\":\"%s\",\"type\":\"message\"},"
				+ "{\"data\":\"%s: %s\",\"type\":\"message\"}"
				+ "]",
				comment,
				errorClass, errorMessage);
		
		saver.storeResult(r, name, parentMetadata);
		
		EventBatch batch = router.getSent().get(0);
		Event event = batch.getEvents(0);
		String body = event.getBody().toStringUtf8();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(event.getName(), name + " - " + r.getComment(), "Name");
		soft.assertEquals(event.getStatus(), r.isSuccess() ? EventStatus.SUCCESS : EventStatus.FAILED, "Status");
		soft.assertEquals(event.getType(), "Comparison", "Type");
		soft.assertEquals(body, expectedBody, "Body");
		soft.assertAll();
	}
	
	@Test
	public void detailedResultTest() throws UnsupportedResultException, TestExecutionHandlingException
	{
		ResultDetail passed = new ResultDetail("Passed", "Value1", "Value1", true, false),
				failed = new ResultDetail("Failed", "Value2", "FailedValue", false, false),
				info = new ResultDetail("Info", "X", "Y", false, false),
				error = new ResultDetail("Error", "E", "R", false, false);
		info.setInfo(true);
		error.setErrorMessage("Could not compare values");
		
		String expectedBody = "[{\"type\":\"verification\",\"fields\":{"
				+ getVerificationRow(passed, getStatusText(passed.isIdentical()))+","
				+ getVerificationRow(failed, getStatusText(failed.isIdentical()))+","
				+ getVerificationRow(info, "NA")+","
				+ String.format("\"%s\":{\"actual\":\"%s\",\"expected\":\"%s\",\"status\":\"%s\",\"key\":false,\"hint\":\"%s\"}",
						error.getParam(), error.getActual(), error.getExpected(), getStatusText(error.isIdentical()), error.getErrorMessage())
				+ "}}]";
		
		DetailedResult r = new DetailedResult();
		r.addResultDetail(passed);
		r.addResultDetail(failed);
		r.addResultDetail(info);
		r.addResultDetail(error);
		
		saver.storeResult(r, parentMetadata);
		
		String body = getFirstEventBody(router.getSent().get(0));
		Assert.assertEquals(body, expectedBody);
	}
	
	@Test
	public void tableResultTest() throws UnsupportedResultException, TestExecutionHandlingException
	{
		String nameColumn = "ValueName",
				valueColumn = "CalculatedValue",
				value1Name = "Value1",
				value1Value = "X",
				value2Name = "Value2",
				value2Value = "Could not evaluate";
		
		TableResultDetail passed = new DefaultTableResultDetail(true, value1Name, value1Value),
				failed = new DefaultTableResultDetail(false, value2Name, value2Value);
		
		String expectedBody = "[{\"type\":\"table\",\"rows\":["
				+ String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"Status\":\"%s\"},",
						nameColumn, value1Name, valueColumn, value1Value, getStatusText(passed.isIdentical()))
				+ String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"Status\":\"%s\"}",
						nameColumn, value2Name, valueColumn, value2Value, getStatusText(failed.isIdentical()))
				+ "]}]";
		
		TableResult r = new TableResult(null, Arrays.asList(nameColumn, valueColumn), true);
		r.addDetail(passed);
		r.addDetail(failed);
		
		saver.storeResult(r, parentMetadata);
		
		String body = getFirstEventBody(router.getSent().get(0));
		Assert.assertEquals(body, expectedBody);
	}
	
	@Test
	public void multiDetailedResultTest() throws UnsupportedResultException, TestExecutionHandlingException
	{
		ResultDetail passed = new ResultDetail("Passed", "Value1", "Value1", true, false),
				failed = new ResultDetail("Failed", "Value2", "FailedValue", false, false);
		String block1Name = "Row #1",  //It will be not assigned to result block but will be added by ResultSaver,
				block2Name = "Failed rows",
				blockType = "Comparison";
		
		MultiDetailedResult r = new MultiDetailedResult();
		r.addResultDetail(passed);
		
		r.startNewBlock(block2Name);
		r.addResultDetail(failed);
		
		saver.storeResult(r, parentMetadata);
		
		EventBatch batch = router.getSent().get(0);
		Assert.assertEquals(batch.getEventsCount(), 3, "Event count");
		
		Event resultEvent = batch.getEvents(0),
				block1Event = batch.getEvents(1),
				block2Event = batch.getEvents(2);
		Assert.assertEquals(resultEvent.getType(), "Container", "Type of root event");
		Assert.assertEquals(resultEvent.getBody().toStringUtf8(), "[]", "Body of root event");
		
		assertEvent(block1Event, "block1", 
				resultEvent.getId(), block1Name, blockType,
				getVerificationBody(passed));
		
		assertEvent(block2Event, "block2", 
				resultEvent.getId(), block2Name, blockType, 
				getVerificationBody(failed));
	}
	
	@Test
	public void attachedFilesResultTest() throws IOException, UnsupportedResultException, TestExecutionHandlingException
	{
		Path filesRoot = Paths.get("src", "test", "resources", "attachedFiles"),
				dataFile = filesRoot.resolve("data.txt"),
				keyValueFile = filesRoot.resolve("keyValue.txt");
		String dataFileName = "Data file",
				dataFileContent = FileUtils.readFileToString(dataFile.toFile(), StandardCharsets.UTF_8),
				keyValueFileName = "Key-value",
				keyValueFileContent = FileUtils.readFileToString(keyValueFile.toFile(), StandardCharsets.UTF_8),
				fileEventType = "Output";
		
		AttachedFilesResult r = new AttachedFilesResult();
		r.attach(dataFileName, dataFile);
		r.attach(keyValueFileName, keyValueFile);
		
		saver.storeResult(r, parentMetadata);
		
		EventBatch batch = router.getSent().get(0);
		Assert.assertEquals(batch.getEventsCount(), 3, "Event count");
		
		Event resultEvent = batch.getEvents(0),
				dataFileEvent = batch.getEvents(1),
				keyValueFileEvent = batch.getEvents(2);
		Assert.assertEquals(resultEvent.getType(), "Container", "Type of root event");
		Assert.assertEquals(resultEvent.getBody().toStringUtf8(), "[]", "Body of root event");
		
		assertEvent(dataFileEvent, "data file event",
				resultEvent.getId(), dataFileName, fileEventType, 
				String.format("[{\"data\":\"%s\",\"type\":\"message\"}]", dataFileContent));
		
		assertEvent(keyValueFileEvent, "key-value file event",
				resultEvent.getId(), keyValueFileName, fileEventType,
				String.format("[{\"data\":\"%s\",\"type\":\"message\"}]", keyValueFileContent));
	}
	
	@Test
	public void csvDetailedResultTest() throws IOException, UnsupportedResultException, TestExecutionHandlingException
	{
		Path tempPath = Paths.get("src", "test", "resources", "temp");
		Files.createDirectories(tempPath);
		
		String name = "All rows",
				paramName = "Param1",
				rowType = "Comparison";
		ResultDetail passed = new ResultDetail(paramName, "Value1", "Value1", true, false),
				failed = new ResultDetail(paramName, "Value2", "FailedValue", false, false),
				extra = new ResultDetail(paramName, "", "UnexpectedValue", false, false);
		
		DetailedResult passedRow = new DetailedResult();
		passedRow.setComment("Row 1");
		passedRow.addResultDetail(passed);
		
		DetailedResult failedRow = new DetailedResult();
		failedRow.setComment("Row 2");
		failedRow.addResultDetail(failed);
		
		DetailedResult extraRow = new DetailedResult();
		extraRow.setComment("Row 3");
		extraRow.addResultDetail(extra);
		
		CsvDetailedResult r = new CsvDetailedResult(name, tempPath.toFile());
		r.setValueHandlers(new StringValuesComparator(new ComparisonUtils()), new StringValueParser());
		r.setMaxDisplayedRowsCount(0);
		r.addDetail(passedRow);
		r.addDetail(failedRow);
		r.addDetail(extraRow);
		r.processDetails(tempPath.toFile(), null);
		
		saver.storeResult(r, parentMetadata);
		
		//Due to ResultSaver configuration, 3 DetailedResults will be split into 2 batches
		List<EventBatch> allBatches = router.getSent();
		Assert.assertEquals(allBatches.size(), 3, "Batch count");
		
		EventBatch resultBatch = allBatches.get(0),
				rowBatch1 = allBatches.get(1),
				rowBatch2 = allBatches.get(2);
		
		Assert.assertEquals(resultBatch.getEventsCount(), 1, "Event count in result batch");
		Event resultEvent = resultBatch.getEvents(0);
		Assert.assertEquals(resultEvent.getType(), "Container", "Type of root event");
		Assert.assertEquals(resultEvent.getBody().toStringUtf8(), "[]", "Body of root event");
		
		EventID resultId = resultEvent.getId();
		
		Assert.assertEquals(rowBatch1.getEventsCount(), 2, "Event count in batch 1");
		Assert.assertEquals(rowBatch1.getParentEventId(), resultId, "Parent ID of batch 1");
		Event passedEvent = rowBatch1.getEvents(0),
				failedEvent = rowBatch1.getEvents(1);
		assertEvent(passedEvent, "passed row event", resultId, 
				passedRow.getComment(), rowType, 
				getVerificationBody(passed));
		assertEvent(failedEvent, "failed row event", resultId, 
				failedRow.getComment(), rowType, 
				getVerificationBody(failed));
		
		Assert.assertEquals(rowBatch2.getEventsCount(), 1, "Event count in batch 2");
		Assert.assertEquals(rowBatch2.getParentEventId(), resultId, "Parent ID of batch 2");
		Event extraEvent = rowBatch2.getEvents(0);
		assertEvent(extraEvent, "extra row event", resultId, 
				extraRow.getComment(), rowType, 
				getVerificationBody(extra));
	}
	
	@Test
	public void containerResultTest() throws UnsupportedResultException, TestExecutionHandlingException
	{
		String name = "Test container",
				containerType = "Container",
				comparisonType = "Comparison",
				emptyBody = "[]";
		Result passedResult = DefaultResult.passed("Passed block"),
				failedResult = DefaultResult.failed("Failed block"),
				topResult = DefaultResult.passed("Top block");
		
		//This one creates additional nesting level in ClearTH report, but it will be flattened for th2 as it contains only one child, 
		//i.e. flatResult will be skipped, but passedResult will be stored with header of flatResult
		ContainerResult flatResult = ContainerResult.createBlockResult("Simple container", Collections.singletonList(passedResult));
		
		ContainerResult subContainer = ContainerResult.createBlockResult("Sub-container");
		subContainer.addDetail(flatResult);
		subContainer.addDetail(failedResult);
		
		ContainerResult container = ContainerResult.createPlainResult("Top");
		container.addDetail(topResult);
		container.addDetail(subContainer);
		
		saver.storeResult(container, name, parentMetadata);
		
		List<EventBatch> allBatches = router.getSent();
		Assert.assertEquals(allBatches.size(), 5, "Batch count");
		
		EventBatch resultBatch = allBatches.get(0),
				topResultBatch = allBatches.get(1),
				subContainerBatch = allBatches.get(2),
				passedResultBatch = allBatches.get(3),
				failedResultBatch = allBatches.get(4);
		
		Assert.assertEquals(resultBatch.getEventsCount(), 1, "Event count in result batch");
		Event resultEvent = resultBatch.getEvents(0);
		assertEvent(resultEvent, "result event", parentMetadata.getId(), name, containerType, emptyBody);
		
		
		EventID resultId = resultEvent.getId();
		
		Assert.assertEquals(topResultBatch.getEventsCount(), 1, "Event count in top result batch");
		assertEvent(topResultBatch.getEvents(0), "top result event", resultId, "Result - "+topResult.getComment(), comparisonType,
				String.format("[{\"data\":\"%s\",\"type\":\"message\"}]", topResult.getComment()));
		
		Assert.assertEquals(subContainerBatch.getEventsCount(), 1, "Event count in sub-container batch");
		Event subContainerEvent = subContainerBatch.getEvents(0);
		assertEvent(subContainerEvent, "sub-container event", resultId, subContainer.getHeader(), containerType, emptyBody);
		
		
		EventID subId = subContainerEvent.getId();
		
		Assert.assertEquals(passedResultBatch.getEventsCount(), 1, "Event count in passed result batch");
		assertEvent(passedResultBatch.getEvents(0), "passed event", subId, flatResult.getHeader()+" - "+passedResult.getComment(), comparisonType, 
				String.format("[{\"data\":\"%s\",\"type\":\"message\"}]", passedResult.getComment()));
		
		Assert.assertEquals(failedResultBatch.getEventsCount(), 1, "Event count in failed result batch");
		assertEvent(failedResultBatch.getEvents(0), "failed event", subId, "Result - "+failedResult.getComment(), comparisonType, 
				String.format("[{\"data\":\"%s\",\"type\":\"message\"}]", failedResult.getComment()));
	}
	
	
	private String getStatusText(boolean success)
	{
		return success ? "PASSED" : "FAILED";
	}
	
	private String getFirstEventBody(EventBatch batch)
	{
		Event event = batch.getEvents(0);
		return event.getBody().toStringUtf8();
	}
	
	private void assertEvent(Event event, String description, EventID parentId, String name, String type, String body)
	{
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(event.getParentId(), parentId, "Parent ID of "+description);
		soft.assertEquals(event.getName(), name, "Name of "+description);
		soft.assertEquals(event.getType(), type, "Type of "+description);
		soft.assertEquals(event.getBody().toStringUtf8(), body, "Body of "+description);
		soft.assertAll();
	}
	
	private String getVerificationRow(ResultDetail rd, String status)
	{
		return String.format("\"%s\":{\"actual\":\"%s\",\"expected\":\"%s\",\"status\":\"%s\",\"key\":false}",
				rd.getParam(), rd.getActual(), rd.getExpected(), status);
	}
	
	private String getVerificationBody(ResultDetail rd)
	{
		return "[{\"type\":\"verification\",\"fields\":{"
				+ getVerificationRow(rd, getStatusText(rd.isIdentical()))
				+ "}}]";
	}
}