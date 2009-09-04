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
package com.newatlanta.appengine.nio.file.spi;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.newatlanta.appengine.nio.file.GaeFileSystem;
import com.newatlanta.appengine.nio.file.GaePath;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.file.FileSystem;
import com.newatlanta.nio.file.FileSystemAlreadyExistsException;
import com.newatlanta.nio.file.OpenOption;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.ProviderMismatchException;
import com.newatlanta.nio.file.attribute.FileAttribute;
import com.newatlanta.nio.file.spi.FileSystemProvider;
import com.newatlanta.nio.file.FileSystemNotFoundException;

public class GaeFileSystemProvider extends FileSystemProvider {
    
    private Map<URI, FileSystem> fileSystems = new HashMap<URI, FileSystem>();
    
    public static FileSystemProvider create() {
        return new GaeFileSystemProvider( null );
    }

    public GaeFileSystemProvider( FileSystemProvider defaultProvider ) {
        // defaultProvider will be null (for now)
        try {
            // create the default file system
            newFileSystem( URI.create( "file:///" ), null );
        } catch ( IOException e ) {
            throw new Error( e );
        }
    }

    @Override
    public FileSystem getFileSystem( URI uri ) {
        FileSystem fs = fileSystems.get( uri );
        if ( fs == null ) {
            throw new FileSystemNotFoundException();
        }
        return fs;
    }

    @Override
    public Path getPath( URI uri ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScheme() {
        return "file"; // required of the default provider
    }

    @Override
    public FileSystem newFileSystem( URI uri, Map<String, ?> env ) throws IOException {
        if ( fileSystems.containsKey( uri ) ) {
            throw new FileSystemAlreadyExistsException();
        }
        FileSystem fs = new GaeFileSystem( this );
        fileSystems.put( uri, fs );
        return fs;
    }
    
    @Override
    public FileChannel newFileChannel( Path path, Set<? extends OpenOption> options,
                                        FileAttribute<?> ... attrs ) throws IOException {
        if ( !( path instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        return ((GaePath)path).newByteChannel( options, attrs );
    }
}
