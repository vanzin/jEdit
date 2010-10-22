/**
 * 
 */
package org.gjt.sp.jedit.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.Task;

/**
 * The Task for asynchronous saving local file to disk ommiting VFS API stack.
 * Can be used to store backup files, settings, etc
 * 
 * @example
 * 	This class used for example by ftp.FtpVFS to perform local backups of remote files in background with no GUI freeze
 * 
 * @example
 * 	ThreadUtilities.runInBackground( new LocalFileSaveTask(...) )
 * 
 * @author Vadim Voituk
 * @since jEdit 4.5pre
 * 
 * TODO: Add setLabel() && ProgressObserver methods
 */
public class LocalFileSaveTask extends Task 
{

	private static final int BUFFER_SIZE = 4096;
	
	private File file;
	private String body;
	private String charset;

	public LocalFileSaveTask(File file, String body, String charset) 
	{
		this.file = file;
		this.body = body;
		this.charset = charset;
	}
	
	/**
	 * @see org.gjt.sp.util.Task#_run()
	 */
	@Override
	public void _run() 
	{
		FileOutputStream os = null;
		FileChannel ch      = null;
		try 
		{
			os = new FileOutputStream(file);
			ch = os.getChannel();
			byte[] src = body.getBytes(charset);
			
			os = new FileOutputStream(file);
			ch = os.getChannel();
			
			ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
			
			int length = src.length;
			setMaximum(length);
			setStatus("Saving " + length + " bytes to " + file.getPath() ); //TODO: Change this
			
			int written = 0;
			
			while (written < length) 
			{
				written += ch.write( (ByteBuffer)buff.put(src, written, Math.min(BUFFER_SIZE, length-written) ).flip() );
				buff.rewind();
				setValue(written);
			}
			
		} 
		catch (IOException e) 
		{
			Log.log(Log.ERROR, this, e, e);
		} 
		finally 
		{
			IOUtilities.closeQuietly(ch);
			IOUtilities.closeQuietly(os);
		}
	}

}
