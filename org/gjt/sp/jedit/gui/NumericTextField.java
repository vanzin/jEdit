/*
 * NumericTextField.java - A TextField that accepts only numeric values
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.util.SyntaxUtilities;

/** A TextField that accepts only numeric values. The numeric values may be 
 * either integer or float values.
 * @author Matthieu Casanova
 * @version $Id: KeyEventWorkaround.java 12889 2008-06-23 20:14:00Z kpouer $
 * @since jEdit 4.3pre15
 */
public class NumericTextField extends JTextField implements ComboBoxEditor
{
	private final boolean positiveOnly;
	private final boolean integerOnly;
	private Number minValue;
	private Number maxValue;
	private SyntaxStyle invalidStyle;
	private Color defaultBackground;
	private Color defaultForeground;
	
	public NumericTextField(String text)
	{
		this(text, false);
	}
	
	public NumericTextField(String text, boolean positiveOnly)
	{
		super(text);
		this.positiveOnly = positiveOnly;
		integerOnly = true;
		minValue = positiveOnly ? Integer.valueOf(0) : Integer.MIN_VALUE;
		maxValue = Integer.MAX_VALUE;
		addFilter();
		loadInvalidStyle();
	}
	
	public NumericTextField(String text, boolean positiveOnly, boolean integerOnly)
	{
		super(text);
		this.positiveOnly = positiveOnly;
		this.integerOnly = integerOnly;
		if (integerOnly)
		{
			minValue = positiveOnly ? Integer.valueOf(0) : Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
		}
		else 
		{
			minValue = positiveOnly ? Float.valueOf(0.0f) : Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
		}
		addFilter();
		loadInvalidStyle();
	}
	
	public NumericTextField(String text, int columns, boolean positiveOnly) 
	{
		super(text, columns);
		this.positiveOnly = positiveOnly;
		integerOnly = true;
		minValue = positiveOnly ? Integer.valueOf(0) : Integer.MIN_VALUE;
		maxValue = Integer.MAX_VALUE;
		addFilter();
		loadInvalidStyle();
	}
	
	public NumericTextField(String text, int columns, boolean positiveOnly, boolean integerOnly)
	{
		super(text, columns);
		this.positiveOnly = positiveOnly;
		this.integerOnly = integerOnly;
		if (integerOnly)
		{
			minValue = positiveOnly ? Integer.valueOf(0) : Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
		}
		else 
		{
			minValue = positiveOnly ? Float.valueOf(0.0f) : Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
		}
		addFilter();
		loadInvalidStyle();
	}
	
	private void loadInvalidStyle()
	{
		Font font = getFont();
		String family = font.getFamily();
		int size = font.getSize();
		invalidStyle = SyntaxUtilities.parseStyle(jEdit.getProperty("view.style.invalid"), family, size, true);
		defaultForeground = getForeground();
		defaultBackground = getBackground();
	}
	
	// set the minimum allowed value for this text field. If this NumericTextField
	// was constructed with positive only, then values less than zero are ignored.
	public void setMinValue(Number n)
	{
		if (positiveOnly)
		{
			float f = n.floatValue();
			if (f < 0.0)
				return;
		}
		
		if (integerOnly)
		{
			int i = n.intValue();
			int max = maxValue.intValue();
			if (i > max)
				return;
		}
		else 
		{
			float f = n.floatValue();
			float max = maxValue.floatValue();
			if (f > max)
				return;
		}
		minValue = n;
	}
	
	// set the maximum allowed value for this text field. If this NumericTextField
	// was constructed with positive only, then values less than zero are ignored.
	public void setMaxValue(Number n)
	{
		if (positiveOnly)
		{
			float f = n.floatValue();
			if (f < 0)
				return;
		}
		
		if (integerOnly)
		{
			int i = n.intValue();
			int min = minValue.intValue();
			if (i < min)
				return;
		}
		else 
		{
			float f = n.floatValue();
			float min = minValue.floatValue();
			if (f < min)
				return;
		}
		maxValue = n;
	}
	
	/**
  	 * 	@return The value of the text field as either an Integer or a Float, 
 	 * 	depending on whether this text field allows Integers or Floats.
 	 */
	public Number getValue()
	{
		if (integerOnly)
			return Integer.valueOf(getText());
		else
			return Float.valueOf(getText());
	}
	
