/*
 * Progress.java
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
 * An interface for reporting installation progress. ConsoleProgress and
 * SwingProcess are the two existing implementations.
 */
public interface Progress
{
	public void setMaximum(int max);

	public void advance(int value);

	public void done();

	public void message(String message);
	public void error(String message);
}
