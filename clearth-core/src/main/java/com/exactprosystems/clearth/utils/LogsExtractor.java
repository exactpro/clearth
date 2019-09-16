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

package com.exactprosystems.clearth.utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogsExtractor
{
	private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
	private static final Pattern logFindPattern = Pattern.compile("^(DEBUG|INFO|WARN|TRACE|ERROR)\\s.*");
	private static final String PATTERN_START_STOP_STRING = "[A-Z]+\\s%s(:\\w*)?\\s%s,\\d{3}\\s.*";

	private String schedulerName;
	private Set<String> allowedThreadNames;
	private File logsDir;

	private File[] logFiles;

	public LogsExtractor(File logsDir, final String logName, String schedulerName, Set<String> allowedThreadNames)
	{
		this.logsDir = logsDir;
		this.schedulerName = schedulerName;
		this.allowedThreadNames = allowedThreadNames;

		logFiles = logsDir.listFiles(pathname -> pathname.getName().contains(logName));
		if (logFiles != null)
			Arrays.sort(logFiles, Collections.reverseOrder());
	}

	public File extractLogByRun(Date startTime, Date finishTime, String resultName) throws IOException
	{
		if (logFiles == null || logFiles.length == 0)
			return null;
		
		File outputDir = new File(logsDir, "output/");
		if (!outputDir.exists())
			outputDir.mkdir();

		File shortLog = new File(outputDir, resultName);
		
		boolean readToEnd = finishTime == null;
		String start = LOG_TIME_FORMAT.format(startTime);

		Pattern startPattern = Pattern.compile(String.format(PATTERN_START_STOP_STRING, schedulerName, start));
		Pattern endPattern = null;
		if (!readToEnd)
		{
			String end = LOG_TIME_FORMAT.format(finishTime);
			endPattern = Pattern.compile(String.format(PATTERN_START_STOP_STRING, schedulerName, end));
		}

		boolean complete;
		boolean previousMatch = false;
		try (OutputStream out = new FileOutputStream(shortLog))
		{
			boolean startFound = false;
			complete = false;
			boolean passMultilineRecords = true;

			for (File logFile : logFiles)
			{
				if (complete)
					break;
				try (BufferedReader in = new BufferedReader(new FileReader(logFile)))
				{
					String line;
					while ((line = in.readLine()) != null)
					{
						if (!startFound && matchLine(line, startPattern))
						{
							writeLine(out, line);
							startFound = true;
							continue;
						}

						if (startFound)
						{
							String extractedThread = extractThreadName(line);

							//if line is another one from some previous log record
							if (extractedThread == null)
							{
								if (passMultilineRecords)
								{
									writeLine(out, line);
								}
								continue;
							}

							//if this is log from different thread
							if (!allowedThreadNames.contains(extractedThread))
							{
								passMultilineRecords = false;
								continue;
							}

							passMultilineRecords = true;

							if (!readToEnd)
							{
								boolean currentMatch = matchLine(line, endPattern);
								if (previousMatch && !currentMatch)
								{
									complete = true;
									break;
								}
								previousMatch = currentMatch;
							}
							writeLine(out, line);
						}
					}
				}
			}
		}

		if (!readToEnd && !previousMatch)
			return null;

		return shortLog;
	}
	
	private void writeLine(OutputStream writer, String line) throws IOException
	{
		writer.write(line.getBytes());
		writer.write(Utils.EOL.getBytes());
	}

	private boolean matchLine(String line, Pattern pattern)
	{
		Matcher matcher = pattern.matcher(line);
		return matcher.matches();
	}



	/* NOTE This algorithm has little chance to don't work in some cases and
	   probably need little improvement(s) that makes it more "stronger".
	 */
	/**
	 * Extracts logging thread from log record
	 * @param logLine
	 * @return Name of logging thread. null if line has non-specific for log record prefix.
	 */
	private String extractThreadName(String logLine) {
		final int threadIndex = 1;
		final int firstPartSubThdName = 0;
		if (matchLine(logLine, logFindPattern))
			return logLine.split("\\s")[threadIndex].split(":")[firstPartSubThdName];

		return null;
	}
}
