/*
 * ConsoleProgress.java
 * Copyright (C) 1999, 2001 Slava Pestov
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

	public void error(String message)
	{
		System.err.println("*** An error occurred: " + message);
	}
}
