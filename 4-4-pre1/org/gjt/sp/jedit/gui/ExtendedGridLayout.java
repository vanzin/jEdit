/*
 * ExtendedGridLayout.java - a grid layout manager with variable cell sizes
 * that supports colspans and rowspans
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Originally written by Björn Kautler for the jEdit project. This work has been
 * placed into the public domain. You may use this work in any way and for any
 * purpose you wish.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.gjt.sp.jedit.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.awt.Component.CENTER_ALIGNMENT;

import static org.gjt.sp.jedit.gui.ExtendedGridLayoutConstraints.REMAINDER;

/**
  * A layout manager that places components in a rectangular grid
  * with variable cell sizes that supports colspans and rowspans.
  * <p>
  * The container is divided into rectangles, and each component is placed
  * in a rectangular space defined by its colspan and rowspan.
  * Each row is as large as the largest component in
  * that row, and each column is as wide as the widest component in
  * that column. </p>
  * <p>
  * This behavior is similar to 
  * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/awt/GridLayout.html">{@code java.awt.GridLayout}</a>
  * but it supports different row heights and
  * column widths for each row/column. </p>
  * <p>
  * For example, the following is a Dialog that lays out ten buttons
  * exactly the same as in the example of the JavaDoc of
  * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/awt/GridBagLayout.html">{@code java.awt.GridBagLayout}</a>
  * with the difference of vertical and horizontal gaps that can be configured:
  * <hr>
  * <blockquote><pre><font color="#000000">
  * <font color="#000000">   1:</font><font color="#009966"><strong>import</strong></font> java.awt.Button;
  * <font color="#000000">   2:</font><font color="#009966"><strong>import</strong></font> java.awt.Dimension;
  * <font color="#000000">   3:</font>
  * <font color="#000000">   4:</font><font color="#009966"><strong>import</strong></font> javax.swing.JDialog;
  * <font color="#990066">   5:</font>
  * <font color="#000000">   6:</font><font color="#009966"><strong>import</strong></font> org.gjt.sp.jedit.gui.ExtendedGridLayout;
  * <font color="#000000">   7:</font><font color="#009966"><strong>import</strong></font> org.gjt.sp.jedit.gui.ExtendedGridLayoutConstraints;
  * <font color="#000000">   8:</font>
  * <font color="#000000">   9:</font><font color="#009966"><strong>import</strong></font> <font color="#006699"><strong>static</strong></font> org.gjt.sp.jedit.gui.ExtendedGridLayoutConstraints.REMAINDER;
  * <font color="#990066">  10:</font>
  * <font color="#000000">  11:</font><font color="#006699"><strong>public</strong></font> <font color="#0099ff"><strong>class</strong></font> ExampleDialog <font color="#006699"><strong>extends</strong></font> JDialog <font color="#000000"><strong>{</strong></font>
  * <font color="#000000">  12:</font>    <font color="#006699"><strong>public</strong></font> <font color="#9966ff">ExampleDialog</font>() <font color="#000000"><strong>{</strong></font>
  * <font color="#000000">  13:</font>        <font color="#cc00cc">super</font>(<font color="#cc00cc">null</font>,<font color="#ff00cc">&quot;</font><font color="#ff00cc">Example</font><font color="#ff00cc"> </font><font color="#ff00cc">Dialog</font><font color="#ff00cc">&quot;</font>,<font color="#cc00cc">true</font>);
  * <font color="#000000">  14:</font>        <font color="#9966ff">setLayout</font>(<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayout</font>(<font color="#ff0000">5</font>,<font color="#ff0000">5</font>,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">Insets</font>(<font color="#ff0000">5</font>,<font color="#ff0000">5</font>,<font color="#ff0000">5</font>,<font color="#ff0000">5</font>)));
  * <font color="#990066">  15:</font>        
  * <font color="#000000">  16:</font>        <font color="#9966ff">add</font>(<font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button1</font><font color="#ff00cc">&quot;</font>));
  * <font color="#000000">  17:</font>        <font color="#9966ff">add</font>(<font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button2</font><font color="#ff00cc">&quot;</font>));
  * <font color="#000000">  18:</font>        <font color="#9966ff">add</font>(<font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button3</font><font color="#ff00cc">&quot;</font>));
  * <font color="#000000">  19:</font>        <font color="#9966ff">add</font>(<font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button4</font><font color="#ff00cc">&quot;</font>));
  * <font color="#990066">  20:</font>        Button button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button5</font><font color="#ff00cc">&quot;</font>);
  * <font color="#000000">  21:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">1</font>,REMAINDER,<font color="#ff0000">1</font>,button));
  * <font color="#000000">  22:</font>        button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button6</font><font color="#ff00cc">&quot;</font>);
  * <font color="#000000">  23:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">2</font>,<font color="#ff0000">3</font>,<font color="#ff0000">1</font>,button));
  * <font color="#000000">  24:</font>        button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button7</font><font color="#ff00cc">&quot;</font>);
  * <font color="#990066">  25:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">2</font>,button));
  * <font color="#000000">  26:</font>        button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button8</font><font color="#ff00cc">&quot;</font>);
  * <font color="#000000">  27:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">3</font>,<font color="#ff0000">1</font>,<font color="#ff0000">2</font>,button));
  * <font color="#000000">  28:</font>        button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button9</font><font color="#ff00cc">&quot;</font>);
  * <font color="#000000">  29:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">3</font>,<font color="#ff0000">3</font>,<font color="#ff0000">1</font>,button));
  * <font color="#990066">  30:</font>        button <font color="#000000"><strong>=</strong></font> <font color="#9966ff">makeButton</font>(<font color="#ff00cc">&quot;</font><font color="#ff00cc">Button10</font><font color="#ff00cc">&quot;</font>);
  * <font color="#000000">  31:</font>        <font color="#9966ff">add</font>(button,<font color="#006699"><strong>new</strong></font> <font color="#9966ff">ExtendedGridLayoutConstraints</font>(<font color="#ff0000">4</font>,REMAINDER,<font color="#ff0000">1</font>,button));
  * <font color="#000000">  32:</font>        
  * <font color="#000000">  33:</font>        <font color="#9966ff">pack</font>();
  * <font color="#000000">  34:</font>        <font color="#9966ff">setLocationRelativeTo</font>(<font color="#cc00cc">null</font>);
  * <font color="#990066">  35:</font>        <font color="#9966ff">setVisible</font>(<font color="#cc00cc">true</font>);
  * <font color="#000000">  36:</font>    <font color="#000000"><strong>}</strong></font>
  * <font color="#000000">  37:</font>    
  * <font color="#000000">  38:</font>    <font color="#006699"><strong>private</strong></font> Button <font color="#9966ff">makeButton</font>(String name) <font color="#000000"><strong>{</strong></font>
  * <font color="#000000">  39:</font>        Button button <font color="#000000"><strong>=</strong></font> <font color="#006699"><strong>new</strong></font> <font color="#9966ff">Button</font>(name);
  * <font color="#990066">  40:</font>        button.<font color="#9966ff">setMaximumSize</font>(<font color="#006699"><strong>new</strong></font> <font color="#9966ff">Dimension</font>(Integer.MAX_VALUE,Integer.MAX_VALUE));
  * <font color="#000000">  41:</font>        <font color="#006699"><strong>return</strong></font> button;
  * <font color="#000000">  42:</font>    <font color="#000000"><strong>}</strong></font>
  * <font color="#000000">  43:</font><font color="#000000"><strong>}</strong></font>
  * </font></pre></blockquote>
  * <hr>
  * If you use {@code REMAINDER} as colspan or rowspan then a component takes
  * up the remaining space in that column or row. Any additional components in
  * a row are ignored and not displayed. Additional components in a column are
  * moved rightside. If a rowspan hits a colspan, the colspan ends and the
  * rowspan takes precedence.
  * <p>
  * Components for which {@code isVisible() == false} are ignored. Because
  * of this, components can be replaced "in-place" by adding two components next to
  * each other, with different {@code isVisible()} values, and toggling the 
  * {@code setVisible()} values of both when we wish to swap the currently
  * visible component with the one that is hidden. </p>
  *
  * <p>
  * If you want to reserve free space in a row inbetween components,  
  * add a <a href="http://java.sun.com/j2se/1.5.0/docs/api/javax/swing/Box.Filler.html">{@code javax.swing.Box.Filler}</a>
  * to the layout if the free space is in the middle of a row,
  * or just don't add components if the free space
  * should be at the end of a row.</p>
  * <p>
  * If a row is taller, or a column is wider than the {@code maximumSize} of a component,
  * the component is resized to its maximum size and aligned according to its
  * {@code alignmentX} and {@code alignmentY} values. </p>
  * <p>
  * One instance of this class can be used to layout multiple
  * containers at the same time. </p>
  *
  * @author Björn "Vampire" Kautler
  * @version 1.0
  * @see ExtendedGridLayoutConstraints
  * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/awt/Component.html"><code>java.awt.Component</code></a>
  * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/javax/swing/Box.Filler.html"><code>javax.swing.Box.Filler</code></a>
  */
