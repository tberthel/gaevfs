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

import static com.newatlanta.appengine.nio.attribute.GaeFileAttributes.BASIC_VIEW;
import static com.newatlanta.appengine.nio.attribute.GaeFileAttributes.GAE_VIEW;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import com.newatlanta.appengine.locks.ExclusiveLock;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributeView;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributes;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
import com.newatlanta.nio.channels.SeekableByteChannel;
import com.newatlanta.nio.file.AccessDeniedException;
import com.newatlanta.nio.file.AccessMode;
import com.newatlanta.nio.file.CopyOption;
import com.newatlanta.nio.file.DirectoryNotEmptyException;
import com.newatlanta.nio.file.DirectoryStream;
import com.newatlanta.nio.file.FileAlreadyExistsException;
import com.newatlanta.nio.file.FileStore;
import com.newatlanta.nio.file.FileSystem;
import com.newatlanta.nio.file.InvalidPathException;
import com.newatlanta.nio.file.LinkOption;
import com.newatlanta.nio.file.NoSuchFileException;
import com.newatlanta.nio.file.OpenOption;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.ProviderMismatchException;
import com.newatlanta.nio.file.StandardOpenOption;
import com.newatlanta.nio.file.WatchKey;
import com.newatlanta.nio.file.WatchService;
import com.newatlanta.nio.file.DirectoryStream.Filter;
import com.newatlanta.nio.file.WatchEvent.Kind;
import com.newatlanta.nio.file.WatchEvent.Modifier;
import com.newatlanta.nio.file.attribute.BasicFileAttributeView;
import com.newatlanta.nio.file.attribute.FileAttribute;
import com.newatlanta.nio.file.attribute.FileAttributeView;

public class GaePath extends Path {
    
    private FileSystem fileSystem;
    private FileObject fileObject;
    private String path;
    private ExclusiveLock lock;

