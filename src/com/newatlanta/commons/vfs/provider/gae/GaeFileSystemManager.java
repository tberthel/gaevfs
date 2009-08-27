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

import java.io.File;
import java.net.URL;

import org.apache.commons.vfs.CacheStrategy;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.cache.LRUFilesCache;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.vfs.provider.UriParser;

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

    private boolean isCombinedLocal = true;
    private String rootPath;
    private FileObject rootObject;

    public GaeFileSystemManager() {
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

    public void init( String rootPath ) throws FileSystemException {
        prepare( rootPath, getClass().getSuperclass().getResource( CONFIG_RESOURCE ) );
        this.init();
    }

    /**
     * Prepare for initialization. 
     */
    public void prepare( String rootPath, URL configUrl ) throws FileSystemException {
        setFilesCache( new LRUFilesCache() );
        setCacheStrategy( CacheStrategy.ON_RESOLVE );
        setConfiguration( configUrl );

        this.rootPath = new File( rootPath ).getAbsolutePath();
    }

    @Override
    public void init() throws FileSystemException {
        super.init();
        rootObject = resolveFile( "gae://" + rootPath );
        if ( !rootObject.exists() ) {
            rootObject.createFolder();
        }
        super.setBaseFile( rootObject );
    }

    /**
     * Sets the base file to use when resolving relative URI. Base file must
     * be a sub-directory of the root path; set equal to the root path if null.
     * 
     * @param baseFile The new base FileObject.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public void setBaseFile( FileObject baseFile ) throws FileSystemException {
        if ( baseFile == null ) {
            baseFile = rootObject;
        } else if ( !rootObject.getName().isDescendent( baseFile.getName() ) ) {
            throw new FileSystemException( "Base file must be a descendent of root." );
        }
        super.setBaseFile( baseFile );
    }

    /**
     * Resolves a URI, relative to a base file with the specified FileSystem
     * configuration options.
     */
    @Override
    public FileObject resolveFile( FileObject baseFile, String uri, FileSystemOptions opts )
            throws FileSystemException
    {
        // let the specified provider handle it
        if ( !isCombinedLocal || isSchemeSpecified( uri ) ) {
            return super.resolveFile( baseFile, uri, opts );
        }

        FileObject localFile;
        FileObject gaeFile;

        if ( baseFile != null ) {
            FileObject fileObject = super.resolveFile( baseFile, uri, opts );
            if ( fileObject.exists() && ( fileObject.getType().hasContent() ) ) {
                return fileObject; // return existing file
            }
            // fileObject doesn't exist or is a folder, check other file system
            if ( fileObject.getName().getScheme().equals( "gae" ) ) {
                gaeFile = fileObject;
                FileName baseName = baseFile.getName();
                if ( baseName instanceof GaeFileName ) {
                    String localUri = "file://" + ((GaeFileName)baseName).getRootPath() +
                                                baseName.getPath() + "/" + uri;
                    localFile = super.resolveFile( null, localUri, opts );
                } else {
                    localFile = super.resolveFile( baseFile, "file://" + uri, opts );
                }
                if ( localFile.exists() && ( localFile.getType().hasContent() ) ) {
                    return localFile; // return existing local files
                }
            } else {
                localFile = fileObject;
                gaeFile = super.resolveFile( baseFile, "gae://" + uri, opts );
            }
        } else {
            // neither scheme nor baseFile specified, check local first
            localFile = super.resolveFile( null, uri, opts );
            if ( localFile.exists() && ( localFile.getType().hasContent() ) ) {
                return localFile; // return existing local files
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

        // never return local folders
        if ( localFile.exists() ) {
            gaeFile.createFolder(); // create GAE "shadow" for existing local folder
            return gaeFile;
        }
        return gaeFile; // neither local nor GAE file/folder exists
    }

    private boolean isSchemeSpecified( String uri ) {
        String scheme = UriParser.extractScheme( uri );
        return ( ( scheme != null ) && super.hasProvider( scheme ) );
    }
}
