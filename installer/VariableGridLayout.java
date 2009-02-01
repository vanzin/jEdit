/*
 * VariableGridLayout.java - a grid layout manager with variable cell sizes
 *
 * Originally written by Dirk Moebius for the jEdit project. This work has been
 * placed into the public domain. You may use this work in any way and for any
 * purpose you wish.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package installer;


import java.awt.*;

// This is copied from jEdit's org.gjt.sp.jedit.gui package.

/**
 * The <code>VariableGridLayout</code> class is a layout manager
 * that lays out a container's components in a rectangular grid
 * with variable cell sizes.<p>
 *
 * The container is divided into rectangles, and one component is placed
 * in each rectangle. Each row is as large as the largest component in
 * that row, and each column is as wide as the widest component in
 * that column.<p>
 *
 * This behavior is basically the same as in
 * <code>java.awt.GridLayout</code>, but with different row heights and
 * column widths for each row/column.<p>
 *
 * For example, the following is an applet that lays out six buttons
 * into three rows and two columns:<p>
 *
 * <blockquote><pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * public class ButtonGrid extends Applet {
 *     public void init() {
 *         setLayout(new VariableGridLayout(VariableGridLayout.FIXED_NUM_COLUMNS, 2));
 *         add(new Button("1"));
 *         add(new Button("2"));
 *         add(new Button("3"));
 *         add(new Button("4"));
 *         add(new Button("5"));
 *         add(new Button("6"));
 *     }
 * }
 * </pre></blockquote><p>
 *
 * <b>Programmer's remark:</b> VariableGridLayout could be faster, if it would
 * reside in the package java.awt, because then it could access some
 * package private fields of <code>Container</code> or
 * <code>Component</code>. Instead, it has to call
 * <code>Component.getSize()</code>,
 * which allocates memory on the heap.<p>
 *
 * <b>Todo:</b>
 * <ul>
 * <li>Use alignmentX/Y property if the grid cell is larger than the preferred size of the component.
 * <li>Ability to span components over more than one cell horizontally
 * </ul>
 *
 * @author Dirk Moebius
 * @version 1.0
 * @see java.awt.GridLayout
 */
public class VariableGridLayout implements LayoutManager2, java.io.Serializable
{

	public static final int FIXED_NUM_ROWS = 1;
	public static final int FIXED_NUM_COLUMNS = 2;


	public VariableGridLayout(int mode, int size, int hgap, int vgap) {
		if (mode != FIXED_NUM_ROWS && mode != FIXED_NUM_COLUMNS) {
			throw new IllegalArgumentException("illegal mode; value is " + mode);
		}
		if (size <= 0) {
			throw new IllegalArgumentException("size cannot be zero or less; value is " + size);
		}
		if (hgap < 0) {
			throw new IllegalArgumentException("hgap cannot be negative; value is " + hgap);
		}
		if (vgap < 0) {
			throw new IllegalArgumentException("vgap cannot be negative; value is " + vgap);
		}
		this.mode = mode;
		this.size = size;
		this.hgap = hgap;
		this.vgap = vgap;
	}


	/**
	 * Creates a variable grid layout manager with the specified mode
	 * and zero horizontal and vertical gap.
	 */
	public VariableGridLayout(int mode, int size) {
		this(mode, size, 0, 0);
	}


	/**
	 * Creates a variable grid layout manager with mode FIXED_NUM_ROWS,
	 * number of rows == 1 and zero horizontal and vertical gap.
	 */
	public VariableGridLayout() {
		this(FIXED_NUM_ROWS, 1, 0, 0);
	}


	/**
	 * Not used in this class.
	 */
	public void addLayoutComponent(String name, Component component) { }


	/**
	 * Not used in this class.
	 */
	public void addLayoutComponent(Component component, Object constraints) { }


	/**
	 * Not used in this class.
	 */
	public void removeLayoutComponent(Component component) { }


	/**
	 * Always returns 0.5.
	 */
	public float getLayoutAlignmentX(Container container) {
		return 0.5f;
	}


	/**
	 * Always returns 0.5.
	 */
	public float getLayoutAlignmentY(Container container) {
		return 0.5f;
	}


	public Dimension preferredLayoutSize(Container parent) {
		return getLayoutSize(parent, 2);
	}


	public Dimension minimumLayoutSize(Container parent) {
		return getLayoutSize(parent, 0);
	}


	public Dimension maximumLayoutSize(Container parent) {
		return getLayoutSize(parent, 1);
	}


	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			update(parent);

			int ncomponents = parent.getComponentCount();

			if (ncomponents == 0) {
				return;
			}

