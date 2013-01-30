package am.app.mappingEngine;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;

import am.app.Core;
import am.app.osgi.MatcherNotFoundException;
import am.userInterface.Colors;

public class MatcherFactory {
	
	/**
	 * When adding a matcher add the line names[NEWINDEX] = "My name"; Name shouldn't be too long but at the same time should be a user clear name;
	 * @return the list of matchers names ordered by the indexes of each matcher, this is the same list shown in the AgreementMaker combo box, so the selectedIndex of the combobox must correspond to a valid matcher
	 */
	public static String[] getMatcherNames() {
		EnumSet<MatchersRegistry> matchers = EnumSet.allOf(MatchersRegistry.class);
		
		Object[] matchersArray = matchers.toArray();
		String[] matchersList = new String[matchersArray.length];
		for( int i = 0; i < matchersArray.length; i++ ) {
			matchersList[i] = ((MatchersRegistry) matchersArray[i]).getMatcherName();
		}
		
		return matchersList;
	}

	/**
	 * When adding a matcher add the line names[NEWINDEX] = "My name"; Name shouldn't be too long but at the same time should be a user clear name;
	 * @return the list of matchers names ordered by the indexes of each matcher, this is the same list shown in the AgreementMaker combo box, so the selectedIndex of the combobox must correspond to a valid matcher
	 */
	public static AbstractMatcher[] getMatcherComboList() {
		List<AbstractMatcher> matchers = Core.getInstance().getRegistry().getMatchers();
		return matchers.toArray(new AbstractMatcher[0]);
	}
	
	
	/**
	 * @param matcherClass The matcher's class name.
	 */
	public static AbstractMatcher getMatcherInstance( String matcherClass ) {
		try {
			return Core.getInstance().getRegistry().getMatcherByClass(matcherClass);
		}
		catch (MatcherNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static AbstractMatcher getMatcherInstance(Class<? extends AbstractMatcher> clazz) 
			throws MatcherNotFoundException {
		return Core.getInstance().getRegistry().getMatcherByClass(clazz);
	}
	
	/**
	 * Returns the MatchersRegistry entry of the matcher with the given class.
	 * @param name The Class object representing the Class of the matcher.
	 * @return The MatchersRegistry entry for the matcher.  If no matchers are found to equal the name, it returns null.
	 */
	public static MatchersRegistry getMatchersRegistryEntry( Class<? extends AbstractMatcher> cls ) {

		EnumSet<MatchersRegistry> matchers = EnumSet.allOf(MatchersRegistry.class);
		
		// Alternate suggestion: Do this with an iterator() instead of toArray and a for-loop.
		Object[] matchersArray = matchers.toArray();
		for( int i = 0; i < matchersArray.length; i++ ) {
			if( ((MatchersRegistry) matchersArray[i]).getMatcherClass().equals(cls.getName())  ) {
				return (MatchersRegistry) matchersArray[i];
			}
		}
		return null;
	}
	
	private static Color getColorFromIndex(int instanceIndex) {
		// TODO there should be an array of predefined colors
		int arrayIndex = (int) (instanceIndex % Colors.matchersColors.length); //this is the module operation, we need to do this because we may have more matchers then the possible colors in the array
		return Colors.matchersColors[arrayIndex];
	}

	public static boolean isTheUserMatcher(AbstractMatcher toBeDeleted) {
		return toBeDeleted.getRegistryEntry() == MatchersRegistry.UserManual && toBeDeleted.getIndex() == 0;
	}
	
	

}