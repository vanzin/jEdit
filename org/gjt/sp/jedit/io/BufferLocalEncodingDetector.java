/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Kazutoshi Satoda
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

package org.gjt.sp.jedit.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * An encoding detector which finds buffer-local-property syntax.
 *
 * This reads the sample in the system default encoding for first 10
 * lines and look for ":encoding=..." syntax. This can fail if the
 * stream cannot be read in the system default encoding or
 * ":encoding=..." is not placed at near the top of the stream.
 *
 * @since 4.3pre10
 * @author Kazutoshi Satoda
 */
public class BufferLocalEncodingDetector implements EncodingDetector
{
	public String detectEncoding(InputStream sample) throws IOException
	{
		BufferedReader reader
			= new BufferedReader(new InputStreamReader(sample));
		int i = 0;
		while (i < 10)
		{
			i++;
			String line = reader.readLine();
			if (line == null)
				return null;
			int pos = line.indexOf(":encoding=");
			if (pos != -1)
			{
				int p2 = line.indexOf(':', pos + 10);
				String encoding = line.substring(pos + 10, p2);
				return encoding;
			}
		}
		return null;
	}
}
