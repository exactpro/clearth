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

package com.exactprosystems.clearth.connectivity.xml;

import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.Pair;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.filefilter.FileFilterUtils.prefixFileFilter;
import static org.testng.AssertJUnit.assertEquals;

/**
 *         22 December 2016
 */
public class XmlDictionaryTest
{
	private static final String DICTIONARY = "dicts/dictionary.xml";
	
	private XmlDictionary dictionary;
	private Marshaller marshaller;
	
	@BeforeClass
	public void prepare() throws DictionaryLoadException, JAXBException, FileNotFoundException {
		File file = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource(DICTIONARY));

		if (file == null)
			throw new FileNotFoundException(DICTIONARY + " file not found");
		
		
		dictionary = new XmlDictionary(file.getAbsolutePath());
		marshaller = JAXBContext.newInstance(XmlDictionaryDesc.class).createMarshaller();
		marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
	}
	
	@Test(dataProvider = "provider")
	public void testCommonFieldsResolution(XmlDictionaryDesc sourceDesc, String expectedXml) throws DictionaryLoadException, JAXBException
	{
		dictionary.solveReferencesToCommonFields(sourceDesc);
		String actualXml = toXml(sourceDesc);
		assertEquals(expectedXml, actualXml);
	}
	
	private String toXml(XmlDictionaryDesc dictionaryDesc) throws JAXBException
	{
		StringWriter sw = new StringWriter();
		marshaller.marshal(dictionaryDesc, sw);
		return sw.toString();
	}
	
	@DataProvider(name = "provider")
	public Iterator<Object[]> createData() throws IOException, JAXBException
	{
		return new ParametersIterator();
	}	
	
	private static class ParametersIterator implements Iterator<Object[]>
	{
		private static final String BASE_DIR = "commonFieldsTest";
		
		private final List<Pair<XmlDictionaryDesc, String>> parameters;
		private int index;

		public ParametersIterator() throws IOException, JAXBException
		{
			File file = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource(BASE_DIR));

			if (file == null)
				throw new FileNotFoundException(BASE_DIR + " file not found");
			
			parameters = loadParameters(file);
		}

		private List<Pair<XmlDictionaryDesc, String>> loadParameters(File baseDir) throws IOException, JAXBException
		{
			List<Pair<XmlDictionaryDesc, String>> parameters = new ArrayList<>();
			Iterator iterator = iterateFiles(baseDir, prefixFileFilter("src"), null);
			while (iterator.hasNext())
			{
				File srcFile = (File) iterator.next();
				XmlDictionaryDesc srcDesc = fromXml(srcFile);
				File expFile = expFile(srcFile);
				String expXml = readFileToString(expFile, Charset.forName("UTF-8"));
				parameters.add(new Pair<>(srcDesc, expXml));
			}
			return parameters;
		}
		
		private XmlDictionaryDesc fromXml(File file) throws JAXBException
		{
			Unmarshaller u = JAXBContext.newInstance(XmlDictionaryDesc.class).createUnmarshaller();
			return u.unmarshal(new StreamSource(file), XmlDictionaryDesc.class).getValue();
		}
		
		private File expFile(File srcFile)
		{
			String srcName = srcFile.getName();
			String expName = srcName.replace("src", "exp");
			return new File(srcFile.getParent(), expName);
		}
		
		@Override
		public boolean hasNext()
		{
			return index < parameters.size();
		}

		@Override
		public Object[] next()
		{
			Pair<XmlDictionaryDesc, String> pair = parameters.get(index++);
			return new Object[] { pair.getFirst(), pair.getSecond() };
		}

		@Override
		public void remove() { }
	}
}
