package modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import scala.collection.IndexedSeq;
import clusterer.IAttribute;
import clusterer.IMaxCategoryUtilitySearcher;
import clusterer.IMergeResult;
import clusterer.INode;




public class CobwebMaxCategoryUtilitySearcher implements IMaxCategoryUtilitySearcher {
	
	/**
	 * All generated subsets have size <= MAX_SUBSET_SIZE.
	 * This limit reduces the number of generated sets from
	 * <br>
	 * <pre>
	 * C(set_size, 2) +  C(set_size, 3) + ... + C(set_size,set_size)
	 * </pre>
	 * <br>
	 * to
	 * <br>
	 * <pre>
	 * C(set_size, 2) +  C(set_size, 3) + ... + C(set_size, MAX_SUBSET_SIZE)
	 * </pre>
	 * <br>
	 * with n = set_size, m = length_of_sublist, and
	 * <br>
	 * <pre>
	 *                  n!
	 *  C(n,m) = ----------------
	 *              m! (n - m)!
	 * </pre>
	 * <br>
	 * Which is the binomial coefficient for nonnegative n and m:
	 * the number of ways of picking unordered outcomes from  possibilities,
	 * also known as a combination or combinatorial number. 
	 * <br>
	 * (Calculating the complete power set would result in generating 2^n subsets.)
	 */
	private static final int MAX_SUBSET_SIZE = 2;

	
	@Override
	public IMergeResult getMaxCategoryUtilityMerge(Set<INode> openNodes) {
		Logger log = Logger.getLogger(getClass().getName());
		
		// first we need a set of all possible subsets of open nodes.
		// This set is called a power set.
		// however we want to exclude the following sets from the power set:
		// - the empty set
		// - sets with size == 1
		// Furthermore, generating all possible subsets results in 2^n
		// new sets. Keeping all this objects in memory is clearly not possible.
		// Therefore we only generate "small" subsets, i.e. sets with size > 1 
		// and size <= MAX_SUBSET_SIZE.
		List<INode> openNodesList = new ArrayList<INode>(openNodes);
		List<IndexedSeq<Object>> permutationIndicesList = new ArrayList<IndexedSeq<Object>>();
		long count = 0;
		for (int sublistLength = 2; sublistLength <= MAX_SUBSET_SIZE; sublistLength++) {
			if (openNodesList.size() < sublistLength) continue;
			scala.collection.Iterator<scala.collection.immutable.IndexedSeq<Object>> it
					= SubsetsGenerator.subsets(openNodesList.size(), sublistLength);
			while (it.hasNext()) {
				IndexedSeq<java.lang.Object> sublist = (IndexedSeq<java.lang.Object>) it.next();
				log.finer("sublist "+ count+": "+ sublist);
//				count++;
				permutationIndicesList.add(sublist);
			}
		}
		
				
		
		// Calculate for all indices combinations in 
		// permutationIndicesList the category
		// utility and store it in a IMergeResult object.
		// Add the IMergeResults to a list.
		List<IMergeResult> mergeResults = new ArrayList<IMergeResult>();
		for (IndexedSeq<Object> sublist : permutationIndicesList) {
			INode[] possibleMerge = new INode[sublist.length()];
			int i = 0;
			scala.collection.Iterator<Object> it = sublist.iterator();
			while (it.hasNext()) {
				Integer openNodesListIndex = (Integer) it.next();
				possibleMerge[i] = openNodesList.get(openNodesListIndex);
				i++;
			}
			mergeResults.add(new MergeResult(calculateCobwebCategoryUtility(possibleMerge), possibleMerge));
		}
		
		// get the IMergeResult with the highest utility value
		// from the list of all IMergeResults
		IMergeResult best = null;
		double max = -Double.MAX_VALUE; // initialized with the smallest possible double value
 		for (IMergeResult iMergeResult : mergeResults) {
			double cUt = iMergeResult.getCategoryUtility();
			if (max < cUt) {
				best = iMergeResult;
				max = cUt;
			}
		}
		if (best == null) {
			log.severe("Err: Best merge is == null; in: " + getClass().getSimpleName() );
			System.exit(-1);
		}
		return best;
	}
	

	private static double calculateCobwebCategoryUtility(INode[] possibleMerge) {
				
		Set<INode> allAttributes = new HashSet<INode>();
		int totalLeafCount = 0;
		for (INode node : possibleMerge) {
			allAttributes.addAll(node.getAttributeKeys());
			totalLeafCount += node.getNumberOfLeafNodes();
		}
		
		List<Double> probabilities = new ArrayList<Double>();		
		for (INode node : allAttributes) {
			probabilities.addAll(calculateAttributeProbabilities(node, possibleMerge, totalLeafCount).values());
		}
		if (probabilities.size() == 0) {
			return -Double.MAX_VALUE;
		}
		
		double res = 0.0;
		for (Double prob : probabilities) {
			res += Math.pow(prob, 2.0);
		}
		
		// Normalize sum with total number of attributes
		return res / (double) allAttributes.size();
		
	}
	
