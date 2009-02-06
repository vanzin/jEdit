/*
 * ConsoleProgress.java
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

/*
 * Displays install progress when running in text-only mode.
 */
public class ConsoleProgress implements Progress
{

	public void setMaximum(int max)
	{
	}

	public void advance(int value)
	{
	}

	public void done()
	{
		System.out.println("*** Installation complete");
	}

	public void message(String message)
	{
		System.out.println(message);
	}

	public void error(String message)
	{
		System.err.println("*** An error occurred: " + message);
	}
}
