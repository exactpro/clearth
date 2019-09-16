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

package com.exactprosystems.clearth.web.beans;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.functions.MethodData;
import com.exactprosystems.clearth.automation.functions.MvelExpressionHelper;

public class MvelHelperBean extends ClearThBean
{
	private int activeTab = 0;
	private List<MethodData> methodsList;
	private TreeNode methodsTree;
	private TreeNode selectedMethod;
	private boolean initialized;
	
	private MethodData selectedMethodData;

	public MvelHelperBean()
	{
		init();
	}
	
	public MethodData getSelectedMethodData()
	{
		if(selectedMethod != null)
		{
			return (MethodData) selectedMethod.getData();
		} 
		else
		{
			if(initialized)
			{
				return (MethodData) methodsTree.getChildren().get(0).getChildren().get(0).getData();
			}
		}
		return null;
	}

	public void init()
	{
		methodsList = getMethods(ClearThCore.getInstance().getMatrixFunctionsClass());
		methodsTree = getTree();
		initialized = true;
	}
	
	public List<MethodData> getMethods(Class c)
	{
		MvelExpressionHelper helper = new MvelExpressionHelper(c);
		return helper.getMethodsList();
	}

	public TreeNode getTree()
	{
		TreeNode root = new DefaultTreeNode("root", null);

		Set<String> groups = new HashSet<String>();

		for (MethodData m : methodsList)
		{
			groups.add(m.group);
		}

		for (String group : groups)
		{
			TreeNode g = new DefaultTreeNode("group", group, root);
			g.setSelectable(false);
		}

		for (MethodData method : methodsList)
		{
			for (TreeNode node : root.getChildren())
			{
				if(node.getData().equals(method.group))
					new DefaultTreeNode("method", method, node);
			}
		}

		return root;
	}

	public void onNodeSelect(NodeSelectEvent event)
	{
		selectedMethod = event.getTreeNode();
	}

	public int getActiveTab()
	{
		return activeTab;
	}

	public void setActiveTab(int activeTab)
	{
		this.activeTab = activeTab;
	}

	public List<MethodData> getMethodsList()
	{
		return methodsList;
	}

	public void setMethodsList(List<MethodData> methodsList)
	{
		this.methodsList = methodsList;
	}

	public TreeNode getMethodsTree()
	{
		return methodsTree;
	}

	public void setMethodsTree(TreeNode methodsTree)
	{
		this.methodsTree = methodsTree;
	}

	public TreeNode getSelectedMethod()
	{
		return selectedMethod;
	}

	public void setSelectedMethod(TreeNode selectedMethod)
	{
		this.selectedMethod = selectedMethod;
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public void setInitialized(boolean initialized)
	{
		this.initialized = initialized;
	}

	public void setSelectedMethodData(MethodData selectedMethodData)
	{
		this.selectedMethodData = selectedMethodData;
	}
}
