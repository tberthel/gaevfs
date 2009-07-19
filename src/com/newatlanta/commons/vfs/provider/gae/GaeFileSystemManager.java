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

import org.apache.commons.vfs.CacheStrategy;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.vfs.provider.UriParser;

import com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache;

/**
 * Implements the
 * <a href="http://code.google.com/p/gaevfs/wiki/CombinedLocalOption" target="_blank">Combined
 * Local</a> option for GaeVFS. Other than the {@link GaeFileSystemManager#setCombinedLocal(boolean)}
 * method, this is primarily an internal GaeVFS implementation class that is normally
 * not referenced directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>. See {@link GaeVFS} as the entry point for application
 * code that interacts with GaeVFS.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileSystemManager extends StandardFileSystemManager {

    private static final String CONFIG_RESOURCE = "providers.xml";

    private GaeMemcacheFilesCache filesCache;
    private boolean isCombinedLocal = true;
    
    GaeFileSystemManager() {
    }

    /**
     * Sets the Combined Local option for GaeVFS.
     * @param combinedLocal True to enable the Combined Local option; false to disable it.
     * @return The <code>GaeFileSystemManager</code> instance, for method chaining.
     */
    public GaeFileSystemManager setCombinedLocal( boolean combinedLocal ) {
        isCombinedLocal = combinedLocal;
        return this;
    }

    /**
     * Returns the current setting of the Combined Local option.
     * @return True if the Combined Local option is enabled; false if disabled.
     */
    public boolean isCombinedLocal() {
        return isCombinedLocal;
    }

    /**
     * Clears the GaeVFS local file cache. See the
     * {@link com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache} class for an
     * explanation of why this is necessary.
     */
    public void clearFilesCache() {
        filesCache.clear();
    }

    void init( String rootPath ) throws FileSystemException {   
        filesCache = new GaeMemcacheFilesCache();
        setFilesCache( filesCache );
        setCacheStrategy( CacheStrategy.MANUAL );
        
        // make sure our superclass initializes properly
        super.setConfiguration( getClass().getSuperclass().getResource( CONFIG_RESOURCE ) );
        super.init();
        
        if ( !rootPath.startsWith( "/" ) ) {
            rootPath = "/" + rootPath;
        }
        setBaseFile( new java.io.File( rootPath ) );
    }

    /**
     * Resolves a URI, relative to a base file with the specified FileSystem
     * configuration options.
     */
    @Override
    public FileObject resolveFile( final FileObject baseFile, String uri, final FileSystemOptions opts )
            throws FileSystemException
    {
        // let the specified provider handle it
        if ( !isCombinedLocal || isSchemeSpecified( uri ) ) {
            return super.resolveFile( baseFile, uri, opts );
        }

        FileObject localFile;
        FileObject gaeFile;

        if ( baseFile != null ) {
            // if uri starts with "/", determine if it includes the base path;
            // if it doesn't, then remove the leading "/" to create a relative path
            uri = checkRelativity( baseFile, uri );
            
            FileObject fileObject = super.resolveFile( baseFile, uri, opts );
            if ( fileObject.exists() && ( fileObject.getType().hasContent() ) ) {
                return fileObject; // return existing file
            }
            // fileObject doesn't exist or is a folder, check other file system
            if ( fileObject.getName().getScheme().equals( "gae" ) ) {
                gaeFile = fileObject;
                localFile = super.resolveFile( null, "file://" + baseFile.getName().getPath() + "/" + uri, opts );
            } else {
                localFile = fileObject;
                gaeFile = super.resolveFile( baseFile, "gae://" + uri, opts );
            }
        } else {
            // neither scheme nor baseFile specified, check local first
            localFile = super.resolveFile( null, uri, opts );
            if ( localFile.exists() && ( localFile.getType().hasContent() ) ) {
                // return existing local files
                return localFile;
            }
            // localFile doesn't exist or is a folder, check GAE file system
            gaeFile = super.resolveFile( null, "gae://" + uri, opts );
        }

        ((GaeFileObject)gaeFile).setCombinedLocal( true );
        
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
    
    private String checkRelativity( FileObject baseFile, String uri ) {
        if ( uri.startsWith( "/" ) && ( baseFile != null ) ) {
            String basePath = GaeFileNameParser.getInstance().getBasePath( baseFile );
            if ( !uri.startsWith( basePath ) ) {
                return uri.substring( 1 );
            }
        }
        return uri;
    }
}
