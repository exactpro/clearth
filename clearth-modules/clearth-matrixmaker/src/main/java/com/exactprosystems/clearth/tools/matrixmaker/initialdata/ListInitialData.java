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

package com.exactprosystems.clearth.tools.matrixmaker.initialdata;

import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;

import java.util.NoSuchElementException;

/**
 * Created by alexey.karpukhin on 12/18/17
 */
public class ListInitialData implements InitialData {
	
	private String formula;
	private String[] dataArray;
	private int dataLength;
	private int currentIndex;

	public ListInitialData(String formula, String name, String[] dataArray) {
		this.formula = formula;
		this.dataArray = dataArray;
		this.currentIndex = -1;
		this.dataLength = dataArray.length;
	}

	@Override
	public String getField(String[] params) throws GenerateException {
		if (currentIndex < 0 || currentIndex >= dataLength)
			throw new GenerateException("Incorrect index: " + currentIndex + ". Available elements: " + (this.dataLength - 1));
		return this.dataArray[currentIndex];
	}

	@Override
	public void next() {
		if (!this.hasNext())
			throw new NoSuchElementException();
		currentIndex ++;
	}

	@Override
	public boolean hasNext() {
		return currentIndex + 1 < dataLength;
	}

	@Override
	public String getFormula() {
		return this.formula;
	}
}
