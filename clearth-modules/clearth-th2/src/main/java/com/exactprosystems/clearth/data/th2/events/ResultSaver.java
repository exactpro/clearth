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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.exactpro.th2.common.event.Event;
import com.exactpro.th2.common.event.IBodyData;
import com.exactpro.th2.common.event.bean.IRow;
import com.exactpro.th2.common.event.bean.Message;
import com.exactpro.th2.common.event.bean.Table;
import com.exactpro.th2.common.event.bean.Verification;
import com.exactpro.th2.common.event.bean.VerificationEntry;
import com.exactpro.th2.common.event.bean.VerificationStatus;
import com.exactpro.th2.common.event.bean.builder.TableBuilder;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.AttachedFilesResult;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.MultiDetailedResult;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.automation.report.results.TableResultDetail;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvDetailedResultReader;
import com.exactprosystems.clearth.automation.report.results.subclasses.DetailsBlock;
import com.exactprosystems.clearth.data.TestExecutionHandlingException;
import com.exactprosystems.clearth.data.th2.tables.MapRow;

public class ResultSaver
{
	public static final String NAME_RESULT = "Result",
			TYPE_COMPARISON = "Comparison",
			TYPE_CONTAINER = "Container",
			TYPE_OUTPUT = "Output",
			FIELD_STATUS = "Status";
	
	private final MessageRouter<EventBatch> router;
	private final int maxBatchSize;
	
	public ResultSaver(MessageRouter<EventBatch> router, ResultSavingConfig config)
	{
		this.router = router;
		this.maxBatchSize = config.getMaxBatchSize();
	}
	
	public void storeResult(Result result, String name, Th2EventMetadata parentMetadata) throws UnsupportedResultException, TestExecutionHandlingException
	{
		if (storeCustom(result, name, parentMetadata))
			return;
		
		if (result instanceof DefaultResult)  //This is needed in case when DefaultResult is part of ContainerResult. DefaultResult of Action is saved as part of Action event
		{
			store((DefaultResult)result, name, parentMetadata);
			return;
		}
		
		if (result instanceof DetailedResult)
		{
			store((DetailedResult)result, name, parentMetadata);
			return;
		}
		
		if (result instanceof TableResult)
		{
			store((TableResult)result, name, parentMetadata);
			return;
		}
		
		if (result instanceof MultiDetailedResult)
		{
			store((MultiDetailedResult)result, name, parentMetadata);
			return;
		}
		
		if (result instanceof AttachedFilesResult)
		{
			store((AttachedFilesResult)result, name, parentMetadata);
			return;
		}
		
		if (result instanceof CsvDetailedResult)
		{
			store((CsvDetailedResult)result, parentMetadata);
			return;
		}
		
		if (result instanceof ContainerResult)
		{
			store((ContainerResult)result, name, parentMetadata);
			return;
		}
		
		throw new UnsupportedResultException("Result of class "+result.getClass().getCanonicalName()+" is not supported");
	}
	
	public void storeResult(Result result, Th2EventMetadata parentMetadata) throws UnsupportedResultException, TestExecutionHandlingException
	{
		storeResult(result, NAME_RESULT, parentMetadata);
	}
	
	
	protected boolean storeCustom(Result result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		return false;
	}
	
	protected MessageRouter<EventBatch> getRouter()
	{
		return router;
	}
	
	protected com.exactpro.th2.common.grpc.Event storeEvent(Event event, EventID parentId) throws TestExecutionHandlingException
	{
		try
		{
			com.exactpro.th2.common.grpc.Event protoEvent = event.toProto(parentId);
			EventBatch protoBatch = EventUtils.wrap(protoEvent);
			storeProto(protoBatch);
			return protoEvent;
		}
		catch (TestExecutionHandlingException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw conversionError(e);
		}
	}
	
	protected com.exactpro.th2.common.grpc.EventBatch storeBatch(Event event, EventID parentId) throws TestExecutionHandlingException
	{
		try
		{
			com.exactpro.th2.common.grpc.EventBatch protoBatch = event.toBatchProto(parentId);
			storeProto(protoBatch);
			return protoBatch;
		}
		catch (TestExecutionHandlingException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw conversionError(e);
		}
	}
	
