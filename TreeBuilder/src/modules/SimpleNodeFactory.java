package modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import clusterer.AttributeFactory;
import clusterer.ENodeType;
import clusterer.IAttribute;
import clusterer.INode;
import clusterer.INodeDistanceCalculator;
import clusterer.NodeFactory;

public class SimpleNodeFactory extends NodeFactory {

	private static NodeFactory factory = new SimpleNodeFactory();
	
	private SimpleNodeFactory() {
		// singleton
	}
	
	public static NodeFactory getInstance() {
		return factory;
	}

	@Override
	public INode createLeafNode(ENodeType typeOfNewNode,
			INodeDistanceCalculator nodeDistanceCalculator) {
			
		return new SimpleNode(typeOfNewNode, nodeDistanceCalculator);
	}


	
	/**
	 * Creates a new node and initializes its attribute map.
	 * The new node is added to the cluster tree as new
	 * root with all nodes in {@code nodesToMerge} as children.
	 * <br>
	 * <br>
	 * <b>This implementations make the following assumptions: 
	 * nodesToMerge.size() == 2 && nodesToMerge != null<b>
	 */
	@Override
	public INode createInternalNode(ENodeType typeOfNewNode,
			List<INode> nodesToMerge,
			INodeDistanceCalculator nodeDistanceCalculator,
			AttributeFactory attributeFactory) {
				
		if (nodesToMerge == null || nodesToMerge.size() != 2) {
			System.err.println("Err: Merge attempt with nodesToMerge == null" +
					" or number of nodes != 2; in: " +getClass().getSimpleName());
			System.exit(-1);
		}
		INode n1 = nodesToMerge.get(0);
		INode n2 = nodesToMerge.get(1);
		
		Set<INode> n1Keys = n1.getAttributeKeys();
		Set<INode> union = new HashSet<INode>(n1Keys);
		Set<INode> n2Keys = n2.getAttributeKeys();
		union.addAll(n2.getAttributeKeys());

		Map<INode, IAttribute> attMap = new HashMap<INode, IAttribute>();
		for (INode node : union) {
			List<IAttribute> attArr = new ArrayList<IAttribute>();
			if (n1Keys.contains(node) && n2Keys.contains(node)) {
				attArr.add(n1.getAttributeValue(node));
				attArr.add(n2.getAttributeValue(node));
			} else {
				if (n1Keys.contains(node)) {
					attArr.add(n1.getAttributeValue(node));
				} else {
					attArr.add(n2.getAttributeValue(node));
				}
			}
			IAttribute newAttribute = attributeFactory.createAttribute(attArr);
			attMap.put(node, newAttribute);
		}

		return new SimpleNode(typeOfNewNode, nodeDistanceCalculator, new HashSet<INode>(nodesToMerge), attMap);
	}

	@Override
	public INode createCalculationNode(List<INode> nodesToMerge) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
		
}