    public GaePath( FileSystem fileSystem, String path ) {
        this.fileSystem = fileSystem;
        this.path = path;
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
        path = fileObject.getName().getPath();
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
    public boolean exists() {
        try {
            checkAccess();
            return true;
        } catch ( IOException e ) {
            return false;
        }
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
    public int compareTo( Path other ) {
        // throws NullPointerException and ClassCastException per Comparable
        return path.compareTo( ((GaePath)other).path );
    }
    
    @Override
    public boolean equals( Object other ) {
        if ( ( other == null ) || !( other instanceof GaePath ) ) {
            return false;
        }
        return path.equals( ((GaePath)other).path );
    }
    
    @Override
    public int hashCode() {
        return path.hashCode();
    }
    
    @Override
    public boolean isSameFile( Path other ) throws IOException {
        if ( ( other == null ) || !( other instanceof GaePath ) ) {
            return false;
        }
        if ( ( this == other ) || this.equals( other ) ) {
            return true;
        }
        return fileObject.getName().getPath().equals(
                                ((GaePath)other).fileObject.getName().getPath() );
    }

    @Override
    public Path createDirectory( FileAttribute<?> ... attrs ) throws IOException {
        if ( attrs.length > 0 ) {
            throw new UnsupportedOperationException( attrs[ 0 ].name() );
        }
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

    @Override
    public Path createFile( FileAttribute<?> ... attrs ) throws IOException {
        for ( FileAttribute<?> attr : attrs ) {
            if ( attr.name().equals( GaeFileAttributes.BLOCK_SIZE ) ) {
                GaeVFS.setBlockSize( fileObject, (Integer)attr.value() );
            } else {
                throw new UnsupportedOperationException( attr.name() );
            }
        }
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
        checkAccess();
        doDelete();
    }

    @Override
    public void deleteIfExists() throws IOException {
        if ( exists() ) {
            doDelete();
        }
    }
    
    private void doDelete() throws IOException {
        if ( fileObject.getType().hasChildren() ) { // directory
            lock.lock(); // prevent rename or create children
            try {
                if ( fileObject.getChildren().length > 0 ) { // not empty
                    throw new DirectoryNotEmptyException( toString() );
                }
                fileObject.delete();
            } finally {
                lock.unlock();
            }
        } else { // file
            fileObject.delete();
        }
    }

    @Override
    public FileStore getFileStore() throws IOException {
        return GaeFileStore.getInstance();
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public Path getName() {
        return new GaePath( fileSystem, fileObject.getName().getBaseName() );
    }

    private static final String PATH_DELIMS = "/\\"; // include Windows for development
    
    @Override
    public Path getName( int index ) {
        StringTokenizer st = new StringTokenizer( path, PATH_DELIMS );
        int numEntries = st.countTokens();
        if ( ( index < 0 ) || ( index >= numEntries ) ) {
            throw new IllegalArgumentException();
        }
        for ( int i = 0; i < numEntries; i++ ) {
            st.nextToken();
        }
        return new GaePath( fileSystem, st.nextToken() );
    }

    @Override
    public int getNameCount() {
        return new StringTokenizer( path, PATH_DELIMS ).countTokens();
    }
    
    @Override
    public Iterator<Path> iterator() {
        StringTokenizer st = new StringTokenizer( path, PATH_DELIMS );
        List<Path> entryList = new ArrayList<Path>();
        while ( st.hasMoreTokens() ) {
            entryList.add( new GaePath( fileSystem, st.nextToken() ) );
        }
        return entryList.iterator();
    }
    
    @Override
    public Path subpath( int beginIndex, int endIndex ) {
        StringTokenizer st = new StringTokenizer( path, PATH_DELIMS, true );
        int numEntries = st.countTokens();
        if ( ( beginIndex < 0 ) || ( beginIndex >= numEntries ) ||
                ( endIndex <= beginIndex ) || ( endIndex > numEntries ) ) {
            throw new IllegalArgumentException();
        }
        StringBuffer sb = new StringBuffer();
        for ( int i = beginIndex; i < endIndex; i++ ) {
            sb.append( st.nextToken() );
        }
        return new GaePath( fileSystem, sb.toString() );
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
        return new GaePath( fileSystem, fileObject.getName().getRootURI() );
    }

    @Override
    public boolean isAbsolute() {
        return startsWith( getRoot() );
    }

    @Override
    public boolean isHidden() throws IOException {
        return false; // hidden files not supported
    }
    
    @Override
    public Path copyTo( Path target, CopyOption ... options ) throws IOException {
        if ( !( target instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path moveTo( Path target, CopyOption ... options ) throws IOException {
        if ( !( target instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        // if directory, then lock to prevent creation of children
        if ( fileObject.getType().hasChildren() ) {
        }
        // TODO Auto-generated method stub
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

    public InputStream newInputStream( OpenOption ... options ) throws IOException {
        for ( OpenOption option : options ) {
            if ( option != StandardOpenOption.READ ) {
                throw new UnsupportedOperationException( option.toString() );
            }
        }
        checkAccess( AccessMode.READ );
        // TODO: Commons VFS automatically returns a buffered stream, but the
        // docs for this method state: "The stream will not be buffered."
        return fileObject.getContent().getInputStream();
    }
    
    @Override
    public OutputStream newOutputStream( OpenOption ... options ) throws IOException {
        Set<OpenOption> optionsSet = new HashSet<OpenOption>( options.length );
        Collections.addAll( optionsSet, options );
        if ( optionsSet.contains( StandardOpenOption.READ ) ) {
            throw new UnsupportedOperationException( StandardOpenOption.READ.name() );
        }
        if ( optionsSet.contains( StandardOpenOption.SYNC ) ) {
            throw new UnsupportedOperationException( StandardOpenOption.SYNC.name() );
        }
        if ( optionsSet.contains( StandardOpenOption.DSYNC ) ) {
            throw new UnsupportedOperationException( StandardOpenOption.DSYNC.name() );
        }
        if ( optionsSet.contains( StandardOpenOption.DELETE_ON_CLOSE ) ) {
            throw new UnsupportedOperationException( StandardOpenOption.DELETE_ON_CLOSE.name() );
        }
        if ( optionsSet.contains( StandardOpenOption.SPARSE ) ) {
            throw new UnsupportedOperationException( StandardOpenOption.SPARSE.name() );
        }
        boolean append = optionsSet.contains( StandardOpenOption.APPEND );
        boolean truncate = optionsSet.contains( StandardOpenOption.TRUNCATE_EXISTING );
        if ( append && truncate ) {
            throw new IllegalArgumentException( "cannot specify both " +
                                    StandardOpenOption.APPEND.name() + " and " +
                                    StandardOpenOption.TRUNCATE_EXISTING.name() );
        }
        if ( optionsSet.contains( StandardOpenOption.CREATE_NEW ) ||
            ( optionsSet.contains( StandardOpenOption.CREATE ) && notExists() ) )
        {
            createFile();
        }
        if ( truncate ) {
            // TODO: how to truncate? delete and create new?
        }
        // TODO: Commons VFS automatically returns a buffered stream, but the
        // docs for this method state: "The stream will not be buffered."
        return fileObject.getContent().getOutputStream( append );
    }

    @Override
    public Path normalize() {
        // FileObject paths are normalized upon creation
        return new GaePath( fileSystem, fileObject.getName().getPath() );
    }

    @Override
    public Path readSymbolicLink() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register( WatchService watcher, Kind<?>[] events, Modifier ... modifiers ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register( WatchService watcher, Kind<?> ... events ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolve( Path other ) {
        if ( other == null ) {
            return this;
        }
        if ( !( other instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        if ( other.isAbsolute() ) {
            return other;
        }
        return resolve( ((GaePath)other).path );
    }

    @Override
    public Path resolve( String other ) {
        try {
            return new GaePath( fileSystem, fileObject.resolveFile( other ) );
        } catch ( FileSystemException e ) {
            throw new InvalidPathException( other, e.toString() );
        }
    }
    
    @Override
    public Path relativize( Path other ) {
        if ( !( other instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        if ( this.equals( other ) ) {
            return null;
        }
        try {
            return new GaePath( fileSystem, fileObject.getName().getRelativeName(
                                            ((GaePath)other).fileObject.getName() ) );
        } catch ( FileSystemException e ) {
            throw new InvalidPathException( other.toString(), e.toString() );
        }
    }

    @Override
    public boolean startsWith( Path other ) {
        if ( !( other instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        return path.startsWith( ((GaePath)other).path );
    }
    
    @Override
    public boolean endsWith( Path other ) {
        if ( !( other instanceof GaePath ) ) {
            throw new ProviderMismatchException();
        }
        return path.endsWith( ((GaePath)other).path );
    }

    @Override
    public Path toAbsolutePath() {
        return new GaePath( fileSystem, fileObject.getName().getURI() );
    }

    @Override
    public Path toRealPath( boolean resolveLinks ) throws IOException {
        return toAbsolutePath();
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public URI toUri() {
        try {
            return fileObject.getURL().toURI();
        } catch ( Exception e ) {
            throw new IOError( e );
        }
    }
    
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView( Class<V> type,
            LinkOption ... options )
    {
        if ( type == BasicFileAttributeView.class ) {
            return (V)new GaeFileAttributeView( BASIC_VIEW, fileObject );
        } else if ( type == GaeFileAttributeView.class ) {
            return (V)new GaeFileAttributeView( GAE_VIEW, fileObject );
        }
        return null;
    }
    
    private GaeFileAttributeView getGaeFileAttributeView( String viewName ) {
        if ( BASIC_VIEW.equals( viewName ) || GAE_VIEW.equals( viewName ) ) {
            return new GaeFileAttributeView( viewName, fileObject );
        }
        return null;
    }
    
    public Object getAttribute( String attribute, LinkOption ... options ) throws IOException {
        AttributeName attr = new AttributeName( attribute );
        GaeFileAttributeView attrView = getGaeFileAttributeView( attr.viewName );
        if ( attrView == null ) {
            return null;
        }
        return attrView.readAttributes().getAttribute( attr.viewName, attr.attrName );
    }

    public Map<String, ?> readAttributes( String attributes, LinkOption ... options )
            throws IOException
    {
        AttributeName attr = new AttributeName( attributes );
        GaeFileAttributeView gaeAttrView = getGaeFileAttributeView( attr.viewName );
        if ( gaeAttrView == null ) {
            return new HashMap<String, Object>();
        }
        if ( "*".equals( attr.attrName ) ) {
            return gaeAttrView.readAttributes().getSupportedAttributes( attr.viewName );
        } else {
            Map<String, Object> attrMap = new HashMap<String, Object>();
            GaeFileAttributes gaeAttrs = gaeAttrView.readAttributes();
            StringTokenizer st = new StringTokenizer( attr.attrName, "," );
            while ( st.hasMoreTokens() ) {
                String attrName = st.nextToken();
                Object attrValue = gaeAttrs.getAttribute( attr.viewName, attrName );
                if ( attrValue != null ) {
                    attrMap.put( attrName, attrValue );
                }
            }
            return attrMap;
        }
    }

    public void setAttribute( String attribute, Object value, LinkOption ... options )
            throws IOException
    {
        AttributeName attr = new AttributeName( attribute );
        GaeFileAttributeView attrView = getGaeFileAttributeView( attr.viewName );
        if ( attrView != null ) {
            attrView.readAttributes().setAttribute( attr.viewName, attr.attrName, value );
        }
    }
    
    private class AttributeName {
        
        private String viewName;
        private String attrName;
        
        private AttributeName( String attribute ) {
            int colonPos = attribute.indexOf( ':' );
            if ( colonPos == -1 ) {
                viewName = BASIC_VIEW;
                attrName = attribute;
            } else {
                viewName = attribute.substring( 0, colonPos++ );
                attrName = attribute.substring( colonPos );
            }
        }
    }
}
