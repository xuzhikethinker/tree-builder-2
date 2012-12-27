package modules;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import clusterer.AttributeFactory;
import clusterer.IAttribute;
import clusterer.INode;

import com.google.common.collect.ImmutableMap;

public class CobwebAttributeFactory extends AttributeFactory implements Serializable {

	/**
	 * Determines if a de-serialized file is compatible with this class.
	 * <br>
	 * <br>
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions.
	 */
	private static final long serialVersionUID = 1L;
	
	private static CobwebAttributeFactory factory = new CobwebAttributeFactory();
	
	/*
	 * Must not be instantiated with constructor.
	 */
	private CobwebAttributeFactory() {
		// singleton
	}
	
	public static  AttributeFactory getInstance() {
		return factory;
	}
	
	@Override
	public IAttribute createAttribute(double rating) {
		Map<Double, Double> attMap = ImmutableMap.of(rating, 1.0);
		return new Attribute(attMap);
	}

	@Override
	public IAttribute createAttribute(INode attributeKey, List<INode> nodesToMerge) {
		int totalLeafCount = 0;
		for (INode node : nodesToMerge) {
			totalLeafCount += node.getNumberOfLeafNodes();
		}
		Map<Object, Double> attMap = ImmutableMap.copyOf(
				CobwebMaxCategoryUtilitySearcher
					.calculateAttributeProbabilities(
							attributeKey, nodesToMerge.toArray(new INode[nodesToMerge.size()]), totalLeafCount
					)
				);
		return new Attribute(attMap);
	}
}
