package clusterer;

import java.util.List;

public interface Factory {
	
	/**
	 * for now we assume nodesToMerge.size() == 2 or nodesToMerge == null
	 * @param List nodesToMerge
	 * @return a new node already initialized with parameters
	 */
	public INode createNode(List<INode> nodesToMerge, AttributeFactory attributeFactory);
}
