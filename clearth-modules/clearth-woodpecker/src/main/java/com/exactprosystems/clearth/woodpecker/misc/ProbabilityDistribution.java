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

package com.exactprosystems.clearth.woodpecker.misc;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.exactprosystems.clearth.utils.MathUtils.normalizeArray;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.fill;
import static java.util.Objects.requireNonNull;

public class ProbabilityDistribution<E>
{
	private static final int ATTEMPTS_COUNT = 5;
	
	private final List<E> elements;
	private IdentityHashMap<E, Integer> elementToIndexMap;
	
	private double[] probabilities;	
	private double[] cumulativeProbabilities;
	
	private long[] currentDistribution;
	private long invocationsCount;
	
	
	public ProbabilityDistribution(List<E> elements, double[] probabilities)
	{
		requireNonNull(elements, "elements");
		requireNonNull(probabilities, "probabilities");
		
		this.elements = elements;
		this.elementToIndexMap = createElementToIndexMap(elements);
		
		this.probabilities = normalizeArray(probabilities, 1.0);		
		calculateCumulativeProbabilities();
		
		this.currentDistribution = new long[probabilities.length];
	}
	
	
	public E nextElement()
	{
		return nextElement(1);
	}
	
	public E nextElement(int usagesCount)
	{
		if (elements.isEmpty())
			return null;
		if (elements.size() == 1)
			return elements.get(0);
		
		int index = nextMostSuitableIndex(usagesCount);		
		return elements.get(index);
	}
	
	private int nextMostSuitableIndex(int usagesCount)
	{
		int index = -1;
		if (invocationsCount == 0)
			index = nextIndex();
		else 
		{
			double minError = 1.0;
			for (int attempt = 0; attempt < ATTEMPTS_COUNT; attempt++)
			{
				int i = nextIndex();
				double error = calculateError(i);
				if ((attempt == 0) || (error < minError))
				{
					minError = error;
					index = i;
				}
			}
		}
		updateCurrentDistribution(index, usagesCount);
		return index;
	}
	
	private int nextIndex()
	{
		if (elements.isEmpty())
			return -1;

		double randomValue = ThreadLocalRandom.current().nextDouble();

		int index = binarySearch(cumulativeProbabilities, randomValue);
		
		if (index < 0)              // See javadoc to binarySearch 
			index = -index - 1;
		
		return min(index, elements.size() - 1);
	}
	
	private double calculateError(int newIndex)
	{
		double error = 0.0;
		long sum = invocationsCount + 1;
		for (int index = 0; index < currentDistribution.length; index++)
		{
			long count = currentDistribution[index] + ((newIndex == index) ? 1 : 0);
			double part = (double) count / sum;
			error += abs(probabilities[index] - part);
		}
		return error;
	}
	
	
	public void updateCurrentDistribution(E element, int usagesCount)
	{
		int index = indexOfElement(element);
		if (index != -1)
			updateCurrentDistribution(index, usagesCount);
	}
	
	private void updateCurrentDistribution(int index, int usagesCount)
	{
		if (usagesCount > 0)
		{
			currentDistribution[index] += usagesCount;
			invocationsCount += usagesCount;
		}
	}
	
	
	public Collection<E> getElements()
	{
		return elements;
	}
	
	
	public double getProbability(E element)
	{
		int index = elements.indexOf(element);
		return (index >= 0) ? probabilities[index] : 0;
	}
	
	
	public int size()
	{
		return  elements.size();
	}
	
	public boolean isEmpty()
	{
		return elements.isEmpty();
	}
	
	
	public void remove(E element)
	{
		int index = indexOfElement(element);
		if (index >= 0)
		{
			elements.remove(index);
			if (!elements.isEmpty())
			{
				elementToIndexMap = createElementToIndexMap(elements);
				removeProbability(index);
				calculateCumulativeProbabilities();
				currentDistribution = new long[elements.size()];
				invocationsCount = 0;
			}
		}
	}
	
	private void removeProbability(int index)
	{
		double[] newProbabilities = new double[probabilities.length - 1];
		
		if (index > 0)
			arraycopy(probabilities, 0, newProbabilities, 0, index);
		
		int lastIndex = probabilities.length - 1;
		if (index != lastIndex)
			arraycopy(probabilities, index + 1, newProbabilities, index, lastIndex - index);
		
		probabilities = normalizeArray(newProbabilities, 1.0);
	}
	
	
	public void reset()
	{
		resetCurrentDistribution();
	}
	
	
	private void calculateCumulativeProbabilities()
	{
		cumulativeProbabilities = new double[probabilities.length];
		double sum = 0.0;
		for (int i = 0; i < probabilities.length; i++)
		{
			sum += probabilities[i];
			cumulativeProbabilities[i] = sum;
		}
	}
	
	private IdentityHashMap<E, Integer> createElementToIndexMap(List<E> elements)
	{
		IdentityHashMap<E, Integer> map = new IdentityHashMap<>();
		for (int i = 0; i < elements.size(); i++)
		{
			map.put(elements.get(i), i);
		}
		return map;
	}
	
	
	private void resetCurrentDistribution()
	{
		if (invocationsCount > 0)
		{
			fill(currentDistribution, 0);
			invocationsCount = 0;
		}
	}
	
	
	private int indexOfElement(E element)
	{
		Integer index = elementToIndexMap.get(element);
		return (index != null) ? index : -1;
	}
}
