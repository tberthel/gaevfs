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
package com.newatlanta.appengine.nio.file;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import com.newatlanta.appengine.locks.ExclusiveLock;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributeView;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
import com.newatlanta.nio.file.AccessDeniedException;
import com.newatlanta.nio.file.AccessMode;
import com.newatlanta.nio.file.CopyOption;
import com.newatlanta.nio.file.DirectoryStream;
import com.newatlanta.nio.file.FileAlreadyExistsException;
import com.newatlanta.nio.file.FileStore;
import com.newatlanta.nio.file.FileSystem;
import com.newatlanta.nio.file.InvalidPathException;
import com.newatlanta.nio.file.LinkOption;
import com.newatlanta.nio.file.NoSuchFileException;
import com.newatlanta.nio.file.OpenOption;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.WatchKey;
import com.newatlanta.nio.file.WatchService;
import com.newatlanta.nio.file.DirectoryStream.Filter;
import com.newatlanta.nio.file.WatchEvent.Kind;
import com.newatlanta.nio.file.WatchEvent.Modifier;
import com.newatlanta.nio.file.attribute.Attributes;
import com.newatlanta.nio.file.attribute.BasicFileAttributeView;
import com.newatlanta.nio.file.attribute.FileAttribute;
import com.newatlanta.nio.file.attribute.FileAttributeView;

public class GaePath extends Path {
    
    private FileSystem fileSystem;
    private FileObject fileObject;
    private ExclusiveLock lock;

    public GaePath( FileSystem fileSystem, String path ) {
        this.fileSystem = fileSystem;
        try {
            fileObject = GaeVFS.resolveFile( path );
        } catch ( FileSystemException e ) {
            throw new InvalidPathException( path, e.toString() );
        }
        lock = new ExclusiveLock( fileObject.getName().getPath() );
    }
    
    private GaePath( FileSystem fileSystem, FileObject fileObject ) {
        this.fileSystem = fileSystem;
        this.fileObject = fileObject;
        lock = new ExclusiveLock( fileObject.getName().getPath() );
    }

    @Override
    public void checkAccess( AccessMode ... modes ) throws IOException {
        if ( !fileObject.exists() ) {
            throw new NoSuchFileException( toString() );
        }
        for ( AccessMode mode : modes ) {
            if ( ( ( mode == AccessMode.READ ) && !fileObject.isReadable() ) ||
                 ( ( mode == AccessMode.WRITE ) && !fileObject.isWriteable() ) ||
                   ( mode == AccessMode.EXECUTE ) ) {
                throw new AccessDeniedException( toString(), null, mode.toString() );
            }
        }
    }

    @Override
    public int compareTo( Path other ) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Path copyTo( Path target, CopyOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path createDirectory( FileAttribute<?> ... attrs ) throws IOException {
        GaePath parent = getParent();
        parent.lock.lock(); // prevent delete or rename of parent
        try {
            parent.checkAccess( AccessMode.WRITE );
            if ( notExists() ) {
                fileObject.createFolder();
                return this;
            } else {
                throw new FileAlreadyExistsException( toString() );
            }
        } finally {
            parent.lock.unlock();
        }
    }

    /**
     * attributes: block size, compression (?)
     */
    @Override
    public Path createFile( FileAttribute<?> ... attrs ) throws IOException {
        GaePath parent = getParent();
        parent.lock.lock(); // prevent delete or rename of parent
        try {
            parent.checkAccess( AccessMode.WRITE );
            if ( notExists() ) {
                fileObject.createFile();
                return this;
            } else {
                throw new FileAlreadyExistsException( toString() );
            }
        } finally {
            parent.lock.unlock();
        }
    }

    @Override
    public Path createLink( Path existing ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path createSymbolicLink( Path target, FileAttribute<?> ... attrs ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() throws IOException {
        // TODO Auto-generated method stub
        // if directory, then lock to prevent creation of children
        if ( Attributes.readBasicFileAttributes( this ).isDirectory() ) {
        }
        //fileObject.delete();
    }

    @Override
    public void deleteIfExists() throws IOException {
        // TODO Auto-generated method stub
        // if directory, then lock to prevent creation of children
        if ( Attributes.readBasicFileAttributes( this ).isDirectory() ) {
        }
    }

    @Override
    public boolean endsWith( Path other ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean equals( Object other ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean exists() {
        try {
            checkAccess();
            return true;
        } catch ( IOException e ) {
            return false;
        }
    }

    @Override
    public FileStore getFileStore() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public Path getName() {
//        return fileObject.getName().getBaseName();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path getName( int index ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNameCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public GaePath getParent() {
        try {
            FileObject parentObject = fileObject.getParent();
            if ( parentObject == null ) {
                return null;
            }
            return new GaePath( fileSystem, parentObject );
        } catch ( FileSystemException e ) {
            throw new InvalidPathException( fileObject.getName().getParent().getPath(),
                                                e.toString() );
        }
    }

    @Override
    public Path getRoot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isAbsolute() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHidden() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSameFile( Path other ) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterator<Path> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path moveTo( Path target, CopyOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        // if directory, then lock to prevent creation of children
        if ( Attributes.readBasicFileAttributes( this ).isDirectory() ) {
        }
        return null;
    }

    @Override
    public SeekableByteChannel newByteChannel( Set<? extends OpenOption> options, FileAttribute<?> ... attrs )
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SeekableByteChannel newByteChannel( OpenOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( String glob ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( Filter<? super Path> filter ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OutputStream newOutputStream( OpenOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path normalize() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean notExists() {
        try {
            checkAccess();
            return false;
        } catch ( NoSuchFileException e ) {
            return true; // confirmed does not exist
        } catch ( IOException e ) {
            return false; // unknown
        }
    }

    @Override
    public Path readSymbolicLink() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register( WatchService watcher, Kind<?>[] events, Modifier ... modifiers ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WatchKey register( WatchService watcher, Kind<?> ... events ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path relativize( Path other ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolve( Path other ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolve( String other ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean startsWith( Path other ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Path subpath( int beginIndex, int endIndex ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path toRealPath( boolean resolveLinks ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        return fileObject.toString();
    }

    @Override
    public URI toUri() {
        try {
            return fileObject.getURL().toURI();
        } catch ( Exception e ) {
            throw new IOError( e );
        }
    }

    public Object getAttribute( String attribute, LinkOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView( Class<V> type, LinkOption ... options ) {
        if ( type == BasicFileAttributeView.class ) {
            return (V)new GaeFileAttributeView( "basic", fileObject );
        } else if ( type == GaeFileAttributeView.class ) {
            return (V)new GaeFileAttributeView( "gae", fileObject );
        }
        return null;
    }

    public InputStream newInputStream( OpenOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, ?> readAttributes( String attributes, LinkOption ... options ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setAttribute( String attribute, Object value, LinkOption ... options ) throws IOException {
        // TODO Auto-generated method stub

    }
}
