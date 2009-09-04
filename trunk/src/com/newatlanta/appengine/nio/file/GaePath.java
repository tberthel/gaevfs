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
import static com.newatlanta.nio.file.StandardOpenOption.APPEND;
import static com.newatlanta.nio.file.StandardOpenOption.CREATE;
import static com.newatlanta.nio.file.StandardOpenOption.CREATE_NEW;
import static com.newatlanta.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static com.newatlanta.nio.file.StandardOpenOption.DSYNC;
import static com.newatlanta.nio.file.StandardOpenOption.READ;
import static com.newatlanta.nio.file.StandardOpenOption.SPARSE;
import static com.newatlanta.nio.file.StandardOpenOption.SYNC;
import static com.newatlanta.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

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
import java.util.concurrent.locks.Lock;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.util.RandomAccessMode;

import com.newatlanta.appengine.locks.ExclusiveLock;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributeView;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributes;
import com.newatlanta.appengine.nio.channels.GaeFileChannel;
import com.newatlanta.commons.vfs.provider.gae.GaeOutputStream;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
import com.newatlanta.nio.channels.FileChannel;
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
import com.newatlanta.nio.file.NotDirectoryException;
import com.newatlanta.nio.file.OpenOption;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.ProviderMismatchException;
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
    private Lock lock; // access via getLock()

    public GaePath( FileSystem fileSystem, String path ) {
        this.fileSystem = fileSystem;
        this.path = path;
        try {
            fileObject = GaeVFS.resolveFile( path );
        } catch ( FileSystemException e ) {
            throw new InvalidPathException( path, e.toString() );
        }
    }
    
    GaePath( FileSystem fileSystem, FileObject fileObject ) {
        this.fileSystem = fileSystem;
        this.fileObject = fileObject;
        path = fileObject.getName().getPath();
    }
    
    private synchronized Lock getLock() {
        if ( lock == null ) {
            lock = new ExclusiveLock( fileObject.getName().getPath() );
        }
        return lock;
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
        parent.getLock().lock(); // prevent delete or rename of parent
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
        parent.getLock().lock(); // prevent delete or rename of parent
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
            getLock().lock(); // prevent rename or create children
            try {
                if ( fileObject.getChildren().length > 0 ) { // not empty
                    throw new DirectoryNotEmptyException( toString() );
                }
                fileObject.delete();
            } finally {
                lock.unlock();
            }
        } else { // file
            fileObject.close();
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
        // TODO: THIS IS NOT RIGHT--THE ABSOLUTE PATH IS INCORRECT
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
        // if directory, then lock to prevent creation of children while moving
        if ( fileObject.getType().hasChildren() ) {
        }
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public DirectoryStream<Path> newDirectoryStream() throws IOException {
        if ( !fileObject.getType().hasChildren() ) {
            throw new NotDirectoryException( toString() );
        }
        return new GaeDirectoryStream( fileSystem, fileObject.getChildren() );
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( String glob ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( Filter<? super Path> filter )
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileChannel newByteChannel( Set<? extends OpenOption> options,
                                          FileAttribute<?> ... attrs ) throws IOException {
        checkByteChannelOpenOptions( options );
        if ( options.contains( CREATE_NEW ) ) {
            createFile( attrs ); // throws FileAlreadyExistsException
        } else if ( options.contains( CREATE ) ) {
            try {
                createFile( attrs );
            } catch ( FileAlreadyExistsException ignore ) {
            }
        }
        RandomAccessMode mode = RandomAccessMode.READ;
        if ( options.contains( WRITE ) ) {
            checkAccess( AccessMode.READ, AccessMode.WRITE );
            mode = RandomAccessMode.READWRITE;
        } else {
            checkAccess( AccessMode.READ );
        }
        FileChannel fc = new GaeFileChannel( fileObject.getContent().getRandomAccessContent( mode ),
                                                options.contains( APPEND ) );
        if ( options.contains( TRUNCATE_EXISTING ) ) {
            fc.truncate( 0 );
        }
        return fc;
    }
    
    @SuppressWarnings("unchecked")
    private static void checkByteChannelOpenOptions( Set<? extends OpenOption> options ) {
        if ( options.contains( SYNC ) ) {
            throw new UnsupportedOperationException( SYNC.name() );
        }
        if ( options.contains( DSYNC ) ) {
            throw new UnsupportedOperationException( DSYNC.name() );
        }
        if ( options.contains( DELETE_ON_CLOSE ) ) {
            throw new UnsupportedOperationException( DELETE_ON_CLOSE.name() );
        }
        if ( options.contains( APPEND ) ) {
            if ( options.contains( READ ) ) {
                throw new IllegalArgumentException( "Cannot specify both " + APPEND.name()
                                        + " and " + READ.name() + " options." );
            }
            if ( options.contains( TRUNCATE_EXISTING ) ) {
                throw new IllegalArgumentException( "Cannot specify both " + APPEND.name()
                        + " and " + TRUNCATE_EXISTING.name() + " options." );
            }
            ((Set<OpenOption>)options).add( WRITE ); // APPEND implies WRITE
        }
        if ( !options.contains( WRITE ) ) { // some options ignored if not writing
            options.remove( TRUNCATE_EXISTING );
            options.remove( CREATE );
            options.remove( CREATE_NEW );
            options.remove( SPARSE ); // ignored if not creating new file
        }
        if ( options.contains( SPARSE ) ) {
            throw new UnsupportedOperationException( SPARSE.name() );
        }
    }

    @Override
    public FileChannel newByteChannel( OpenOption ... options ) throws IOException {
        return newByteChannel( getOpenOptionSet( options ), new FileAttribute[ 0 ] );
    }

    private static Set<OpenOption> getOpenOptionSet( OpenOption ... options ) {
        Set<OpenOption> optionSet = new HashSet<OpenOption>( options.length );
        Collections.addAll( optionSet, options );
        return optionSet;
    }

    public InputStream newInputStream( OpenOption ... options ) throws IOException {
        for ( OpenOption option : options ) {
            if ( option != READ ) {
                throw new UnsupportedOperationException( option.toString() );
            }
        }
        checkAccess( AccessMode.READ );
        // don't use fileObject.getContent().getInputStream(); this avoids
        // automatic buffering by Commons VFS
        return fileObject.getContent().getRandomAccessContent(
                                            RandomAccessMode.READ ).getInputStream();
    }
    
    @Override
    public OutputStream newOutputStream( OpenOption ... options ) throws IOException {
        Set<OpenOption> optionSet = checkOutputStreamOpenOptions( options );
        if ( optionSet.contains( CREATE_NEW ) ) {
            createFile(); // throws FileAlreadyExistsException
        } else if ( optionSet.contains( CREATE ) ) {
            try {
                createFile();
            } catch ( FileAlreadyExistsException ignore ) {
            }
        }
        checkAccess( AccessMode.WRITE );
        // don't use fileObject.getContent().getOutputStream(); this avoids
        // automatic buffering by Commons VFS, and Commons VFS restriction that
        // allows only one open OutputStream
        return new GaeOutputStream( fileObject.getContent().getRandomAccessContent(
                    RandomAccessMode.READWRITE ), optionSet.contains( APPEND ) );
    }
    
    private static Set<OpenOption> checkOutputStreamOpenOptions( OpenOption ... options ) {
        Set<OpenOption> optionSet = getOpenOptionSet( options );
        if ( optionSet.contains( READ ) ) {
            throw new IllegalArgumentException( READ.name() );
        }
        if ( optionSet.contains( SYNC ) ) {
            throw new UnsupportedOperationException( SYNC.name() );
        }
        if ( optionSet.contains( DSYNC ) ) {
            throw new UnsupportedOperationException( DSYNC.name() );
        }
        if ( optionSet.contains( DELETE_ON_CLOSE ) ) {
            throw new UnsupportedOperationException( DELETE_ON_CLOSE.name() );
        }
        if ( optionSet.contains( SPARSE ) ) {
            throw new UnsupportedOperationException( SPARSE.name() );
        }
        if ( optionSet.contains( TRUNCATE_EXISTING ) ) {
            throw new UnsupportedOperationException( TRUNCATE_EXISTING.name() );
        }
        return optionSet;
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
