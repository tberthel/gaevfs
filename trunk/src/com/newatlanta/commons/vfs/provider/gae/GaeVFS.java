/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.commons.vfs.provider.gae;

import static com.newatlanta.appengine.nio.channels.GaeFileLock.releaseAllLocks;

import java.util.logging.Logger;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import com.newatlanta.appengine.servlet.GaeVfsServletEventListener;

/**
 * This class is the entry point for interacting with the Google App Engine Virtual
 * File System (GaeVFS). Its primary function is to create and initialize the
 * {@link GaeFileSystemManager} static instance, which is done via the static
 * {@link GaeVFS#getManager()} method. Nearly all other interaction with GaeVFS
 * is done via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>.
 * <blockquote>
 * <b>IMPORTANT!</b> Do not use the <code>org.apache.commons.vfs.VFS.getManager()</code>
 * method provided by Commons VFS to get a <code>FileSystemManager</code> when running
 * within GAE.
 * </blockquote>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeVFS {
    
    static final Logger log = Logger.getLogger( GaeVFS.class.getName() );

    public static final int MAX_BLOCK_SIZE = 1000; // in units of KB
    public static final int MIN_BLOCK_SIZE = 8; // in units of KB
    
    private static final int DEFAULT_BLOCK_SIZE = 1024 * 128;
    
    static {
        // GAE doesn't set these values; Commons VFS will fail to initialize if
        // they're not set, so do it here
        System.setProperty( "os.arch", "" );
        System.setProperty( "os.version", "" );
    }

    private static GaeFileSystemManager fsManager;
    private static int blockSize = DEFAULT_BLOCK_SIZE;
    
    private GaeVFS() {
    }

    /**
     * Creates and initializes the {@link GaeFileSystemManager} static instance.
     * 
     * @return The {@link GaeFileSystemManager} static instance.
     * @throws FileSystemException
     */
    public static GaeFileSystemManager getManager() throws FileSystemException {
        if ( fsManager == null ) {
            fsManager = new GaeFileSystemManager();
            fsManager.init( System.getProperty( "user.dir" ) );
        }
        return fsManager;
    }
    
    /**
     * Gets the current default block size used when creating new files.
     * 
     * @return The current default block size as an absolute number of bytes.
     */
    public static int getBlockSize() {
        return blockSize;
    }
    
    /**
     * Sets the default block size used when creating new files. GaeVFS stores
     * files as a series of blocks. Each block corresponds to a Google App Engine
     * datastore entity and therefore has a maximum size of 1 megabyte (due to
     * entity overhead, the actual limit is 1000 * 1024 = 1,024,000 bytes). The
     * default block size is 128KB (131,072 bytes).
     * 
     * @param size The default block size in units of K (1024) bytes. The minimum
     * size is 8 and the maximum size is 1000.
     */
    public void setBlockSize( int size ) {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        blockSize = checkBlockSize( size * 1024 );
    }
    
    /**
     * Sets the block size for the specified file. GaeVFS stores files as a series
     * of blocks. Each block corresponds to a Google App Engine datastore entity
     * and therefore has a maximum size of 1 megabyte (due to entity overhead, the
     * actual limit is 1020 * 1024 = 1,044,280 bytes). The default block size is
     * 128KB (131,072 bytes).
     * 
     * @param fileObject The file for which the block size is to be set. The file
     * must not exist; if it does, a <code>FileSystemException</code> is thrown.
     * @param size The block size in units of K (1024) bytes. The minimum size is
     * 1 and the maximum size is 1020.
     * @return The <code>fileObject</code> for which the block size was set, to 
     * support method chaining.
     * @throws FileSystemException
     */
    public static FileObject setBlockSize( FileObject fileObject, int size )
            throws FileSystemException {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        if ( fileObject instanceof GaeFileObject ) {
            ((GaeFileObject)fileObject).setBlockSize( size * 1024 );
        }
        return fileObject;
    }
    
    public static int checkBlockSize( int size ) {
        size = Math.min( size, MAX_BLOCK_SIZE * 1024 ); // no larger than MAX
        size = Math.max( size, MIN_BLOCK_SIZE * 1024 ); // no smaller than MIN
        return size;
    }

    /**
     * Locates a file by name. A convenience method equivalent to 
     * <code>GaeVFS.getManager().resolveFile(name)</code>. The file name URI format
     * supported by GaeVFS is:
     * <blockquote><code>gae://<i>path</i></code></blockquote>
     * where <i>path</i> is a UNIX-style (or URI-style) absolute or relative path.
     * Paths that do not start with "/" are interpreted as relative paths from the
     * webapp root directory. Paths that start with "/" are interpreted (initially)
     * as full absolute paths.
     * <p>
     * Absolute paths must specify sub-directories of the webapp root directory.
     * Any absolute path that does not specify such a sub-directory is interpreted
     * to be a relative path from the webapp root directory, regardless of the fact
     * that it starts with "/".  It's probably easiest to just use relative paths
     * and let GaeVFS handle the path translations transparently. The exception
     * might be in cases where you're writing portable code to run in both GAE and
     * non-GAE environments.
     * <p>Examples:
     * <blockquote><code>
     * gae://myfile.zip<br>
     * gae://images/picture.jpg<br>
     * gae://docs/mydocument.pdf
     * </code></blockquote> 
     * <b>NOTE:</b> the <a href="http://code.google.com/p/gaevfs/wiki/CombinedLocalOption"
     * target="_blank">Combined Local Option</a>--which is enabled by default--allows
     * you to access GaeVFS file system resources by specifying URIs that omit the
     * <code>gae://</code> scheme. See the {@link GaeFileSystemManager} for further
     * information on enabling and disabling the GaeVFS Combined Local option.
     * 
     * @param name The name of the file system resource (file or folder) to locate.
     * @return The file system resource (file or folder).
     * @throws FileSystemException
     */
    public static FileObject resolveFile( String name ) throws FileSystemException {
        return getManager().resolveFile( name );
    }

    /**
     * Releases all resources used by GaeVFS. Required to release all file locks
     * when an application terminates. This will be done automatically if the
     * {@link GaeVfsServletEventListener} is configured within <tt>web.xml</tt>.
     * Otherwise, it should be done within the servlet <tt>destroy()</tt> method:
     * <blockquote><code>
     * public void destroy() {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;GaeVFS.close();
     * }
     */
    public static void close() {
        if ( fsManager != null ) {
            fsManager.close();
            fsManager = null;
        }
        releaseAllLocks();
    }
}