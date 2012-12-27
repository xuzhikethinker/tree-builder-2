package modules;

import java.io.Serializable;
import java.util.List;

import clusterer.AttributeFactory;
import clusterer.IAttribute;
import clusterer.INode;

public class ClassitAttributeFactory extends AttributeFactory implements Serializable {

	/**
	 * Determines if a de-serialized file is compatible with this class.
	 * <br>
	 * <br>
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions.
	 */
	private static final long serialVersionUID = 1L;
	
	private static ClassitAttributeFactory factory = new ClassitAttributeFactory();
	
	/*
	 * Must not be instantiated with constructor.
	 */
	private ClassitAttributeFactory() {
		// singleton
	}
	
	public static  AttributeFactory getInstance() {
		return factory;
	}
	
	/**
	 * Used to create the (single) attribute object of leaf nodes
	 */
	@Override
	public IAttribute createAttribute(double rating) {
		// avg = 1.0; stdev = 0.0; support = 1
		return new Attribute(1.0, 0.0, 1);
	}

	/**
	 * Used to calculate new nodes in the merging process
	 */
	@Override
	public IAttribute createAttribute(INode attributeKey, List<INode> nodesToMerge) {
		// TODO incremental stdev calculation
		return null;
	}

	
// ----------------------------------  For deletion ---------------------------------------------------
	
//	/**
//	 * Here attributes are finally combined, calculation of average, stddev, support, .. is done here
//	 */
//	private IAttribute calcAttributeValues(List<IAttribute> attributesToCombine) {
//		
//		// No Attributes
//		if (attributesToCombine.size() == 0) {
//			System.err.println("attempt to combine 0 attributes, "+getClass().getSimpleName());
//			System.exit(-1);
//		}
//		
//		// Only one occurrence of Attribute
//		if (attributesToCombine.size() == 1) {
//			IAttribute a = attributesToCombine.get(0);
//			return new Attribute(a.getAverage(), a.getStdDev(), a.getSupport(), a.getConsideredRatings());
//		}
//		
//		// ??? #####################################################
////		int sizeOfNewLeafList = 0;
////		for (IAttribute attribute : attributesToCombine) {
////			sizeOfNewLeafList += attribute.getConsideredRatings().size();
////		}		
////		Double[] tmpAr = new Double[sizeOfNewLeafList];
////		int prevAttLength = 0;
////		for (IAttribute attribute : attributesToCombine) {
////			System.arraycopy(attribute.getConsideredRatings(), 0, tmpAr, prevAttLength, attribute.getConsideredRatings().size());
////		}
//		// ??? #####################################################
//		
//		// Determine Considered Ratings
//		ArrayList<Double> tmpAr = new ArrayList<Double>();
//		for (IAttribute attribute : attributesToCombine) {
//			tmpAr.addAll(attribute.getConsideredRatings());
//		}
//		
//		// Average
//		double tmpAvg = 0.0;
//		for (Double avgLi : tmpAr) {
//			tmpAvg += avgLi;
//		}
//		tmpAvg = tmpAvg / tmpAr.size();
//		
//		// Support
//		int tmpSup = 0;
//		for (IAttribute attribute : attributesToCombine) {
//			 tmpSup += attribute.getSupport();
//		}
//		
//		// Standard Deviation
//		double tmpStD = 0.0;
//		for (Double avgLi : tmpAr) {
//			tmpStD += Math.pow((avgLi - tmpAvg),2.0);
//		}
//		tmpStD = Math.sqrt(tmpStD/(tmpAr.size() - 1.0));
////		
//		//Double[] doubleArray = ArrayUtils.toObject(tmpAr);
//		
//		return new Attribute(tmpAvg, tmpStD, tmpSup, tmpAr);
//	}

}
