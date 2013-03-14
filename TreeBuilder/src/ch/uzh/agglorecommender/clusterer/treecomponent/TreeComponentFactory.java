package ch.uzh.agglorecommender.clusterer.treecomponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ch.uzh.agglorecommender.clusterer.treesearch.ClassitMaxCategoryUtilitySearcher;
import ch.uzh.agglorecommender.clusterer.treesearch.CobwebMaxCategoryUtilitySearcher;
import ch.uzh.agglorecommender.util.TBLogger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;


public class TreeComponentFactory implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8378733065044532885L;
	
	private static TreeComponentFactory factory = new TreeComponentFactory();
	
	private TreeComponentFactory() {
		// singleton
	}

	public static TreeComponentFactory getInstance() {
		return factory;
	}
	
	public INode createLeafNode(
			ENodeType typeOfNewNode,
			String dataSetId,
			Multimap<String, Object> nominalMeta,
			Multimap<String, Double> numericalMeta,
			ImmutableMap<String, Boolean> clusteringConfig) {
		
		INode newNode = new Node(typeOfNewNode, dataSetId,  clusteringConfig);
		if (nominalMeta != null) {
			for (Entry<String, Collection<Object>> entry : nominalMeta.asMap().entrySet()) {
				newNode.addNominalMetaAttribute(entry.getKey(), createNominalLeafAttribute(entry.getValue()));
			}	
		}
		if (numericalMeta != null) {
			for (Entry<String, Collection<Double>> entry : numericalMeta.asMap().entrySet()) {
				newNode.addNumericalMetaAttribute(entry.getKey(), createNumericalLeafAttribute(entry.getValue().iterator().next()));
			}	
		}
		return newNode;
	}
	
	private IAttribute createNominalLeafAttribute(Collection<Object> attValues) {
		double initialProb = 1.0 / (double)attValues.size();
		Map<Object, Double> r = new HashMap<>();
		for (Object aV : attValues) {
			r.put(aV, initialProb);
		}
		Map<Object, Double> attMap = ImmutableMap.copyOf(r);
		return new CobwebAttribute(attMap);
	}
	
	/**
	 * Used to create the (single) attribute object of leaf nodes
	 */
	public IAttribute createNumericalLeafAttribute(double rating) {
		// the stddev would be equal 0 but we use the acuity to prevent division by 0.
		// avg = rating, stdev = acuity, support = 1, sum of ratings = rating,
		// sum of squared ratings  = ratings^2
		return new ClassitAttribute(1, rating, Math.pow(rating, 2.0));
	}
	
	public INode createInternalNode(ENodeType typeOfNewNode,
			Collection<INode> nodesToMerge, double categoryUtility) {

		if (nodesToMerge.size() < 2) {
			TBLogger.getLogger(getClass().getName())
				.severe("Merge attempt with number of nodes < 2, in: "+getClass().getSimpleName());
			System.exit(-1);
		}

		Map<INode, IAttribute> ratMap = createRatingInternalAttMap(nodesToMerge);
		Map<String, IAttribute> nomMetMap = createNominalMetaInternalAttMap(nodesToMerge);
		Map<String, IAttribute> numMetMap = createNumericalMetaInternalAttMap(nodesToMerge);
		
		INode newN = new Node(typeOfNewNode, nodesToMerge, ratMap, numMetMap, nomMetMap, categoryUtility);

		return newN;
	}
	
	private Map<String, IAttribute> createNumericalMetaInternalAttMap(Collection<INode> nodesToMerge) {
		Map<String, IAttribute> map = new HashMap<String, IAttribute>();
		for (INode node : nodesToMerge) {
			for (String attNodes : node.getNumericalMetaAttributeKeys()) {
				map.put(attNodes, null);
			}			
		}
		for (Map.Entry<String, IAttribute> entry : map.entrySet()) {
			IAttribute newAtt = createNumericalInternalAttribute(entry.getKey(), nodesToMerge);
			entry.setValue(newAtt);
		}
		if (map.containsValue(null)) {
			TBLogger.getLogger(getClass().getName()).severe("Numerical attribute map of node resulting of merge contains null" +
					" as value; in : "+getClass().getSimpleName());
			System.exit(-1);
		}
		return map;
	}
	
	private  Map<INode,IAttribute> createRatingInternalAttMap(Collection<INode> nodesToMerge) {
		Map<INode, IAttribute> map = new HashMap<INode, IAttribute>();
		for (INode node : nodesToMerge) {
			for (INode attNodes : node.getRatingAttributeKeys()) {
				map.put(attNodes, null);
			}			
		}
		for (Map.Entry<INode, IAttribute> entry : map.entrySet()) {
			IAttribute newAtt = createNumericalInternalAttribute(entry.getKey(), nodesToMerge);
			entry.setValue(newAtt);
		}
		if (map.containsValue(null)) {
			TBLogger.getLogger(getClass().getName()).severe("Numerical attribute map of node resulting of merge contains null" +
					" as value; in : "+getClass().getSimpleName());
			System.exit(-1);
		}
		return map;		
	}
	
	private IAttribute createNumericalInternalAttribute(Object attributeKey, Collection<INode> nodesToMerge) {
		List<IAttribute> atts = new ArrayList<>();
		for (INode n : nodesToMerge) {
			IAttribute tmp = n.getNumericalAttributeValue(attributeKey);
			if (tmp != null) {
				atts.add(tmp);
			}
		}
		return createNumericalInternalAttribute(atts);
	}

	private IAttribute createNumericalInternalAttribute(Collection<IAttribute> atts) {

		
		int support = ClassitMaxCategoryUtilitySearcher.calcSupportOfAttribute(atts);
		if (support < 1) {
			TBLogger.getLogger(getClass().getName()).severe("Attempt to initialize attribute object with support smaller 1." );
			System.exit(-1);
		}
		double sumOfRatings = ClassitMaxCategoryUtilitySearcher.calcSumOfRatingsOfAttribute(atts);
//		double average = sumOfRatings / (double) support;
		double sumOfSquaredRatings = ClassitMaxCategoryUtilitySearcher.calcSumOfSquaredRatingsOfAttribute(atts);
//		double stdDev = ClassitMaxCategoryUtilitySearcher.calcStdDevOfAttribute(attributeKey, merge);
		
		return new ClassitAttribute(support, sumOfRatings, sumOfSquaredRatings);

	}
	
	private Map<String, IAttribute> createNominalMetaInternalAttMap(Collection<INode> nodesToMerge) {
		Map<String, IAttribute> map = new HashMap<String, IAttribute>();
		for (INode node : nodesToMerge) {
			for (String atts : node.getNominalMetaAttributeKeys()) {
				map.put(atts, null);
			}			
		}
		for (Map.Entry<String, IAttribute> entry : map.entrySet()) {
			IAttribute newAtt = createNominalInternalAttribute(entry.getKey(), nodesToMerge);
			entry.setValue(newAtt);
		}
		if (map.containsValue(null)) {
			TBLogger.getLogger(getClass().getName()).severe("Nominal attribute map of node resulting of merge contains null" +
					" as value; in : "+getClass().getSimpleName());
			System.exit(-1);
		}
		return map;	
	}
	
	private IAttribute createNominalInternalAttribute(Object attributeKey, Collection<INode> nodesToMerge) {
		int totalLeafCount = 0;
		for (INode node : nodesToMerge) {
			totalLeafCount += node.getNumberOfLeafNodes();
		}
		Map<Object, Double> attMap = ImmutableMap.copyOf(
				CobwebMaxCategoryUtilitySearcher
					.calculateAttributeProbabilities(
							attributeKey, nodesToMerge, totalLeafCount
					)
				);
		return new CobwebAttribute(attMap);
	}
	

	
}
