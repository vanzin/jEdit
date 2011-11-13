package org.gjt.sp.util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JComboBox;

/** A class that consumes return and escape keys for JComboBox.
 *   Useful for Combo Boxes that are in OptionPanes, which need to 
 *   to consume the "return" and "escape" keys, which for some reason,
 *   the JComboBox does not do by default. 
 *   
 *   @author ezust
 *   
 **/

public class ComboKeyListener implements KeyListener {

	ArrayList<Integer> yummyKeys;
	JComboBox m_comboBox;
	//[] =  { KeyEvent.VK_ESCAPE, KeyEvent.VK_ENTER };
	public ComboKeyListener(JComboBox combobox) {
		m_comboBox = combobox;
		yummyKeys = new ArrayList<Integer>();
		yummyKeys.add(KeyEvent.VK_ESCAPE);
		yummyKeys.add(KeyEvent.VK_ENTER);
	}
	
	@Override
	public void keyTyped(KeyEvent e)
	{
		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		Integer i = new Integer(e.getKeyCode());
		if (yummyKeys.contains(i)) {
			e.consume();
			if (i.equals(KeyEvent.VK_ENTER) && !m_comboBox.isPopupVisible()) 
				m_comboBox.showPopup();
			else
				m_comboBox.hidePopup();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// TODO Auto-generated method stub
		
	}
	
	
}
