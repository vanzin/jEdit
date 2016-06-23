/*
 * ColorChooserDialog.java - Shows a dialog with a color chooser.
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2015 Dale Anson
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

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.GenericGUIUtilities;
//}}}


public class ColorChooserDialog extends JDialog 
{
    //{{{ Fields
    private Color initialColor = null;
    private JColorChooser colorChooser = null;
    //}}}
    
    //{{{ ColorChooserDialog
    public ColorChooserDialog(Window owner, Color initialColor) 
    {
        super(owner);
        setModal(true);
        this.initialColor = initialColor;
        init();
    }
    //}}}
    
    //{{{ init()
    private void init()
    {
        setTitle(jEdit.getProperty("colorChooser.title"));
        JPanel contents = new JPanel();
        contents.setLayout( new BorderLayout() );
        contents.setBorder( BorderFactory.createEmptyBorder( 12, 12, 11, 11 ) );
        colorChooser = new JColorChooser(initialColor);
        contents.add( colorChooser, BorderLayout.CENTER );
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(17, 0, 0, 0));
        JButton ok = new JButton(jEdit.getProperty("common.ok"));
        ok.addActionListener( 
            new ActionListener() 
            {
                public void actionPerformed( ActionEvent ae ) 
                {
                    ColorChooserDialog.this.setVisible(false);
                    ColorChooserDialog.this.dispose();
                }
            }
        );
        getRootPane().setDefaultButton(ok);
        JButton cancel = new JButton(jEdit.getProperty("common.cancel"));
        cancel.addActionListener( 
            new ActionListener() 
            {
                public void actionPerformed( ActionEvent ae ) 
                {
                    ColorChooserDialog.this.setVisible(false);
                    ColorChooserDialog.this.dispose();
                }
            }
        );
        JButton reset = new JButton(jEdit.getProperty("common.reset"));
        reset.addActionListener( 
            new ActionListener() 
            {
                public void actionPerformed( ActionEvent ae ) 
                {
                    colorChooser.setColor(initialColor);
                }
            }
        );
        GenericGUIUtilities.makeSameSize(ok, cancel, reset);

        buttonPanel.add(Box.createGlue());
        buttonPanel.add(ok);
        buttonPanel.add(Box.createHorizontalStrut(6));
        buttonPanel.add(cancel);
        buttonPanel.add(Box.createHorizontalStrut(6));
        buttonPanel.add(reset);

        contents.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane( contents );
        pack();
        setLocationRelativeTo( getParent() );
        setVisible(true);
    }
    //}}}
    
    //{{{ getColor()
    public Color getColor()
    {
        Color selectedColor = colorChooser.getColor();
        return selectedColor == null ? initialColor : selectedColor;	
    }
    //}}}
}