			// Pass 1: compute preferred row heights / column widths
			int total_height = 0;
			for (int r = 0, i = 0; r < nrows; r++) {
				for (int c = 0; c < ncols; c++, i++) {
					if (i < ncomponents) {
						Dimension d = parent.getComponent(i).getPreferredSize();
						row_heights[r] = Math.max(row_heights[r], d.height);
						col_widths[c] = Math.max(col_widths[c], d.width);
					} else {
						break;
					}
				}
				total_height += row_heights[r];
			}

			int total_width = 0;
			for (int c = 0; c < ncols; c++) {
				total_width += col_widths[c];
			}

			// Pass 2: redistribute free space
			Dimension parent_size = parent.getSize();
			Insets insets = parent.getInsets();
			int free_height = parent_size.height - insets.top - insets.bottom - (nrows - 1) * vgap;
			int free_width = parent_size.width - insets.left - insets.right - (ncols - 1) * hgap;

			if (total_height != free_height) {
				double dy = (double)free_height / (double)total_height;
				for (int r = 0; r < nrows; r++) {
					row_heights[r] = (int) ((double)row_heights[r] * dy);
				}
			}

			if (total_width != free_width) {
				double dx = ((double)free_width) / ((double)total_width);
				for (int c = 0; c < ncols; c++) {
					col_widths[c] = (int) ((double)col_widths[c] * dx);
				}
			}

			// Pass 3: layout components
			for (int r = 0, y = insets.top, i = 0; r < nrows; y += row_heights[r] + vgap, r++) {
				for (int c = 0, x = insets.left; c < ncols; x += col_widths[c] + hgap, c++, i++) {
					if (i < ncomponents) {
						parent.getComponent(i).setBounds(x, y, col_widths[c], row_heights[r]);
					}
				}
			}

		} // synchronized
	}


	public void invalidateLayout(Container container) {}


	/**
	 * Returns the string representation of this variable grid layout's values.
	 * @return  a string representation of this variable grid layout.
	 */
	public String toString() {
		return getClass().getName() + "[mode=" + mode + ",size=" + size
			   + ",hgap=" + hgap + ",vgap=" + vgap + "]";
	}


	/**
	 * @param  which  if 0 compute minimum layout size,
	 *				if 1 compute maximum layout size,
	 *				otherwise compute preferred layout size.
	 */
	private Dimension getLayoutSize(Container parent, int which) {
		synchronized (parent.getTreeLock()){
			update(parent);

			int ncomponents = parent.getComponentCount();
			int h = 0;
			int w = 0;

			for (int r = 0, i = 0; r < nrows; r++) {
				int row_height = 0;
				for (int c = 0; c < ncols; c++, i++) {
					if (i < ncomponents) {
						switch (which) {
							case 0:
								row_height = Math.max(row_height, parent.getComponent(i).getMinimumSize().height);
								break;
							case 1:
								row_height = Math.max(row_height, parent.getComponent(i).getMaximumSize().height);
								break;
							default:
								row_height = Math.max(row_height, parent.getComponent(i).getPreferredSize().height);
								break;
						}
					} else {
						break;
					}
				}
				h += row_height;
			}

			for (int c = 0; c < ncols; c++) {
				int col_width = 0;
				for (int r = 0; r < nrows; r++) {
					int i = r * ncols + c;
					if (i < ncomponents) {
						switch (which) {
							case 0:
								col_width = Math.max(col_width, parent.getComponent(i).getMinimumSize().width);
								break;
							case 1:
								col_width = Math.max(col_width, parent.getComponent(i).getMaximumSize().width);
								break;
							default:
								col_width = Math.max(col_width, parent.getComponent(i).getPreferredSize().width);
								break;
						}
					} else {
						break;
					}
				}
				w += col_width;
			}

			Insets insets = parent.getInsets();
			return new Dimension(w + insets.left + insets.right + ((ncols - 1) * hgap),
								 h + insets.top + insets.bottom + ((nrows - 1) * vgap));
		}
	}


	private void update(Container container) {
		int ncomponents = container.getComponentCount();
		int old_nrows = nrows;
		int old_ncols = ncols;
		if (this.mode == FIXED_NUM_ROWS) {
			nrows = this.size;
			ncols = (ncomponents + nrows - 1) / nrows;
		} else {
			ncols = this.size;
			nrows = (ncomponents + ncols - 1) / ncols;
		}
		if (old_nrows != nrows) {
			row_heights = new int[nrows];
		}
		if (old_ncols != ncols) {
			col_widths = new int[ncols];
		}
	}


	private int mode;
	private int size;
	private int hgap;
	private int vgap;
	private transient int nrows = -1;
	private transient int ncols = -1;
	private transient int[] row_heights = null;
	private transient int[] col_widths = null;
}
