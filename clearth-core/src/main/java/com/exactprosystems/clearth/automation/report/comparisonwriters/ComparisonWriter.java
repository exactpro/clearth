/*******************************************************************************
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

package com.exactprosystems.clearth.automation.report.comparisonwriters;

import com.exactprosystems.clearth.automation.report.Result;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Class that is needed to store result of comparison made by {@link com.exactprosystems.clearth.automation.actions.CompareDataSets}
 * @param <A> class that is made after comparing actual and expected rows
 */
public interface ComparisonWriter<A extends Result> extends AutoCloseable
{
	/**
	 * Adds detail for storing and stores it if necessary
	 * @param result detail made by CompareDataSets after comparing 2 rows
	 * @throws IOException if problem occurred while storing detail to file
	 */
	void addDetail(A result) throws IOException;

	/**
	 * Signals that all details were proceed and now report can be finished.
	 * @param targetDir directory where report should be found after finishing (if created)
	 * @param fileNamePrefix prefix for file report
	 * @param fileNameSuffix ending for file report (without extension)
	 * @param forceWrite write reportFile in any case   
	 * @return path of report file that was made; null if file was not made
	 * @throws IOException if problem occurred during finishing file
	 */
	Path finishReport(Path targetDir, String fileNamePrefix, String fileNameSuffix, boolean forceWrite) throws IOException;
}
