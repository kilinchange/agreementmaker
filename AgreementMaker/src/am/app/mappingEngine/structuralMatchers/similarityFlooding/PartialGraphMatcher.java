package am.app.mappingEngine.structuralMatchers.similarityFlooding;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import am.app.mappingEngine.AbstractMatcherParametersPanel;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.SimilarityMatrix;
import am.app.mappingEngine.structuralMatchers.SimilarityFlooding;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PCGEdge;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PCGEdgeData;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PCGVertex;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PCGVertexData;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PairwiseConnectivityGraph;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.WGraphEdge;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.WGraphVertex;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.WrappingGraph;
import am.app.ontology.Node;
import am.utility.DirectedGraphEdge;
import am.utility.Pair;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class PartialGraphMatcher extends SimilarityFlooding {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4674863017457273447L;
	
	private HashMap<String, PCGEdge> edgesMap;
	private enum EdgeDirection{IN, OUT};
	
	// intialized when initialized the classes and properties matrix (loadSimilarityMatrix())
	private SimilarityMatrix prevRoundClasses;
	private SimilarityMatrix prevRoundProperties;
	
	/**
	 * 
	 */
	public PartialGraphMatcher() {
		super();
		edgesMap = new HashMap<String, PCGEdge>();
	}

	/**
	 * @param params_new
	 */
	public PartialGraphMatcher(SimilarityFloodingMatcherParameters params_new) {
		edgesMap = new HashMap<String, PCGEdge>();
	}
	
	@Override 
	public AbstractMatcherParametersPanel getParametersPanel() { return new SimilarityFloodingParametersPanel(); };
	
	/**
	 * Similarity Flooding Algorithm. 
	 * @see am.app.mappingEngine.AbstractMatcher#align(ArrayList<Node> sourceList, ArrayList<Node> targetList, alignType typeOfNodes)
	 * NOTE: we are using graphs instead of arrayList
	 */
	@Override
	 protected void align() {

		progressDisplay.clearReport();
		
		// load the matrices
		loadSimilarityMatrices();
		
		// cannot align just one ontology (this is here to catch improper invocations)
		if( sourceOntology == null ) throw new NullPointerException("sourceOntology == null");   
		if( targetOntology == null ) throw new NullPointerException("targetOntology == null");
		
		// starting phase: create wrapping graphs
		progressDisplay.appendToReport("Creating Wrapping Graphs...");
		WrappingGraph sourceGraph = new WrappingGraph(sourceOntology.getModel());
		WrappingGraph targetGraph = new WrappingGraph(targetOntology.getModel());
		if( DEBUG_FLAG ) System.out.println(sourceGraph.toString());
		if( DEBUG_FLAG ) System.out.println(targetGraph.toString());
		progressDisplay.appendToReport("done.\n");
		
		progressDisplay.appendToReport("Start Computation...");

		//TODO: check collateral effects and details

		int round = 0;
		Vector<Double> cOldVect, cNewVect;
		do{
			// new round starts
			round++;
			
			// phase 0: CLEAN all the data used before, matrix and WGraph survive and update old matrices
			pcg = new PairwiseConnectivityGraph();
			prevRoundClasses = (SimilarityMatrix) classesMatrix.clone();
			prevRoundProperties = (SimilarityMatrix) propertiesMatrix.clone();
			
			// phase 1 to 5
			executeRoundOperations(sourceGraph.vertices(), targetGraph.vertices());
			
			// phase 6: get global max similarity
			double roundMax = getGlobalMaxSimilarity(classesMatrix, propertiesMatrix);
			
			// phase 7: normalize all values
			normalizeSimilarities(classesMatrix, roundMax);
			normalizeSimilarities(propertiesMatrix, roundMax);
			
			// phase 8: prepare Vectors for delta check
			Vector<Double> pVect = prevRoundProperties.toSimilarityArray(prevRoundProperties.toMappingArray());
			
			cOldVect = prevRoundClasses.toSimilarityArray(prevRoundClasses.toMappingArray());
			cOldVect.addAll(pVect);
			
			pVect = propertiesMatrix.toSimilarityArray(propertiesMatrix.toMappingArray());
			
			cNewVect = classesMatrix.toSimilarityArray(classesMatrix.toMappingArray());
			cNewVect.addAll(pVect);
			
		} while(!checkStopCondition(round, cOldVect, cNewVect));
		// until delta less then value
		
		// phase 9: compute relative similarities (at the very end)
		progressDisplay.appendToReport("Computing Relative Similarities...");
		computeRelativeSimilarities(classesMatrix);
		computeRelativeSimilarities(propertiesMatrix);
		progressDisplay.appendToReport("done.\n");
		
	 }
	 
	private void executeRoundOperations(Iterator<WGraphVertex> sVertices, Iterator<WGraphVertex> tVertices){
		Iterator<WGraphVertex> sLocalItr = sVertices;
		Iterator<WGraphVertex> tLocalItr = tVertices;

		WGraphVertex s = null, t = null;
		// until all cells are covered
		while(sLocalItr.hasNext()){
			s = sLocalItr.next();
			while(tLocalItr.hasNext()){
				t = tLocalItr.next();
				
				if(s.getNodeType().equals(t.getNodeType())){
					
					// phase 1: get a pcg vertex and inserts it in the pcg 
					PCGVertex pcgV = getPCGVertex(s, t);
					
					// phase 2: compute pcg graph on that vertex
					createPairwiseConnectivityGraph(pcgV);
					
					// phase 3: grab matrix values
					grabMatrixValues(pcg.vertices());
					
					// phase 4: one round of the fixpoint
					computeFixpointRound(pcg.vertices());
					
					// phase 5: translate results in matrix
					populateSimilarityMatrices(pcg);
				}
			}
			tLocalItr = tVertices;
		}
	}

	/*
	 * private int startComputation(int pairs) {
//		System.out.println("N° of cells filled: " + pairs);
		return pairs;
	 }
	 */
	
	 
	 // PHASE 1: get a pcg vertex and inserts it in the pcg //
	 @Override
	 protected PCGVertex getPCGVertex(WGraphVertex s, WGraphVertex t){
		 return super.getPCGVertex(s, t);
	 }
	 
	 // PHASE 2: compute pcg graph on that vertex //
	 protected boolean createPairwiseConnectivityGraph(PCGVertex pcgV){
		 
		 if(pcgV.isVisited()){
			return false;	
		 }
		 else{
			 pcgV.setVisited(true);
			 
			 performEdgesLookup(pcgV, EdgeDirection.IN);
			 performEdgesLookup(pcgV, EdgeDirection.OUT);
			 
			 return true;
		 }

	 }
	 
	 private void performEdgesLookup(PCGVertex pcgV, EdgeDirection ed){
	
		 // WGraphEdges iterators
		 Iterator<DirectedGraphEdge<String,RDFNode>> sourceIterator = null;
		 Iterator<DirectedGraphEdge<String,RDFNode>> targetIterator = null;
		 Iterator<DirectedGraphEdge<String,RDFNode>> targetStart = null; // used as a starting point for the target iterator
		 
		 // WGraphVertices for edges lookup
		 WGraphVertex s = pcgV.getObject().getStCouple().getLeft();
		 WGraphVertex t = pcgV.getObject().getStCouple().getRight();
		 
		 switch(ed){
		 case IN:
			 sourceIterator = s.edgesInIter();
			 targetStart = t.edgesOutIter();
			 break;
		 case OUT:
			 sourceIterator = s.edgesOutIter();
			 targetStart = t.edgesOutIter();
			 break;
		 default:
			 try {
				throw new Exception("Should not be here. Make sure EdgeDirection is provided and not null");
			} catch (Exception e) {
				e.printStackTrace();
			}
		 }

		 targetIterator = targetStart;
		 
		 WGraphEdge sEdge = null, tEdge = null;
		 String edgeLabel = null;
//		 System.out.println("source: " + sourceIterator.hasNext() + " target: " + targetIterator.hasNext());
		 while(sourceIterator.hasNext()){
			 sEdge = (WGraphEdge) sourceIterator.next();
			 
			 while(targetIterator.hasNext()){
				 tEdge = (WGraphEdge) targetIterator.next();
//				 System.out.println("source: " + sEdge.toString() + " target: " + tEdge.toString());
				 
				 if(sEdge.getObject().equals(tEdge.getObject())){
					 edgeLabel = sEdge.getObject();
					 // in addNewElementsToPCG I'll call the recursive step
					 switch(ed){
					 case IN:
						 addNewElementsToPCG(pcgV, (WGraphVertex) sEdge.getOrigin(), edgeLabel, (WGraphVertex) tEdge.getOrigin(), ed);
						 break;
					 case OUT:
						 addNewElementsToPCG(pcgV, (WGraphVertex) sEdge.getDestination(), edgeLabel, (WGraphVertex) tEdge.getDestination(), ed);
						 break;
					 default:
						 try {
							throw new Exception("Should not be here. Make sure EdgeDirection is provided and not null");
						} catch (Exception e) {
							e.printStackTrace();
						}
					 }
				 }
				 // base case: if failure and nothing is done
			 }
			 targetIterator = targetStart;
		 }
	 }
	 
	 private void addNewElementsToPCG(PCGVertex pcgV, WGraphVertex s, String edgeLabel, WGraphVertex t, EdgeDirection ed) {
			
			PCGVertex secondPCGVertex = getPCGVertex(s, t);
			PCGEdge edge = null;
//			System.out.println(pcgV + " ----- " + secondPCGVertex.toString());
			 
			// insertion
			switch(ed){
			 case IN:
				 if((edge = getEdge(secondPCGVertex, edgeLabel, pcgV)) != null){
					 insertEdgeInPCG(secondPCGVertex, edge, pcgV);
				 }
				 break;
			 case OUT:
				 if((edge = getEdge(pcgV, edgeLabel, secondPCGVertex)) != null){
					 insertEdgeInPCG(pcgV, edge, secondPCGVertex);
				 }
				 break;
			 default:
				 try {
					throw new Exception("Should not be here. Make sure EdgeDirection is provided and not null");
				} catch (Exception e) {
					e.printStackTrace();
				}
			 }
			
			// recursive step
			createPairwiseConnectivityGraph(secondPCGVertex);
			
		}
	 
		private PCGEdge getEdge(PCGVertex pcgV, String edgeLabel, PCGVertex pcgV2) {
			
			PCGEdge edgeNew = edgesMap.get(pcgV.toString() + edgeLabel + pcgV2.toString());
			
			if (edgeNew == null) {
				// we don't have that edge, we create it
				edgeNew = new PCGEdge(pcgV, pcgV2, new PCGEdgeData(edgeLabel));
				edgesMap.put(pcgV.toString() + edgeLabel + pcgV2.toString(), edgeNew);
				return edgeNew;
			}
			else{
				// we already have that edge (it would give a duplicate)
				return null;
			}
		}
	 
		// PHASE 3: grab matrix values
		private void grabMatrixValues(Iterator<PCGVertex> iVert) {
			
			PCGVertex vert = null;
			RDFNode s, t;
			while(iVert.hasNext()){
				 
				// take the current vertex
				vert = iVert.next();
				
				// take the RDFNodes associated to vert
				s = vert.getObject().getStCouple().getLeft().getObject();
				t = vert.getObject().getStCouple().getRight().getObject();
				 
				// take both source and target ontResources (values can be null, means not possible to take resources
				 OntResource sourceRes = getOntResourceFromRDFNode(s);
				 OntResource targetRes = getOntResourceFromRDFNode(t);
				 if(sourceRes != null && targetRes != null){
					
					 // try to get the Node and check they belong to the same alignType
					 Node sourceClass = getNodefromOntResource(sourceOntology, sourceRes, alignType.aligningClasses);
					 Node targetClass = getNodefromOntResource(targetOntology, targetRes, alignType.aligningClasses);
					 // test if both nodes are classes
					 if(sourceClass == null || targetClass == null){
						 Node sourceProperty = getNodefromOntResource(sourceOntology, sourceRes, alignType.aligningProperties);
						 Node targetProperty = getNodefromOntResource(targetOntology, targetRes, alignType.aligningProperties);
						 // test if both nodes are properties
						 if(sourceProperty == null || targetProperty == null){
							 continue;
						 }
						 else{
							 // put the similarity value in the current pcgVertex from the properties matrix
							 vert.getObject().setOldSimilarityValue(propertiesMatrix.getSimilarity(sourceProperty.getIndex(), targetProperty.getIndex()));
						 }
					 }
					 else{
						 // put the similarity value in the current pcgVertex from the properties matrix
						 vert.getObject().setOldSimilarityValue(classesMatrix.getSimilarity(sourceClass.getIndex(), targetClass.getIndex()));
					 }
				 }
				 else{
					 continue;
				 }
				
			 }
		}
		
		 // PHASE 4: one round of the fixpoint 		//
		 @Override
		 protected double computeFixpointRound(Iterator<PCGVertex> iVert){
			 return super.computeFixpointRound(iVert);
		 }
		 
		 // PHASE 5: translate results in matrix	//
		 @Override
		 protected void populateSimilarityMatrices(PairwiseConnectivityGraph pcg){
			 super.populateSimilarityMatrices(pcg);
		 }
		 
		 // PHASE 6: get global max similarity		//
		 private double getGlobalMaxSimilarity(SimilarityMatrix c, SimilarityMatrix p){
			 return Math.max(c.getMaxValue(), p.getMaxValue());
		 }
		
		 // PHASE 7: normalize all values 			//
		 private void normalizeSimilarities(SimilarityMatrix localMatrix, double roundMax) {
			 double oldValue = 0;
			 Mapping current = null;
			 
			 for(int i = 0; i < localMatrix.getRows(); i++){
				 for(int j = 0; j < localMatrix.getColumns(); j++){
					 
					 current = localMatrix.get(i, j);
					 if(current != null){
						 oldValue = current.getSimilarity();
						 localMatrix.get(i, j).setSimilarity(oldValue / roundMax);
					 }
					 
				 }
			 }
		 }	
	
		 
		 // INHERITED FUNCTIONS //
		 
		@Override
		protected void loadSimilarityMatrices() {
			// load classesMatrix
			classesMatrix = new SimilarityMatrix(sourceOntology.getClassesList().size(),
					targetOntology.getClassesList().size(),
					alignType.aligningClasses);
			classesMatrix.fillMatrix(1.0, sourceOntology.getClassesList(), targetOntology.getClassesList());
			prevRoundClasses = new SimilarityMatrix(classesMatrix);
			// load propertiesMatrix
			propertiesMatrix = new SimilarityMatrix(sourceOntology.getPropertiesList().size(),
					targetOntology.getPropertiesList().size(),
					alignType.aligningProperties);
			propertiesMatrix.fillMatrix(1.0, sourceOntology.getPropertiesList(), targetOntology.getPropertiesList());
			prevRoundProperties = new SimilarityMatrix(propertiesMatrix);
		}
		
		
	
		@Override
		protected PCGVertexData selectInput(Pair<RDFNode, RDFNode> pair) {
			// TODO Auto-generated method stub
			return null;
		}
}