/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data.th2;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ActionSettings;
import com.exactprosystems.clearth.automation.CoreStepKind;
import com.exactprosystems.clearth.automation.DefaultStep;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.MvelVariablesFactory;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.actions.SetStatic;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.data.TestExecutionHandlingException;
import com.exactprosystems.clearth.data.th2.config.EventsConfig;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.events.EventFactory;
import com.exactprosystems.clearth.data.th2.events.EventUtils;
import com.exactprosystems.clearth.data.th2.events.ResultSaver;
import com.exactprosystems.clearth.data.th2.events.ResultSavingConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;
import com.google.protobuf.Timestamp;

import static org.testng.Assert.*;

public class Th2TestExecutionHandlerTest
{
	private String actionName;
	private MvelVariablesFactory mvelFactory;
	
	@BeforeClass
	public void init()
	{
		actionName = "SetStatic";
		mvelFactory = new MvelVariablesFactory(null, null);
	}
	
	@Test
	public void eventContents() throws Exception
	{
		CollectingRouter<EventBatch> router = new CollectingRouter<>();
		String book = "book1",
				scope = "default",
				schedulerName = "main",
				userName = "user1",
				matrixName = "test_matrix",
				initStepName = "Initialization",
				procStepName = "Processing",
				actionId = "firstAction";
		StorageConfig config = new StorageConfig(book, new EventsConfig(scope, 100));
		Instant schedulerStart = Instant.ofEpochSecond(Instant.now().getEpochSecond()),  //It will be converted to Date which lacks nanoseconds and then back to Instant
				initStepStart,
				procStepStart,
				now = Instant.now();
		
		MessageID msgId = MessageID.newBuilder()
				.setBookName(book)
				.setTimestamp(Timestamp.newBuilder()
						.setSeconds(now.getEpochSecond())
						.setNanos(now.getNano())
						.build())
				.setConnectionId(ConnectionID.newBuilder()
						.setSessionAlias("Con1")
						.build())
				.setDirection(Direction.FIRST)
				.setSequence(100)
				.build();
		ClearThMessageMetadata msgMetadata = new ClearThMessageMetadata(ClearThMessageDirection.RECEIVED, now, new HashMap<>());
		MessageHandlingUtils.setMessageId(msgMetadata, new Th2MessageId(msgId));
		
		try (TestExecutionHandler handler = new Th2TestExecutionHandler(schedulerName, router, 
				new EventFactory(config), 
				new ResultSaver(router, new ResultSavingConfig())))
		{
			GlobalContext gc = createGlobalContext(schedulerStart, userName, handler);
			
			handler.onTestStart(Collections.singleton(matrixName), gc);
			
			initStepStart = Instant.now();
			handler.onGlobalStepStart(new StepMetadata(initStepName, initStepStart));
			handler.onGlobalStepEnd();
			
			procStepStart = Instant.now();
			handler.onGlobalStepStart(new StepMetadata(procStepName, procStepStart));
			onAction(createSetStatic(actionId, createMatrix(matrixName), createStep(procStepName), new EncodedClearThMessage("Test message", msgMetadata)),
					handler);
			handler.onGlobalStepEnd();
			handler.onTestEnd();
		}
		
		List<EventBatch> stored = router.getSent();
		//6 events = scheduler start + matrix + step + action + action parameters + action status
		assertEquals(stored.size(), 6, "Number of stored events");
		
		Iterator<EventBatch> it = stored.iterator();
		
		Event schedulerEvent = getFirstEvent(it.next());
		EventID schedulerEventId = schedulerEvent.getId();
		assertEquals(schedulerEvent.getName(), 
				String.format("'%s' scheduler execution - Started by '%s'", schedulerName, userName), 
				"Scheduler event name");
		assertEquals(schedulerEvent.getBody().toStringUtf8(), 
				String.format("[{\"data\":\"Started by '%s'\",\"type\":\"message\"}]", userName),
				"Scheduler event body");
		assertEquals(schedulerEventId.getBookName(), book, "Scheduler event book");
		assertEquals(schedulerEventId.getScope(), scope, "Scheduler event scope");
		assertEquals(EventUtils.getTimestamp(schedulerEventId.getStartTimestamp()), schedulerStart, "Scheduler event start timestamp");
		
		Event matrixEvent = getFirstEvent(it.next());
		assertEquals(matrixEvent.getName(), matrixName, "Matrix event name");
		assertEquals(EventUtils.getTimestamp(matrixEvent.getId().getStartTimestamp()), schedulerStart, "Matrix event start timestamp");
		
		Event stepEvent = getFirstEvent(it.next());
		assertEquals(stepEvent.getName(), procStepName, "Step event name");
		assertEquals(EventUtils.getTimestamp(stepEvent.getId().getStartTimestamp()), procStepStart, "Step event start timestamp");
		
		Event actionEvent = getFirstEvent(it.next());
		assertEquals(actionEvent.getName(), 
				String.format("%s - SetStatic", actionId), 
				"Action event name");
		
		Event actionParamsEvent = getFirstEvent(it.next());
		assertEquals(actionParamsEvent.getName(), "Input parameters", "Action parameters event name");
		
		Event actionStatusEvent = getFirstEvent(it.next());
		assertEquals(actionStatusEvent.getName(), "Action status", "Action status event name");
		assertEquals(actionStatusEvent.getAttachedMessageIdsCount(), 1, "Number of attached messages");
		assertEquals(actionStatusEvent.getAttachedMessageIds(0), msgId, "Attached message ID");
	}
	