public class ExtendedGridLayout implements LayoutManager2
{
	/**
	  * This hashtable maintains the association between
	  * a component and its ExtendedGridLayoutConstraints.
	  * The Keys in {@code comptable} are the components and the
	  * values are the instances of {@code ExtendedGridLayoutConstraints}.
	  *
	  * @see ExtendedGridLayoutConstraints
	  */
	private final Hashtable<Component,ExtendedGridLayoutConstraints> comptable;
	
	/**
	  * Specifies the horizontal space between two columns.
	  * The default value is 0.
	  * 
	  * @see #distanceToBorders
	  * @see #vgap
	  */
	private final int hgap;
	
	/**
	  * Specifies the vertical space between two rows.
	  * The default value is 0.
	  * 
	  * @see #distanceToBorders
	  * @see #hgap
	  */
	private final int vgap;
	
	/**
	  * Specifies the gap between the grid and the borders of the parent container.
	  * The default value is 0 for all four borders.
	  * 
	  * @see #hgap
	  * @see #vgap
	  */
	private final Insets distanceToBorders;
	
	/**
	  * An enum to tell the {@code getSize()} method which size is requested.
	  * 
	  * @see #getSize()
	  */
	private static enum LayoutSize { MINIMUM, PREFERRED, MAXIMUM }
	
	/**
	  * Creates an extended grid layout manager with the specified horizontal
	  * and vertical gap, and the specified distance to the borders
	  * of the parent container.
	  * 
	  * @param hgap The horizontal space between two columns ({@literal >=0})
	  * @param vgap The vertical space between two rows ({@literal >=0})
	  * @param distanceToBorders The distances to the borders of the parent container
	  * @throws IllegalArgumentException if hgap {@literal < 0}
	  * @throws IllegalArgumentException if vgap {@literal < 0}
	  */
	public ExtendedGridLayout(int hgap, int vgap, Insets distanceToBorders)
	{
		if (hgap < 0)
		{
			throw new IllegalArgumentException("hgap must be non-negative (" + hgap + ')');
		}
		if (vgap < 0)
		{
			throw new IllegalArgumentException("vgap must be non-negative (" + vgap + ')');
		}
		this.hgap = hgap;
		this.vgap = vgap;
		this.distanceToBorders = (Insets)distanceToBorders.clone();
		comptable = new Hashtable<Component,ExtendedGridLayoutConstraints>();
	}
	
	/**
	  * Creates an extended grid layout manager with zero horizontal
	  * and vertical gap, and zero distance to the borders
	  * of the parent container.
	  */
	public ExtendedGridLayout()
	{
		this(0,0,new Insets(0,0,0,0));
	}
	
	/**
	  * If the layout manager uses a per-component string,
	  * adds the component <code>component</code> to the layout,
	  * associating it with the string specified by <code>name</code>.
	  * 
	  * @param name      The string to be associated with the component.
	  *                  Has to be {@code null}, so that default constraints are used.
	  * @param component The component to be added
	  * @throws IllegalArgumentException if {@code name} is not {@code null}
	  * @see #addLayoutComponent(java.awt.Component, java.lang.Object)
	  */
	public void addLayoutComponent(String name, Component component)
	{
		addLayoutComponent(component,name);
	}
	
