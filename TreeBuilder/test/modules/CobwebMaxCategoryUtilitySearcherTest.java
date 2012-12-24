package modules;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import clusterer.ENodeType;
import clusterer.IAttribute;
import clusterer.IMaxCategoryUtilitySearcher;
import clusterer.INode;

public class CobwebMaxCategoryUtilitySearcherTest {

	@Test
	public void testGetMaxCategoryUtilityMergeSetOfINode() {
		
		// create attributes
		// interval of ratings: [1, 5]
		// ratings are integers
		Map<Integer, Double> attMap = new HashMap<Integer, Double>();
		attMap.put(4, 1.0);
		IAttribute attribute_1A = new Attribute(attMap);
		attMap = new HashMap<Integer, Double>();
		attMap.put(3, 1.0);
		IAttribute attribute_1B = new Attribute(attMap);
		attMap = new HashMap<Integer, Double>();
		attMap.put(5, 1.0);
		IAttribute attribute_2B = new Attribute(attMap);
		attMap = new HashMap<Integer, Double>();
		attMap.put(5, 1.0);
		IAttribute attribute_2C = new Attribute(attMap);
		
		// we want to calc the category utility of node 1 merged with node 2
		// attribute map of node 1
		Map<INode, IAttribute> attMap1 = new HashMap<INode, IAttribute>();
		
		// this node is an attribute of node 1 and node 2
		INode sharedAttribute = new Node(ENodeType.Content, null, null);
		
		// add the corresponding attributes to the attribute map of node 1
		attMap1.put(new Node(ENodeType.Content, null, null), attribute_1A);
		attMap1.put(sharedAttribute, attribute_1B);
		
		// create node 1
		INode node1 = new Node(ENodeType.User, null, null);
		node1.setAttributes(attMap1);
		
		// attribute map of node 2
		Map<INode, IAttribute> attMap2 = new HashMap<INode, IAttribute>();
		
		// add the corresponding attributes to the attribute map of node 2
		attMap2.put(sharedAttribute, attribute_2B);
		attMap2.put(new Node(ENodeType.Content, null, null), attribute_2C);
		
		// create node 2
		INode node2 = new Node(ENodeType.User, null, null);
		node2.setAttributes(attMap2);
		
		// create the utility calcultaor
		IMaxCategoryUtilitySearcher utilityCalc = new CobwebMaxCategoryUtilitySearcher();
		
		// add the two created user nodes to a set (set of open nodes)
		Set<INode> openNodes = new HashSet<INode>();
		openNodes.add(node1);
		openNodes.add(node2);
		
		// get the utility of the node resulting of a merge of node 1 and node 2
		double calcCatUt = utilityCalc.getMaxCategoryUtilityMerge(openNodes).getCategoryUtility();
		
		// evaluate the category utility result
		assertEquals("category utility", 1.0/3.0, calcCatUt, 0.000001);

	}

}