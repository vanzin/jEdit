/*
 * JEditMetalTheme.java - Minor Metal L&F tweaks for jEdit installer
 *
 * Originally written by Slava Pestov for the jEdit installer project. This work
 * has been placed into the public domain. You may use this work in any way and
 * for any purpose you wish.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package installer;

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;
import java.awt.Font;

public class JEditMetalTheme extends DefaultMetalTheme
{
	public String getName()
	{
		return "jEdit";
	}

	public ColorUIResource getSystemTextColor()
	{
		return getBlack();
	}

	public FontUIResource getControlTextFont()
	{
		return primaryFont;
	}

	public FontUIResource getSystemTextFont()
	{
		return secondaryFont;
	}

	public FontUIResource getUserTextFont()
	{
		return secondaryFont;
	}

	public FontUIResource getMenuTextFont()
	{
		return primaryFont;
	}

	// private members
	private FontUIResource primaryFont = new FontUIResource("Dialog",
		Font.PLAIN,12);
	private FontUIResource secondaryFont = new FontUIResource("Dialog",
		Font.PLAIN,12);
}