	// add an Integer or Float document filter as appropriate
    private void addFilter() {
    	if (integerOnly)
    		( ( AbstractDocument ) this.getDocument() ).setDocumentFilter( new IntegerDocumentFilter() );
    	else
    		( ( AbstractDocument ) this.getDocument() ).setDocumentFilter( new FloatDocumentFilter() );
    	
    	// apply the filter to the current value
    	try 
    	{
    		String text = getText();
    		((AbstractDocument)getDocument()).getDocumentFilter().replace(null, 0, text.length(), text, null); 
		}
		catch(Exception e) {}	// NOPMD
    }

    class IntegerDocumentFilter extends DocumentFilter {
        public void insertString( FilterBypass fb, int offset, String string, AttributeSet attr )
        throws BadLocationException 
        {
            if ( string == null || string.length() == 0 ) 
            {
                return ;
            }
            String newString = new StringBuilder(getText()).insert(offset, string).toString();
            if (!isInteger( newString ))
			{
				return;				
			}
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
			super.insertString( fb, offset, string, attr );
        }

        public void remove( FilterBypass fb, int offset, int length )
        throws BadLocationException 
        {
        	String newString = new StringBuilder(getText()).delete(offset, offset + length).toString();
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
            super.remove( fb, offset, length );
        }

        public void replace( FilterBypass fb, int offset, int length, String text, AttributeSet attrs )
        throws BadLocationException 
        {
            if ( text == null || text.length() == 0 ) 
            {
                return ;
            }
            String newString = new StringBuilder(getText()).replace(offset, offset + length, text).toString();
            if (!isInteger( newString ))
			{
				return;				
			}
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
			super.replace( fb, offset, length, text, attrs );
        }

        private boolean isInteger( String string ) 
        {
        	if (string == null || string.isEmpty())
        	{
        		return false;	
        	}
        	try 
        	{
        		if (!positiveOnly && "-".equals(string))
        		{
        			return true;	
        		}
				Integer.parseInt(string);
				return true;
			}
			catch(Exception e) 
			{
				return false;
			}
        }

        private boolean inRange( String string ) 
        {
        	if (string == null || string.isEmpty())
        	{
        		return false;	
        	}
            int value = Integer.parseInt( string );
            return value <= maxValue.intValue() && value >= minValue.intValue();
        }
    }
	
    class FloatDocumentFilter extends DocumentFilter {
        public void insertString( FilterBypass fb, int offset, String string, AttributeSet attr )
        throws BadLocationException 
        {
            if ( string == null || string.length() == 0 ) 
            {
                return ;
            }
            String newString = new StringBuilder(getText()).insert(offset, string).toString();
            if (!isFloat( newString ))
			{
				return;				
			}
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
			super.insertString( fb, offset, string, attr );
        }

        public void remove( FilterBypass fb, int offset, int length )
        throws BadLocationException 
        {
        	String newString = new StringBuilder(getText()).delete(offset, offset + length).toString();
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
            super.remove( fb, offset, length );
        }

        public void replace( FilterBypass fb, int offset, int length, String text, AttributeSet attrs )
        throws BadLocationException 
        {
            if ( text == null || text.length() == 0 ) 
            {
                return ;
            }
            String newString = new StringBuilder(getText()).replace(offset, offset + length, text).toString();
            if (!isFloat( newString ))
			{
				return;				
			}
			setBackground(inRange( newString ) ? defaultBackground : invalidStyle.getBackgroundColor());
			setForeground(inRange( newString ) ? defaultForeground : invalidStyle.getForegroundColor());
			super.replace( fb, offset, length, text, attrs );
        }

        private boolean isFloat( String string ) 
        {
        	if (string == null || string.isEmpty())
        	{
        		return false;	
        	}
        	try 
        	{
        		if (".".equals(string))
        		{
        			return true;	
        		}
        		if (!positiveOnly && "-".equals(string))
        		{
        			return true;	
        		}
				Float.parseFloat(string);
				return true;
			}
			catch(Exception e) 
			{
				return false;
			}
        }

        private boolean inRange( String string ) 
        {
        	if (string == null || string.isEmpty())
        	{
        		return false;	
        	}
			if (".".equals(string))
			{
				return true;	
			}
            float value = Float.parseFloat( string );
            boolean toReturn = value <= maxValue.floatValue() && value >= minValue.floatValue();
            return toReturn;
        }
    }
	
	//{{{ ComboBoxEditor methods
	public Component getEditorComponent()
	{
		return this;	
	}
	
	public Object getItem()
	{
		return getText();	
	}
	
	public void setItem(Object item)
	{
		if (item == null)
			setText("");
		else
			setText(item.toString());
	}
	//}}}
}
