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

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.*;

/**
 * Parses GAE URIs and creates {@link GaeFileName} instances. This is an
 * internal GaeVFS implementation class that is
 * normally not referenced directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>. See {@link GaeVFS} as the entry point for application
 * code that interacts with GaeVFS.
 *  
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileNameParser extends AbstractFileNameParser {

    private static GaeFileNameParser instance = new GaeFileNameParser();
    
    private GaeFileNameParser() {
    }

    static GaeFileNameParser getInstance() {
        return instance;
    }

    /**
     * Makes sure <code>filename</code> always specifies a path that is within a
     * sub-directory of the webapp root path.
     */
    public FileName parseUri( VfsComponentContext context, FileName ignore, String filename )
            throws FileSystemException
    {
        StringBuffer name = new StringBuffer();

        // Extract the scheme
        String scheme = UriParser.extractScheme( filename, name );
        if ( scheme == null ) { // this should never happen
            scheme = "gae";
        }

        // Remove encoding, and adjust the separators
        UriParser.canonicalizePath( name, 0, name.length(), this );
        UriParser.fixSeparators( name );

        // Normalise the path
        FileType fileType = UriParser.normalisePath( name );
        
        // all GAE files *must* be relative to the base file, which must be the
        // webapp root (though we have no way of enforcing this); if given a uri
        // that doesn't start with the base path, add the base path
        FileObject baseFile = context.getFileSystemManager().getBaseFile();
        if ( baseFile != null ) {
            String basePath = getBasePath( baseFile );
            if( name.indexOf( basePath ) != 0 ) { // if ( !name.startsWith( basePath ) )
                name.insert( 0, basePath );
            }
        }
        return new GaeFileName( scheme, name.toString(), fileType );
    }
    
    String getBasePath( FileObject baseFile ) {
        StringBuffer basePath = new StringBuffer();
        String scheme = UriParser.extractScheme( baseFile.getName().getURI(), basePath );
        if ( scheme.equals( "file" ) ) {
            basePath.delete( 0, 2 ); // remove leading "//"
        }
        return basePath.toString().intern();
    }
}
