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

package com.exactprosystems.clearth.web.beans;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.exactprosystems.clearth.automation.functions.SpecialData;

import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.functions.MethodData;
import com.exactprosystems.clearth.automation.functions.MvelExpressionHelper;

public class MvelHelperBean extends ClearThBean
{
	private static final String VALUES = "Values",
								FUNCTIONS = "Functions";
	
	private int activeTab = 0;
	
	private List<MethodData> methodsList;
	private List<SpecialData> specialDataList;
	
	private TreeNode<Object> methodsTreeRoot;
	private TreeNode<Object> specialDataTreeRoot;
	private TreeNode<Object> selectedMethodNode;
	private TreeNode<Object> selectedSpecialDataNode;
	
	private boolean initialized;
	
	public MvelHelperBean()
	{
		init();
	}
	
	protected Object getSelectedNodeData(TreeNode<Object> selected, TreeNode<Object> root)
	{
		if (selected != null)
			return selected.getData();
		return initialized ? root.getChildren().get(0).getChildren().get(0).getData() : null;
	}
	
	public MethodData getSelectedMethodData()
	{
		return (MethodData) getSelectedNodeData(selectedMethodNode, methodsTreeRoot);
	}
	
	public SpecialData getSelectedSpecialData()
	{
		return (SpecialData) getSelectedNodeData(selectedSpecialDataNode, specialDataTreeRoot);
	}
	
	public void init()
	{
		methodsList = getMethods(ClearThCore.getInstance().getMatrixFunctionsClass());
		
		initializeSpecialData();
		
		buildMethodsDataTree();
		buildSpecialDataTree();
		
		initialized = true;
	}
	
	private void initializeSpecialData()
	{
		Class c = ClearThCore.getInstance().getComparisonUtils().getClass();
		MvelExpressionHelper helper = createMvelExpressionHelper(c);
		specialDataList = helper.getSpecialDataList();
	}
	
	private MvelExpressionHelper createMvelExpressionHelper(Class c)
	{
		return new MvelExpressionHelper(c);
	}
	
	public List<MethodData> getMethods(Class c)
	{
		return createMvelExpressionHelper(c).getMethodsList();
	}
	
	public void buildMethodsDataTree()
	{
		methodsTreeRoot = new DefaultTreeNode<>("root", null);

		Set<String> groups = methodsList
								.stream()
								.map(met -> met.group)
								.collect(Collectors.toSet());
		
		for (String group : groups)
			createGroupTreeNode(group, methodsTreeRoot);
		
		for (MethodData method : methodsList)
		{
			for (TreeNode<Object> node : methodsTreeRoot.getChildren())
			{
				if(node.getData().equals(method.group))
					new DefaultTreeNode<>("method", method, node);
			}
		}
	}
	
	public void buildSpecialDataTree()
	{
		specialDataTreeRoot = new DefaultTreeNode<>("root", null);
		
		TreeNode<Object> valueGroupRoot = createGroupTreeNode(VALUES, specialDataTreeRoot);
		TreeNode<Object> functionGroupRoot = createGroupTreeNode(FUNCTIONS, specialDataTreeRoot);
		
		for (SpecialData data : specialDataList)
		{
			switch (data.getType())
			{
				case VALUE: new DefaultTreeNode<>(VALUES, data, valueGroupRoot); break;
				case FUNCTION: new DefaultTreeNode<>(FUNCTIONS, data, functionGroupRoot); break;
			}
		}
	}
	
	private TreeNode<Object> createGroupTreeNode(String group, TreeNode<Object> root)
	{
		TreeNode<Object> node = new DefaultTreeNode<>("group", group, root);
		node.setSelectable(false);
		return node;
	}

	public void onMethodNodeSelect(NodeSelectEvent event)
	{
		selectedMethodNode = event.getTreeNode();
	}
	
	public void onSpecialDataNodeSelect(NodeSelectEvent event)
	{
		selectedSpecialDataNode = event.getTreeNode();
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
	
	public List<SpecialData> getSpecialDataList()
	{
		return specialDataList;
	}
	
	public TreeNode<Object> getMethodsTreeRoot()
	{
		return methodsTreeRoot;
	}
	
	public TreeNode<Object> getSpecialDataTreeRoot()
	{
		return specialDataTreeRoot;
	}
	
	public TreeNode getSelectedMethodNode()
	{
		return selectedMethodNode;
	}

	public void setSelectedMethodNode(TreeNode selectedMethodNode)
	{
		this.selectedMethodNode = selectedMethodNode;
	}
	
	public TreeNode<Object> getSelectedSpecialDataNode()
	{
		return selectedSpecialDataNode;
	}
	
	public void setSelectedSpecialDataNode(TreeNode<Object> selectedSpecialDataNode)
	{
		this.selectedSpecialDataNode = selectedSpecialDataNode;
	}
}
