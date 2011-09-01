package am.app.mappingEngine.manualMatcher;


import am.app.Core;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.AbstractParameters;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.MatcherFeature;
import am.app.mappingEngine.MatchersRegistry;
import am.app.mappingEngine.SimilarityMatrix;
import am.app.mappingEngine.Mapping.MappingRelation;
import am.app.mappingEngine.similarityMatrix.ArraySimilarityMatrix;
import am.app.mappingEngine.similarityMatrix.SparseMatrix;
import am.app.ontology.Node;
import am.app.ontology.Ontology;
import am.userInterface.Colors;

/**This class is used to represent the user manual matching
 * this matching will not be part of the matcher list in the combo box
 * so the user won't be able to select this matcher 
 * the matcher is generated by the system on startup with 0 alignments found
 * The user can manually add alignments to this one selecting this matcher from the table and adding the relation on canvas
 * the user can also add manually alignments to other matchers, but if this matcher is not selected matchings will not be added to the user one
 *
 */
public class UserManualMatcher extends AbstractMatcher {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6353884415922188426L;

	public UserManualMatcher() {
		//super(); VERY IMPORTANT NOT CALL SUPER FROM THIS MATCHER BECAUSE THIS MATCHER IS INVOKED BEFORE THE ONTOLOGY LOADING
		
		//maybe this first 3 lines are not needed anymore after the matcherregistry change
		index = 0;
		registryEntry = MatchersRegistry.UserManual;
		color = Colors.matchersColors[0];
		
		param = new AbstractParameters();
		param.storeProvenance = true;
		addFeature(MatcherFeature.MAPPING_PROVENANCE);
		isAutomatic = false;
		needsParam = false;
		isShown = true;
		modifiedByUser = false;
		setThreshold(0.01); //the minimum value != 0 in the threshold list;
		setMaxSourceAlign(ANY_INT);
		setMaxTargetAlign(ANY_INT);
		alignClass = true;
		alignProp = true;
		minInputMatchers = 0;
		maxInputMatchers = 0;
		relation = MappingRelation.EQUIVALENCE;
		setOptimized(false);
	}
	
	@Override
	protected void beforeAlignOperations() throws Exception {
		sourceOntology = Core.getInstance().getSourceOntology();  // we need to set the source ontology
		targetOntology = Core.getInstance().getTargetOntology();  // we need to set the target ontology
		super.beforeAlignOperations();
	}
	
	/**Set all alignment sim to 0*/
	@Override public void align() throws Exception {
		
		classesMatrix=new SparseMatrix(alignType.aligningProperties, relation);
		
		//classesMatrix = new ArraySimilarityMatrix(sourceOntology.getClassesList().size(), targetOntology.getClassesList().size(), 
		//		alignType.aligningClasses, relation);
		
		propertiesMatrix=new SparseMatrix(alignType.aligningProperties, relation);
		//propertiesMatrix = new ArraySimilarityMatrix(sourceOntology.getPropertiesList().size(), targetOntology.getPropertiesList().size(), 
		//		alignType.aligningProperties, relation);
		
		return;
	}
    
	/**These 3 methods are invoked any time the user select a matcher in the matcherscombobox. Usually developers don't have to override these methods unless their default values are different from these.*/
	@Override public double getDefaultThreshold() { return 0.01; }
	@Override public int getDefaultMaxSourceRelations() { return ANY_INT; }
	@Override public int getDefaultMaxTargetRelations() {	return ANY_INT;	}

	@Override
	public String getReport() {
		String result =  "An empty matching has been created!\n\n";
		result += "Select the matching in the control panel table \nbefore adding manual mappings to it.\n";
		return result;
	}
}

