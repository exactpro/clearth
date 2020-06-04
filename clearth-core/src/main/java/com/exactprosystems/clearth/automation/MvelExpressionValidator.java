/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.utils.ClearThException;
import org.mvel2.Operator;
import org.mvel2.ast.ASTNode;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExecutableAccessor;

import java.io.Serializable;


public class MvelExpressionValidator
{
	public void validateExpression(Serializable expression) throws ClearThException
	{
		ASTNode node = null;
		if (expression instanceof CompiledExpression)
		{
			CompiledExpression castedExpression = (CompiledExpression) expression;
			node = castedExpression.getFirstNode();
		}
		else if (expression instanceof ExecutableAccessor)
		{
			ExecutableAccessor castedExpression = (ExecutableAccessor) expression;
			node = castedExpression.getNode();
		}

		if (node != null && node.nextASTNode != null)
		{
			int operator = node.nextASTNode.getOperator();

			if (operator == Operator.TERNARY)
			{
				return;
			}
			throw new ClearThException("Invalid expression: '" + String.valueOf(node.getExpr()) + "'. Possibly it was" +
					" received from another reference.");
		}

	}
}