	/**
	 * Calculates the probabilities in a merge set for the non-zero attribute values of a single attribute.
	 * Used in category utility calculation and in creating probability map of a new node.
	 *
	 * @param attribute the attribute for which the probability is calculated.
	 * @param possibleMerge the set of a possible merge.
	 * @param leafCount the number of all leaves of the resulting subtree of the merge.
	 * @return the map of all non-zero probabilities for the passed attribute (rating-value is key, probability is value).
	 */
	public static Map<Object,Double> calculateAttributeProbabilities(INode attribute, INode[] possibleMerge, int leafCount) {
		Map<Object,Double> probabilities = new HashMap<Object, Double>();
		for (INode node : possibleMerge) {
			if (node.hasAttribute(attribute)) {
				int currLeafCount = node.getNumberOfLeafNodes();
				
				IAttribute aV = node.getAttributeValue(attribute);
				Iterator<Map.Entry<Object, Double>> it = aV.getProbabilities();
				while (it.hasNext()) {
					Map.Entry<Object, Double> entry = it.next();
					Double prevValueInMap = probabilities.get(entry.getKey());
					if (prevValueInMap != null) {
						probabilities.put(entry.getKey(), prevValueInMap + (entry.getValue() * ((double) currLeafCount / (double) leafCount)));						
					} else {
						probabilities.put(entry.getKey(), (entry.getValue() * ((double) currLeafCount / (double) leafCount)));
					}
				}
			}
		}
		return probabilities;
	}
	
	
	////////////////////////////////////////////////////////////////////
	/////////////////	    		Back up 			////////////////
	////////////////////////////////////////////////////////////////////
	