	protected void storeProto(com.exactpro.th2.common.grpc.EventBatch protoBatch) throws TestExecutionHandlingException
	{
		try
		{
			getRouter().send(protoBatch);
		}
		catch (Exception e)
		{
			throw new TestExecutionHandlingException("Could not save action result", e);
		}
	}
	
	
	protected void store(DefaultResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_COMPARISON, parentMetadata);
		storeEvent(event, parentMetadata.getId());
	}
	
	protected void store(DetailedResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_COMPARISON, parentMetadata)
				.bodyData(createComparisonTable(result.getResultDetails()));
		
		storeEvent(event, parentMetadata.getId());
	}
	
	protected void store(TableResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_COMPARISON, parentMetadata)
				.bodyData(createTable(result.getColumns(), result.getDetails(), result.isHasStatus()));
		
		storeEvent(event, parentMetadata.getId());
	}
	
	protected void store(MultiDetailedResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_CONTAINER, parentMetadata);
		int i = 0;
		for (DetailsBlock block : result.getDetails())
		{
			i++;
			String blockName = block.getComment();
			if (blockName == null)
				blockName = "Row #"+i;
			
			event.addSubEventWithSamePeriod()
					.name(blockName)
					.type(TYPE_COMPARISON)
					.status(EventUtils.getStatus(block.isSuccess()))
					.bodyData(createComparisonTable(block.getDetails()));
		}
		
		storeBatch(event, parentMetadata.getId());
	}
	
	protected void store(AttachedFilesResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_CONTAINER, parentMetadata);
		for (String id : result.getIds())
		{
			Path file = result.getPath(id);
			String text;
			try
			{
				text = FileUtils.readFileToString(file.toFile(), StandardCharsets.UTF_8);
			}
			catch (Exception e)
			{
				throw new TestExecutionHandlingException("Could not read file '"+file+"' attached to action result", e);
			}
			
			Message resultText = new Message();
			resultText.setData(text);
			
			event.addSubEventWithSamePeriod()
					.name(id)
					.type(TYPE_OUTPUT)
					.bodyData(resultText);
		}
		
		storeBatch(event, parentMetadata.getId());
	}
	
	protected void store(CsvDetailedResult result, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event container = createEvent(result, 
				result.getName()+" ("+result.getTotalRowsCount()+")", 
				TYPE_CONTAINER, 
				parentMetadata);
		com.exactpro.th2.common.grpc.Event storedContainer = storeEvent(container, parentMetadata.getId());
		
		if (result.getMaxStoredRowsCount() == 0 || result.getReportFile() == null)
		{
			storePlainDetails(result.getDetails(), storedContainer);
			return;
		}
		
		storeDetailsFromFile(result, storedContainer);
	}
	
	protected void store(ContainerResult result, String name, Th2EventMetadata parentMetadata) throws TestExecutionHandlingException
	{
		Event event = createEvent(result, name, TYPE_CONTAINER, parentMetadata);
		com.exactpro.th2.common.grpc.Event stored = storeEvent(event, parentMetadata.getId());
		Th2EventMetadata storedMetadata = new Th2EventMetadata(stored.getId(), event.getStartTimestamp(), event.getEndTimestamp());
		
		for (Result sub : result.getDetails())
		{
			if (sub instanceof ContainerResult)
			{
				ContainerResult subContainer = (ContainerResult)sub;
				List<Result> subDetails = subContainer.getDetails();
				if (subDetails.size() == 1)
				{
					//subContainer is simple container, i.e. it contains only one detail and, probably, can be flattened:
					//- we'll save this detail to th2 using header of subContainer.
					//- subContainer itself won't be saved
					Result sd = subDetails.get(0);
					if (!(sd instanceof ContainerResult) || (((ContainerResult)sd).getHeader() == null))
					{
						storeResult(sd, subContainer.getHeader(), storedMetadata);  //Flattening simple container
						continue;
					}
				}
				
				store(subContainer, subContainer.getHeader(), storedMetadata);
			}
			else
				storeResult(sub, storedMetadata);
		}
	}
	
	
	protected Event createEvent(Result result, String name, String type, Th2EventMetadata metadata)
	{
		Throwable error = result.getError();
		Event event = ClearThEvent.fromTo(metadata.getStartTimestamp(), metadata.getEndTimestamp())
				.name(name)
				.type(type)
				.description(result.getComment())
				.status(EventUtils.getStatus(result.isSuccess()));
		if (error != null)
			event.exception(error, true);
		return event;
	}
	
	
	protected Verification createComparisonTable(Collection<ResultDetail> details)
	{
		Map<String, VerificationEntry> rows = new LinkedHashMap<>();
		for (ResultDetail rd : details)
		{
			VerificationEntry entry = createVerificationEntry(rd);
			rows.put(rd.getParam(), entry);
		}
		
		Verification result = new Verification();
		result.setFields(rows);
		return result;
	}
	
	protected IBodyData createTable(Collection<String> columns, List<TableResultDetail> details, boolean withStatus) throws EventCreationException
	{
		List<IRow> rows = new ArrayList<>(details.size());
		for (TableResultDetail d : details)
		{
			List<String> dValues = d.getValues();
			if (columns.size() != dValues.size())
				throw new EventCreationException(String.format("Invalid table result: number of columns (%s) differs from number of values in row (%s)",
						columns.size(), dValues.size()));
			
			Map<String, String> values = new LinkedHashMap<>(dValues.size());
			
			Iterator<String> columnIt = columns.iterator(),
					valueIt = dValues.iterator();
			while (columnIt.hasNext())
				values.put(columnIt.next(), valueIt.next());
			if (withStatus)
				values.put(FIELD_STATUS, EventUtils.getStatus(d.isIdentical()).toString());
			
			rows.add(new MapRow(values));
		}
		
		Table table = new Table();
		table.setType(TableBuilder.TABLE_TYPE);
		table.setFields(rows);
		return table;
	}
	
	
	protected VerificationEntry createVerificationEntry(ResultDetail rd)
	{
		VerificationEntry entry = new VerificationEntry();
		entry.setExpected(rd.getExpected());
		entry.setActual(rd.getActual());
		entry.setStatus(getVerificationStatus(rd));
		entry.setHint(rd.getErrorMessage());
		return entry;
	}
	
	protected VerificationEntry createVerificationEntry(String expected, String actual, boolean success)
	{
		VerificationEntry entry = new VerificationEntry();
		entry.setExpected(expected);
		entry.setActual(actual);
		entry.setStatus(success ? VerificationStatus.PASSED : VerificationStatus.FAILED);
		return entry;
	}
	
	
	private VerificationStatus getVerificationStatus(ResultDetail rd)
	{
		if (rd.isInfo())
			return VerificationStatus.NA;
		
		return rd.isIdentical() ? VerificationStatus.PASSED : VerificationStatus.FAILED;
	}
	
	private TestExecutionHandlingException conversionError(Throwable e)
	{
		return new TestExecutionHandlingException("Could not convert action result", e);
	}
	
	
	private EventBatch.Builder storeEventInBatch(com.exactpro.th2.common.grpc.Event event, EventBatch.Builder batch, 
			com.exactpro.th2.common.grpc.Event batchContainer) throws TestExecutionHandlingException
	{
		if (batch == null)
			batch = EventBatch.newBuilder().setParentEventId(batchContainer.getId());
		batch.addEvents(event);
		if (batch.getEventsCount() >= maxBatchSize)
		{
			storeProto(batch.build());
			batch = null;
		}
		return batch;
	}
	
	private void storePlainDetails(Collection<DetailedResult> details, com.exactpro.th2.common.grpc.Event storedContainer) throws TestExecutionHandlingException
	{
		EventBatch.Builder rowsBatch = null;
		Instant start = EventUtils.getTimestamp(storedContainer.getId().getStartTimestamp()),
				end = EventUtils.getTimestamp(storedContainer.getEndTimestamp());
		for (DetailedResult dr : details)
		{
			try
			{
				com.exactpro.th2.common.grpc.Event rowEvent = ClearThEvent.fromTo(start, end)
						.name(dr.getComment())
						.type(TYPE_COMPARISON)
						.status(EventUtils.getStatus(dr.isSuccess()))
						.bodyData(createComparisonTable(dr.getResultDetails()))
						.toProto(storedContainer.getId());
				rowsBatch = storeEventInBatch(rowEvent, rowsBatch, storedContainer);
			}
			catch (Exception e)
			{
				throw new TestExecutionHandlingException("Error while processing comparison result", e);
			}
		}
		
		if (rowsBatch != null)
			storeProto(rowsBatch.build());
	}
	
	private void storeDetailsFromFile(CsvDetailedResult result, com.exactpro.th2.common.grpc.Event storedContainer) throws TestExecutionHandlingException
	{
		try (CsvDetailedResultReader reader = result.getReader())
		{
			EventBatch.Builder rowsBatch = null;
			Instant start = EventUtils.getTimestamp(storedContainer.getId().getStartTimestamp()),
					end = EventUtils.getTimestamp(storedContainer.getEndTimestamp());
			DetailedResult dr;
			while ((dr = reader.readNext()) != null)
			{
				com.exactpro.th2.common.grpc.Event rowEvent = ClearThEvent.fromTo(start, end)
						.name(dr.getComment())
						.type(TYPE_COMPARISON)
						.status(EventUtils.getStatus(dr.isSuccess()))
						.bodyData(createComparisonTable(dr.getResultDetails()))
						.toProto(storedContainer.getId());
				rowsBatch = storeEventInBatch(rowEvent, rowsBatch, storedContainer);
			}
			
			if (rowsBatch != null)
				storeProto(rowsBatch.build());
		}
		catch (Exception e)
		{
			throw new TestExecutionHandlingException("Error while processing CSV comparison result from file "+result.getReportFile(), e);
		}
	}
}