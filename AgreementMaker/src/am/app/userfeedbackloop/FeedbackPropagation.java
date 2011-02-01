package am.app.userfeedbackloop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.EventListenerList;

public abstract class FeedbackPropagation {
	EventListenerList listeners;  // list of listeners for this class
	
	public FeedbackPropagation() {
		listeners = new EventListenerList();
	}
	
	public abstract void propagate( UserFeedback feedback );
	
	public void addActionListener( ActionListener l ) {
		listeners.add(ActionListener.class, l);
	}
	
	/**
	 * This method fires an action event.
	 * @param e Represents the action that was performed.
	 */
	protected void fireEvent( ActionEvent e ) {
		ActionListener[] actionListeners = listeners.getListeners(ActionListener.class);
		
		for( int i = actionListeners.length-1; i > 0; i-- ) {
			actionListeners[i].actionPerformed(e);
		}
	}
	
}