	/**
	 * Calculates the power set (all possible subsets) of a given set.
	 * Reference:
	 * <a href="http://stackoverflow.com/questions/4640034/calculating-all-of-the-subsets-of-a-set-of-numbers">
	 * link</a> see answer of Joao Silva.
	 * 
	 * CURRENTLY NOT USED!
	 * 
	 * @param originalSet the set from which the power set is calculated
	 * @return the set containing all subsets of the given set
	 */
	private static Set<Set<INode>> powerSet(Set<INode> originalSet) {
        Set<Set<INode>> sets = new HashSet<Set<INode>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<INode>());
            return sets;
        }
        List<INode> list = new ArrayList<INode>(originalSet);
        INode head = list.get(0);
        Set<INode> rest = new HashSet<INode>(list.subList(1, list.size()));
        for (Set<INode> set : powerSet(rest)) {
            Set<INode> newSet = new HashSet<INode>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);

        }       
        return sets;
    }

	/**
	 * Calculates the power set minus the sets of size == 1 and the empty set of a given set.
	 * 
	 * CURRENTLY NOT USED!
	 * 
	 * @param originalSet the set from which the power set is calculated
	 * @return the set containing all subsets excluding sets of size == 1 and the empty set
	 */
	private static Set<Set<INode>> reducedPowerSet(Set<INode> originalSet) {
		Set<Set<INode>> sets = powerSet(originalSet);
		Iterator<Set<INode>> it = sets.iterator();
		while (it.hasNext()) {
			Set<INode> set = it.next();
			if (set.size() < 2) {
				it.remove();
			}
		}
		return sets;
	}
	
	/**
	 * For testing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		scala.collection.Iterator<scala.collection.immutable.IndexedSeq<Object>> it = SubsetsGenerator.subsets(1000, 3);
		System.out.println(it);
		while (it.hasNext()) {
			IndexedSeq<java.lang.Object> indexedSeq = (IndexedSeq<java.lang.Object>) it
					.next();
			System.out.println(indexedSeq);
		}
		System.out.println("done");
	}

// ----------------------------------  For deletion ---------------------------------------------------
	
//	//TODO: When merging more than 2 nodes, utility can't be simply translated into distance, since utility
//		// depends on which nodes are merged
//		
//		float pClass = 0; //Probability of the class
//		double pAttrInData = 0; //Sum of Probabilities of the attribute value in the data set
//		double pAttrInClass = 0; //Sum of Probabilities of the attribute values, given membership in class
//		double k = 0; //Number of categories, e.g. all nodes in this tree	
//		
//		double utility = 0; //The utility metric to be calculated
//		double distance = 0;
//			
//		// Calculates distance of two nodes based on Cobweb utility function from Gennari, Langley and Fischer "Models 
//		// of Incremental Concept Formation", p. 26
//		public double calculateDistance(INode n1, INode n2, Counter counter, Set<INode> openNodes) {
//			
//			// Should already be a list in the method definition
//			List<INode> nodesToMerge = new ArrayList(); 
//			nodesToMerge.add(n1);
//			nodesToMerge.add(n2);
//			
//			// Merge attributes of all nodes
//			ConcreteNodeFactory nodeFactory = new ConcreteNodeFactory();
//			ClassitAttributeFactory attFactory = new ClassitAttributeFactory();
//			Map<INode,IAttribute> mergedAttributes = nodeFactory.createAttMap(nodesToMerge, attFactory);
//			
//			// Count total children of all nodes
//			int totalChildren = 0;
//			for (INode nodeToMerge : nodesToMerge) {
//				totalChildren += nodeToMerge.getNumberOfNodesInSubtree();
//			}
//			
//			// Retrieve Data from Counter
//			long numberOfAllNodes = 0;
//			long nodeCountOnCurrentLevel = 0;
//			switch(n1.getNodeType()) {
//				case User: 
//					numberOfAllNodes = counter.getUserNodeCount();
//					nodeCountOnCurrentLevel = counter.getOpenUserNodeCount();
//					break;
//				case Content: 
//					numberOfAllNodes = counter.getUserNodeCount();
//					nodeCountOnCurrentLevel = counter.getOpenMovieNodeCount();
//			}
//			
//			//2. Calculate variables necessary for utility calculation
//			//2.1 Calculate probability of the class (pClass). This is calculated from number of children / total number of nodes on current level
//			if(totalChildren < 1) {
//				pClass = (float) 1/nodeCountOnCurrentLevel; // At the first level were all nodes are leaf nodes
//			}
//			else {
//				pClass = (float) totalChildren / (numberOfAllNodes - 1); // All further levels
//			}
//			//System.out.println("pClass:" + pClass);
//			
//			//2.2 Calculate Sum of Probabilities of the attribute value in the data set (pAttrInData).
//			//Can be translated to: 
//			//For each attribute value in tempNode, calculate how often it occurs in dataset compared to size of dataset
//			//and then calculate multiplicative inverse (Kehrwert)
//			
//			// Calculate all Nodes with the same attributes
////			double pAttrInData = 0;
////			for(INode attribute : tempNode.getAttributeKeys()) {
////				int numberOfAllNodesWithAttribute = 0;
////				for(INode testNode : openNodes) {
////					if(testNode.hasAttribute(attribute)) {
////						numberOfAllNodesWithAttribute += testNode.getAttributeValue(attribute).getSupport(); // Adds the number of all subnodes with this attribute
////					}
////				}
////				pAttrInData += numberOfAllNodes / numberOfAllNodesWithAttribute;
////			}
////			System.out.println("pAttrInData:" + pAttrInData);
//			
//			//2.3 Calculate sum of Probabilities of the attribute values, given membership in class (pAttrInClass)
//			//i.e. the expected number of attribute values that one can correctly guess for an arbitrary member of tempNode (= probability of occurring)
//
//			
//			// Decide if nominal or scalar
//			double pAttrInClass = 0;
//			if(1==1)
//				pAttrInClass = attrInClassNominal(mergedAttributes);
//			else
//				pAttrInClass = attrInClassScalar(mergedAttributes);
//				
////			double pAttrInClass = 0;
////			for(Entry<INode, IAttribute> attribute : mergedAttributes.entrySet()) {
////				pAttrInClass += attribute.getValue().getAverage();
////			}
//			//System.out.println("pAttrInClass:" + pAttrInClass);
//			
//			//2.4 Calculate number of categories (k)
//			//k = numberOfAllNodes;
//			//System.out.println("k:" + k);
//			
//			//3. TODO: Calculate utility for the new node
//			if(pAttrInClass != 0){
//				//utility = (pClass * pAttrInClass - pAttrInData) / k;
//				utility = (pClass * pAttrInClass);
//			}
//			
//			//4. Calculate distance from utility (Nodes with high utility should have low distance)
//			if(utility !=0)
//				distance = 1/utility;
//			else
//				distance = Double.MAX_VALUE;
//			
//			System.out.println("utility/distance: " + distance);
//			
//			return distance;
//		}
//		
//		private double attrInClassNominal(Map<INode,IAttribute> mergedAttributes) {
//			double pAttrInClass = 0;
//			for(Entry<INode, IAttribute> attribute : mergedAttributes.entrySet()) {
//				pAttrInClass += attribute.getValue().getAverage();
//			}
//			return pAttrInClass;
//		}
//		
//		private double attrInClassScalar(Map<INode, IAttribute> mergedAttributes) {
//			return 0.0;
//		}

}