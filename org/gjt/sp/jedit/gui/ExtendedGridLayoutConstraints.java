/*
 * ExtendedGridLayoutConstraints.java - a constraints clss for the ExtendedGridLayout
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

/**
 * Specifies constraints for components
 * that are laid out using the {@code ExtendedGridLayout} class.
 *
 * @version 1.0
 * @author  Björn "Vampire" Kautler
 * @see     ExtendedGridLayout
 * @since   jEdit 4.3pre10
 */
public class ExtendedGridLayoutConstraints
{
	/**
	  * Specifies that this component is the
	  * last component in its column or row
	  * and takes up the remaining space.
	  */
	public static final int REMAINDER = Integer.MAX_VALUE;

	/**
	  * Specifies the row in which a component starts its display area.
	  * {@code row} has to be non-negative and the default
	  * value is 0.
	  */
	private int row;

	/**
	  * Specifies the column in which a component starts its display area.
	  * {@code col} has to be non-negative.
	  */
	private int col;

	/**
	  * Specifies the number of cells in a row for the
	  * component's display area.
	  * <p>
	  * Use {@code REMAINDER} to specify that the component's
	  * display area will be from its grid position to the last
	  * cell in the row.
	  * <p>
	  * {@code colspan} has to be {@literal >= 1} and the default
	  * value is 1.
	  *
	  * @see #REMAINDER
	  * @see #rowspan
	  */
	private int colspan;

	/**
	  * Specifies the effective number of cells in a row for the
	  * component's display area. This is used internally
	  * to get the effective number of cells in a row in cases
	  * where {@code REMAINDER} is used for colspan.
	  *
	  * @see #REMAINDER
	  * @see #colspan
	  */
	private int effectiveColspan;

	/**
	  * Specifies the number of cells in a column for the
	  * component's display area.
	  * <p>
	  * Use {@code REMAINDER} to specify that the component's
	  * display area will be from its grid position to the last
	  * cell in the column.
	  * <p>
	  * {@code rowspan} has to be {@literal >= 1} and the default
	  * value is 1.
	  *
	  * @see #REMAINDER
	  * @see #colspan
	  */
	private int rowspan;

	/**
	  * Specifies the effective number of cells in a column for the
	  * component's display area. This is used internally
	  * to get the effective number of cells in a column in cases
	  * where {@code REMAINDER} is used for rowspan.
	  *
	  * @see #REMAINDER
	  * @see #rowspan
	  */
	private int effectiveRowspan;

	/**
	  * Specifies if this Constraint is used as placeholder to build the grid.
	  * This is used internally and the default value is {@code false}.
	  */
	private boolean placeholder;

	/**
	  * Specifies the mainConstraints object for which this constraints
	  * object is a placeholder. If this constraints object is no placeholder,
	  * mainConstraints is set to {@code null}.
	  */
	private ExtendedGridLayoutConstraints mainConstraints;

