/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
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

package org.gjt.sp.jedit.pluginmgr;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;
import org.jedit.io.HttpException;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * @author Matthieu Casanova
 */
public class RemotePluginList
{
	/**
	 * Magic numbers used for auto-detecting GZIP files.
	 */
	public static final int GZIP_MAGIC_1 = 0x1f;
	public static final int GZIP_MAGIC_2 = 0x8b;

	private final ProgressObserver progressObserver;
	private final String id;

	//{{{ RemotePluginList constructor
	public RemotePluginList(ProgressObserver progressObserver, String id)
	{
		this.progressObserver = progressObserver;
		this.id = id;
	} //}}}

	//{{{ openPluginListStream() method
	@Nonnull
	String getPluginList() throws IOException, MalformedURLException, HttpException
	{

		progressObserver.setStatus(jEdit.getProperty("plugin-manager.list-download"));
		String gzipURL = jEdit.getProperty("plugin-manager.export-url") + "?mirror=" + buildMirror(id) + "&new_url_scheme";

		/* download the plugin list, while trying to show informative error messages.
		 * Currently when :
		 * - the proxy requires authentication
		 * - another HTTP error happens (may be good to know that the site is broken)
		 * - the host can't be reached (reported as internet access error)
		 * Otherwise, only an error message is logged in the activity log.
		 */

		URL downloadURL = new URL(gzipURL);
		HttpURLConnection httpURLConnection = (HttpURLConnection)downloadURL.openConnection();
		int responseCode = httpURLConnection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK)
		{
			long start = System.currentTimeMillis();
			try (InputStream inputStream = openPluginListStream(httpURLConnection.getInputStream()))
			{
				String xml = IOUtilities.toString(inputStream);
				jEdit.setProperty("plugin-manager.mirror.cached-id", id);
				Log.log(Log.MESSAGE, this, "Updated cached pluginlist " + (System.currentTimeMillis() - start));
				return xml;
			}
		}
		else
		{
			throw new HttpException(responseCode, httpURLConnection.getResponseMessage());
		}
	} //}}}

	//{{{ openPluginListStream() method
	static InputStream openPluginListStream(InputStream inputStream) throws IOException
	{
		InputStream in = new BufferedInputStream(inputStream);
		if(in.markSupported())
		{
			in.mark(2);
			int b1 = in.read();
			int b2 = in.read();
			in.reset();

			if(b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2)
				in = new GZIPInputStream(in);
		}
		return in;
	} //}}}

	//{{{ buildMirror() method
	@Nonnull
	private static String buildMirror(String id)
	{
		return id != null && !MirrorList.Mirror.NONE.equals(id) ? id : "default";
	} //}}}
}
