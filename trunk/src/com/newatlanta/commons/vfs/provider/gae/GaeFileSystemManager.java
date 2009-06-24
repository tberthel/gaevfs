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
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.vfs.provider.UriParser;

import com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache;

/**
 * Implements the Combined Local option for GaeVFS. See the following:
 * 
 *      http://code.google.com/p/gaevfs/wiki/CombinedLocalOption
 * 
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeFileSystemManager extends StandardFileSystemManager {

    private static final String CONFIG_RESOURCE = "providers.xml";

    private GaeMemcacheFilesCache filesCache;
    private boolean isCombinedLocal = true;

    public GaeFileSystemManager setCombinedLocal( boolean combinedLocal ) {
        isCombinedLocal = combinedLocal;
        return this;
    }

    public boolean isCombinedLocal() {
        return isCombinedLocal;
    }

    public void clearFilesCache() {
        filesCache.clear();
    }

    @Override
    public void init() throws FileSystemException {
        filesCache = new GaeMemcacheFilesCache();
        this.setFilesCache( filesCache );
        // make sure our superclass initializes properly
        super.setConfiguration( getClass().getSuperclass().getResource( CONFIG_RESOURCE ) );
        super.init();
    }

    /**
     * Resolves a URI, relative to a base file with specified FileSystem
     * configuration
     */
    @Override
    public FileObject resolveFile( final FileObject baseFile, final String uri, final FileSystemOptions opts )
            throws FileSystemException {
        // let the specified provider handle it
        if ( !isCombinedLocal || isSchemeSpecified( uri ) ) {
            // baseFile should be null if scheme specified (do we care?)
            return super.resolveFile( baseFile, uri, opts );
        }

        FileObject localFile;
        FileObject gaeFile;

        if ( baseFile != null ) {
            FileObject fileObject = baseFile.resolveFile( uri );
            if ( fileObject.exists() && ( fileObject.getType() == FileType.FILE ) ) {
                return fileObject; // return existing file
            }
            // fileObject doesn't exist or is a folder, check other file system
            if ( fileObject.getName().getScheme().equals( "gae" ) ) {
                gaeFile = fileObject;
                localFile = super.resolveFile( null, "file://" + baseFile.getName().getPath() + "/" + uri, opts );
            } else {
                localFile = fileObject;
                StringBuffer basePath = new StringBuffer();
                UriParser.extractScheme( baseFile.getName().getURI(), basePath );
                gaeFile = super.resolveFile( null, "gae:" + basePath + "/" + uri, opts );
            }
        } else {
            // neither scheme nor baseFile specified, check local first
            localFile = super.resolveFile( null, uri, opts );
            if ( localFile.exists() && ( localFile.getType() == FileType.FILE ) ) {
                // return existing local files
                return localFile;
            }
            // localFile doesn't exist or is a folder, check the GAE file system
            gaeFile = super.resolveFile( null, "gae://" + uri, opts );
        }

        // when we get here we either have a non-existing file, or a folder;
        // return the GAE file/folder if it exists
        if ( gaeFile.exists() ) {
            return gaeFile;
        }

        // never return non-existing local folders, which can't be created
        if ( localFile.exists() ) {
            return localFile; // an existing local folder and no GAE folder
        }
        return gaeFile; // neither local nor GAE file/folder exists
    }

    private boolean isSchemeSpecified( String uri ) {
        String scheme = UriParser.extractScheme( uri );
        return ( ( scheme != null ) && super.hasProvider( scheme ) );
    }
}
