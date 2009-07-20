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

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

/**
 * This class is the entry point for interacting with the Google App Engine Virtual
 * File System (GaeVFS). Its primary function is to create and initialize the
 * {@link GaeFileSystemManager} static instance, which is done via the static
 * {@link GaeVFS#getManager()} method. Nearly all other interaction with GaeVFS
 * is done via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>.
 * <blockquote>
 * <b>IMPORTANT!</b> The webapp root directory must be set via the
 * {@link GaeVFS#setRootPath(String)} method before invoking {@link GaeVFS#getManager()}
 * the first time.
 * </blockquote><blockquote>
 * <b>IMPORTANT!</b> The GaeVFS local file cache must be cleared at the end of every
 * request via the {@link GaeVFS#clearFilesCache()} method. See the
 * {@link com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache} class for an
 * explanation of why this is necessary.
 * </blockquote><blockquote>
 * <b>IMPORTANT!</b> Do not use the <code>org.apache.commons.vfs.VFS.getManager()</code>
 * method provided by Commons VFS to get a <code>FileSystemManager</code> when running
 * within GAE.
 * </blockquote>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeVFS {

    private static final int DEFAULT_BLOCK_SIZE = 1024 * 128; // max 1024 x 1023
    
    static {
        // GAE doesn't set these values; Commons VFS will fail to initialize if
        // they're not set, so do it here
        System.setProperty( "os.arch", "" );
        System.setProperty( "os.version", "" );
    }

    private static GaeFileSystemManager fsManager;
    private static String rootPath;
    private static int blockSize = DEFAULT_BLOCK_SIZE;
    
    private GaeVFS() {
    }

    /**
     * Creates and initializes the {@link GaeFileSystemManager} static instance.
     * <blockquote>
     * <b>IMPORTANT!</b> The webapp root directory must be set via the
     * {@link GaeVFS#setRootPath(String)} method before invoking <code>getManager()</code>
     * the first time. This will normally be done within the servlet <code>init()</code>
     * method:<br><br>
     * <code>
     * public void init() throws ServletException {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );<br>
     * }
     * </code>
     * </blockquote>
     * Failure to set the webapp root path properly will result in unexpected
     * errors.
     * 
     * @return The {@link GaeFileSystemManager} static instance.
     * @throws FileSystemException
     */
    public static GaeFileSystemManager getManager() throws FileSystemException {
        if ( fsManager == null ) {
            if ( rootPath == null ) {
                throw new FileSystemException( "root path not defined" );
            }
            fsManager = new GaeFileSystemManager();
            fsManager.init( rootPath );
        }
        return fsManager;
    }
    
    /**
     * Sets the webapp root path for resolving file names.
     * <p>
     * File path URIs that do not start with "/" are interpreted as relative paths
     * from the webapp root directory. Paths that start with "/" are interpreted
     * (initially) as full absolute paths.
     * <p>
     * Absolute paths must specify sub-directories of the webapp root directory.
     * Any absolute path that does not specify such a sub-directory is interpreted
     * to be a relative path from the webapp root directory, regardless of the fact
     * that it starts with "/".
     * 
     * <b>IMPORTANT!</b> The webapp root directory must be set via the
     * {@link GaeVFS#setRootPath(String)} method before invoking <code>getManager()</code>
     * the first time. This will normally be done within the servlet <code>init()</code>
     * method:<br><br>
     * <code>
     * public void init() throws ServletException {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );<br>
     * }
     * </code>
     * </blockquote>
     * Failure to set the webapp root path properly will result in unexpected
     * errors.
     * @param rootPath The webapp root path.
     */
    public static void setRootPath( String rootPath ) {
        GaeVFS.rootPath = rootPath;
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
     * entity overhead, the actual limit is 1023 * 1024 = 1,047,552 bytes). The
     * default block size is 128KB (131,072 bytes).
     * 
     * @param size The default block size in units of K (1024) bytes. The minimum
     * size is 1 and the maximum size is 1023.
     */
    public void setBlockSize( int size ) {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        blockSize = Math.min( size, 1023 ) * 1024; // max size is 1023 * 1024
    }
    
    /**
     * Sets the block size for the specified file. GaeVFS stores files as a series
     * of blocks. Each block corresponds to a Google App Engine datastore entity
     * and therefore has a maximum size of 1 megabyte (due to entity overhead, the
     * actual limit is 1023 * 1024 = 1,047,552 bytes). The default block size is
     * 128KB (131,072 bytes).
     * 
     * @param fileObject The file for which the block size is to be set. The file
     * must not exist; if it does, a <code>FileSystemException</code> is thrown.
     * @param size The block size in units of K (1024) bytes. The minimum size is
     * 1 and the maximum size is 1023.
     * @return The <code>fileObject</code> for which the block size was set, to 
     * support method chaining.
     * @throws FileSystemException
     */
    public static FileObject setBlockSize( FileObject fileObject, int size ) throws FileSystemException {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        if ( fileObject instanceof GaeFileObject ) {
            ((GaeFileObject)fileObject).setBlockSize( Math.min( size, 1023 ) * 1024 );
        }
        return fileObject;
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
     * Clears the GaeVFS local file cache by invoking {@link GaeFileSystemManager#clearFilesCache()}.
     * 
     * It's very important that the GaeVFS
     * local file cache is cleared at the end of every servlet request via the
     * <code>clearFilesCache()</code> method, best placed within a
     * <code>finally</code> clause; for example:
     * <blockquote><code>
     * public void doGet( HttpServletRequest req, HttpServletResponse res )<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;throws ServletException, IOException {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// ...process the GET request...<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;} finally {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;GaeVFS.clearFilesCache(); // this is important!<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
     * }
     * </code></blockquote>
     * See the
     * {@link com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache} class for an
     * explanation of why this is necessary.
     */
    public static void clearFilesCache() {
        if ( fsManager != null ) {
            fsManager.clearFilesCache();
        }
    }

    /**
     * Releases all resources used by GaeVFS. It's good practice, but not strictly
     * necessary, to close GaeVFS when your servlet is destroyed to aid in clean-up
     * of GaeVFS resources:
     * <blockquote><code>
     * public void destroy() {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;GaeVFS.close(); // this is not strictly required, but good practice<br>
     * }
     */
    public static void close() {
        if ( fsManager != null ) {
            fsManager.close();
            fsManager = null;
        }
    }
}