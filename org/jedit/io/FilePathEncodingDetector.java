/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2012 jEdit contributors
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

package org.jedit.io;

import org.gjt.sp.jedit.io.EncodingDetector;

/**
 * An interface to detect a reasonable encoding from some bytes at the
 * beginning of a file or the file path. To offer your own EncodingDetector,
 * implement this interface and define a service in your <tt>services.xml</tt> file.
 * For example:<pre>
 * &lt;SERVICE CLASS=&quot;org.gjt.sp.jedit.io.EncodingDetector&quot; NAME=&quot;XML-PI&quot;&gt;
 * 	new XMLEncodingDetector();
 * &lt;/SERVICE&gt;</pre>
 *
 * @since 5.1pre1
 */
public interface FilePathEncodingDetector extends EncodingDetector
{
}
