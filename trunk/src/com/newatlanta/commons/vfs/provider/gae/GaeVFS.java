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
 * Creates and initializes a GaeFileSystemManager.
 * 
 * IMPORTANT! Servlets that use GaeVFS must clear the ThreadLocal cache at the
 * end of every request via the GaeVFS.clearFilesCache() method. See additional
 * comments within the GaeMemcacheFilesCache class.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeVFS {

    static {
        // GAE doesn't set these values; set them here so Commons VFS will
        // initialize properly
        System.setProperty( "os.arch", "" );
        System.setProperty( "os.version", "" );
    }

    private static GaeFileSystemManager fsManager;

    public static GaeFileSystemManager getManager() throws FileSystemException {
        if ( fsManager == null ) {
            fsManager = new GaeFileSystemManager();
            fsManager.init();
        }
        return fsManager;
    }

    public static FileObject resolveFile( String name ) throws FileSystemException {
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