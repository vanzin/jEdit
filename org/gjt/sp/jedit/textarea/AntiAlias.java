package org.gjt.sp.jedit.textarea;

import org.gjt.sp.jedit.jEdit;


/**
 * Class for representing AntiAlias values.
 * Currently the following are modes are supported:
 * 	none
 * 	standard
 * 	lcd subpixel (JDK 1.6 only)
 * 
 * @author ezust
 * @since jedit 4.3pre4
 */
public class AntiAlias extends Object
{
	public static final Object none = "none";
	public static final Object standard = "standard";
	public static final Object subpixel = "subpixel";

	public static final Object comboChoices[] = new Object[] {
		none, standard, subpixel
	};
	
	public AntiAlias() {
		fromString(jEdit.getProperty("view.antiAlias", "none"));
	}
	
	public AntiAlias(int val) {
		m_val = val;
	}
	
	public AntiAlias(String v) {
		fromString(v);
	}

	public boolean equals(Object other) {
		return toString().equals(other.toString());
		
	}
	public void fromString(String v) {
		for (int i=0; i<comboChoices.length; ++i) {
			if (comboChoices[i].equals(v)) {
				m_val = i;
			}
		}
	}

	public String toString() {
		return comboChoices[m_val].toString();
	}
	public int val() {return m_val;}
	

	private int m_val=0;
}