	/**
	  * Adds the specified component to the layout, using the specified
	  * constraints object.
	  * 
	  * @param component    The component to be added
	  * @param constraints  Where/how the component is added to the layout.
	  * @throws IllegalArgumentException if {@code constraints} is not an ExtendedGridLayoutConstraints object
	  * @throws IllegalArgumentException if {@code constraints} is a placeholder
	  * @throws IllegalArgumentException if {@code constraints} is not the right one for the component
	  * @see ExtendedGridLayoutConstraints
	  */
	public void addLayoutComponent(Component component, Object constraints)
	{
		if (null == constraints)
		{
			constraints = new ExtendedGridLayoutConstraints(component);
		}
		if (constraints instanceof ExtendedGridLayoutConstraints)
		{
			ExtendedGridLayoutConstraints eglConstraints = (ExtendedGridLayoutConstraints)constraints;
			if (eglConstraints.isPlaceholder())
			{
				throw new IllegalArgumentException("constraints must not be a placeholder");
			}
			else if (component != eglConstraints.getComponent())
			{
				throw new IllegalArgumentException("constraints is not the right one for this component");
			}
			comptable.put(component,eglConstraints);
		}
		else 
		{
			throw new IllegalArgumentException("constraints must not be an ExtendedGridLayoutConstraints object");
		}
	}
	
	/**
	  * Retrieves the constraints for the specified {@code component}.
	  * If {@code component} is not in the {@code ExtendedGridLayout},
	  * a set of default {@code ExtendedGridLayoutConstraints} are returned.
	  * 
	  * @param component the {@code component} to be queried
	  * @return the contraints for the specified {@code component}
	  * @throws NullPointerException if {@code component} is {@code null}
	  * @see ExtendedGridLayoutConstraints
	  */
	private ExtendedGridLayoutConstraints lookupConstraints(Component component)
	{
		if (null == component)
		{
			throw new NullPointerException("component must not be null");
		}
		ExtendedGridLayoutConstraints constraints = comptable.get(component);
		if (null == constraints)
		{
			constraints = new ExtendedGridLayoutConstraints(component);
			comptable.put(component,constraints);
		}
		return constraints;
	}
	
	/**
	  * Removes the specified component from the layout.
	  * 
	  * @param component The component to be removed
	  */
	public void removeLayoutComponent(Component component)
	{
		comptable.remove(component);
	}
	
	/**
	  * Returns the alignment along the X axis.  This specifies how
	  * the component would like to be aligned relative to other
	  * components.  The value should be a number between 0 and 1
	  * where 0 represents alignment along the origin, 1 is aligned
	  * the furthest away from the origin, 0.5 is centered, etc.
	  * 
	  * @param container The container for which the alignment should be returned
	  * @return {@code java.awt.Component.CENTER_ALIGNMENT}
	  */
	public float getLayoutAlignmentX(Container container)
	{
		return CENTER_ALIGNMENT;
	}
	
	/**
	  * Returns the alignment along the Y axis. This specifies how
	  * the component would like to be aligned relative to other
	  * components. The value should be a number between 0 and 1
	  * where 0 represents alignment along the origin, 1 is aligned
	  * the furthest away from the origin, 0.5 is centered, etc.
	  * 
	  * @param container The container for which the alignment should be returned
	  * @return {@code java.awt.Component.CENTER_ALIGNMENT}
	  */
	public float getLayoutAlignmentY(Container container)
	{
		return CENTER_ALIGNMENT;
	}
	