	@Test
	public void executionHierarchy() throws Exception
	{
		CollectingRouter<EventBatch> router = new CollectingRouter<>();
		
		StorageConfig config = new StorageConfig("book1", new EventsConfig("default", 100));
		String testMatrixName = "test_matrix",
				anotherMatrixName = "another_matrix",
				step1Name = "step1",
				step2Name = "step2",
				step3Name = "step3";
		Matrix testMatrix = createMatrix(testMatrixName),
				anotherMatrix = createMatrix(anotherMatrixName);
		Collection<String> matrices = Arrays.asList(testMatrixName, anotherMatrixName);
		Step step1 = createStep(step1Name),
				step2 = createStep(step2Name),
				step3 = createStep(step3Name);
		
		try (TestExecutionHandler handler = new Th2TestExecutionHandler("main", router, 
				new EventFactory(config), 
				new ResultSaver(router, new ResultSavingConfig())))
		{
			GlobalContext gc = createGlobalContext(Instant.now(), "admin", handler);
			
			handler.onTestStart(matrices, gc);
			
			Instant step1Start = Instant.now();
			handler.onGlobalStepStart(new StepMetadata(step1Name, step1Start));
			onAction(createSetStatic("id1", testMatrix, step1, null), handler);
			onAction(createSetStatic("id2", anotherMatrix, step1, null), handler);
			handler.onGlobalStepEnd();
			
			Instant step2Start = Instant.now();
			handler.onGlobalStepStart(new StepMetadata(step2Name, step2Start));
			onAction(createSetStatic("id2", anotherMatrix, step2, null), handler);
			handler.onGlobalStepEnd();
			
			Instant step3Start = Instant.now();
			handler.onGlobalStepStart(new StepMetadata(step3Name, step3Start));
			onAction(createSetStatic("id5", testMatrix, step3, null), handler);
			handler.onGlobalStepEnd();
			
			handler.onTestEnd();
		}
		
		List<EventBatch> stored = router.getSent();
		//19 events = scheduler start + 2 matrices 
		//  + 2 steps for testMatrix + 2 actions + parameters of each action + status of each action
		//  + 2 steps for anotherMatrix + 2 actions + parameters of each action + status of each action
		assertEquals(stored.size(), 19, "Number of stored events");
		
		Iterator<EventBatch> it = stored.iterator();
		
		Event schedulerEvent = getFirstEvent(it.next()),
				testMatrixEvent = getFirstEvent(it.next()),
				anotherMatrixEvent = getFirstEvent(it.next());
		assertEquals(testMatrixEvent.getParentId(), schedulerEvent.getId(), "Parent of "+testMatrixName);
		assertEquals(anotherMatrixEvent.getParentId(), schedulerEvent.getId(), "Parent of "+anotherMatrixName);
		
		Event testMatrixStep1Event = getFirstEvent(it.next()),
				testMatrixAction1Event = getFirstEvent(it.next()),
				testMatrixAction1StatusEvent = getFirstEvent(it.next()),
				testMatrixAction1ResultEvent = getFirstEvent(it.next());
		assertEquals(testMatrixStep1Event.getParentId(), testMatrixEvent.getId(), "Parent of "+step1Name+" from "+testMatrixName);
		assertEquals(testMatrixAction1Event.getParentId(), testMatrixStep1Event.getId(), "Parent of first action from "+testMatrixName);
		assertEquals(testMatrixAction1StatusEvent.getParentId(), testMatrixAction1Event.getId(), "Parent of status of first action from "+testMatrixName);
		assertEquals(testMatrixAction1ResultEvent.getParentId(), testMatrixAction1Event.getId(), "Parent of result of first action from "+testMatrixName);
		
		Event anotherMatrixStep1Event = getFirstEvent(it.next()),
				anotherMatrixAction1Event = getFirstEvent(it.next()),
				anotherMatrixAction1StatusEvent = getFirstEvent(it.next()),
				anotherMatrixAction1ResultEvent = getFirstEvent(it.next());
		assertEquals(anotherMatrixStep1Event.getParentId(), anotherMatrixEvent.getId(), "Parent of "+step1Name+" from "+anotherMatrixName);
		assertEquals(anotherMatrixAction1Event.getParentId(), anotherMatrixStep1Event.getId(), "Parent of first action from "+anotherMatrixName);
		assertEquals(anotherMatrixAction1StatusEvent.getParentId(), anotherMatrixAction1Event.getId(), "Parent of status of first action from "+anotherMatrixName);
		assertEquals(anotherMatrixAction1ResultEvent.getParentId(), anotherMatrixAction1Event.getId(), "Parent of result of first action from "+anotherMatrixName);
		
		Event anotherMatrixStep2Event = getFirstEvent(it.next()),
				anotherMatrixAction2Event = getFirstEvent(it.next()),
				anotherMatrixAction2StatusEvent = getFirstEvent(it.next()),
				anotherMatrixAction2ResultEvent = getFirstEvent(it.next());
		assertEquals(anotherMatrixStep2Event.getParentId(), anotherMatrixEvent.getId(), "Parent of "+step2Name+" from "+anotherMatrixName);
		assertEquals(anotherMatrixAction2Event.getParentId(), anotherMatrixStep2Event.getId(), "Parent of second action from "+anotherMatrixName);
		assertEquals(anotherMatrixAction2StatusEvent.getParentId(), anotherMatrixAction2Event.getId(), "Parent of status of second action from "+anotherMatrixName);
		assertEquals(anotherMatrixAction2ResultEvent.getParentId(), anotherMatrixAction2Event.getId(), "Parent of result of second action from "+anotherMatrixName);
		
		Event testMatrixStep3Event = getFirstEvent(it.next()),
				testMatrixAction2Event = getFirstEvent(it.next()),
				testMatrixAction2StatusEvent = getFirstEvent(it.next()),
				testMatrixAction2ResultEvent = getFirstEvent(it.next());
		assertEquals(testMatrixStep3Event.getParentId(), testMatrixEvent.getId(), "Parent of "+step3Name+" from "+testMatrixName);
		assertEquals(testMatrixAction2Event.getParentId(), testMatrixStep3Event.getId(), "Parent of second action from "+testMatrixName);
		assertEquals(testMatrixAction2StatusEvent.getParentId(), testMatrixAction2Event.getId(), "Parent of status of second action from "+testMatrixName);
		assertEquals(testMatrixAction2ResultEvent.getParentId(), testMatrixAction2Event.getId(), "Parent of result of second action from "+testMatrixName);
	}
	
	
	private Matrix createMatrix(String name)
	{
		Matrix result = new Matrix(mvelFactory);
		result.setName(name);
		return result;
	}
	
