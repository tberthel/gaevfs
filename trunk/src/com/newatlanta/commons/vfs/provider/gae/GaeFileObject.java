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

import static com.newatlanta.commons.vfs.provider.gae.GaeRandomAccessContent.copyContent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.provider.AbstractFileSystem;
import org.apache.commons.vfs.util.RandomAccessMode;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

/**
 * Stores metadata for "files" and "folders" within GaeVFS and manages interactions
 * with the Google App Engine datastore. This is an internal GaeVFS implementation
 * class that is normally not referenced directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>. See {@link GaeVFS} as the entry point for application
 * code that interacts with GaeVFS.
 *
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileObject extends AbstractFileObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    private static final String ENTITY_KIND = "GaeFileObject";

    // metadata property names
    private static final String FILETYPE = "filetype";
    private static final String LAST_MODIFIED = "last-modified";
    private static final String CHILD_KEYS = "child-keys";
    private static final String BLOCK_KEYS = "block-keys";
    private static final String CONTENT_SIZE = "content-size";
    private static final String BLOCK_SIZE = "block-size";

    private Entity metadata; // the wrapped GAE datastore entity

    private boolean isCombinedLocal;

    public GaeFileObject( FileName name, AbstractFileSystem fs ) {
        super( name, fs );
    }

    public void setCombinedLocal( boolean b ) {
        isCombinedLocal = b;
    }

    private int getBlockSize() throws FileSystemException {
        return ( (Long)metadata.getProperty( BLOCK_SIZE ) ).intValue();
    }

    public void setBlockSize( int size ) throws FileSystemException {
        if ( exists() ) {
            throw new FileSystemException( "cannot set block size after file is created" );
        }
        // exists() guarantees that metadata != null
        metadata.setProperty( BLOCK_SIZE, Long.valueOf( size ) );
    }

    @SuppressWarnings("unchecked")
    private List<Key> getBlockKeys() throws FileSystemException {
        return (List<Key>)metadata.getProperty( BLOCK_KEYS );
    }

    @SuppressWarnings("unchecked")
    private List<Key> getChildKeys() throws FileSystemException {
        if ( !getType().hasChildren() ) {
            throw new FileSystemException( "vfs.provider/list-children-not-folder.error",
                                                getName() );
        }
        return (List<Key>)metadata.getProperty( CHILD_KEYS );
    }

    // FileType is not a valid property type, so store the name
    private FileType getEntityFileType() {
        String typeName = (String)metadata.getProperty( FILETYPE );
        if ( typeName != null ) {
            if ( typeName.equals( FileType.FILE.getName() ) ) {
                return FileType.FILE;
            }
            if ( typeName.equals( FileType.FOLDER.getName() ) ) {
                return FileType.FOLDER;
            }
        }
        return FileType.IMAGINARY;
    }

    /**
     * Attaches this file object to its file resource.  This method is called
     * before any of the doBlah() or onBlah() methods.  Sub-classes can use
     * this method to perform lazy initialization.
     */
    @Override
    protected void doAttach() throws FileSystemException {
        if ( metadata == null ) {
            getMetaData( createKey() );
        }
        injectType( getEntityFileType() );
    }

    private synchronized void getMetaData( Key key ) throws FileSystemException {
        try {
            metadata = (Entity)memcache.get( key );
            if ( metadata == null ) {
                metadata = getEntity( key );
                memcache.put( key, metadata );
            }
        } catch ( EntityNotFoundException e ) {
            metadata = new Entity( ENTITY_KIND, key.getName() );
            setBlockSize( GaeVFS.getBlockSize() );
        }
    }

    private Key createKey() throws FileSystemException {
        return createKey( getName() );
    }

    private Key createKey( FileName fileName ) throws FileSystemException {
        // key name is relative path from the webapp root directory
        return KeyFactory.createKey( ENTITY_KIND, fileName.getPath() );
    }

    /**
     * Detaches this file object from its file resource.
     * 
     * Called when this file is closed.  Note that the file object may be
     * reused later, so should be able to be reattached.
     */
    @Override
    protected void doDetach() throws FileSystemException {
        metadata = null;
    }

    /**
     * Returns the file type. The main use of this method is to determine if the
     * file exists. As long as we always set the superclass type via injectType(),
     * this method never gets invoked (which is a good thing, because it's expensive).
     */
    @Override
    protected FileType doGetType() {
        try {
            // the only way to check if the metadata exists is to try to read it
            if ( ( metadata != null ) && ( getEntity( metadata.getKey() ) != null ) ) {
                return getName().getType();
            }
        } catch ( EntityNotFoundException e ) {
        }
        return FileType.IMAGINARY; // file doesn't exist
    }

    /**
     * Lists the children of this file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.
     * 
     * GAE note: this method only lists the GAE children, and not the local
     * children. But, with the current superclass implementation, this method
     * is never invoked if doListChildrenResolved() is implemented (see below).
     */
    @Override
    protected String[] doListChildren() throws FileSystemException {
        List<Key> childKeys = getChildKeys();
        if ( ( childKeys == null ) || ( childKeys.size() == 0 ) ) {
            return new String[ 0 ];
        }
        String[] childNames = new String[ childKeys.size() ];
        int i = 0;
        for ( Key childKey : childKeys ) {
            childNames[ i++ ] = childKey.getName();
        }
        return childNames;
    }

    /**
     * Lists the children of this file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.<br>
     * Other than <code>doListChildren</code> you could return FileObject's to
     * e.g. reinitialize the type of the file.<br>
     */
    @Override
    protected FileObject[] doListChildrenResolved() throws FileSystemException {
        List<Key> childKeys = getChildKeys();
        FileObject[] localChildren = getLocalChildren();
        if ( ( childKeys == null ) || ( childKeys.size() == 0 ) ) {
            return localChildren;
        }
        FileObject[] children = new FileObject[ localChildren.length + childKeys.size() ];

        if ( localChildren.length > 0 ) {
            System.arraycopy( localChildren, 0, children, 0, localChildren.length );
        }
        int i = localChildren.length;

        for ( Key child : childKeys ) {
            children[ i++ ] = resolveFile( child.getName() );
        }
        return children;
    }

    private FileObject[] getLocalChildren() throws FileSystemException {
        if ( isCombinedLocal ) {
            GaeFileName fileName = (GaeFileName)getName();
            String localUri = "file://" + fileName.getRootPath() + fileName.getPath();
            FileObject localFile = getFileSystem().getFileSystemManager().resolveFile( localUri );
            if ( localFile.exists() ) {
                return localFile.getChildren();
            }
        }
        return new FileObject[ 0 ];
    }

    /**
     * Deletes the file.
     */
    @Override
    protected void doDelete() {
        // the real work of deleting happens in onChange(), but we need a
        // do-nothing implementation to override the superclass, which throws
        // an exception
    }

    /**
     * Renames the file. If a folder, recursively rename the children.
     */
    @Override
    protected void doRename( FileObject newfile ) throws FileSystemException {
        if ( this.getType().hasChildren() ) { // rename the children
            for ( FileObject child : this.getChildren() ) {
                String newChildPath = child.getName().getPath().replace( this.getName().getPath(),
                                                                         newfile.getName().getPath() );
                child.moveTo( resolveFile( newChildPath ) );
            }
            newfile.createFolder();
        } else {
            GaeFileObject newGaeFile = (GaeFileObject)newfile;
            if ( newGaeFile.metadata == null ) { // newfile was deleted during rename
                newGaeFile.doAttach();
            }
            // TODO: getBlockKeys() can return null?
            int numBlocks = getBlockKeys().size(); // copy contents to the new file
            for ( int i = 0; i < numBlocks; i++ ) {
                // TODO: use Entity.setPropertiesFrom() added in SDK 1.2.2?
                // Entity newBlock = newGaeFile.getBlock( i );
                // newBlock.setPropertiesFrom( getBlock( i ) );
                // putBlock( newBlock );
                putBlock( copyContent( getBlock( i ), newGaeFile.getBlock( i ) ) );
                // TODO: write new blocks in a batch, not one at a time
            }
            // TODO: test copying a file to one with a different block size
            newGaeFile.metadata.setProperty( CONTENT_SIZE, this.metadata.getProperty( CONTENT_SIZE ) );
            newGaeFile.createFile();
        }
    }

    /**
     * Creates this file as a folder.  Is only called when:
     * <ul>
     * <li>{@link #doGetType} returns {@link FileType#IMAGINARY}.
     * <li>The parent folder exists and is writeable, or this file is the
     * root of the file system.
     * </ul>
     * <p/> 
     */
    @Override
    protected void doCreateFolder() throws FileSystemException {
        // an important side-effect of getType() is that it causes this object
        // to be attached; if not attached, then onChange() doesn't get invoked
        if ( getType() != FileType.FOLDER ) {
            injectType( FileType.FOLDER ); // always inject before putEntity()
            metadata.removeProperty( BLOCK_SIZE ); // not needed for folders
        }
        // onChange() will be invoked after this to put the metadata
    }

    /**
     * Called when the children of this file change.
     */
    protected void onChildrenChanged( FileName child, FileType newType ) throws FileSystemException {
        Key childKey = createKey( child );
        List<Key> childKeys = getChildKeys();
        if ( newType == FileType.IMAGINARY ) { // child being deleted
            if ( childKeys != null ) {
                childKeys.remove( childKey );
                if ( childKeys.size() == 0 ) {
                    metadata.removeProperty( CHILD_KEYS );
                }
            }
        } else { // child being added
            if ( childKeys == null ) {
                childKeys = new ArrayList<Key>();
                childKeys.add( childKey );
                metadata.setProperty( CHILD_KEYS, childKeys );
            } else if ( !childKeys.contains( childKey ) ) {
                childKeys.add( childKey );
            }
        }
        putMetaData();
    }

    /**
     * Called when the type or content of this file changes, or when it is created
     * or deleted.
     */
    @Override
    protected void onChange() throws FileSystemException {
        if ( getType() == FileType.IMAGINARY ) { // file/folder is being deleted
            if ( getName().getType().hasContent() ) {
                deleteBlocks();
            }
            deleteMetaData();
        } else { // file/folder is being created or modified
            putMetaData();
        }
    }

    private void deleteMetaData() throws FileSystemException {
        deleteEntity( metadata.getKey() );
        memcache.delete( metadata.getKey() );
        // metadata.getProperties().clear(); // see issue #1395
        Object[] properties = metadata.getProperties().keySet().toArray();
        for ( int i = 0; i < properties.length; i++ ) {
            metadata.removeProperty( properties[ i ].toString() );
        }
        setBlockSize( GaeVFS.getBlockSize() );
    }

    /**
     * Write the metadata to the GAE datastore. Make sure the file type is set
     * correctly and update the last modified time.
     */
    private synchronized void putMetaData() throws FileSystemException {
        metadata.setProperty( FILETYPE, getType().getName() );
        doSetLastModTime( System.currentTimeMillis() );
        putEntity( metadata );
        // memcache uses a "first created, first deleted" algorithm when purging
        // so remove first, then put to refresh the creation time
        memcache.delete( metadata.getKey() );
        memcache.put( metadata.getKey(), metadata );
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() {
        Long lastModified = (Long)metadata.getProperty( LAST_MODIFIED );
        return ( lastModified != null ? lastModified.longValue() : 0 );
    }

    /**
     * Sets the last modified time of this file.
     */
    @Override
    protected boolean doSetLastModTime( final long modtime ) {
        metadata.setProperty( LAST_MODIFIED, Long.valueOf( modtime ) );
        return true;
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws FileSystemException {
        if ( !getType().hasContent() ) {
            throw new FileSystemException( "vfs.provider/get-size-not-file.error", getName() );
        }
        Long contentSize = (Long)metadata.getProperty( CONTENT_SIZE );
        return ( contentSize != null ? contentSize.longValue() : 0 );
    }

    /**
     * Intended for use by GaeRandomAccessContent.
     */
    void updateContentSize( long newSize ) throws FileSystemException {
        updateContentSize( newSize, false );
    }

    void updateContentSize( long newSize, boolean force ) throws FileSystemException {
        if ( force || ( newSize > doGetContentSize() ) ) {
            metadata.setProperty( CONTENT_SIZE, Long.valueOf( newSize ) );
            putMetaData();
        }
    }

    /**
     * Creates an input stream to read the file content from.
     * 
     * The returned stream does not have to be buffered.
     */
    @Override
    protected InputStream doGetInputStream() throws IOException {
        if ( !getType().hasContent() ) {
            throw new FileSystemException( "vfs.provider/read-not-file.error", getName() );
        }
        return new GaeRandomAccessContent( this, RandomAccessMode.READ,
                                                getBlockSize() ).getInputStream();
    }

    /**
     * Creates access to the file for random i/o. Is only called if doGetType()
     * returns FileType.FILE
     * 
     * It is guaranteed that there are no open output streams for this file
     * when this method is called.
     */
    protected RandomAccessContent doGetRandomAccessContent( RandomAccessMode mode )
            throws IOException {
        return new GaeRandomAccessContent( this, mode, getBlockSize() );
    }

    /**
     * Creates an output stream to write the file content to.
     * 
     * It is guaranteed that there are no open stream (input or output) for
     * this file when this method is called.
     * 
     * The returned stream does not have to be buffered.
     */
    @Override
    protected OutputStream doGetOutputStream( boolean bAppend ) throws IOException {
        return new GaeRandomAccessContent( this, RandomAccessMode.READWRITE, getBlockSize(),
                                            bAppend && exists() ? doGetContentSize() : 0 );
    }

    /**
     * The following methods related to blocks are for use by
     * GaeRandomAccessContent.
     */
    Entity getBlock( int i ) throws FileSystemException {
        if ( !exists() ) {
            createFile();
        }
        Entity block = null;
        List<Key> blockKeys = getBlockKeys();
        if ( blockKeys == null ) {
            blockKeys = new ArrayList<Key>();
            metadata.setProperty( BLOCK_KEYS, blockKeys );
        }
        if ( i < blockKeys.size() ) {
            Key key = blockKeys.get( i );
            try {
                return getEntity( key );
            } catch ( EntityNotFoundException e ) {
                blockKeys.remove( key );
                block = createBlock( blockKeys, i );
            }
        } else {
            for ( int j = blockKeys.size(); j <= i; j++ ) {
                block = createBlock( blockKeys, j );
            }
        }
        // a new block was created
        putMetaData();
        return block;
    }

    private Entity createBlock( List<Key> blockKeys, int i ) {
        Entity block = new Entity( ENTITY_KIND, "block." + i, metadata.getKey() );
        blockKeys.add( i, block.getKey() );
        return block;
    }

    void putBlock( Entity block ) {
        putEntity( block );
    }

    private void deleteBlocks() throws FileSystemException {
        List<Key> blockKeys = getBlockKeys();
        if ( blockKeys != null ) {
            deleteEntities( blockKeys );
        }
    }

    /**
     * Truncate blocks up to but exclusive of the specified index.
     */
    void deleteBlocks( int stopIndex ) throws FileSystemException {
        List<Key> blockKeys = getBlockKeys();
        if ( ( blockKeys != null ) && ( blockKeys.size() > ( stopIndex + 1 ) ) ) {
            List<Key> deleteKeyList = new ArrayList<Key>();
            for ( int i = blockKeys.size() - 1; i > stopIndex; i-- ) {
                deleteKeyList.add( blockKeys.remove( i ) );
            }
            deleteEntities( deleteKeyList );
            putMetaData();
        }
    }

    private Entity getEntity( Key key ) throws EntityNotFoundException {
        try {
            return datastore.get( key );
        } catch ( DatastoreTimeoutException e ) {
            return datastore.get( key ); // try twice upon timeout
        }
    }

    private void putEntity( Entity entity ) {
        try {
            datastore.put( entity );
        } catch ( DatastoreTimeoutException e ) {
            datastore.put( entity ); // try twice upon timeout
        }
    }

    private void deleteEntity( Key key ) {
        try {
            datastore.delete( key );
        } catch ( DatastoreTimeoutException e ) {
            datastore.delete( key ); // try twice upon timeout
        }
    }

    private void deleteEntities( List<Key> keys ) {
        try {
            datastore.delete( keys );
        } catch ( DatastoreTimeoutException e ) {
            datastore.delete( keys ); // try twice upon timeout
        }
    }

    protected void finalize() throws Throwable {
        if ( getFileSystem() != null ) { // avoid NPE in super.finalize()
            super.finalize();
        }
    }

    /**
     * For testing and debugging.
     */
    public static Iterable<Entity> getAllEntities() {
        return datastore.prepare( new Query( ENTITY_KIND ) ).asIterable();
    }

    public static void removeAllEntities() {
        Iterable<Entity> entities = getAllEntities();
        for ( Entity e : entities ) {
            datastore.delete( e.getKey() );
        }
    }
}
