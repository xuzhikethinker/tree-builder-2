package ch.uzh.agglorecommender.clusterer.treesearch;

import gnu.trove.TCollections;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import ch.uzh.agglorecommender.clusterer.treecomponent.INode;
import ch.uzh.agglorecommender.util.TBLogger;

/**
 * 
 * Responsible for calculating category utilities for the passed list of nodes calculation.
 * Is the concrete component of the decorator pattern used with the {@link IMaxCategoryUtilitySearcher}
 * interface.
 *
 */
public abstract class BasicMaxCategoryUtilitySearcher implements IMaxCategoryUtilitySearcher, Serializable {

	/**
	 * Determines if a de-serialized file is compatible with this class.
	 * <br>
	 * <br>
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions.
	 */
	private static final long serialVersionUID = 1L;


	@Override
	public TIntDoubleMap getMaxCategoryUtilityMerges(TIntSet combinationIds,
			IClusterSetIndexed<INode> clusterSet) {
		Logger log = TBLogger.getLogger(getClass().getName());
		long time = System.nanoTime();
		
		List<SplitWorker> workers = new ArrayList<SplitWorker>();
		List<TIntList> splits = splitCombinationIdsSet(combinationIds);
		for (TIntList split : splits) {
			SplitWorker w = new SplitWorker(split, this, clusterSet);
			workers.add(w);
			w.start();
		}
		TIntDoubleMap res = new TIntDoubleHashMap(combinationIds.size());
		for (SplitWorker t : workers) {
			try {
				t.join();
			} catch (InterruptedException e) {
				log.severe(e.getStackTrace().toString());
				log.severe(e.getMessage());
				log.severe("InterruptedException while waiting at join().");
				System.exit(-1);
			}
			res.putAll(t.getCalcRes());
		}
		SplitWorker.maxCUFound = false;
				
		time = System.nanoTime() - time;
		log.finer("Time to calculate new category utility values: " + ( (double) (time) ) / 1000000000.0 + " seconds. On "
					+ combinationIds.size() + " combinations. Calculated combinations: " + res.keySet().size());
		return res;
	}
	
	private List<TIntList> splitCombinationIdsSet(TIntSet combinationIds) {
		double numOfSplits = Runtime.getRuntime().availableProcessors();
		int splitSize = (int) Math.ceil((double)combinationIds.size() / numOfSplits);
		
		List<TIntList> res = new ArrayList<TIntList>();
		int splitCount = 0;
		TIntList split = new TIntArrayList(splitSize + 1);
		TIntIterator iterator = combinationIds.iterator();
		for ( int i = combinationIds.size(); i-- > 0; ) { // faster iteration by avoiding hasNext()
			if (splitCount > splitSize) {
				res.add(split);
				split = new TIntArrayList(splitSize + 1);
				splitCount = 0;
			}
			split.add(iterator.next());
			splitCount++;
		}
		res.add(split);
		return res;
	}
	

	
	private TIntDoubleMap obtainMergeResultsMultithreadedMaxCUCheckIndexed(TIntSet combinationIds, final IClusterSetIndexed<INode> clusterSet) {

		final Logger log = TBLogger.getLogger(getClass().getName());
		
		final TIntDoubleMap pRes = TCollections.synchronizedMap(new TIntDoubleHashMap(combinationIds.size()));

	    ExecutorService service = new IndexedThreadPoolExecutor();

	    List<Future<Double>> futures = new ArrayList<Future<Double>>(combinationIds.size());
		final TIntIterator iterator = combinationIds.iterator();
		for ( int i = combinationIds.size(); i-- > 0; ) { // faster iteration by avoiding hasNext()
			final int combinationId = iterator.next();
	        Callable<Double> callable = new Callable<Double>() {
	        	
	            public Double call() {
	            	double catUtil = calculateCategoryUtility(clusterSet.getCombination(combinationId));
	            	pRes.put(combinationId, catUtil);
	    			return catUtil;
	            }
	        };
	        try {
	        	futures.add(service.submit(callable));
			} catch (RejectedExecutionException e) {
				break; 
			}
	        
	    }

	    service.shutdown();
	    try {
			service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.severe(e.getStackTrace().toString());
			log.severe(e.getMessage());
			log.severe("InterruptedException while awaitTermination for infinite time.");
			System.exit(-1);
		}

	    return pRes;
	}
	
	@Override
	public Set<IMergeResult> getMaxCategoryUtilityMerges(Set<Collection<INode>> combinationsToCheck, IClusterSet<INode> clusterSet) {
		Logger log = TBLogger.getLogger(getClass().getName());
		long time = System.nanoTime();
		
		// Calculates for all indices combinations in 
		// combinations the category
		// utility and stores it in a IMergeResult object.
		// Add the IMergeResults to a set.
		Set<IMergeResult> mergeResults = obtainMergeResultsMultithreadedMaxCUCheck(combinationsToCheck);

		time = System.nanoTime() - time;
		log.finer("Time to calculate new category utility values: " + ( (double) (time) ) / 1000000000.0 + " seconds");
		return mergeResults;
	}
	
