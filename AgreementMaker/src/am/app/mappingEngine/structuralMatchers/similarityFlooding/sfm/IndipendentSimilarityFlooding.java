/**
 * 
 */
package am.app.mappingEngine.structuralMatchers.similarityFlooding.sfm;

import com.hp.hpl.jena.rdf.model.RDFNode;

import am.app.mappingEngine.SimilarityMatrix;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.FullGraphMatcher;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.SimilarityFloodingMatcherParameters;
import am.app.mappingEngine.structuralMatchers.similarityFlooding.utils.PCGVertexData;
import am.utility.Pair;

/**
 * @author michele
 *
 */
public class IndipendentSimilarityFlooding extends FullGraphMatcher {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7551045342225723696L;

	/**
	 * 
	 */
	public IndipendentSimilarityFlooding() {
		super();
		minInputMatchers = 0;
		maxInputMatchers = 0;
	}

	/**
	 * @param params_new
	 */
	public IndipendentSimilarityFlooding(SimilarityFloodingMatcherParameters params_new) {
		super(params_new);
		minInputMatchers = 0;
		maxInputMatchers = 0;
	}
	
	/**
	 *
	 */
	@Override
	protected void loadSimilarityMatrices(){
		// load classesMatrix
		classesMatrix = new SimilarityMatrix(sourceOntology.getClassesList().size(),
				targetOntology.getClassesList().size(),
				alignType.aligningClasses);
		// load propertiesMatrix
		propertiesMatrix = new SimilarityMatrix(sourceOntology.getPropertiesList().size(),
				targetOntology.getPropertiesList().size(),
				alignType.aligningProperties);
	}

	/**
	 *
	 */
	@Override
	protected PCGVertexData selectInput(Pair<RDFNode, RDFNode> pair) {
//		return new PCGVertexData( pair );
		return null;
	}

}