	private Step createStep(String name)
	{
		return new DefaultStep(name, CoreStepKind.Default.getLabel(), null, null, false, null, false, false, true, null);
	}
	
	private GlobalContext createGlobalContext(Instant start, String startedByUser, TestExecutionHandler handler)
	{
		GlobalContext result = new GlobalContext(new Date(), false, null, null, startedByUser, handler);
		result.setStarted(Date.from(start));
		return result;
	}
	
	private Action createSetStatic(String id, Matrix matrix, Step step, EncodedClearThMessage message)
	{
		ActionSettings settings = new ActionSettings();
		settings.setActionId(id);
		settings.setActionName(actionName);
		settings.setMatrix(matrix);
		settings.setStep(step);
		settings.setMatrixInputParams(Collections.emptySet());
		
		Action result = new SetStatic();
		result.preInit(null, actionName, Collections.emptyMap());
		result.init(settings);
		
		if (message != null)
		{
			DefaultResult actionResult = new DefaultResult();
			actionResult.addLinkedMessage(message);
			result.setResult(actionResult);
		}
		return result;
	}
	
	private void onAction(Action action, TestExecutionHandler handler) throws TestExecutionHandlingException
	{
		HandledTestExecutionId execId = handler.onAction(action);
		action.setTestExecutionId(execId);
		
		handler.onActionResult(action.getResult(), action);
	}
	
	private Event getFirstEvent(EventBatch batch)
	{
		return batch.getEvents(0);
	}
}
