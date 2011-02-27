package am.app.mappingEngine;

import java.util.ArrayList;

import am.app.ontology.Node;
import am.app.ontology.Ontology;

/**
 * An Alignment is a set of mappings between concepts from two ontologies.
 * An Alignment is meant to represent a complete set of mappings between the ontologies.
 *
 * @param <E>  This represents a Mapping object.
 */
public class Alignment<E extends Mapping> extends ArrayList<E>
{
	private static final long serialVersionUID = -8090732896473529999L;
	

    // adds all the alignments in the set a, but checking for duplicates, making sure it doesn't add duplicate alignments
    public void addAllNoDuplicate(Alignment<E> alignment)
    {
    	if( alignment == null ) return;
    	for( E mapping : alignment ) if( !contains(mapping) ) add(mapping);        
    }    
    
    public double getSimilarity(Node left, Node right)
    {
        E align = contains(left, right);
        if (align == null) return 0.0d;
        return align.getSimilarity();
    }

    public void setSimilarity(Node left, Node right, double sim)
    {
    	E mapping = contains(left, right);
    	if (mapping != null) mapping.setSimilarity(sim);
    }

    public E contains(Node left, Node right)
    {
    	for( E mapping : this ) if( mapping.getEntity1().equals(left) && mapping.getEntity2().equals(right) ) return mapping;
    	return null;
    }
    
    public E contains(Node nod, int ontType)
    {
    	for( E mapping : this ) {
    		if(ontType == Ontology.SOURCE && mapping.getEntity1().equals(nod) ) return mapping;
    		if(ontType == Ontology.TARGET && mapping.getEntity2().equals(nod) ) return mapping;
    	}
    	return null;
    }
    

    public boolean contains( int row, int col ) {
    	for( E mapping : this ) if( mapping.getSourceKey() == row && mapping.getTargetKey() == col ) return true;
    	return false;
    }

    public void cut(double threshold)
    {
        for (int i = size()-1; i >= 0; i--)
        	if (get(i) == null || get(i).getSimilarity() <= threshold) remove(i);
    }

    
    public int size(double threshold)
    {
        int count = 0;
        for( E mapping : this )	if( mapping.getSimilarity() > threshold ) count++;
        return count;
    }

    public String getStringList() {
		String result = "";
		E a;
		for(int i = 0; i < size(); i++) {
			a = get(i);
			result += a.getString();
			if(i == size()-1)
				result+= "\n";
		}
		return result;
	}
    
   
/*    *//** ****************** Serialization methods *******************//*
	
	  *//**
	   * readObject: gets the state of the object.
	   * @author michele
	   *//*
	  protected Alignment<Mapping> readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
		  Alignment<Mapping> thisClass = (Alignment<Mapping>) in.readObject();
		  in.close();
		  return thisClass;
	  }

	   *//**
	    * writeObject: saves the state of the object.
	    * @author michele
	    *//*
	  protected void writeObject(ObjectOutputStream out) throws IOException {
		  out.writeObject(this);
		  out.close();
	  }

	  protected void testSerialization(){
		  Alignment<Mapping> as = null;
			try {
				writeObject(new ObjectOutputStream(new FileOutputStream("testFile")));
				as = readObject(new ObjectInputStream(new FileInputStream("testFile")));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println(as.getStringList());
	  }*/
}