	/**
	  * Specifies the {@code Component} this constraints object describes.
	  */
	private Component component;

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object with
	  * all of its fields set to their default value. For further information
	  * about the default values see
	  * {@link #ExtendedGridLayoutConstraints(int, int, int, java.awt.Component)}.
	  *
	  * @param component The {@code Component} this constraints object describes
	  */
	public ExtendedGridLayoutConstraints(Component component)
	{
		this(0,0,1,1,component,false,null);
	}

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object with
	  * all of its fields set to their default value
	  * except of the row which is specified. For further information
	  * about the default values see
	  * {@link #ExtendedGridLayoutConstraints(int, int, int, java.awt.Component)}.
	  *
	  * @param row       The row in which a component starts its display area. First row is 0
	  * @param component The {@code Component} this constraints object d describes
	  * @throws IllegalArgumentException If row {@literal < 0}
	  */
	public ExtendedGridLayoutConstraints(int row, Component component)
	{
		this(row,0,1,1,component,false,null);
	}

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object with
	  * all of its fields set to the passed-in arguments.
	  *
	  * @param row       The row in which a component starts its display area.
	  *                  First row is 0. Default value is 0.
	  * @param colspan   The number of cells in a row for the component's display area.
	  *                  Use {@code REMAINDER} to specify that the component's
	  *                  display area will be from its grid position to the last
	  *                  cell in the row. Default value is 1.
	  * @param rowspan   The number of cells in a column for the component's display area.
	  *                  Use {@code REMAINDER} to specify that the component's
	  *                  display area will be from its grid position to the last
	  *                  cell in the column. Default value is 1.
	  * @param component The {@code Component} this constraints object describes
	  * @throws IllegalArgumentException If row {@literal < 0}
	  * @throws IllegalArgumentException If colspan {@literal < 1}
	  * @throws IllegalArgumentException If rowspan {@literal < 1}
	  */
	public ExtendedGridLayoutConstraints(int row, int colspan, int rowspan, Component component)
	{
		this(row,0,colspan,rowspan,component,false,null);
	}

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object with
	  * all of its fields set to the passed-in arguments.
	  *
	  * @param row             The row in which a component starts its display area.
	  *                        First row is 0.
	  * @param col             The col in which a component starts its display area.
	  *                        First col is 0.
	  * @param colspan         The number of cells in a row for the component's display area.
	  *                        Use {@code REMAINDER} to specify that the component's
	  *                        display area will be from its grid position to the last
	  *                        cell in the row.
	  * @param rowspan         The number of cells in a column for the component's display area.
	  *                        Use {@code REMAINDER} to specify that the component's
	  *                        display area will be from its grid position to the last
	  *                        cell in the column.
	  * @param component       The {@code Component} this constraints object describes
	  * @param placeholder     If this constraints are used as placeholder to build the grid
	  * @param mainConstraints The mainConstraints object for which this constraints
	  *                        object is a placeholder
	  * @throws IllegalArgumentException If row {@literal < 0}
	  * @throws IllegalArgumentException If col {@literal < 0}
	  * @throws IllegalArgumentException If colspan {@literal < 1}
	  * @throws IllegalArgumentException If rowspan {@literal < 1}
	  */
	private ExtendedGridLayoutConstraints(int row, int col, int colspan, int rowspan, Component component, boolean placeholder, ExtendedGridLayoutConstraints mainConstraints)
	{
		if (row < 0)
		{
			throw new IllegalArgumentException("row must be non-negative (" + row + ')');
		}
		if (col < 0)
		{
			throw new IllegalArgumentException("col must be non-negative (" + col + ')');
		}
		if (colspan < 1)
		{
			throw new IllegalArgumentException("colspan must be at least 1 (" + colspan + ')');
		}
		if (rowspan < 1)
		{
			throw new IllegalArgumentException("rowspan must be at least 1 (" + rowspan + ')');
		}
		this.row = row;
		this.col = col;
		this.colspan = colspan;
		effectiveColspan = 1;
		this.rowspan = rowspan;
		effectiveRowspan = 1;
		this.component = component;
		this.placeholder = placeholder;
		this.mainConstraints = mainConstraints;
	}

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object which can be
	  * used as placeholder for building a grid with colspans.
	  *
	  * @param forUsage If the returned object will be used in the grid
	  *                 and therefor the effectiveColspan should be raised by one
	  * @return The newly created {@code ExtendedGridLayoutConstraints}
	  *         object or {@code null} if no colspan is applicable
	  * @see #getRowspanPlaceholder(boolean)
	  */
	ExtendedGridLayoutConstraints getColspanPlaceholder(boolean forUsage)
	{
		if (1 == colspan)
		{
			return null;
		}
		ExtendedGridLayoutConstraints result = new ExtendedGridLayoutConstraints(row,col+1,colspan==REMAINDER ? REMAINDER : colspan-1,rowspan,component,true,null == mainConstraints ? this : mainConstraints);
		if (forUsage && (result.mainConstraints.row == row))
		{
			result.mainConstraints.effectiveColspan++;
		}
		return result;
	}

	/**
	  * Creates an {@code ExtendedGridLayoutConstraints} object which can be
	  * used as placeholder for building a grid with rowspans.
	  *
	  * @param forUsage If the returned object will be used in the grid
	  *                 and therefor the effectiveRowspan should be raised by one
	  * @return The newly created {@code ExtendedGridLayoutConstraints}
	  *         object or {@code null} if no rowspan is applicable
	  * @see #getColspanPlaceholder(boolean)
	  */
	ExtendedGridLayoutConstraints getRowspanPlaceholder(boolean forUsage)
	{
		if (1 == rowspan)
		{
			return null;
		}
		ExtendedGridLayoutConstraints result = new ExtendedGridLayoutConstraints(row+1,col,colspan,rowspan==REMAINDER ? REMAINDER : rowspan-1,component,true,null == mainConstraints ? this : mainConstraints);
		if (forUsage && (result.mainConstraints.col == col))
		{
			result.mainConstraints.effectiveRowspan++;
		}
		return result;
	}

	/**
	  * @return The row in which the component starts its display area.
	  */
	public int getRow()
	{
		return row;
	}

	/**
	  * @return The column in which the component starts its display area.
	  */
	public int getCol()
	{
		return col;
	}

	/**
	  * @param col The column in which the component starts its display area.
	  */
	void setCol(int col)
	{
		if (col < 0)
		{
			throw new IllegalArgumentException("col must be non-negative (" + col + ')');
		}
		this.col = col;
	}

	/**
	  * @return The number of cells in a row for the component's display area
	  *         or {@code REMAINDER} if the component's display area will be
	  *         from its grid position to the last cell in the row.
	  */
	public int getColspan()
	{
		return colspan;
	}

	/**
	  * @return The effective number of cells in a row for the component's display area.
	  */
	int getEffectiveColspan()
	{
		return null == mainConstraints ? effectiveColspan : mainConstraints.effectiveColspan;
	}

	/**
	  * @return The number of cells in a column for the component's display area
	  *         or {@code REMAINDER} if the component's display area will be
	  *         from its grid position to the last cell in the column.
	  */
	public int getRowspan()
	{
		return rowspan;
	}

	/**
	  * @return The effective number of cells in a column for the component's display area.
	  */
	int getEffectiveRowspan()
	{
		return null == mainConstraints ? effectiveRowspan : mainConstraints.effectiveRowspan;
	}

	/**
	  * @return The {@code Component} this constraints object describes
	  */
	Component getComponent()
	{
		return component;
	}

	/**
	  * @return Whether this constraints object is a placeholder or not
	  */
	public boolean isPlaceholder()
	{
		return placeholder;
	}

	/**
	  * @return A work copy if this constraints object. This is a flat copy
	  *         which means that the reference to the component stays the same.
	  *         The returned object could be used without modifying this
	  *         constraints object.
	  */
	ExtendedGridLayoutConstraints getWorkCopy()
	{
		return new ExtendedGridLayoutConstraints(row,col,colspan,rowspan,component,placeholder,(null == mainConstraints ? null : mainConstraints.getWorkCopy()));
	}

	/**
	  * Indicates whether some other object is "equal to" this one.
	  * <p>
	  * The {@code equals} method implements an equivalence relation
	  * on non-null object references:
	  * <ul>
	  * <li>It is <i>reflexive</i>: for any non-null reference value
	  *     {@code x}, {@code x.equals(x)} returns
	  *     {@code true}.
	  * <li>It is <i>symmetric</i>: for any non-null reference values
	  *     {@code x} and {@code y}, {@code x.equals(y)}
	  *     returns {@code true} if and only if
	  *     {@code y.equals(x)} returns {@code true}.
	  * <li>It is <i>transitive</i>: for any non-null reference values
	  *     {@code x}, {@code y}, and {@code z}, if
	  *     {@code x.equals(y)} returns {@code true} and
	  *     {@code y.equals(z)} returns {@code true}, then
	  *     {@code x.equals(z)} returns {@code true}.
	  * <li>It is <i>consistent</i>: for any non-null reference values
	  *     {@code x} and {@code y}, multiple invocations of
	  *     <tt>x.equals(y)</tt> consistently return {@code true}
	  *     or consistently return {@code false}, provided no
	  *     information used in {@code equals} comparisons on the
	  *     objects is modified.
	  * <li>For any non-null reference value {@code x},
	  *     {@code x.equals(null)} returns {@code false}.
	  * </ul>
	  * <p>
	  * The <tt>equals</tt> method for class
	  * {@code ExtendedGridLayoutConstraints} returns {@code true}
	  * if and only if the constraints objects describe the same {@code Component}
	  *
	  * @param o the reference object with which to compare.
	  * @return {@code true} if this object is the same as the o
	  *         argument; {@code false} otherwise.
	  * @see #hashCode()
	  * @see <a href="http://download.oracle.com/javase/6/docs/api/java/util/Hashtable.html"><code>java.util.Hashtable</code></a>
	  */
	public boolean equals(Object o)
	{
		if ((o == null) ||
		    (!(o instanceof ExtendedGridLayoutConstraints)))
		{
			return false;
		}
		if (component == null)
		{
			return ((ExtendedGridLayoutConstraints)o).component == null;
		}
		return component.equals(((ExtendedGridLayoutConstraints)o).component);
	}

	/**
	  * Returns a hash code value for the object. This method is
	  * supported for the benefit of hashtables such as those provided by
	  * {@code java.util.Hashtable}.
	  * <p>
	  * The general contract of {@code hashCode} is:
	  * <ul>
	  * <li>Whenever it is invoked on the same object more than once during
	  *     an execution of a Java application, the <tt>hashCode</tt> method
	  *     must consistently return the same integer, provided no information
	  *     used in <tt>equals</tt> comparisons on the object is modified.
	  *     This integer need not remain consistent from one execution of an
	  *     application to another execution of the same application.
	  * <li>If two objects are equal according to the <tt>equals(Object)</tt>
	  *     method, then calling the {@code hashCode} method on each of
	  *     the two objects must produce the same integer result.
	  * <li>It is <em>not</em> required that if two objects are unequal
	  *     according to the
	  *     <a href="http://download.oracle.com/javase/6/docs/api/java/lang/Object.html#equals(java.lang.Object)">{@code java.lang.Object#equals(java.lang.Object)}</a>
	  *     method, then calling the <tt>hashCode</tt> method on each of the
	  *     two objects must produce distinct integer results.  However, the
	  *     programmer should be aware that producing distinct integer results
	  *     for unequal objects may improve the performance of hashtables.
	  * </ul>
	  *
	  * @return a hash code value for this object.
	  * @see #equals(java.lang.Object)
	  * @see <a href="http://download.oracle.com/javase/6/docs/api/java/util/Hashtable.html"><code>java.util.Hashtable</code></a>
	  */
	public int hashCode()
	{
		if (null == component)
		{
			return 0;
		}
		return component.hashCode();
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
		return getClass().getName() + "[row=" + row + ",col=" + col
			+ ",colspan=" + colspan + ",effectiveColspan=" + effectiveColspan
			+ ",rowspan=" + rowspan + ",effectiveRowspan=" + effectiveRowspan
			+ ",placeholder=" + placeholder + ",component=" + component
			+ ",mainConstraints=" + mainConstraints + "]";
	}
}
