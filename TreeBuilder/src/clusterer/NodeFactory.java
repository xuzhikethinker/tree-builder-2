package clusterer;

import java.util.List;

/**
 * Creates instances of a {@code INode}s.
 */
public abstract class NodeFactory {
	
	/**
	 * Instantiates the leaf nodes of the cluster tree.
	 * 
	 * @param typeOfNewNode the type of new node.
	 * @param nodeDistanceCalculator the node distance calculator of the new node.
	 * @return a new node instance.
	 */
	public abstract INode createLeafNode(ENodeType typeOfNewNode, INodeDistanceCalculator nodeDistanceCalculator);

	/**
	 * 
	 * Creates a new node and initializes its attribute map.
	 * The new node is added to the cluster tree as new
	 * root with all nodes in {@code nodesToMerge} as children.
	 * 
	 * @param typeOfNewNode the type of the node to create.
	 * @param nodesToMerge the nodes to combine for the new node.
	 * @param nodeDistanceCalculator the node distance calculator of the new node
	 * @param attributeFactory the attribute factory which initializes the attribute map of the new node.
	 * @return a new node instance.
	 */
	public abstract INode createInternalNode(ENodeType typeOfNewNode, List<INode> nodesToMerge, INodeDistanceCalculator nodeDistanceCalculator, AttributeFactory attributeFactory);


	/**
	 * Creates a new temporary node to obtain its utility.
	 * 
	 * @param nodesToMerge nodes to combine in this node.
	 * @return creates a node for distance calculation.
	 * This node is not added to the tree.
	 */
	public abstract INode createCalculationNode(List<INode> nodesToMerge);
}
