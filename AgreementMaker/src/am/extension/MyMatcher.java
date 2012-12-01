package am.extension;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import simpack.measure.external.alignapi.Hamming;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.Alignment;
import am.app.mappingEngine.DefaultMatcherParameters;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.MatcherFactory;
import am.app.mappingEngine.MatchersRegistry;
import am.app.mappingEngine.SimilarityMatrix;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentMatcher;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentParameters;
import am.app.mappingEngine.referenceAlignment.ReferenceEvaluationData;
import am.app.mappingEngine.referenceAlignment.ReferenceEvaluator;
import am.app.ontology.Node;
import am.app.ontology.Ontology;
import am.app.ontology.ontologyParser.OntoTreeBuilder;
import am.app.ontology.ontologyParser.OntologyDefinition.OntologyLanguage;
import am.app.ontology.ontologyParser.OntologyDefinition.OntologySyntax;
import am.app.similarity.JaroWinklerSim;
import am.app.similarity.LevenshteinEditDistance;
import am.extension.semanticExplanation.CombinationCriteria;
import am.extension.semanticExplanation.ExplanationNode;
import am.extension.semanticExplanation.SubTreeLayout;
import am.extension.semanticExplanation.mouseWorks.MyMouseMenus;
import am.extension.semanticExplanation.mouseWorks.PopupVertexEdgeMenuMousePlugin;
import am.utility.FromWordNetUtils;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.MouseListenerTranslator;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class MyMatcher extends AbstractMatcher {
	
	   public static class VertexPaintTransformer implements Transformer<ExplanationNode,Paint> {

	        private final PickedInfo<ExplanationNode> pi;

	        VertexPaintTransformer ( PickedInfo<ExplanationNode> pi ) { 
	            super();
	            if (pi == null)
	                throw new IllegalArgumentException("PickedInfo instance must be non-null");
	            this.pi = pi;
	        }

	        @Override
	        public Paint transform(ExplanationNode i) {
	            Color p = null;
	            //Edit here to set the colours as reqired by your solution
	                p = Color.GREEN;
	            //Remove if a selected colour is not required
	            if ( pi.isPicked(i)){
	                p = Color.yellow;
	            }
	            return p;
	        }
	    }

    private static final long serialVersionUID = -3772449944360959530L;

    private static Logger log = Logger.getLogger(MyMatcher.class);

    ExplanationNode stringSimilarityExplanation = new ExplanationNode("String Similarity");
    ExplanationNode wordNetSimilarityExplanation = new ExplanationNode("WordNet Similarity");
    ExplanationNode wordNetStringCombinationExplanation = new ExplanationNode("WordNet- String Combination");
    ExplanationNode absoluteSimilarityExplanation = new ExplanationNode("Absolute Similarity");
    ExplanationNode resultExplanation = new ExplanationNode("Final Explanation");
    ExplanationNode levenshteinExplanation = new ExplanationNode("Levenshtein Distance");
    ExplanationNode jarowinglerExplanation = new ExplanationNode("Jaro-Wingler metric");
    
    
    //the 2 dimensional structure which stores the explanation node between a source and target
    protected int rows;             // number of rows
    protected int columns;             // number of columns
    protected static ExplanationNode[][] explanationMatrix;
    private static  JLabel nodeDescriptionValue;
    private static JLabel criteriaLabel;
    private static JLabel valueLabel;
    private static JPanel panel = new JPanel(new BorderLayout());
    private static JPanel labelPanel = new JPanel(new GridLayout(5,1));
    public MyMatcher() {
        setName("My Matcher"); // change this to something else if you want
    }

    @Override
    protected Mapping alignTwoNodes(Node source, Node target, alignType typeOfNodes, SimilarityMatrix matrix) throws Exception {
    	resultExplanation = new ExplanationNode("Final Explanation");
    	levenshteinExplanation = new ExplanationNode("Levenshtein Distance");
    	jarowinglerExplanation = new ExplanationNode("Jaro-Wingler metric");
    	absoluteSimilarityExplanation = new ExplanationNode("Absolute Similarity");
    	wordNetStringCombinationExplanation = new ExplanationNode("WordNet- String Combination");
    	wordNetSimilarityExplanation = new ExplanationNode("WordNet Similarity");
    	stringSimilarityExplanation = new ExplanationNode("String Similarity");
    	
    	rows = matrix.getRows();
    	columns = matrix.getColumns();
    	
        FromWordNetUtils wordNetUtils = new FromWordNetUtils();

        Map<String, String> sourceMap = getInputs(source);
        Map<String, String> targetMap = getInputs(target);
        
        
        double stringSimilarity = findStringSimilarity(sourceMap, targetMap);
        
        double finalSimilarity = 0;

//        double dividedSynonymSimilarity = findDividedSynonymSimilarity(source, target);

        /*
         * Checking if values already map exactly
         */
        
        if (    (sourceMap.containsKey("name") && targetMap.containsKey("name") && sourceMap.get("name").equals(targetMap.get("name"))) || 
                (sourceMap.containsKey("name") && targetMap.containsKey("label") && sourceMap.get("name").equals(targetMap.get("label"))) ||
                (sourceMap.containsKey("name") && targetMap.containsKey("comment") && sourceMap.get("name").equals(targetMap.get("comment"))  ) ){
            finalSimilarity = 1.0;
            absoluteSimilarityExplanation.setDescription("Absolute Similarity");
            absoluteSimilarityExplanation.setVal(1.0);
        }

        else if((sourceMap.containsKey("label") && targetMap.containsKey("label") && sourceMap.get("label").equals(targetMap.get("label"))) ||
                (sourceMap.containsKey("label") && targetMap.containsKey("comment") && sourceMap.get("label").equals(targetMap.get("comment"))) ||
                (sourceMap.containsKey("label") && targetMap.containsKey("name") && sourceMap.get("label").equals(targetMap.get("name"))) ||
                
                (sourceMap.containsKey("comment") && targetMap.containsKey("label") && sourceMap.get("comment").equals(targetMap.get("label"))) ||
                (sourceMap.containsKey("comment") && targetMap.containsKey("name") && sourceMap.get("comment").equals(targetMap.get("name")))  ||
                (sourceMap.containsKey("comment") &&targetMap.containsKey("comment") && sourceMap.get("comment").equals(targetMap.get("comment")))) {
                if(finalSimilarity ==0)
                	finalSimilarity = 0.9;
                
                absoluteSimilarityExplanation.setDescription("Absolute Similarity- Inter values");
                absoluteSimilarityExplanation.setVal(0.9);
        }
        /*
         * Computing Synonym Similarity using Wordnet
         */
        if (wordNetUtils.areSynonyms(source.getLabel(), target.getLabel()) || 
                wordNetUtils.areSynonyms(source.getLocalName(), target.getLocalName()) || 
                wordNetUtils.areSynonyms(source.getComment(), target.getComment()) ||
                wordNetUtils.areSynonyms(source.getLabel(), target.getLocalName()) || 
                wordNetUtils.areSynonyms(source.getLabel(), target.getComment()) ||
                wordNetUtils.areSynonyms(source.getComment(), target.getLocalName()) || 
                wordNetUtils.areSynonyms(source.getComment(), target.getLabel()) ||
                wordNetUtils.areSynonyms(source.getLocalName(), target.getLabel())|| 
                wordNetUtils.areSynonyms(source.getLocalName(), target.getComment())) {
            if(finalSimilarity == 0)
            	finalSimilarity = (.80);
            wordNetSimilarityExplanation.setDescription("WordNet Similarity");
            wordNetSimilarityExplanation.setVal(.8);
            
            wordNetStringCombinationExplanation.setVal(.8);
            wordNetStringCombinationExplanation.setCriteria(CombinationCriteria.VOTING);
        } 
        if(finalSimilarity <stringSimilarity){
            finalSimilarity = stringSimilarity;
            
            wordNetStringCombinationExplanation.setVal(stringSimilarity);
            wordNetStringCombinationExplanation.setCriteria(CombinationCriteria.VOTING);
        }
        wordNetStringCombinationExplanation.addChild(wordNetSimilarityExplanation);
        wordNetStringCombinationExplanation.addChild(stringSimilarityExplanation);
        
        resultExplanation.addChild(wordNetStringCombinationExplanation);
        resultExplanation.addChild(absoluteSimilarityExplanation);
        
        resultExplanation.setVal(finalSimilarity);
        resultExplanation.setCriteria(CombinationCriteria.VOTING);
       
       //storing into the appropriate location inside the explanation matrix
        explanationMatrix[source.getIndex()][target.getIndex()] = resultExplanation;
        return new Mapping(source, target, finalSimilarity);
    }
    
    /**
     * Essential combination algorithm to compute the basic string similarity between the source and target. 
     * Three Algorithms are combined- Levenshtein and Jaro-Winkler.
     * @param sourceMap
     * @param targetMap
     * @return
     */

    private double findStringSimilarity(Map<String, String> sourceMap, Map<String, String> targetMap) {
        double levenshteinSimilarity = 0;
        double jarowinglerSimilarity = 0;
        int divisor = 0;

        
        if (sourceMap.containsKey("name") && targetMap.containsKey("name")) {
            levenshteinSimilarity += 2 * levenshteinStringSimilarity(sourceMap.get("name"), targetMap.get("name"));
            divisor += 2;
        }
        if (sourceMap.containsKey("label") && targetMap.containsKey("label")) {
            levenshteinSimilarity += 2 * levenshteinStringSimilarity(sourceMap.get("label"), targetMap.get("label"));
            divisor += 2;
        }
        if (sourceMap.containsKey("comment") && targetMap.containsKey("comment")) {
            levenshteinSimilarity += 2 * levenshteinStringSimilarity(sourceMap.get("comment"), targetMap.get("comment"));
            divisor += 2;
        }

        if (sourceMap.containsKey("name") && targetMap.containsKey("label")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("name"), targetMap.get("label"));
            divisor++;
        }
        if (sourceMap.containsKey("name") && targetMap.containsKey("comment")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("name"), targetMap.get("comment"));
            divisor++;
        }

        if (sourceMap.containsKey("label") && targetMap.containsKey("comment")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("label"), targetMap.get("comment"));
            divisor++;
        }
        if (sourceMap.containsKey("label") && targetMap.containsKey("name")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("label"), targetMap.get("name"));
            divisor++;
        }

        if (sourceMap.containsKey("comment") && targetMap.containsKey("name")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("comment"), targetMap.get("name"));
            divisor++;
        }
        if (sourceMap.containsKey("comment") && targetMap.containsKey("label")) {
            levenshteinSimilarity += levenshteinStringSimilarity(sourceMap.get("comment"), targetMap.get("label"));
            divisor++;
        }

        levenshteinSimilarity = levenshteinSimilarity / divisor;
        
        levenshteinSimilarity = pruneValues(levenshteinSimilarity);
        
        levenshteinExplanation.setVal(levenshteinSimilarity);
        levenshteinExplanation.setDescription("Levenshtein Distance");
        
        divisor = 0;
        if (sourceMap.containsKey("name") && targetMap.containsKey("name")) {
            jarowinglerSimilarity += 2 * jarowinklerStringSimilarity(sourceMap.get("name"), targetMap.get("name"));
            divisor += 2;
        }
        if (sourceMap.containsKey("label") && targetMap.containsKey("label")) {
            jarowinglerSimilarity += 2 * jarowinklerStringSimilarity(sourceMap.get("label"), targetMap.get("label"));
            divisor += 2;
        }
        if (sourceMap.containsKey("comment") && targetMap.containsKey("comment")) {
            jarowinglerSimilarity += 2 * jarowinklerStringSimilarity(sourceMap.get("comment"), targetMap.get("comment"));
            divisor += 2;
        }

        if (sourceMap.containsKey("name") && targetMap.containsKey("label")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("name"), targetMap.get("label"));
            divisor++;
        }
        if (sourceMap.containsKey("name") && targetMap.containsKey("comment")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("name"), targetMap.get("comment"));
            divisor++;
        }

        if (sourceMap.containsKey("label") && targetMap.containsKey("comment")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("label"), targetMap.get("comment"));
            divisor++;
        }
        if (sourceMap.containsKey("label") && targetMap.containsKey("name")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("label"), targetMap.get("name"));
            divisor++;
        }

        if (sourceMap.containsKey("comment") && targetMap.containsKey("name")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("comment"), targetMap.get("name"));
            divisor++;
        }
        if (sourceMap.containsKey("comment") && targetMap.containsKey("label")) {
            jarowinglerSimilarity += jarowinklerStringSimilarity(sourceMap.get("comment"), targetMap.get("label"));
            divisor++;
        }

        jarowinglerSimilarity = jarowinglerSimilarity / divisor;
        jarowinglerSimilarity = pruneValues(jarowinglerSimilarity);
        
        jarowinglerExplanation.setVal(jarowinglerSimilarity);
        jarowinglerExplanation.setDescription("JaroWingler Similarity Metric");
        /*
         * Weighted average of String Similarity
         * 
         * Through iterations, I came to a conclusion that the results are given
         * in the ratio of Levenshtein > Jaro-Wingler So, the
         * corresponding weight-age was given while calculating the mean.
         */
        double finalsimilarity = pruneValues((3 * levenshteinSimilarity + jarowinglerSimilarity) / 4);
        
        stringSimilarityExplanation.addChild(jarowinglerExplanation);
        stringSimilarityExplanation.addChild(levenshteinExplanation);
        
        stringSimilarityExplanation.setVal(finalsimilarity);
        stringSimilarityExplanation.setDescription("Combined String Similarity");
        stringSimilarityExplanation.setCriteria(CombinationCriteria.LWC);
        return finalsimilarity;
    }

    private static double pruneValues(double distance) {
    	distance = (double)Math.round(distance * 100) / 100;
    	return distance;
    }
    
    /**
     * Takes in a node, and returns a Map<String,String> of label, localName and Comment
     */
    private Map<String, String> getInputs(Node node) {
        Map<String, String> stringMap = new HashMap<String, String>();
        if (node.getLabel() != null && !node.getLabel().isEmpty())
            stringMap.put("label", node.getLabel().toLowerCase().replaceAll("\\s", ""));
        if (node.getLocalName() != null && !node.getLocalName().isEmpty())
            stringMap.put("name", node.getLocalName().toLowerCase().replaceAll("\\s", ""));
        if (node.getComment() != null && !node.getComment().isEmpty())
            stringMap.put("comment", node.getComment().toLowerCase().replaceAll("\\s", ""));

        return stringMap;
    }
    
    /**
     * Run MyMatcher on a set of ontologies and print out the Precision, Recall,
     * and F-Measure as compared against the reference alignment.
     * 
     * 
     * to where you saved the Benchmarks dataset.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

    	Ontology source = readOntology("/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/101/onto.rdf"); 
    	Ontology target1 = readOntology("/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/203/onto.rdf"); 
       	Ontology target2 = readOntology("/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/223/onto.rdf"); 
    	Ontology target3 = readOntology("/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/205/onto.rdf"); 
    	Ontology target4 = readOntology("/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/206/onto.rdf"); 
    	
    	String reference1 = "/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/203/refalign.rdf";
    	String reference2 = "/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/223/refalign.rdf";
    	String reference3 = "/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/205/refalign.rdf";
    	String reference4 = "/Users/meriyathomas/Documents/fall2012/DWSemantics/benchmark/206/refalign.rdf";

    	
/*    	Ontology source =  readOntology("/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/101/onto.rdf"); 
        Ontology target1 = readOntology("/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/203/onto.rdf"); 
        Ontology target2 = readOntology("/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/223/onto.rdf"); 
        Ontology target3 = readOntology("/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/205/onto.rdf"); 
        Ontology target4 = readOntology("/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/206/onto.rdf"); 
        
        String reference1 = "/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/203/refalign.rdf";
        String reference2 = "/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/223/refalign.rdf";
        String reference3 = "/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/205/refalign.rdf";
        String reference4 = "/home/jeevs/Dropbox/CS586/ExtractedFiles/AgreementMaker/ontologies/OAEI2010_OWL_RDF/BenchmarkTrack/206/refalign.rdf";
        */
        
        try{
            ontologyMatcher(source, target1, reference1);
/*            ontologyMatcher(source, target2, reference2);
            ontologyMatcher(source, target3, reference3);
            ontologyMatcher(source, target4, reference4);*/
        }catch (Exception e) {
            log.error("Caught exception when running MyMatcher.", e);
        }
    }

    /**
     * 
     * All the ontologyMatching consolidated together under a single function to improve code reuse. 
     */
    private static void ontologyMatcher(Ontology source, Ontology target, String reference) {

        MyMatcher mm = new MyMatcher();

        mm.setSourceOntology(source);
        mm.setTargetOntology(target);
        explanationMatrix = new ExplanationNode[source.getTreeCount()][target.getTreeCount()];
        DefaultMatcherParameters param = new DefaultMatcherParameters();
        param.threshold = 0.6;
        param.maxSourceAlign = 1;
        param.maxTargetAlign = 1;

        mm.setParameters(param);
        try {
            mm.match();
            mm.referenceEvaluation(reference); 
        } catch (Exception e) {
            log.error("Caught exception when running MyMatcher.", e);
        }
		Alignment<Mapping> alignmentMappings = mm.getAlignment();
		final Border blackline;
		blackline = BorderFactory.createLineBorder(Color.BLACK);
		labelPanel.add(new JPanel());
		for(Mapping m:alignmentMappings) {
			System.out.println("Source Node ---> Target Node");
			System.out.println("-----------------------------");
			System.out.println(m.getString());
			System.out.println("Label");
			System.out.println("-----");
			System.out.println(m.getEntity1().getLabel()+" ---> "+m.getEntity2().getLabel());
			System.out.println("Local Name");
			System.out.println("-----------");
			System.out.println(m.getEntity1().getLocalName()+" ---> "+m.getEntity2().getLocalName());
			System.out.println("Comment");			
			System.out.println("-------");
			System.out.println(m.getEntity1().getComment()+" ---> "+m.getEntity2().getComment());
			explanationMatrix[m.getEntity1().getIndex()][m.getEntity2().getIndex()].describeTopDown();
			Layout<ExplanationNode, String> layout = new SubTreeLayout(explanationMatrix[m.getEntity1().getIndex()][m.getEntity2().getIndex()].tree);
			layout.setSize(new Dimension(200,350)); // sets the initial size of the space
			// The BasicVisualizationServer<V,E> is parameterized by the edge types
			//BasicVisualizationServer<ExplanationNode,String> vv =
			//new BasicVisualizationServer<ExplanationNode,String>(layout);
			final VisualizationViewer<ExplanationNode, String> vv = 
			    		new VisualizationViewer<ExplanationNode, String>(layout);
			final JFrame frame = new JFrame("Simple Graph View");
			vv.setPreferredSize(new Dimension(300,400)); //Sets the viewing area size
			Transformer<ExplanationNode, String> labelTransformer = new Transformer<ExplanationNode,String>() {

				@Override
				public String transform(ExplanationNode node) {
					return String.valueOf(node.getVal());
				}
				
			};
			final Transformer<ExplanationNode,Paint> vertexPaint = new Transformer<ExplanationNode,Paint>() {
				public Paint transform(ExplanationNode i) {
				return Color.GREEN;
				}
			};
			
			vv.getRenderContext().setVertexLabelTransformer(labelTransformer);
			vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
			vv.getRenderer().getVertexLabelRenderer().setPosition(Position.E);
			Transformer<ExplanationNode, String> toolTipTransformer = new Transformer<ExplanationNode, String>() {

				@Override
				public String transform(ExplanationNode node) {
					String nodeDesc = node.getDescription()+": "+node.getVal();
					if(!node.getCriteria().toString().equals(CombinationCriteria.NOTDEFINED.toString())) {
						nodeDesc+="\nChlidren joined by: "+node.getCriteria();
					}
					return nodeDesc;
				}
				
			};
			vv.setVertexToolTipTransformer(toolTipTransformer );
			
			final DefaultModalGraphMouse<ExplanationNode, String> graphMouse = new DefaultModalGraphMouse<ExplanationNode, String>();
		
			graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
			GraphMouseListener<ExplanationNode> mygel = new GraphMouseListener<ExplanationNode>() {

				private ExplanationNode previousNode;

				@Override
				public void graphClicked(final ExplanationNode node, MouseEvent me) {
					vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

					if(me.getButton() == MouseEvent.BUTTON1) {
						if(previousNode != null) {
							vv.getPickedVertexState().pick(previousNode, false);
						}
						vv.getPickedVertexState().pick(node, true);
						 vv.getRenderContext().setVertexFillPaintTransformer(new VertexPaintTransformer(vv.getPickedVertexState()));

						System.out.println("left click");
						System.out.println("Clicked " + node.getDescription());	
						if(nodeDescriptionValue != null) {
							labelPanel.remove(nodeDescriptionValue);
						}
						nodeDescriptionValue = new JLabel("Description: "+node.getDescription());
						nodeDescriptionValue.setOpaque(true);
						labelPanel.add(nodeDescriptionValue);
						if(criteriaLabel != null) {
							labelPanel.remove(criteriaLabel);
						}
						if(!node.getCriteria().toString().equals(CombinationCriteria.NOTDEFINED.toString())) {
							criteriaLabel = new JLabel("Method: "+node.getCriteria().toString());
							criteriaLabel.setOpaque(true);
							labelPanel.add(criteriaLabel);
						} else {
							criteriaLabel = new JLabel("Reached End node!");
							criteriaLabel.setOpaque(true);
							labelPanel.add(criteriaLabel);							
						}
						if(valueLabel != null) {
							labelPanel.remove(valueLabel);
						}
						valueLabel = new JLabel("Value: "+node.getVal());
						valueLabel.setOpaque(true);
						labelPanel.add(valueLabel);
						labelPanel.setBorder(blackline);
						panel.add(labelPanel,BorderLayout.NORTH);
						frame.pack();
						frame.setVisible(true);
					} else if(me.getButton() == MouseEvent.BUTTON3) {
						System.out.println("right click");
						System.out.println("Clicked " + node.getDescription());		
						
						PopupVertexEdgeMenuMousePlugin<ExplanationNode, String> myPlugin = new PopupVertexEdgeMenuMousePlugin<ExplanationNode,String>();
						JPopupMenu vertexMenu = new MyMouseMenus.VertexMenu();
						myPlugin.setVertexPopup(vertexMenu);
						graphMouse.add(myPlugin);
					}
					previousNode = node;
				}

				@Override
				public void graphPressed(ExplanationNode arg0, MouseEvent arg1) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void graphReleased(ExplanationNode arg0, MouseEvent arg1) {
					// TODO Auto-generated method stub
					
				}

				
				
			};
		   // graphMouse.add(popup);
			vv.setGraphMouse(graphMouse);
			vv.addKeyListener(graphMouse.getModeKeyListener());
			vv.addMouseListener(new MouseListenerTranslator<ExplanationNode, String>(mygel, vv));
			Container content = frame.getContentPane();
			//content.setLayout(springLayout);
			panel.add(vv,BorderLayout.CENTER);
			content.add(panel);
			frame.pack();
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			break;
		}

		// Let's see what we have. Note the nice output from the
		// SparseMultigraph<V,E> toString() method
		// Note that we can use the same nodes and edges in two
		// The Layout<V, E> is parameterized by the vertex and edge types

        
        
    }

    /**
     * Levenshtein String Similarity.
     */
    private static double levenshteinStringSimilarity(String sourceString, String targetString) {

        LevenshteinEditDistance levenshtein = new LevenshteinEditDistance();
      //  levenshtein.calculate();
        return levenshtein.getSimilarity(sourceString, targetString);
    }

    /**
     * Hamming String Similarity.
     */
    private static double hammingStringSimilarity(String sourceString, String targetString) {

        Hamming hammingSim = new Hamming(sourceString, targetString);
        hammingSim.calculate();
        return hammingSim.getSimilarity().doubleValue();
    }

    /**
     * Jaro-Winkler String Similarity.
     */
    private static double jarowinklerStringSimilarity(String sourceString, String targetString) {
    	JaroWinklerSim jaro = new JaroWinklerSim();
   //     jaro.calculate();
        return jaro.getSimilarity(sourceString, targetString);
    }

    /**
     * Compute the Precision, Recall and F-Measure for MyMatcher given a
     * reference alignment.
     * 
     * @param pathToReferenceAlignment
     *            The path to the reference alignment file. To be safe, you
     *            should provide the absolute file path.
     * @throws Exception
     *             If bad things happen.
     */
    private void referenceEvaluation(String pathToReferenceAlignment) throws Exception {
        // Run the reference alignment matcher to get the list of mappings in
        // the reference alignment file
        ReferenceAlignmentMatcher refMatcher = (ReferenceAlignmentMatcher) MatcherFactory.getMatcherInstance(
                MatchersRegistry.ImportAlignment, 0);

        // these parameters are equivalent to the ones in the graphical
        // interface
        ReferenceAlignmentParameters parameters = new ReferenceAlignmentParameters();
        parameters.fileName = pathToReferenceAlignment;
        parameters.format = ReferenceAlignmentMatcher.OAEI;
        parameters.onlyEquivalence = false;
        parameters.skipClasses = false;
        parameters.skipProperties = false;

        // When working with sub-superclass relations the cardinality is always
        // ANY to ANY
        if (!parameters.onlyEquivalence) {
            parameters.maxSourceAlign = AbstractMatcher.ANY_INT;
            parameters.maxTargetAlign = AbstractMatcher.ANY_INT;
        }

        refMatcher.setParam(parameters);

        // set the source and target ontologies
        refMatcher.setSourceOntology(getSourceOntology());
        refMatcher.setTargetOntology(getTargetOntology());

        // load the reference alignment
        refMatcher.match();

        Alignment<Mapping> referenceSet;
        if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
            referenceSet = refMatcher.getAlignment(); // class + properties
        } else if (refMatcher.areClassesAligned()) {
            referenceSet = refMatcher.getClassAlignmentSet();
        } else if (refMatcher.arePropertiesAligned()) {
            referenceSet = refMatcher.getPropertyAlignmentSet();
        } else {
            // empty set? -- this should not happen
            referenceSet = new Alignment<Mapping>(Ontology.ID_NONE, Ontology.ID_NONE);
        }

        // the alignment which we will evaluate
        Alignment<Mapping> myAlignment;

        if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
            myAlignment = getAlignment();
        } else if (refMatcher.areClassesAligned()) {
            myAlignment = getClassAlignmentSet();
        } else if (refMatcher.arePropertiesAligned()) {
            myAlignment = getPropertyAlignmentSet();
        } else {
            myAlignment = new Alignment<Mapping>(Ontology.ID_NONE, Ontology.ID_NONE); // empty
        }

        // use the ReferenceEvaluator to actually compute the metrics
        ReferenceEvaluationData rd = ReferenceEvaluator.compare(myAlignment, referenceSet);

        // optional
        setRefEvaluation(rd);

        // output the report
        StringBuilder report = new StringBuilder();
        report.append("Reference Evaluation Complete\n\n").append(getName()).append("\n\n").append(rd.getReport()).append("\n")
                .append(rd.getErrorAlignments());

        log.info(report);

        // use system out if you don't see the log4j output
        // System.out.println(report);
    }

    /**
     * Use the Jena API to return the label of a node.
     */
    private String getLabel(Node n) {

        // get the Jena Resource of this node
        Resource res = n.getResource();

        // get the label of this node (returns the whole triple which defines
        // the label)
        Statement labelTriple = res.getProperty(RDFS.label);
        if (labelTriple == null) {
            return null; // no label defined
        }

        // extract the object of the triple (which is the label)
        RDFNode object = labelTriple.getObject();
        Literal label = object.asLiteral();

        // get the string value of the literal
        return label.getString();
    }
    
	/**
	 * Method to read in an OWL ontology.
	 * @param sourceOntFile Path of the OWL ontology file.
	 * @return Ontology data structure.
	 */
	private static Ontology readOntology(String ontoURI) {
		
		Ontology onto = OntoTreeBuilder.loadOntology(ontoURI, OntologyLanguage.RDFS, OntologySyntax.RDFXML);
//		OntoTreeBuilder ontoBuilder = new OntoTreeBuilder( sourceOntFile,
//				Ontology.SOURCE, GlobalStaticVariables.LANG_OWL, 
//				GlobalStaticVariables.SYNTAX_RDFXML, 
//				false, false);
//		
//		ontoBuilder.build(OntoTreeBuilder.Profile.noReasoner);  // read in the ontology file, create the Ontology object.
		
		return onto;
	}

}