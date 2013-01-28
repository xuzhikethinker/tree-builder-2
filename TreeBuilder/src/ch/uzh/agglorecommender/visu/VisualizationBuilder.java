package ch.uzh.agglorecommender.visu;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.collections15.map.LazyMap;

import ch.uzh.agglorecommender.clusterer.treecomponent.INode;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.samples.TreeCollapseDemo;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.subLayout.TreeCollapser;
import edu.uci.ics.jung.visualization.util.PredicatedParallelEdgeIndexFunction;


//@SuppressWarnings("serial")
public class VisualizationBuilder extends JPanel {

	/**
	 * Factories for the graph, tree, edges and vertices
	 */
	Forest<INode,Integer> graph;

	Factory<DirectedGraph<INode,Integer>> graphFactory = new Factory<DirectedGraph<INode,Integer>>() {
		@Override
		public DirectedGraph<INode, Integer> create() {
			return new DirectedSparseMultigraph<INode,Integer>();
		}
	};

	Factory<Tree<INode,Integer>> treeFactory = new Factory<Tree<INode,Integer>> () {
		@Override
		public Tree<INode, Integer> create() {
			return new DelegateTree<INode,Integer>(graphFactory);
		}
	};

	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i=0;
		@Override
		public Integer create() {
			return i++;
		}
	};

	//	Factory<String> vertexFactory = new Factory<String>() {
	//		int i=0;
	//		@Override
	//		public String create() {
	//			return "V"+i++;
	//		}
	//	};

	/**
	 * the visual component and renderer for the graph
	 */
	private VisualizationViewer<INode,Integer> vv;
	private TreeLayout<INode,Integer> layout;

	private TreeCollapser collapser;

	private Set<INode> nodes;

	public VisualizationBuilder(Set<INode> nodes) {
		this.nodes = nodes;
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		// Create the graphs
		graph = new DelegateForest<INode,Integer>();
		createTree();
		collapser = new TreeCollapser();

		// Define Layout
		layout = new TreeLayout(graph, 50, 100){ // x-dir and y-dir spacing between vertices
			@Override
			public void reset() {
				alreadyDone = new HashSet<INode>();
				basePositions = new HashMap<INode, Integer>();

				locations =    	LazyMap.decorate(new HashMap(),
						new Transformer() {
					public Point2D transform(Object arg0) {
						return new Point2D.Double();
					}
				});
			}

			//			@Override
			//			public void setLocation(Object v, Point2D location) {
			//				System.err.println("Hello!!!!");
			//				Point2D p = (Point2D) locations.get(v);
			//				p.setLocation(location);
			//			}
		};

		// Define Visualization Viewer, add a listener for ToolTips
		vv =  new VisualizationViewer<INode,Integer>(layout);

		vv.setBackground(Color.white);
		vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.setVertexToolTipTransformer(new ToStringLabeller());
		vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
        vv.getRenderContext().setVertexShapeTransformer(new ClusterVertexShapeFunction());
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		vv.getRenderContext().setVertexLabelTransformer(new ConstantTransformer(null) {
			@Override
			public Object transform(Object input) {

				String s = input.toString();
				if (input instanceof Graph) {
					Graph<INode, Integer> g = (Graph<INode, Integer>) input;
					List<Long> li = getCollapsedNodeIds(g);
					Collections.sort(li);
					s = "Ids: " + li.toString();
				} else {
					s = s.replace("User Node", "Id: ");
					s = s.replace("Content Node", "Id: ");
				}
				return s;
			}
			
			private List<Long> getCollapsedNodeIds(Graph<INode, Integer> input) {
				Graph<INode, Integer> g = (Graph<INode, Integer>) input;
				Collection<INode> c = g.getVertices();
				List<Long> li = new ArrayList<Long>();
				for (Object o : c) {
					if (o instanceof Graph) {
						li.addAll(getCollapsedNodeIds((Graph<INode, Integer>) o));
					} else {
						li.add(((INode) o).getId());
					}
				}
				return li;
			}
		});
		

		// Add Elements to Visualization Viewer
		final GraphZoomScrollPane panelC = new GraphZoomScrollPane(vv);
		this.add(panelC);
		panelC.setBorder(BorderFactory.createLoweredBevelBorder());

		this.add(panelC);
		this.add(getControlElements());
	}

	private JComponent getControlElements() {
		final PredicatedParallelEdgeIndexFunction eif = PredicatedParallelEdgeIndexFunction.getInstance();
		final Set exclusions = new HashSet();
		eif.setPredicate(new Predicate() {

			public boolean evaluate(Object e) {

				return exclusions.contains(e);
			}
		});

		final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		vv.setGraphMouse(graphMouse);
		graphMouse.add(new PopupGraphMousePlugin()); // Integration of mouse listener functionality to show attributes of vertex
		JComboBox modeBox = graphMouse.getModeComboBox();
		modeBox.addItemListener(graphMouse.getModeListener());
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);

		final ScalingControl scaler = new CrossoverScalingControl();

		JButton plus = new JButton("+");
		plus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(vv, 1.1f, vv.getCenter());
			}
		});
		JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(vv, 1/1.1f, vv.getCenter());
			}
		});

		JButton collapse = new JButton("Collapse");
		collapse.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Set<INode> picked = new HashSet<INode>(vv.getPickedVertexState().getPicked());
				if(picked.size() > 0) {
					Forest<INode, Integer> inGraph = (Forest<INode, Integer>)layout.getGraph();
					for (INode root : getCollapseRoots(picked)) {
						try {
							collapser.collapse(vv.getGraphLayout(), inGraph, root);
						} catch (InstantiationException e1) {
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							e1.printStackTrace();
						}
					}
					vv.getPickedVertexState().clear();
					vv.repaint();
				}
			}
			
			private List<INode> getCollapseRoots(Set<INode> picked) {
				List<INode> roots = new ArrayList<INode>();
				for (INode n : picked) {
					if (n.isLeaf()) continue;
					if (isCollapseRoot(n, picked)) {
						roots.add(n);
					}
				}
				return roots;
			}
			
			private boolean isCollapseRoot(INode candidate, Set<INode> picked) {
				if (candidate.isRoot()) return true;
				INode p = candidate.getParent();
				if (picked.contains(p)) return false;
				return isCollapseRoot(p, picked);
			}
		});

		JButton expand = new JButton("Expand");
		expand.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Collection<INode> picked = new HashSet<INode>(vv.getPickedVertexState().getPicked());
				for(Object v : picked) {
					if(v instanceof Forest) {
						Forest<INode, Integer> inGraph = (Forest<INode, Integer>)layout.getGraph();
						collapser.expand(inGraph, (Forest<INode, Integer>)v);
					}
					vv.getPickedVertexState().clear();
					vv.repaint();
				}
			}
		});

		JButton reset = new JButton("Reset");
		reset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				layout.setGraph(graph);
				exclusions.clear();
				updateGraph(nodes);
			}
		});

		JPanel controls = new JPanel();
		JPanel zoomControls = new JPanel(new FlowLayout());
		zoomControls.setBorder(BorderFactory.createTitledBorder("Zoom"));
		zoomControls.add(plus);
		zoomControls.add(minus);
		controls.add(zoomControls);
		JPanel collapseControls = new JPanel(new FlowLayout());
		collapseControls.setBorder(BorderFactory.createTitledBorder("Picked"));
		collapseControls.add(collapse);
		collapseControls.add(expand);
		collapseControls.add(reset);
		controls.add(collapseControls);
		controls.add(modeBox);
		controls.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		JScrollPane scrollP = new JScrollPane(controls);
		scrollP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		return scrollP;
	}

	public void updateGraph(Set<INode> nodes) {
		this.nodes = nodes;
		layout.reset();
		graph = new DelegateForest<INode,Integer>();
		createTree();
		layout.setGraph(graph);
		//		collapser = new GraphCollapser(graph);
		vv.repaint();
	}

	/**
	 * @param userNodes 
	 * @param rootNodes 
	 * 
	 */
	private void createTree() {

		// Build Tree recursively
		for (INode node : nodes) {
			processChildren(node);
		}

	}

	private void processChildren(INode parent) {

		Iterator<INode> iter = parent.getChildren();

		// Process every child
		while (iter.hasNext()) {

			// Build edge between parent and child, build subtree recursively
			INode child = (INode) iter.next();
			graph.addEdge(edgeFactory.create(),parent,child);
			processChildren(child);
		}
	}

	/**
	 * a demo class that will create a vertex shape that is either a
	 * polygon or star. The number of sides corresponds to the number
	 * of vertices that were collapsed into the vertex represented by
	 * this shape.
	 * 
	 * @author Tom Nelson
	 *
	 * @param <V>
	 */
	class ClusterVertexShapeFunction<V> extends EllipseVertexShapeTransformer<V>
	{

		ClusterVertexShapeFunction() {
			setSizeTransformer(new ClusterVertexSizeFunction<V>(20));
		}
		@SuppressWarnings("unchecked")
		@Override
		public Shape transform(V v) {
			if(v instanceof Graph) {
				int size = ((Graph)v).getVertexCount();
				if (size < 8) {   
					int sides = Math.max(size, 3);
					return factory.getRegularPolygon(v, sides);
				}
				else {
					return factory.getRegularStar(v, size);
				}
			}
			return super.transform(v);
		}
	}

	/**
	 * A demo class that will make vertices larger if they represent
	 * a collapsed collection of original vertices
	 * @author Tom Nelson
	 *
	 * @param <V>
	 */
	class ClusterVertexSizeFunction<V> implements Transformer<V,Integer> {
		int size;
		public ClusterVertexSizeFunction(Integer size) {
			this.size = size;
		}

		public Integer transform(V v) {
			if(v instanceof Graph) {
				return 25;
			}
			return size;
		}
	}

	/**
	 * a driver for this demo
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		Container content = frame.getContentPane();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		content.add(new TreeCollapseDemo());
		frame.pack();
		frame.setVisible(true);
	}
}