package am.userInterface.canvas2.nodes;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Iterator;

import am.app.Core;
import am.userInterface.canvas2.graphical.GraphicalData;
import am.userInterface.canvas2.graphical.MappingData;
import am.userInterface.canvas2.graphical.GraphicalData.NodeType;
import am.userInterface.canvas2.graphical.MappingData.MappingType;
import am.userInterface.canvas2.layouts.LegacyLayout;
import am.userInterface.canvas2.utility.Canvas2Edge;
import am.userInterface.canvas2.utility.Canvas2Vertex;
import am.utility.DirectedGraphEdge;

public class LegacyMapping extends Canvas2Edge {

	Line2D.Double line; // the line formed by the mapping
	
	public LegacyMapping(Canvas2Vertex orig, Canvas2Vertex dest, GraphicalData o, int matcherID, String label ) {
		super(orig, dest, o);
		
		GraphicalData source = orig.getObject();
		GraphicalData target = dest.getObject();
		
		int sourceX = source.x + source.width;
		int sourceY = source.y + (source.height / 2);
		
		int targetX = target.x + LegacyNode.circlePadding;
		int targetY = target.y + (target.height / 2); 
		
		int width = targetX - sourceX;
		int height = targetY - sourceY;
		
		MappingType mappingType = MappingType.NOT_SET; 
		if( source.type == GraphicalData.NodeType.CLASS_NODE || source.type == GraphicalData.NodeType.CLASSES_ROOT ) {
			mappingType = MappingType.ALIGNING_CLASSES;
		} else if( source.type == GraphicalData.NodeType.PROPERTY_NODE || source.type == GraphicalData.NodeType.PROPERTIES_ROOT ) {
			mappingType = MappingType.ALIGNING_PROPERTIES;
		}
		
		MappingData data = new MappingData( sourceX, sourceY, width, height, source.layout, 
											source.r, target.r, source.ontologyID, target.ontologyID, matcherID, mappingType );
		data.setLabel(label);
		
		setObject( data );
		
		Point2D.Double startPoint = new Point2D.Double(d.x, d.y);
		Point2D.Double endPoint =   new Point2D.Double(d.x+d.width, d.y+d.height);
		
		line = new Line2D.Double( startPoint, endPoint );
		
	}
	
	// Used when the line endpoints have changed.
	@Override
	public void updateBounds( int x, int y, int width, int height ) { 
		super.updateBounds(x, y, width, height);
		
		// recreate the line
		Point2D.Double startPoint = new Point2D.Double(d.x, d.y);
		Point2D.Double endPoint =   new Point2D.Double(d.x+d.width, d.y+d.height);
		
		line = new Line2D.Double( startPoint, endPoint );
	};
	
	// without any parameters, the bounds are updated from the source and target vertices
	public void updateBounds() {
		GraphicalData source = getOrigin().getObject();
		GraphicalData target = getDestination().getObject();

		int sourceX = source.x + source.width;
		int sourceY = source.y + (source.height / 2);
		
		int targetX = target.x + LegacyNode.circlePadding;
		int targetY = target.y + (target.height / 2); 
		
		int width = targetX - sourceX;
		int height = targetY - sourceY;

		updateBounds( sourceX, sourceY, width, height);
		
	}
	
	@Override
	public void draw(Graphics g ) {
		g.setColor( ((MappingData)d).color );

		// get the number of parallel mappings that are currently visible and drawn before this mapping
		int numberOfPreviousMappings = 0;
		Iterator<DirectedGraphEdge<GraphicalData>> edgeOutIter = getOrigin().edgesOut();
		while( edgeOutIter.hasNext() ) {
			Canvas2Edge edge = (Canvas2Edge) edgeOutIter.next();
			if( edge.getObject().type == NodeType.MAPPING ) {
				// check to see if it's this mapping.  if it is this one, stop
				if( this == edge ) { break; }

				// we have a mapping, check to see if it's parallel to this one, but only if we're in the general view.
				// if we're in the single mapping view, space out the mappings even if they're not parallel
				if( ((LegacyLayout)d.layout).isSingleMappingView() ) {
					numberOfPreviousMappings++;
				} else {
					if( this.getOrigin() == edge.getOrigin() && this.getDestination() == edge.getDestination() ) {
						// we have another parallel mapping, and that mapping is iterated before this mapping
						numberOfPreviousMappings++;
					}
				}
	
			}
		}
		
		
		Core.getInstance();
		// ok, now draw the mapping correctly
		FontMetrics fontMetrics = Core.getUI().getCanvas().getFontMetrics(d.font); // need this to calculate the width of the label
		
		// get the width of the label
		int labelWidth = 0;
		if( ((MappingData)d).label != null ) {
			labelWidth = fontMetrics.stringWidth(((MappingData)d).label);
		}
		
		// draw 3 lines  (like shown below)
		/* 
		 * 
		 * ( Node1 ) 
		 *          \
		 *           \     label
		 *            \.___________.< midpoint2
		 *            ^             \
		 *        midpoint1          \
		 *                            \
		 *                             ( Node2 )
		 */
		
		int midpoint1_X, midpoint2_X, midpoint_Y;
		midpoint1_X = d.x+(d.width/2)-(labelWidth/2);
		midpoint2_X = d.x+(d.width/2)+(labelWidth/2);
		midpoint_Y  = d.y+(d.height/2) + numberOfPreviousMappings*(fontMetrics.getHeight()); // this is where we take care not to overlap with parallel mappings
		
		g.drawLine( d.x, d.y, midpoint1_X, midpoint_Y);
		g.drawLine( midpoint1_X, midpoint_Y, midpoint2_X, midpoint_Y);
		g.drawLine( midpoint2_X, midpoint_Y, d.x+d.width, d.y+d.height);
		
		if( ((MappingData)d).label != null )
			g.drawString( ((MappingData)d).label, midpoint1_X, midpoint_Y-1);
	}
	
	@Override
	public boolean isVisible(Rectangle bounds) {
		//if( graph != null ) { if( !graph.visible ) return false; }  // if the graph isn't visible, any of its elements aren't visible either
		if( d == null ) return false; // no GraphicalData -> no visibility
		
		if( !d.visible ) return false;  // if our node was set to be invisible, so no need to check bounds
		
		if( bounds.getBounds2D().intersectsLine(line) ) return true; // if the line formed by the mapping intersects the viewport rectangle
		
		return false;
		
	}
	
	@Override
	public String toString() {
		MappingData data = (MappingData)d;
		String name1 = Integer.toString(data.ontologyID) + ":" + data.r.getLocalName();
		String name2 = Integer.toString(data.ontologyID2) + ":" + data.r2.getLocalName();
		return name1 + " - " + name2;
	}

}
