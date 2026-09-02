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

package com.exactprosystems.clearth.woodpecker.daemons;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.woodpecker.Woodpecker;
import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Map;
import java.lang.reflect.Field;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.DAEMONS_DICTIONARY_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CommonDaemonStopTest
{
	private static final String CLEAR_TH_CORE_INSTANCE_NAME = "instance";
	
	@BeforeClass
	public void before() throws Exception
	{
		ClearThCore application = mock(ClearThCore.class, CALLS_REAL_METHODS);
		when(application.getRootRelative(anyString())).thenAnswer(i -> DAEMONS_DICTIONARY_FILE.equals(i.getArguments()[0]) ?
				resourceToAbsoluteFilePath("daemons.xml") : i.getArguments()[0]);
		setStaticField(ClearThCore.class, CLEAR_TH_CORE_INSTANCE_NAME, application);
		
		WoodpeckerObjectsFactory woodpeckerObjectsFactory = spy(WoodpeckerObjectsFactory.class);
		doReturn(null).when(woodpeckerObjectsFactory).createMessageEncoder();
		Woodpecker.init(woodpeckerObjectsFactory);
	}
	
	@Test
	public void checkTestingMock() throws Exception
	{
		WoodpeckerDaemonFactory daemonFactory = mock(MessageDaemonFactory.class);
		
		WoodpeckerDaemon daemon = new TestingMockDaemon(daemonFactory);
		daemon.start();
		Assert.assertEquals(daemon.getStatus(), DaemonStatus.RUNNING);
		daemon.stop();
		Assert.assertEquals(daemon.getStatus(), DaemonStatus.INACTIVE);
	}

	@Test
	public void testSingleStartAndStop() throws Exception
	{
		WoodpeckerDaemonFactory daemonFactory = mock(MessageDaemonFactory.class);
		
		Map<Integer, DaemonsPool> pools = Woodpecker.woodpecker().getDaemonPools();
		int index = 1;
		DaemonsPool firstPool = new DaemonsPool("aaa");
		firstPool.addDaemon("bbb", new TestingMockDaemon(daemonFactory));
		pools.put(index, firstPool);
		
		Woodpecker.woodpecker().startDaemon(index, "bbb");
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(index, "bbb"));
		Woodpecker.woodpecker().stopDaemon(index, "bbb");
		Assert.assertFalse(Woodpecker.woodpecker().isDaemonRunning(index, "bbb"));
	}

	@Test
	public void testMultiStartAndStop() throws Exception
	{
		WoodpeckerDaemonFactory daemonFactory = mock(MessageDaemonFactory.class);
		
		Map<Integer, DaemonsPool> pools = Woodpecker.woodpecker().getDaemonPools();
		int index = 1;
		DaemonsPool firstPool = new DaemonsPool("aaa");
		firstPool.addDaemon("bbb", new TestingMockDaemon(daemonFactory));
		firstPool.addDaemon("ccc", new TestingMockDaemon(daemonFactory));
		pools.put(index, firstPool);

		int secondIndex = 2;
		DaemonsPool secondPool = new DaemonsPool("ddd");
		secondPool.addDaemon("bbb", new TestingMockDaemon(daemonFactory));
		pools.put(secondIndex, secondPool);
		
		Woodpecker.woodpecker().startDaemon(index, "bbb");
		Woodpecker.woodpecker().startDaemon(index, "ccc");
		Woodpecker.woodpecker().startDaemon(secondIndex, "bbb");
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(index, "bbb"));
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(index, "ccc"));
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(secondIndex, "bbb"));

		Woodpecker.woodpecker().stopDaemon(index, "bbb");
		Assert.assertFalse(Woodpecker.woodpecker().isDaemonRunning(index, "bbb"));
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(index, "ccc"));
		Assert.assertTrue(Woodpecker.woodpecker().isDaemonRunning(secondIndex, "bbb"));

		Woodpecker.woodpecker().stopAllDaemons();
		Assert.assertFalse(Woodpecker.woodpecker().isDaemonRunning(index, "bbb"));
		Assert.assertFalse(Woodpecker.woodpecker().isDaemonRunning(index, "ccc"));
		Assert.assertFalse(Woodpecker.woodpecker().isDaemonRunning(secondIndex, "bbb"));
	}
	
	@AfterClass
	public void after() throws ReflectiveOperationException
	{
		setStaticField(ClearThCore.class, CLEAR_TH_CORE_INSTANCE_NAME, null);
	}
	
	
	private void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws ReflectiveOperationException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clazz, fieldValue);
	}
}
