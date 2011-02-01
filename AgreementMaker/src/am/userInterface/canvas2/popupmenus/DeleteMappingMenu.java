package am.userInterface.canvas2.popupmenus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import am.app.Core;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.AbstractMatcher.alignType;
import am.app.ontology.Node;
import am.app.ontology.Ontology;
import am.userInterface.canvas2.Canvas2;
import am.userInterface.canvas2.graphical.MappingData;
import am.userInterface.canvas2.graphical.MappingData.MappingType;
import am.userInterface.canvas2.nodes.LegacyMapping;
import am.userInterface.canvas2.utility.Canvas2Edge;
import am.userInterface.canvas2.utility.Canvas2Layout;
import am.userInterface.canvas2.utility.Canvas2Vertex;
import am.userInterface.canvas2.utility.CanvasGraph;

public class DeleteMappingMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = 3231794310838624406L;
	private ArrayList<LegacyMapping> mappings;

	Canvas2Layout layout;
	
	public DeleteMappingMenu( Canvas2Layout lay, ArrayList<LegacyMapping> m ) {
		super();
		
		layout = lay;
		mappings = m;
		
		// menu layout - Jan 29, 2010 Cosmin
		//  1. Delete Mappings (selected matchers) (item)
		//  2. Delete Individual Mappings   (menu)  TODO
		//     <autogenerated menu>
		//  --------------------
		//  3. Provenance for each matcher
		//  3. Delete All Mappings (item)
		
		JMenu mDeleteMappings = new JMenu("Delete Mappings");
		JMenu mProvenance = new JMenu("Provenance");
		
		JMenuItem miDeleteAll = new JMenuItem("Delete All Mappings");
        miDeleteAll.setActionCommand("DELETE_ALL");

		
		if( mappings.size() == 0 ) {
			JMenuItem noMappings = new JMenuItem("No mappings for this node.");
			noMappings.setEnabled(false);
			miDeleteAll.setEnabled(false);
			
			mDeleteMappings.add(noMappings);
			
			
		} else {
			
			// build the mappings menu, grouped by matchers
			ArrayList<AbstractMatcher> matchers = Core.getInstance().getMatcherInstances();
			Iterator<AbstractMatcher> matcherIter = matchers.iterator();
			while( matcherIter.hasNext() ) {
				AbstractMatcher matcher = matcherIter.next();
				
				int matcherID = matcher.getID();
				
				JMenu mMatcher = new JMenu(matcher.getName().name() + "(#" + Integer.toString(matcherID) + ")");
				JMenu mMatcherProv = new JMenu(matcher.getName().name() + "(#" + Integer.toString(matcherID) + ")");
				
				boolean hasMappings = false, hasProvenance = false;
				for( int i = 0; i < mappings.size(); i++ ) {
					MappingData currentData = (MappingData) mappings.get(i).getObject();
					int currentID = currentData.matcherID;
					if( currentID == matcherID ) {
						// we found a mapping of this matcher, add the mapping to the delete menu
						LegacyMapping mapping = mappings.get(i);
						JMenuItem miMapping = new JMenuItem( mapping.toString() );
								  miMapping.setActionCommand(Integer.toString(i)); // this is a hack, to be fixed later. - Cosmin (2/14/2010)

						miMapping.addActionListener(this);
						mMatcher.add(miMapping);
						hasMappings = true;
						
						// check for provenance information for this matcher
						Mapping map = ((MappingData)mapping.getObject()).alignment;
						if( map.getProvenance() != null ) {
							hasProvenance = true;
							JMenuItem miMappingProvenance = new JMenuItem( map.getProvenance() );
							mMatcherProv.add(miMappingProvenance);
						} else {
							mMatcherProv.setEnabled(false);
						}
					}
				}
				
				if( hasMappings ) mDeleteMappings.add(mMatcher); 
				if( hasProvenance) mProvenance.add(mMatcherProv);
				
				
			}
		}
		
		add(mDeleteMappings);        
		addSeparator();
		add(mProvenance);
		
		// TODO: add(miDeleteAll);

	}

	public void actionPerformed(ActionEvent e) {

		String actionCommand = e.getActionCommand();
		
		if( actionCommand == "DELETE_ALL" ) {
			// delete all the mappings associated with the node
			for( int i = 0; i < mappings.size(); i++ ) {
				// maybe popup a confirmation dialog? TODO
				removeByIndex(i);
			}
		} else {
			// the action command is the index of the mapping we want to delete
			int index = Integer.parseInt(actionCommand);
			if( index < 0 || index >= mappings.size() ) {
				// invalid index, do nothing.
				return;
			}
			removeByIndex( index );
			
		}
		
	}
	
	private void removeByIndex( int index ) {
		LegacyMapping mapping = mappings.get(index);
		MappingData data = (MappingData) mapping.getObject();
		
		AbstractMatcher matcher = Core.getInstance().getMatcherByID( data.matcherID );
		
		
		alignType mappingType = null;
		if( data.mappingType == MappingType.ALIGNING_CLASSES ) mappingType = alignType.aligningClasses;
		else if( data.mappingType == MappingType.ALIGNING_PROPERTIES ) mappingType = alignType.aligningProperties;
		
		assert (mappingType != null); // to protect our code
		
		// we need to get the nodes associated with this mapping
		Ontology o1 = Core.getInstance().getOntologyByID( data.ontologyID );
		Node n1 = null;
		try {
			n1 = o1.getNodefromOntResource( data.r, mappingType );
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		assert (n1 != null);
		
		Ontology o2 = Core.getInstance().getOntologyByID( data.ontologyID2 );
		Node n2 = null;
		try {
			n2 = o2.getNodefromOntResource( data.r2, mappingType );
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		assert (n2 != null );
		
		// remove the mapping from the matcher.
		matcher.removeMapping( n1, n2, mappingType);
		
		// ok, now that we removed the mapping from the matcher,
		// remove the LegacyMapping from the CanvasGraph 
		
		Canvas2Vertex origin = (Canvas2Vertex) mapping.getOrigin();
		Canvas2Vertex destination = (Canvas2Vertex) mapping.getDestination();
		
		origin.removeOutEdge(mapping);
		destination.removeInEdge(mapping);
		
		Canvas2 canvas = (Canvas2) Core.getUI().getCanvas();
		CanvasGraph gr = canvas.getMatcherGraph( data.matcherID );
		
		assert (gr != null);
		gr.removeEdge( mapping);

		// repaint
		Core.getUI().redisplayCanvas();
	}
	
	
}
