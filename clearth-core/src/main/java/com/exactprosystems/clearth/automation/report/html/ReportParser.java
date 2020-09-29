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

package com.exactprosystems.clearth.automation.report.html;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.utils.TagUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;

import javax.xml.stream.*;

import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.automation.ActionGenerator.HEADER_DELIMITER;

public class ReportParser
{
	protected final static String ATTR_CLASS = "class";
	protected final static String FLAG_INVERTED = "INVERTED TO";
	protected final static String LABEL_COMMENT = "Comment:";
	protected final static String LABEL_TIMEOUT = "Timeout:";
	protected final static String LABEL_SUBACTIONS = "Sub-actions";
	protected final static String LABEL_EMPTY = "None";
	protected final static String LABEL_TITLE = "<title>";
	protected final static String LABEL_BASIC_PARAMS = "Basic parameters";
	protected final static String LABEL_OUTPUT_PARAMS = "Output parameters";
	protected final static String CLASS_DESC = "desc";
	protected final static String CLASS_NODE = "node";
	protected final static String CLASS_STEP = "step";
	protected final static String CLASS_PARAMS = "params";
	protected final static String CLASS_ACTION = "action";
	protected final static String CLASS_ACTION_DATA = "action_data";
	protected final static String CLASS_STATUS = "status";
	protected final static String CLASS_SUBACTION = "subaction";
	protected final static String CLASS_TABLEHEAD = "tablehead";
	protected final static String CLASS_CONTAINER = "container";

	protected final static String TAG_TR = "tr";
	protected final static String TAG_TD = "td";
	protected final static String TAG_DIV = "div";
	protected final static String TAG_SPAN = "span";
	protected final static String TAG_TABLE = "table";
	protected static final String TAG_TBODY = "tbody";
	protected final static String TAG_TITLE = "title";

	protected final static String HTML_BR = "<br>";
	protected final static String XHTML_BR = "<br/>";

	protected final static Pattern TITLE_PATTERN = Pattern.compile("(.+)\\s\\d{4}\\.\\d{2}\\.\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s{2}\\(.+\\)");
	protected final static Pattern NAME_STATUS_PATTERN = Pattern.compile("(.+)\\s-\\s(.+)\\s\\(.+\\)");
	protected final static Pattern SUBACTION_NAME_PATTERN = Pattern.compile("(\\w+)\\s-\\s(\\w+)\\s-\\s(\\w+)");

	protected static final Map<String,String> ACTIONS_DESC_PARAMS_MAP = new HashMap<String, String>()
	{{
		put("Comment", "comment");
		put("Timeout", "timeout");
	}};

	protected XMLInputFactory factory;
	protected LinkedList<ActionDesc> actions = new LinkedList<ActionDesc>();
	protected String matrixName;

	public File writeMatrix(File reportFile, String outputDir) throws IOException
	{
		factory = XMLInputFactory.newInstance();
		try
		{
			parse(reportFile);
		}
		catch (XMLStreamException e)
		{
			throw new IOException("An error occurred while parsing report file", e);
		}
		File matrix = null;
		if (!StringUtils.isEmpty(matrixName))
			matrix = new File(outputDir, matrixName);
		else
			matrix = File.createTempFile("generated_matrix_", ".csv", new File(outputDir));

		CsvWriter writer = null;
		try
		{
			writer = new CsvWriter(matrix.getAbsolutePath());
			Set<String> currentHeader = null;
			for (ActionDesc actionDesc : actions)
			{
				Map<String, String> params = actionDesc.inputParams;
				Set<String> header = params.keySet();
				if (currentHeader == null || !currentHeader.equals(header))
				{
					for (String paramHeader : params.keySet())
						writer.write(HEADER_DELIMITER + paramHeader);
					writer.endRecord();
					currentHeader = header;
				}
				for (String paramHeader : params.keySet())
					writer.write(params.get(paramHeader));
				writer.endRecord();
				currentHeader = params.keySet();
			}
		}
		finally
		{
			Utils.closeResource(writer);
		}
		
		actions = new LinkedList<ActionDesc>();
		matrixName = null;
		factory = null;
		return matrix;
	}

