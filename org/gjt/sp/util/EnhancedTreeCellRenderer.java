/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.util;

//{{{ Imports
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.WeakHashMap;
//}}}

/**
 * An enhancement of the {@link DefaultTreeCellRenderer} to be used as superclass for custom
 * tree cell renderers. Using {@code DefaultTreeCellRenderer} as superclass for a custom tree
 * cell renderer without further measures is not stable in regards to on-the-fly Look and Feel
 * changes, at least not with Java 6. For Java 7 it should be tested again.
 * <p/>
 * With Java 6 the {@code DefaultTreeCellRenderer} initializes some values according to the
 * Look and Feel in its constructor. If the {@code DefaultTreeCellRenderer} is created by the
 * {@link JTree} code, it is recreated on a Look and Feel change. This way all works fine. But
 * if a tree cell renderer is set explicitly on the {@code JTree}, no matter whether
 * {@code DefaultTreeCellRenderer}, a subclass of it or a complete own implementation, the
 * set instance is used beyond Look and Feel boundaries and this causes two problems.
 * <ol>
 *     <li>
 *         The values that were initialized in the constructor of {@code DefaultTreeCellRenderer}
 *         are not reset according to the new Look and Feel. Some values are partly set by the
 *         {@code JTree} code, but this is not complete and reliable and thus the renderer paints
 *         the tree cells wrongly.
 *     </li>
 *     <li>
 *         The Look and Feel change is first applied to the {@code JTree}, then the sizes of
 *         the tree cells which are saved in a cache are recalculated. Only <b>after</b> that,
 *         the children of the {@code JTree} get the new Look and Feel applied, amongst them
 *         also the tree cell renderer.<br/>
 *         So even if a custom tree cell renderer is aware of on-the-fly Look and Feel changes
 *         by reinitializing values from the Look and Feel if it changes, those cached sizes
 *         are still calculated for the old Look and Feel. The only way to work around this is
 *         to cause the cached sizes to be recalculated. This can be done by changing any
 *         significant property of the {@code JTree} which influences size calculations.
 *     </li>
 * </ol>
 * <p/>
 * To work around the described problems this enhanced tree cell renderer listens for Look
 * and Feel changes on the {@code JTree} where this renderer is used, requests a subclass
 * to create a new instance of the renderer and sets it on the {@code JTree}. By doing so
 * the {@code DefaultTreeCellRenderer} reinitializes to the new Look and Feel in its
 * constructor and the {@code JTree} recalculates the cached size values because a different
 * object is set as tree cell renderer.
 */
public abstract class EnhancedTreeCellRenderer extends DefaultTreeCellRenderer
{
	//{{{ getTreeCellRendererComponent() method
	public final Component getTreeCellRendererComponent(JTree tree,
		Object value, boolean selected, boolean expanded,
		boolean leaf, int row, boolean hasFocus)
	{
		if (!propertyChangeListeners.containsKey(tree))
		{
			PropertyChangeListener propertyChangeListener = new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (!(evt.getSource() instanceof JTree))
						return;

					JTree tree = (JTree) evt.getSource();
					if (tree.getCellRenderer() == EnhancedTreeCellRenderer.this)
						tree.setCellRenderer(newInstance());

					tree.removePropertyChangeListener("UI", propertyChangeListeners.remove(tree));
				}
			};
			tree.addPropertyChangeListener("UI", propertyChangeListener);
			propertyChangeListeners.put(tree, propertyChangeListener);
		}

		super.getTreeCellRendererComponent(tree,value,
			selected,expanded,leaf,row,hasFocus);

		configureTreeCellRendererComponent(tree,value,
			selected,expanded,leaf,row,hasFocus);

		return this;
	} //}}}

	//{{{ newInstance() method
	/**
	 * Creates a new instance of the tree cell renderer. Each invocation has to
	 * return a different object. Saving a reference and returning the same
	 * instance from different calls of this method is <b>not</b> appropriate.
	 * <p/>
	 * Any one-time initializations that are necessary and are not made in the
	 * constructor should be made in this method. The simplest implementation
	 * of this method will just call the constructor and return the result.
	 * <p/>
	 * This is an instance method so that the new instance can be set up with
	 * information from the current instance.
	 *
	 * @return a new readily initialized instance of this class
	 */
	protected abstract TreeCellRenderer newInstance();
	//}}}

	//{{{ configureTreeCellRendererComponent() method
	/**
	 * Configures this instance of the renderer component based on the passed in
	 * components. The value is set from messaging the tree with convertValueToText,
	 * which ultimately invokes toString on value. The foreground color is set
	 * based on the selection and the icon is set based on the leaf and expanded
	 * parameters. The parameters of this method are the same as the ones of
	 * {@link #getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)}.
	 *
	 * @param tree     The tree in which this renderer component is used currently
	 * @param value    The value to be displayed for the tree cell to be rendered
	 * @param selected Whether the tree cell to be rendered is selected
	 * @param expanded Whether the tree cell to be rendered is expanded
	 * @param leaf     Whether the tree cell to be rendered is a leaf
	 * @param row      The row index of the tree cell to be rendered
	 * @param hasFocus Whether the tree cell to be rendered has the focus
	 */
	protected abstract void configureTreeCellRendererComponent(JTree tree,
		Object value, boolean selected, boolean expanded,
		boolean leaf, int row, boolean hasFocus);
	//}}}

	//{{{ Instance variables
	private final Map<JTree, PropertyChangeListener> propertyChangeListeners = new WeakHashMap<JTree, PropertyChangeListener>();
	//}}}
}