	/**
	 * Calculates for entries in the combinationIndicesList the resulting IMergeResult
	 * until a MergeResult has the maximal theoretical possible
	 * category utility or all entries in the combinationIndicesList are calculated.
	 * <br>
	 * Multi-threaded implementation using ExecutorService.
	 * 
	 * @param combinations list of merge combinations
	 * @return the list of all IMergeResults (length of return list == combinationIndicesList.size())
	 */
	private Set<IMergeResult> obtainMergeResultsMultithreadedMaxCUCheck(Set<Collection<INode>> combinations) {

		final Logger log = TBLogger.getLogger(getClass().getName());

	    ExecutorService service = new MergeResultThreadPoolExecutor();

	    List<Future<IMergeResult>> futures = new ArrayList<Future<IMergeResult>>(combinations.size());
	    for (final Collection<INode> possibleMerge : combinations) {
	        Callable<IMergeResult> callable = new Callable<IMergeResult>() {
	        	
	            public IMergeResult call() {
	    			return new MergeResult(calculateCategoryUtility(possibleMerge), possibleMerge);
	            }
	        };
	        try {
	        	futures.add(service.submit(callable));
			} catch (RejectedExecutionException e) {
				break; 
			}
	        
	    }

	    service.shutdown();
	    try {
			service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.severe(e.getStackTrace().toString());
			log.severe(e.getMessage());
			log.severe("InterruptedException while awaitTermination for infinite time.");
			System.exit(-1);
		}

	    Set<IMergeResult> mergeResults = new HashSet<IMergeResult>(combinations.size());
	    for (Future<IMergeResult> future : futures) {
	    	IMergeResult tmp = null;

	    	if (future.isDone()) {
	    		try {
	    			tmp = future.get();
	    		} catch (InterruptedException | ExecutionException e) {
	    			log.severe(e.getStackTrace().toString());
	    			log.severe(e.getMessage());
	    			log.severe("InterruptedException or ExecutionException while" +
	    					" retrieving utility calculation results from futures.");
	    			System.exit(-1);
	    		}
	    	}
	    	if (tmp != null) {
	    		mergeResults.add(tmp);
	    	}
	    }
	    return mergeResults;
	}
		
	/**
	 * Calculates the category utility according to the implementing subclass (i.e. Cobweb or Classit)
	 * of a single node resulting from a merge of all nodes in the passed nodes array.
	 * 
	 * @param possibleMerge the nodes to merge
	 * @return the category utility of a node resulting from the merge.
	 */
	protected abstract double calculateCategoryUtility(Collection<INode> possibleMerge);
	
	/**
	 * Gets the theoretical maximal possible Category Utility of a merge
	 * depending on the category utility calculation implementation (Cobweb / Classit).
	 * 
	 * @return the max possible category utility value
	 */
	protected abstract double getMaxTheoreticalPossibleCategoryUtility();
	
	
	private class IndexedThreadPoolExecutor extends ThreadPoolExecutor{
		
		Logger log = TBLogger.getLogger(getClass().getName());
		
		public IndexedThreadPoolExecutor() {			
			super(Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().availableProcessors(),
		    		0L, TimeUnit.MILLISECONDS,
		    		new LinkedBlockingQueue<Runnable>());
		}
		
		@Override
    	protected void afterExecute(Runnable r, Throwable t) {
    		super.afterExecute(r, t);
    		FutureTask<Double> ft = (FutureTask<Double>) r;
    		try {
    			Double cu = ft.get();
    			if (cu == null) return;
    			double theoreticalMaxCu = getMaxTheoreticalPossibleCategoryUtility();
    			if (cu >= theoreticalMaxCu) {
    				if (cu > theoreticalMaxCu) {
    					// error. shouldn't be possible
    					log.severe("calculated category utility is greater than teoretical maximum.");
    					log.severe("Exiting application!");
    					System.exit(-1);
    				}
    				log.fine("Merge result with max theoretical category utility was found." +
    						" Terminating category utilitie calculation for remaining merges.");

    				shutdownNow();
    				awaitTermination(2, TimeUnit.SECONDS);
    			}
    		} catch (InterruptedException | ExecutionException e) {
    			log.info("InterruptedException or ExecutionException in attempt " +
    					"to fetch calculation result in afterExecute call.");  				
    		}
    	}		
	}
	
	
	private class MergeResultThreadPoolExecutor extends ThreadPoolExecutor{
		
		Logger log = TBLogger.getLogger(getClass().getName());
		
		public MergeResultThreadPoolExecutor() {			
			super(Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().availableProcessors(),
		    		0L, TimeUnit.MILLISECONDS,
		    		new LinkedBlockingQueue<Runnable>());
		}
		
		@Override
    	protected void afterExecute(Runnable r, Throwable t) {
    		super.afterExecute(r, t);
    		FutureTask<IMergeResult> ft = (FutureTask<IMergeResult>) r;
    		try {
    			IMergeResult mr = ft.get();
    			if (mr == null) return;
    			double cu = mr.getCategoryUtility();
    			double theoreticalMaxCu = getMaxTheoreticalPossibleCategoryUtility();
    			if (cu >= theoreticalMaxCu) {
    				if (cu > theoreticalMaxCu) {
    					// error. shouldn't be possible
    					log.severe("calculated category utility is greater than teoretical maximum.");
    					log.severe("Exiting application!");
    					System.exit(-1);
    				}
    				log.fine("Merge result with max theoretical category utility was found." +
    						" Terminating category utilitie calculation for remaining merges.");

    				shutdownNow();
    				awaitTermination(2, TimeUnit.SECONDS);
    			}
    		} catch (InterruptedException | ExecutionException e) {
    			log.info("InterruptedException or ExecutionException in attempt " +
    					"to fetch calculation result in afterExecute call.");  				
    		}
    	}
		
	}
}