	protected void parse(File reportFile) throws IOException, XMLStreamException
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(reportFile)))
		{
			String line;
			String currentStep = "";
			String classTmplt = "class=\"%s\"";
			String stepClass = String.format(classTmplt, CLASS_STEP);
			String actionClass = String.format(classTmplt, CLASS_ACTION);
			String actionDataClass = String.format(classTmplt, CLASS_ACTION_DATA);
			String tbodyOpen = TagUtils.openTag("tbody", null);
			while ((line = reader.readLine()) != null)
			{
				if (StringUtils.contains(line, LABEL_TITLE))
				{
					String titleValue = TagUtils.getPureTagValue(line);
					Matcher matcher = TITLE_PATTERN.matcher(titleValue);
					if (matcher.find())
						matrixName = matcher.group(1);
				}
				else if (StringUtils.contains(line, stepClass))
				{
					line = reader.readLine();
					String stepTitle = TagUtils.getPureTagValue(line);
					Matcher matcher = NAME_STATUS_PATTERN.matcher(stepTitle);
					if (matcher.find())
						currentStep = matcher.group(1);
				}
				else if (StringUtils.contains(line, actionClass))
				{
					StringBuilder actionBlockBuilder = new StringBuilder(line);
					line = reader.readLine();
					actionBlockBuilder.append(line);

					String actionBlock = readAction(reader, actionBlockBuilder);
					parseAction(actionBlock, currentStep);
				}
			}
		}

	}

	protected String readAction(BufferedReader reader, StringBuilder builder) throws IOException
	{
		String line = null;
		String endFlag = "<td class=\"comps\">";	//comparison block may contain large data, skip it too
		while ((line = reader.readLine()) != null)
		{
			if (StringUtils.contains(line, endFlag))
			{
				if (builder.toString().contains(TAG_TBODY))
					builder.append("</tr></tbody></table></div></div>");
				else
					builder.append("</tr></table></div></div>");
				break;
			}
			if (StringUtils.contains(line, HTML_BR))
				line = StringUtils.replace(line, HTML_BR, XHTML_BR);
			builder.append(Utils.EOL).append(line);
		}
		return builder.toString();
	}

	protected void parseAction(String actionBlock, String stepName) throws XMLStreamException
	{
		char[] actWithEsc = actionBlock.toCharArray();
		char[] actWithoutEsc = new char[actWithEsc.length];
		for (int i = 0; i < actWithEsc.length; i++) {
			if ((actWithEsc[i] >= '\u0000' && actWithEsc[i] <= '\u0007')
					|| (actWithEsc[i] >= '\u000e' && actWithEsc[i] <= '\u001f')
					|| (actWithEsc[i] >= '\u007f' && actWithEsc[i] <= '\u00a0'))
				actWithoutEsc[i] = ' ';
			else
				actWithoutEsc[i] = actWithEsc[i];
		}
		actionBlock = new String(actWithoutEsc);
		StringReader strReader = null;
		XMLEventReader reader = null;
		try
		{
			strReader = new StringReader(actionBlock);
			reader = factory.createXMLEventReader(strReader);

			LinkedList<ActionDesc> subList = new LinkedList<ActionDesc>();
			findNextTag(reader, TAG_SPAN);
			String actionTitle = prepareTagValue(reader.getElementText());
			boolean inverted = actionTitle.contains(FLAG_INVERTED);
			Matcher matcher = NAME_STATUS_PATTERN.matcher(actionTitle);
			if (!matcher.find())
				return;
			String actionId = matcher.group(1);
			String actionName = matcher.group(2);
			ActionDesc actionDesc = new ActionDesc(actionId, actionName, stepName);
			subList.add(actionDesc);
			nextTag(reader);	// skip action container tag
	
			parseDescParams(reader, actionDesc);
			if (inverted)
				actionDesc.addParam(ActionGenerator.COLUMN_INVERT, "true");
			findNextTag(reader, TAG_TD, ATTR_CLASS, CLASS_PARAMS);
			nextTag(reader);	// skip 'Input parameters' label
			StartElement nextTag = nextTag(reader);
			if (StringUtils.equals(getTagName(nextTag), TAG_TABLE))
			{
				parseParamsTable(reader, actionDesc.inputParams);
			}
			nextTag = nextTag(reader);
			/* basic params formulas present */
			if (StringUtils.equals(getTagName(nextTag), TAG_SPAN) && isNextCharacters(reader)
					&& StringUtils.equals(LABEL_BASIC_PARAMS, reader.getElementText()))
			{
				findNextTag(reader, TAG_TABLE);
				parseParamsTable(reader, actionDesc.inputParams, true);
			}
			findNextTag(reader, TAG_DIV, LABEL_SUBACTIONS);
			parseSubActions(reader, subList, stepName);
	
			actions.addAll(subList);
		} finally {
			Utils.closeResource(strReader);
			if (reader != null)
				reader.close();
		}
	}

	protected void parseDescParams(XMLEventReader reader, ActionDesc actionDesc) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			StartElement event = nextTag(reader);
			if (StringUtils.equals(event.getName().getLocalPart(), TAG_DIV) && checkAttribute(event, ATTR_CLASS, CLASS_STATUS))
				return;
			if (checkAttribute(event, ATTR_CLASS, CLASS_DESC))
				parseDescParam(reader, actionDesc);
			else
				return;
		}
	}

	protected void parseDescParam(XMLEventReader reader, ActionDesc actionDesc) throws XMLStreamException
	{
		nextTag(reader);
		String descLabel = prepareTagValue(reader.getElementText());
		descLabel = descLabel.replace(":", "");
		String descName = getActionDescParamsMap().get(descLabel);
		String descValue = nextCharacters(reader).getData();
		if (descName != null)
			actionDesc.addParam(descName, descValue);
	}

	protected String prepareTagValue(String tagContent)
	{
		if (!StringUtils.isEmpty(tagContent))
		{
			tagContent = tagContent.replaceAll("\n", "");
			String whitespace = " ";
			while (StringUtils.startsWith(tagContent, whitespace))
				tagContent = StringUtils.removeStart(tagContent, whitespace);
			while (StringUtils.endsWith(tagContent, whitespace))
				tagContent = StringUtils.removeEnd(tagContent, whitespace);
		}
		return tagContent;
	}

	/**
	 @return map which contains pairs of action desc params (like comment, timeout and other) and their labels in report
	 */
	protected Map<String,String> getActionDescParamsMap()
	{
		return ACTIONS_DESC_PARAMS_MAP;
	}

	protected void parseSubActions(XMLEventReader reader, LinkedList<ActionDesc> container, String stepName) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement() && ((StartElement)event).getName().getLocalPart().equals(TAG_DIV)
					&& isNextCharacters(reader) && StringUtils.equals(prepareTagValue(reader.getElementText()), LABEL_EMPTY))
				return; /* sub-actions are absent */
			else if (checkAttribute(event, ATTR_CLASS, CLASS_NODE) &&  checkTagContent(reader, LABEL_OUTPUT_PARAMS))
				return; /* sub-actions block processed completly */
			else if (event.isStartElement() && checkAttribute(event, ATTR_CLASS, CLASS_SUBACTION))
			{
				findNextTag(reader, TAG_SPAN);
				String subActionLabel = prepareTagValue(reader.getElementText());
				subActionLabel = StringUtils.replace(subActionLabel, "\n", "");
				Matcher m = SUBACTION_NAME_PATTERN.matcher(subActionLabel);
				if (m.find())
				{
					String subId = m.group(1);
					String subName = m.group(2);
					ActionDesc subAction = new ActionDesc(subId, subName, stepName);
					container.addFirst(subAction);	// add subaction before main action to prepare right actions order before writing to result file

					findNextTag(reader, TAG_TABLE);
					findNextTag(reader, TAG_TABLE);
					parseParamsTable(reader, subAction.inputParams);
				}
			}
		}
	}

	protected void parseParamsTable(XMLEventReader reader, Map<String, String> container) throws XMLStreamException
	{
		parseParamsTable(reader, container, false);
	}

	protected void parseParamsTable(XMLEventReader reader, Map<String, String> container, boolean lowHeader) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement() && StringUtils.equals(TAG_TR, getTagName(event)) && !checkAttribute(event, ATTR_CLASS, CLASS_TABLEHEAD))
			{
				String name = getNextTagValue(reader);
				String value = null;
				nextTag(reader);
				try
				{
					value = prepareTagValue(reader.getElementText());
				}
				catch (XMLStreamException e) // If value contains formula or node wrapper
				{
					value = getFormulaSource(reader, getTagName(event));
				}
				if (lowHeader)
					name = name.toLowerCase();
				container.put(name, value);
			}
			else if (event.isEndElement() && TAG_TABLE.equals(getTagName(event)))
				return;
		}
	}

	protected StartElement nextTag(XMLEventReader reader) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement())
				return (StartElement) event;
		}
		return null;
	}

	protected boolean isNextCharacters(XMLEventReader reader) throws XMLStreamException
	{
		XMLEvent event = reader.peek();
		return event.isCharacters() && !StringUtils.isEmpty(((Characters)event).getData().trim());
	}

	protected Characters nextCharacters(XMLEventReader reader) throws XMLStreamException
	{
		boolean tagOpen = false;
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement())
				tagOpen = true;
			if (event.isCharacters())
			{
				if (!tagOpen)
					return (Characters) event;
			}
			else if (event.isEndElement() && tagOpen)
				tagOpen = false;
		}
		return  null;
	}

	protected String getNextTagValue(XMLEventReader reader) throws XMLStreamException
	{
		boolean tagOpen = false;
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement() && !tagOpen)
				tagOpen = true;
			else if (event.isCharacters() && tagOpen)
				return ((Characters) event).getData();
		}
		return null;
	}

	protected String getFormulaSource(XMLEventReader reader, String currentTag) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement() && (checkAttribute(event, ATTR_CLASS, CLASS_CONTAINER) || checkAttribute(event, ATTR_CLASS, CLASS_NODE)))
			{
				return prepareTagValue(reader.getElementText());
			}
			else if (event.isEndElement() && StringUtils.equals(currentTag, getTagName(event)))
				break;
		}
		return null;
	}

	protected StartElement findNextTag(XMLEventReader reader, String tagName, String attrName, String attrValue) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			StartElement tag = findNextTag(reader, tagName);
			if (tag == null)
				return null;
			if (checkAttribute(tag, attrName, attrValue))
				return tag;
		}
		return null;
	}

	protected StartElement findNextTag(XMLEventReader reader, String tagName, String elementText) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			StartElement tag = findNextTag(reader, tagName);
			if (tag == null)
				return null;
			if (reader.peek().isCharacters() && StringUtils.equals(prepareTagValue(reader.getElementText()), elementText))
				return tag;
		}
		return null;
	}

	protected StartElement findNextTag(XMLEventReader reader, String tagName) throws XMLStreamException
	{
		while (reader.hasNext())
		{
			XMLEvent event = reader.nextEvent();
			if (!event.isStartElement())
				continue;
			if (StringUtils.equals(tagName, getTagName(event)))
				return (StartElement) event;
		}
		return null;
	}

	protected boolean checkTagContent(XMLEventReader reader, String tagContent) throws XMLStreamException
	{
		return isNextCharacters(reader) && StringUtils.equals(prepareTagValue(reader.getElementText()), tagContent);
	}

	protected boolean checkAttribute(XMLEvent event, String attrName, String attrValue)
	{
		if (!event.isStartElement())
			return false;
		Map<String,String> attrMap = getAttributesMap(event);
		return attrMap != null && StringUtils.equals(attrMap.get(attrName), attrValue);
	}

	protected String getAttributeValue(XMLEvent event, String name)
	{
		Map<String,String> attrMap = getAttributesMap(event);
		return attrMap != null ? attrMap.get(name) : null;
	}

	protected Map<String,String> getAttributesMap(XMLEvent event)
	{
		if (!event.isStartElement())
			return null;
		StartElement tag = (StartElement) event;
		Map<String, String> result = new HashMap<String, String>();
		Iterator<Attribute> iter = tag.getAttributes();
		while (iter.hasNext())
		{
			Attribute attr = iter.next();
			result.put(attr.getName().getLocalPart(), attr.getValue());
		}
		return result;
	}

	protected static String getTagName(XMLEvent event)
	{
		if (event.isStartElement())
			return ((StartElement) event).getName().getLocalPart();
		else if (event.isEndElement())
			return ((EndElement) event).getName().getLocalPart();
		else
			return null;
	}

	public class ActionDesc
	{
		public LinkedHashMap<String,String> inputParams;

		public ActionDesc(String id, String name, String stepName)
		{
			inputParams = new LinkedHashMap<String, String>();
			inputParams.put(ActionGenerator.COLUMN_ID, id);
			inputParams.put(ActionGenerator.COLUMN_GLOBALSTEP, stepName);
			inputParams.put(ActionGenerator.COLUMN_ACTION, name);
			inputParams.put(ActionGenerator.COLUMN_EXECUTE, "y");
			inputParams.put(ActionGenerator.COLUMN_TIMEOUT, "0");
		}

		public void addParam(String name, String value)
		{
			inputParams.put(name, value);
		}
	}

	public LinkedList<ActionDesc> getActions()
	{
		return actions;
	}

	public void setActions(LinkedList<ActionDesc> actions)
	{
		this.actions = actions;
	}
}