	/**
	  * Calculates the minimum size dimensions for the specified
	  * container, given the components it contains.
	  * 
	  * @param parent The component to be laid out
	  * @return The minimum size for the container
	  * @see #maximumLayoutSize
	  * @see #preferredLayoutSize
	  */
	public Dimension minimumLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			List<List<ExtendedGridLayoutConstraints>> gridRows = new ArrayList<List<ExtendedGridLayoutConstraints>>();
			Set<ExtendedGridLayoutConstraints> colspans = new HashSet<ExtendedGridLayoutConstraints>();
			Set<ExtendedGridLayoutConstraints> rowspans = new HashSet<ExtendedGridLayoutConstraints>();
			Dimension gridSize = buildGrid(parent,gridRows,colspans,rowspans);
			return getSize(parent,LayoutSize.MINIMUM,false,gridSize,gridRows,colspans,rowspans,new int[0][0]);
		}
	}
	
	/**
	  * Calculates the preferred size dimensions for the specified
	  * container, given the components it contains.
	  * 
	  * @param parent The container to be laid out
	  * @return The preferred size for the container
	  * @see #maximumLayoutSize
	  * @see #minimumLayoutSize
	  */
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			List<List<ExtendedGridLayoutConstraints>> gridRows = new ArrayList<List<ExtendedGridLayoutConstraints>>();
			Set<ExtendedGridLayoutConstraints> colspans = new HashSet<ExtendedGridLayoutConstraints>();
			Set<ExtendedGridLayoutConstraints> rowspans = new HashSet<ExtendedGridLayoutConstraints>();
			Dimension gridSize = buildGrid(parent,gridRows,colspans,rowspans);
			return getSize(parent,LayoutSize.PREFERRED,false,gridSize,gridRows,colspans,rowspans,new int[0][0]);
		}
	}
	
	/**
	  * Calculates the maximum size dimensions for the specified
	  * container, given the components it contains.
	  * 
	  * @param parent The container to be laid out
	  * @return The maximum size for the container
	  * @see #minimumLayoutSize
	  * @see #preferredLayoutSize
	  */
	public Dimension maximumLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			List<List<ExtendedGridLayoutConstraints>> gridRows = new ArrayList<List<ExtendedGridLayoutConstraints>>();
			Set<ExtendedGridLayoutConstraints> colspans = new HashSet<ExtendedGridLayoutConstraints>();
			Set<ExtendedGridLayoutConstraints> rowspans = new HashSet<ExtendedGridLayoutConstraints>();
			Dimension gridSize = buildGrid(parent,gridRows,colspans,rowspans);
			return getSize(parent,LayoutSize.MAXIMUM,false,gridSize,gridRows,colspans,rowspans,new int[0][0]);
		}
	}
	
	/**
	  * Invalidates the layout, indicating that if the layout manager
	  * has cached information it should be discarded.
	  * 
	  * @param container The container for which the cached information should be discarded
	  */
	public void invalidateLayout(Container container)
	{
	}
	
	/**
	  * Lays out the specified container.
	  * 
	  * @param parent The container to be laid out 
	  */
	public void layoutContainer(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			// Pass 1: build the grid
			List<List<ExtendedGridLayoutConstraints>> gridRows = new ArrayList<List<ExtendedGridLayoutConstraints>>();
			Set<ExtendedGridLayoutConstraints> colspans = new HashSet<ExtendedGridLayoutConstraints>();
			Set<ExtendedGridLayoutConstraints> rowspans = new HashSet<ExtendedGridLayoutConstraints>();
			Dimension gridSize = buildGrid(parent,gridRows,colspans,rowspans);
			
			// Pass 2: compute minimum, preferred and maximum column widths / row heights
			int[][] layoutSizes = new int[6][];
			Dimension preferredSize = getSize(parent,LayoutSize.PREFERRED,true,gridSize,gridRows,colspans,rowspans,layoutSizes);
			int[] minimumColWidths = layoutSizes[0];
			int[] minimumRowHeights = layoutSizes[1];
			int[] preferredColWidths = layoutSizes[2];
			int[] preferredRowHeights = layoutSizes[3];
			int[] maximumColWidths = layoutSizes[4];
			int[] maximumRowHeights = layoutSizes[5];
			
			// Pass 3: redistribute free space
			Dimension parentSize = parent.getSize();
			Insets insets = parent.getInsets();
			int freeWidth = parentSize.width
					- insets.left - insets.right
					- (gridSize.width - 1) * hgap
					- distanceToBorders.left - distanceToBorders.right;
			int freeHeight = parentSize.height
					 - insets.top - insets.bottom
					 - (gridSize.height - 1) * vgap
					 - distanceToBorders.top - distanceToBorders.bottom;
			redistributeSpace(preferredSize.width,
					  freeWidth,
					  0,gridSize.width,
					  preferredColWidths,
					  minimumColWidths,
					  maximumColWidths);
			redistributeSpace(preferredSize.height,
					  freeHeight,
					  0,gridSize.height,
					  preferredRowHeights,
					  minimumRowHeights,
					  maximumRowHeights);
			
			// Pass 4: layout components
			for (int row=0, y=insets.top+distanceToBorders.top ; row<gridSize.height ; y+=preferredRowHeights[row]+vgap, row++)
			{
				List<ExtendedGridLayoutConstraints> gridRow = gridRows.get(row);
				for (int col=0, x=insets.left+distanceToBorders.left ; col<gridSize.width; x+=preferredColWidths[col]+hgap, col++)
				{
					ExtendedGridLayoutConstraints cell = gridRow.get(col);
					if ((null != cell) && (null != cell.getComponent()) && !cell.isPlaceholder())
					{
						Component component = cell.getComponent();
						Dimension maxSize = component.getMaximumSize();
						int fromCol = cell.getCol();
						int colspan = cell.getEffectiveColspan();
						int toCol = fromCol + colspan;
						int width = 0;
						for (int col2=fromCol ; col2<toCol ; col2++)
						{
							width += preferredColWidths[col2];
						}
						width += (colspan - 1) * hgap;
						int fromRow = cell.getRow();
						int rowspan = cell.getEffectiveRowspan();
						int toRow = fromRow + rowspan;
						int height = 0;
						for (int row2=fromRow ; row2<toRow ; row2++)
						{
							height += preferredRowHeights[row2];
						}
						height += (rowspan - 1) * vgap;
						int xCorrection = 0;
						int yCorrection = 0;
						if (width > maxSize.width)
						{
							xCorrection = (int)((width - maxSize.width) * component.getAlignmentX());
							width = maxSize.width;
						}
						if (height > maxSize.height)
						{
							yCorrection = (int)((height-maxSize.height) * component.getAlignmentY());
							height = maxSize.height;
						}
						
						component.setBounds(x + xCorrection, y + yCorrection, width, height);
					}
				}
			}
		}
	}
	
	/**
	  * Redistributs free space (positive or negative) to all available
	  * columns or rows while taking elements maximum and minimum sizes into
	  * account if possible.
	  * 
	  * @param totalSize             The cumulated preferred sizes of the components
	  * @param freeSize              The available space for displaying components
	  *                              without any gaps between components or between
	  *                              the grid and the borders of the parent container
	  * @param start                 The start in the arrays of rows or columns inclusive
	  * @param stop                  The stop in the arrays of rows or columns exclusive
	  * @param preferredElementSizes The preferredSizes of the rows or columns.
	  *                              After invocation of this method, this array
	  *                              holds the sizes that should be used
	  * @param minimumElementSizes   The minimumSizes of the rows or columns
	  * @param maximumElementSizes   The maximumSizes of the rows or columns
	  */
	private void redistributeSpace(int totalSize, int freeSize,
				       int start, int stop,
				       int[] preferredElementSizes,
				       int[] minimumElementSizes,
				       int[] maximumElementSizes)
	{
		if (totalSize != freeSize)
		{
			boolean grow = totalSize < freeSize;
			// calculate the size that is available for redistribution
			freeSize = (freeSize - totalSize) * (grow ? 1 : -1);
			while (freeSize > 0)
			{
				// calculate the amount of elements that can be resized without violating
				// the minimum and maximum sizes and their current cumulated size
				int modifyableAmount = 0;
				long modifySize = 0;
				for (int i=start ; i<stop ; i++)
				{
					if ((grow && (preferredElementSizes[i] < maximumElementSizes[i])) ||
					    (!grow && (preferredElementSizes[i] > minimumElementSizes[i])))
					{
						modifyableAmount++;
						modifySize += preferredElementSizes[i];
					}
				}
				boolean checkBounds = true;
				// if all elements are at their minimum or maximum size, resize all elements
				if (0 == modifyableAmount)
				{
					for (int i=start ; i<stop ; i++)
					{
						modifySize += preferredElementSizes[i];
					}
					checkBounds = false;
					modifyableAmount = stop - start;
				}
				// to prevent an endless loop if the container gets resized to a very small amount
				if (modifySize == 0)
				{
					break;
				}
				// resize the elements
				if (freeSize < modifyableAmount)
				{
					for (int i=start ; i<stop ; i++)
					{
						if ((freeSize != 0) &&
						    (!checkBounds ||
						     (checkBounds &&
						      (grow && (preferredElementSizes[i] < maximumElementSizes[i])) ||
						      (!grow && (preferredElementSizes[i] > minimumElementSizes[i])))))
						{
							preferredElementSizes[i] += (grow ? 1 : -1);
							if (0 > preferredElementSizes[i])
							{
								preferredElementSizes[i] = 0;
							}
							freeSize--;
						}
					}
				}
				else
				{
					long modifySizeAddition = 0;
					double factor = (double)(freeSize + modifySize) / (double)modifySize;
					for (int i=start ; i<stop ; i++)
					{
						long modifyableSize = (checkBounds ? (grow ? maximumElementSizes[i] - preferredElementSizes[i] : preferredElementSizes[i] - minimumElementSizes[i]) : Integer.MAX_VALUE - preferredElementSizes[i]);
						long elementModifySize = Math.abs(Math.round((factor * preferredElementSizes[i]) - preferredElementSizes[i]));
						if (elementModifySize <= modifyableSize)
						{
							preferredElementSizes[i] += (grow ? elementModifySize : -elementModifySize);
							modifySizeAddition += (grow ? elementModifySize : -elementModifySize);
							freeSize -= elementModifySize;
						}
						else
						{
							preferredElementSizes[i] += (grow ? modifyableSize : -modifyableSize);
							modifySizeAddition += (grow ? modifyableSize : -modifyableSize);
							freeSize -= modifyableSize;
						}
						if (0 > preferredElementSizes[i])
						{
							preferredElementSizes[i] = 0;
						}
					}
					modifySize += modifySizeAddition;
				}
			}
		}
	}
	
	/**
	  * Calculates the minimum, preferred or maximum size dimensions
	  * for the specified container, given the components it contains.
	  * 
	  * @param parent       The container to be laid out
	  * @param layoutSize   if {@code LayoutSize.MINIMUM} compute minimum layout size,
	  *                     if {@code LayoutSize.PREFERRED} compute preferred layout size,
	  *                     if {@code LayoutSize.MAXIMUM} compute maximum layout size,
	  *                     if {@code fillRawSizes} is {@code true}, the layout size is computed
	  *                     without applying gaps between components or between
	  *                     the grid and the borders of the parent container
	  * @param fillRawSizes Whether to fill the resultArrays with the raw
	  *                     row heights and column widths and whether to apply
	  *                     gaps between components or between
	  *                     the grid and the borders of the parent container
	  *                     when computing the layout size
	  * @param gridSize     The amount of rows and columns in the grid
	  * @param gridRows     The grid holding the constraints for the components
	  * @param colspans     In this {@code Set} the constraints which are part
	  *                     of a colspan are stored
	  * @param rowspans     In this {@code Set} the constraints which are part
	  *                     of a rowspan are stored
	  * @param resultArrays If {@code fillRawSizes} is {@code true}, the first six arrays
	  *                     get filled with the raw row heights and column widths.
	  *                     resultArrays[0] = resultMinimumColWidths;
	  *                     resultArrays[1] = resultMinimumRowHeights;
	  *                     resultArrays[2] = resultPreferredColWidths;
	  *                     resultArrays[3] = resultPreferredRowHeights;
	  *                     resultArrays[4] = resultMaximumColWidths;
	  *                     resultArrays[5] = resultMaximumRowHeights;
	  * @return The minimum, preferred or maximum size dimensions for the specified container
	  * @throws IllegalArgumentException If {@code fillRawSizes == true} and {@code resultArrays.length < 6}
	  */
	private Dimension getSize(Container parent, LayoutSize layoutSize, boolean fillRawSizes,
				  Dimension gridSize, List<List<ExtendedGridLayoutConstraints>> gridRows,
				  Set<ExtendedGridLayoutConstraints> colspans,
				  Set<ExtendedGridLayoutConstraints> rowspans,
				  int[][] resultArrays)
	{
		if (fillRawSizes && (resultArrays.length < 6))
		{
			throw new IllegalArgumentException("If fillRawSizes is true, resultArrays.length must be >= 6 (" + resultArrays.length + ')');
		}
		int[] minimumColWidths = new int[gridSize.width];
		int[] minimumRowHeights = new int[gridSize.height];
		int[] preferredColWidths = new int[gridSize.width];
		int[] preferredRowHeights = new int[gridSize.height];
		int[] maximumColWidths = new int[gridSize.width];
		int[] maximumRowHeights = new int[gridSize.height];
		Arrays.fill(minimumColWidths,0);
		Arrays.fill(minimumRowHeights,0);
		Arrays.fill(preferredColWidths,0);
		Arrays.fill(preferredRowHeights,0);
		Arrays.fill(maximumColWidths,0);
		Arrays.fill(maximumRowHeights,0);
		
		// get the maximum of the minimum sizes,
		//     the maximum of the preferred sizes and
		//     the minimum of the maximum sizes
		// of all rows and columns, not taking
		// rowspans and colspans into account
		for (int row=0 ; row<gridSize.height ; row++)
		{
			List<ExtendedGridLayoutConstraints> gridRow = gridRows.get(row);
			for (int col=0 ; col<gridSize.width ; col++)
			{
				ExtendedGridLayoutConstraints cell = gridRow.get(col);
				if ((null != cell) && (null != cell.getComponent()))
				{
					Component component = cell.getComponent();
					Dimension minimumSize = component.getMinimumSize();
					Dimension preferredSize = component.getPreferredSize();
					Dimension maximumSize = component.getMaximumSize();
					if (!colspans.contains(cell))
					{
						minimumColWidths[col] = Math.max(minimumColWidths[col],minimumSize.width);
						preferredColWidths[col] = Math.max(preferredColWidths[col],preferredSize.width);
						maximumColWidths[col] = Math.max(maximumColWidths[col],maximumSize.width);
					}
					if (!rowspans.contains(cell))
					{
						minimumRowHeights[row] = Math.max(minimumRowHeights[row],minimumSize.height);
						preferredRowHeights[row] = Math.max(preferredRowHeights[row],preferredSize.height);
						maximumRowHeights[row] = Math.max(maximumRowHeights[row],maximumSize.height);
					}
				}
			}
		}
		
		// correct cases where
		// minimumColWidths[col] <= preferredColWidths[col] <= maximumColWidths[col]
		// is not true by clipping to the minimumColWidths and maximumColWidths
		for (int col=0 ; col<gridSize.width ; col++)
		{
			if (minimumColWidths[col] >= maximumColWidths[col])
			{
				maximumColWidths[col] = minimumColWidths[col];
				preferredColWidths[col] = minimumColWidths[col];
			}
			else if (preferredColWidths[col] < minimumColWidths[col])
			{
				preferredColWidths[col] = minimumColWidths[col];
			}
			else if (preferredColWidths[col] > maximumColWidths[col])
			{
				preferredColWidths[col] = maximumColWidths[col];
			}
		}
		
		// plug in the colspans and correct the minimum, preferred and
		// maximum column widths the colspans are part of
		for (ExtendedGridLayoutConstraints cell : colspans)
		{
			int fromCol = cell.getCol();
			int colspan = cell.getEffectiveColspan();
			int toCol = fromCol + colspan;
			int currentMinimumColWidth = 0;
			int currentPreferredColWidth = 0;
			int currentMaximumColWidth = 0;
			for (int col=fromCol ; col<toCol ; col++)
			{
				int minimumColWidth = minimumColWidths[col];
				if ((Integer.MAX_VALUE-minimumColWidth) < currentMinimumColWidth)
				{
					currentMinimumColWidth = Integer.MAX_VALUE;
				}
				else
				{
					currentMinimumColWidth += minimumColWidth;
				}
				int preferredColWidth = preferredColWidths[col];
				if ((Integer.MAX_VALUE-preferredColWidth) < currentPreferredColWidth)
				{
					currentPreferredColWidth = Integer.MAX_VALUE;
				}
				else
				{
					currentPreferredColWidth += preferredColWidth;
				}
				int maximumColWidth = maximumColWidths[col];
				if ((Integer.MAX_VALUE-maximumColWidth) < currentMaximumColWidth)
				{
					currentMaximumColWidth = Integer.MAX_VALUE;
				}
				else
				{
					currentMaximumColWidth += maximumColWidth;
				}
			}
			Component component = cell.getComponent();
			int wantedMaximumColWidth = component.getMaximumSize().width - ((colspan - 1) * hgap);
			if (currentMaximumColWidth < wantedMaximumColWidth)
			{
				redistributeSpace(currentMaximumColWidth,
						  wantedMaximumColWidth,
						  fromCol,toCol,
						  maximumColWidths,
						  maximumColWidths,
						  maximumColWidths);
			}
			int wantedMinimumColWidth = component.getMinimumSize().width - ((colspan - 1) * hgap);
			if (currentMinimumColWidth < wantedMinimumColWidth)
			{
				redistributeSpace(currentMinimumColWidth,
						  wantedMinimumColWidth,
						  fromCol,toCol,
						  minimumColWidths,
						  minimumColWidths,
						  maximumColWidths);
			}
			int wantedPreferredColWidth = component.getPreferredSize().width - ((colspan - 1) * hgap);
			if (currentPreferredColWidth < wantedPreferredColWidth)
			{
				redistributeSpace(currentPreferredColWidth,
						  wantedPreferredColWidth,
						  fromCol,toCol,
						  preferredColWidths,
						  minimumColWidths,
						  maximumColWidths);
			}
		}
		
		// correct cases where
		// minimumColWidths[col] <= preferredColWidths[col] <= maximumColWidths[col]
		// is not true by clipping to the minimumColWidths and maximumColWidths
		for (int col=0 ; col<gridSize.width ; col++)
		{
			if (minimumColWidths[col] >= maximumColWidths[col])
			{
				maximumColWidths[col] = minimumColWidths[col];
				preferredColWidths[col] = minimumColWidths[col];
			}
			else if (preferredColWidths[col] < minimumColWidths[col])
			{
				preferredColWidths[col] = minimumColWidths[col];
			}
			else if (preferredColWidths[col] > maximumColWidths[col])
			{
				preferredColWidths[col] = maximumColWidths[col];
			}
		}
		
		// correct cases where
		// minimumRowHeights[row] <= preferredRowHeights[row] <= maximumRowHeights[row]
		// is not true by clipping to the minimumRowHeights and maximumRowHeights
		for (int row=0 ; row<gridSize.height ; row++)
		{
			if (minimumRowHeights[row] >= maximumRowHeights[row])
			{
				maximumRowHeights[row] = minimumRowHeights[row];
				preferredRowHeights[row] = minimumRowHeights[row];
			}
			else if (preferredRowHeights[row] < minimumRowHeights[row])
			{
				preferredRowHeights[row] = minimumRowHeights[row];
			}
			else if (preferredRowHeights[row] > maximumRowHeights[row])
			{
				preferredRowHeights[row] = maximumRowHeights[row];
			}
		}
		
		// plug in the rowspans and correct the minimum, preferred and
		// maximum row heights the rowspans are part of
		for (ExtendedGridLayoutConstraints cell : rowspans)
		{
			int fromRow = cell.getRow();
			int rowspan = cell.getEffectiveRowspan();
			int toRow = fromRow + rowspan;
			int currentMinimumRowHeight = 0;
			int currentPreferredRowHeight = 0;
			int currentMaximumRowHeight = 0;
			for (int row=fromRow ; row<toRow ; row++)
			{
				int minimumRowHeight = minimumRowHeights[row];
				if ((Integer.MAX_VALUE-minimumRowHeight) < currentMinimumRowHeight)
				{
					currentMinimumRowHeight = Integer.MAX_VALUE;
				}
				else
				{
					currentMinimumRowHeight += minimumRowHeight;
				}
				int preferredRowHeight = preferredRowHeights[row];
				if ((Integer.MAX_VALUE-preferredRowHeight) < currentPreferredRowHeight)
				{
					currentPreferredRowHeight = Integer.MAX_VALUE;
				}
				else
				{
					currentPreferredRowHeight += preferredRowHeight;
				}
				int maximumRowHeight = maximumRowHeights[row];
				if ((Integer.MAX_VALUE-maximumRowHeight) < currentMaximumRowHeight)
				{
					currentMaximumRowHeight = Integer.MAX_VALUE;
				}
				else
				{
					currentMaximumRowHeight += maximumRowHeight;
				}
			}
			Component component = cell.getComponent();
			int wantedMaximumRowHeight = component.getMaximumSize().height - ((rowspan - 1) * vgap);
			if (currentMaximumRowHeight < wantedMaximumRowHeight)
			{
				redistributeSpace(currentMaximumRowHeight,
						  wantedMaximumRowHeight,
						  fromRow,toRow,
						  maximumRowHeights,
						  maximumRowHeights,
						  maximumRowHeights);
			}
			int wantedMinimumRowHeight = component.getMinimumSize().height - ((rowspan - 1) * vgap);
			if (currentMinimumRowHeight < wantedMinimumRowHeight)
			{
				redistributeSpace(currentMinimumRowHeight,
						  wantedMinimumRowHeight,
						  fromRow,toRow,
						  minimumRowHeights,
						  minimumRowHeights,
						  maximumRowHeights);
			}
			int wantedPreferredRowHeight = component.getPreferredSize().height - ((rowspan - 1) * vgap);
			if (currentPreferredRowHeight < wantedPreferredRowHeight)
			{
				redistributeSpace(currentPreferredRowHeight,
						  wantedPreferredRowHeight,
						  fromRow,toRow,
						  preferredRowHeights,
						  minimumRowHeights,
						  maximumRowHeights);
			}
		}
		
		// correct cases where
		// minimumRowHeights[row] <= preferredRowHeights[row] <= maximumRowHeights[row]
		// is not true by clipping to the minimumRowHeights and maximumRowHeights
		for (int row=0 ; row<gridSize.height ; row++)
		{
			if (minimumRowHeights[row] >= maximumRowHeights[row])
			{
				maximumRowHeights[row] = minimumRowHeights[row];
				preferredRowHeights[row] = minimumRowHeights[row];
			}
			else if (preferredRowHeights[row] < minimumRowHeights[row])
			{
				preferredRowHeights[row] = minimumRowHeights[row];
			}
			else if (preferredRowHeights[row] > maximumRowHeights[row])
			{
				preferredRowHeights[row] = maximumRowHeights[row];
			}
		}
		
		// copies the computed sizes to the result arrays
		if (fillRawSizes)
		{
			resultArrays[0] = minimumColWidths;
			resultArrays[1] = minimumRowHeights;
			resultArrays[2] = preferredColWidths;
			resultArrays[3] = preferredRowHeights;
			resultArrays[4] = maximumColWidths;
			resultArrays[5] = maximumRowHeights;
		}
		
		// sums up the sizes for return value
		int[] colWidths;
		int[] rowHeights;
		switch (layoutSize)
		{
			case MINIMUM:
				colWidths = minimumColWidths;
				rowHeights = minimumRowHeights;
				break;
			
			case PREFERRED:
				colWidths = preferredColWidths;
				rowHeights = preferredRowHeights;
				break;
			
			case MAXIMUM:
				colWidths = maximumColWidths;
				rowHeights = maximumRowHeights;
				break;
			
			default:
				throw new InternalError("Missing case branch for LayoutSize: " + layoutSize);
		}
		long totalWidth = 0;
		long totalHeight = 0;
		for (int width : colWidths)
		{
			totalWidth += width;
		}
		for (int height : rowHeights)
		{
			totalHeight += height;
		}
		
		// add space between components or between
		// componetns and the borders of the parent container
		if (!fillRawSizes)
		{
			Insets insets = parent.getInsets();
			totalWidth += insets.left + insets.right + ((gridSize.width - 1) * hgap) + distanceToBorders.left + distanceToBorders.right;
			totalHeight += insets.top + insets.bottom + ((gridSize.height - 1) * vgap) + distanceToBorders.top + distanceToBorders.bottom;
		}
		
		// clip the size to Integer.MAX_VALUE if too big
		if (totalWidth > Integer.MAX_VALUE)
		{
			totalWidth = Integer.MAX_VALUE;
		}
		if (totalHeight > Integer.MAX_VALUE)
		{
			totalHeight = Integer.MAX_VALUE;
		}
		
		return new Dimension((int)totalWidth,(int)totalHeight);
	}
	
	/**
	  * Builds up the grid for the specified container,
	  * given the components it contains.
	  * 
	  * @param parent   The container to be laid out
	  * @param gridRows In this {@code List<List>} the grid gets stored
	  * @param colspans In this {@code Set} the constraints which are part
	  *                 of a colspan get stored
	  * @param rowspans In this {@code Set} the constraints which are part
	  *                 of a rowspan get stored
	  * @return The amount of rows and columns in the grid
	  */
	private Dimension buildGrid(Container parent, List<List<ExtendedGridLayoutConstraints>> gridRows,
				    Set<ExtendedGridLayoutConstraints> colspans, Set<ExtendedGridLayoutConstraints> rowspans)
	{
		// put the parent's components in source rows
		List<List<ExtendedGridLayoutConstraints>> rows = new ArrayList<List<ExtendedGridLayoutConstraints>>();
		Component[] components = parent.getComponents();
		for (Component component : components)
		{
			if (component.isVisible())
			{
				ExtendedGridLayoutConstraints constraints = lookupConstraints(component).getWorkCopy();
				int rowNumber = constraints.getRow();
				for (int i=rowNumber, c=rows.size() ; i>=c ; i--)
				{
					rows.add(new ArrayList<ExtendedGridLayoutConstraints>());
				}
				List<ExtendedGridLayoutConstraints> row = rows.get(rowNumber);
				row.add(constraints);
			}
		}
		
		// initialize the rowIterators, gridRowIterators and gridRows
		List<Iterator<ExtendedGridLayoutConstraints>> rowIterators = new ArrayList<Iterator<ExtendedGridLayoutConstraints>>();
		List<ListIterator<ExtendedGridLayoutConstraints>> gridRowIterators = new ArrayList<ListIterator<ExtendedGridLayoutConstraints>>();
		boolean haveNext = false;
		for (List<ExtendedGridLayoutConstraints> row : rows)
		{
			Iterator<ExtendedGridLayoutConstraints> rowIterator = row.iterator();
			rowIterators.add(rowIterator);
			if (rowIterator.hasNext())
			{
				haveNext = true;
			}
			List<ExtendedGridLayoutConstraints> gridRow = new ArrayList<ExtendedGridLayoutConstraints>();
			gridRows.add(gridRow);
			gridRowIterators.add(gridRow.listIterator());
		}
		
		// build the grid
		int col = -1;
		while (haveNext)
		{
			col++;
			haveNext = false;
			for (int row=0, c=gridRows.size() ; row<c ; row++)
			{
				Iterator<ExtendedGridLayoutConstraints> rowIterator = rowIterators.get(row);
				ListIterator<ExtendedGridLayoutConstraints> gridRowIterator = gridRowIterators.get(row);
				
				// look for a rowspan in the previous row
				if (row > 0)
				{
					ExtendedGridLayoutConstraints rowspanSource = gridRows.get(row-1).get(col);
					if (null != rowspanSource)
					{
						ExtendedGridLayoutConstraints rowspanPlaceholder = rowspanSource.getRowspanPlaceholder(true);
						if (null != rowspanPlaceholder)
						{
							rowspans.add(rowspanSource);
							gridRowIterator.add(rowspanPlaceholder);
							if (null != rowspanPlaceholder.getColspanPlaceholder(false))
							{
								switch (rowspanPlaceholder.getColspan())
								{
									case REMAINDER:
										break;
									
									default:
										haveNext = true;
								}
							}
							else if (rowIterator.hasNext())
							{
								haveNext = true;
							}
							continue;
						}
					}
				}
				
				// look for a colspan in the previous column
				if (gridRowIterator.hasPrevious())
				{
					ExtendedGridLayoutConstraints colspanSource = gridRowIterator.previous();
					gridRowIterator.next();
					if (null != colspanSource)
					{
						ExtendedGridLayoutConstraints colspanPlaceholder = colspanSource.getColspanPlaceholder(true);
						if (null != colspanPlaceholder)
						{
							colspans.add(colspanSource);
							gridRowIterator.add(colspanPlaceholder);
							if (null != colspanPlaceholder.getColspanPlaceholder(false))
							{
								switch (colspanPlaceholder.getColspan())
								{
									case REMAINDER:
										break;
									
									default:
										haveNext = true;
								}
							}
							else if (rowIterator.hasNext())
							{
								haveNext = true;
							}
							continue;
						}
					}
				}
				
				// add a new element or null
				if (rowIterator.hasNext())
				{
					ExtendedGridLayoutConstraints newConstraints = rowIterator.next();
					newConstraints.setCol(col);
					gridRowIterator.add(newConstraints);
					if (null != newConstraints.getColspanPlaceholder(false))
					{
						switch (newConstraints.getColspan())
						{
							case REMAINDER:
								break;
							
							default:
								haveNext = true;
						}
					}
					else if (rowIterator.hasNext())
					{
						haveNext = true;
					}
				}
				else
				{
					gridRowIterator.add(null);
				}
			}
		}
		
		// check the last gridRow for rowspans and probably add rows for these
		haveNext = false;
		int gridRowsSize = gridRows.size();
		if (gridRowsSize > 0)
		{
			ListIterator<ExtendedGridLayoutConstraints> gridRowIterator = gridRows.get(gridRows.size()-1).listIterator();
			while (gridRowIterator.hasNext())
			{
				ExtendedGridLayoutConstraints cell = gridRowIterator.next();
				if ((null != cell) &&
				    ((REMAINDER != cell.getRowspan()) &&
				     (null != cell.getRowspanPlaceholder(false))))
				{
					haveNext = true;
					break;
				}
			}
			while (haveNext)
			{
				haveNext = false;
				gridRowIterator = gridRows.get(gridRows.size()-1).listIterator();
				List<ExtendedGridLayoutConstraints> gridRow = new ArrayList<ExtendedGridLayoutConstraints>();
				gridRows.add(gridRow);
				ListIterator<ExtendedGridLayoutConstraints> newGridRowIterator = gridRow.listIterator();
				while (gridRowIterator.hasNext())
				{
					ExtendedGridLayoutConstraints cell = gridRowIterator.next();
					if ((null != cell) &&
					    (null != cell.getRowspanPlaceholder(false)))
					{
						rowspans.add(cell);
						ExtendedGridLayoutConstraints rowspanPlaceholder = cell.getRowspanPlaceholder(true);
						newGridRowIterator.add(rowspanPlaceholder);
					}
					else
					{
						newGridRowIterator.add(null);
					}
				}
				gridRowIterator = gridRow.listIterator();
				while (gridRowIterator.hasNext())
				{
					ExtendedGridLayoutConstraints cell = gridRowIterator.next();
					if ((null != cell) &&
					    ((REMAINDER != cell.getRowspan()) &&
					     (null != cell.getRowspanPlaceholder(false))))
					{
						haveNext = true;
						break;
					}
				}
			}
		}
		
		return new Dimension(col+1,gridRows.size());
	}
	
	/**
	  * Returns a string representation of the object. In general, the
	  * {@code toString} method returns a string that
	  * "textually represents" this object. The result should
	  * be a concise but informative representation that is easy for a
	  * person to read.
	  * 
	  * @return  a string representation of the object.
	  */
	public String toString()
	{
		return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap
			+ ",distanceToBorders=" + distanceToBorders
			+ ",comptable=" + comptable + "]";
	}
}
