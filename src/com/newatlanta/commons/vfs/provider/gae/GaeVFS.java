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

import java.io.IOException;

import org.apache.commons.vfs.FileObject;

/**
 * Creates and initializes a GaeFileSystemManager.
 * 
 * IMPORTANT! You must set the path to the webapp root directory via the
 * GaeVFS.setRootPath() method before invoking GaeVFS.getManager().
 * 
 * IMPORTANT! You must clear the ThreadLocal cache at the end of every request
 * via the GaeVFS.clearFilesCache() method. See additional comments within the
 * GaeMemcacheFilesCache class.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeVFS {

    private static final int DEFAULT_BLOCK_SIZE = 1024 * 32; // max 1024 x 1023
    
    static {
        // GAE doesn't set these values; Commons VFS will fail to
        // initialize if they're not set
        System.setProperty( "os.arch", "" );
        System.setProperty( "os.version", "" );
    }

    private static GaeFileSystemManager fsManager;
    private static String rootPath;
    private static int blockSize = DEFAULT_BLOCK_SIZE;

    public static GaeFileSystemManager getManager() throws IOException {
        if ( fsManager == null ) {
            if ( rootPath == null ) {
                throw new IOException( "root path not defined" );
            }
            fsManager = new GaeFileSystemManager();
            fsManager.init( rootPath );
        }
        return fsManager;
    }
    
    public static void setRootPath( String _rootPath ) {
        rootPath = _rootPath;
    }
    
    public static int getBlockSize() {
        return blockSize;
    }
    
    public void setBlockSize( int size ) {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        blockSize = Math.min( size, 1023 ) * 1024; // max size is 1023 * 1024
    }
    
    public static void setBlockSize( FileObject fileObject, int size ) throws IOException {
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "invalid block size: " + size );
        }
        if ( fileObject instanceof GaeFileObject ) {
            ((GaeFileObject)fileObject).setBlockSize( Math.min( size, 1023 ) * 1024 );
        }
    }

    public static FileObject resolveFile( String name ) throws IOException {
        return getManager().resolveFile( name );
    }

    public static void clearFilesCache() {
        if ( fsManager != null ) {
            fsManager.clearFilesCache();
        }
    }

    public static void close() {
        if ( fsManager != null ) {
            fsManager.close();
            fsManager = null;
        }
    }
}