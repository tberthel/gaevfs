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
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

/**
 * Does the work of interacting with the GAE datastore. Every "file" and "folder"
 * within the GAE datastore is saved as an Entity; this class is basically a
 * wrapper around the Entity. The parent-child relationships of files and folders
 * are represented within the Entity keys.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeFileObject extends AbstractFileObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    private static final String ENTITY_KIND = "GaeFileObject";

    // entity property names
    private static final String FILETYPE = "filetype";
    private static final String LAST_MODIFIED = "last-modified";
    private static final String CHILD_KEYS = "child-keys";
    private static final String CONTENT_KEYS = "content-keys";
    private static final String CONTENT_SIZE = "content-size";
    private static final String BLOCK_SIZE = "block-size";

    private Entity entity; // the wrapped GAE datastore entity

    private boolean isCombinedLocal;

    public GaeFileObject( FileName name, AbstractFileSystem fs ) {
        super( name, fs );
    }
    
    public void setCombinedLocal( boolean b ) {
        isCombinedLocal = b;
    }
    
    public int getBlockSize() {
        return ((Long)entity.getProperty( BLOCK_SIZE )).intValue();
    }
    
    public void setBlockSize( int size ) throws FileSystemException {
        if ( exists() ) {
            throw new FileSystemException( "cannot set block size after file is created" );
        }
        // exists() guarantees that entity != null
        entity.setProperty( BLOCK_SIZE, Long.valueOf( size ) );
    }
    
    @SuppressWarnings("unchecked")
    private List<Key> getContentKeys() {
        return (List<Key>)entity.getProperty( CONTENT_KEYS );
    }
    
    @SuppressWarnings("unchecked")
    private List<Key> getChildKeys() {
        return (List<Key>)entity.getProperty( CHILD_KEYS );
    }
    
    // FileType is not a valid property type, so store the name
    private FileType getEntityFileType() {
        String typeName = (String)entity.getProperty( FILETYPE );
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
        if ( entity == null ) {
            getEntity( createKey() );
        }
        injectType( getEntityFileType() );
    }
    
    private void getEntity( Key key ) throws FileSystemException {
        try {
            entity = datastore.get( key );
        } catch ( EntityNotFoundException e ) {
            entity = new Entity( ENTITY_KIND, key.getName() );
            setBlockSize( GaeVFS.getBlockSize() );
        }
    }

    private Key createKey() throws FileSystemException {
        return createKey( getName() );
    }
    
    private Key createKey( FileName fileName ) throws FileSystemException {
        String keyName; // key name is relative path from the webapp root directory
        String rootPath = getFileSystem().getRootName().getPath();
        if ( rootPath.equals( fileName.getPath() ) ) {
            keyName = "/";
        } else {
            keyName = fileName.getPath().substring( rootPath.length() );
        }
        return KeyFactory.createKey( ENTITY_KIND, keyName );
    }

    /**
     * Detaches this file object from its file resource.
     * 
     * Called when this file is closed.  Note that the file object may be
     * reused later, so should be able to be reattached.
     */
    @Override
    protected void doDetach() throws FileSystemException {
        if ( getType() != FileType.IMAGINARY ) {
            putEntity(); // write entity to the datastore
        }
        entity = null;
    }

    /**
     * Returns the file type. The main use of this method is to determine if the
     * file exists. As long as we always set the superclass type via injectType(),
     * this method never gets invoked (which is a good thing, because it's expensive).
     */
    @Override
    protected FileType doGetType() {
        try {
            // the only way to check if the entity exists is to try to read it
            if ( ( entity != null ) && ( datastore.get( entity.getKey() ) != null ) ) {
                return getName().getType();
            }
        } catch ( EntityNotFoundException e ) {
        }
        return FileType.IMAGINARY; // file doesn't exist
    }
    
    /**
     * Because GaeVFS uses CacheStrategy.MANUAL we must clear the children cache
     * of our superclass before resolving the children. With the default
     * CacheStrategy.ON_RESOLVE this would have been done for us.
     */
    @Override
    public FileObject[] getChildren() throws FileSystemException {
        super.refresh(); // clear the children cache
        return super.getChildren();
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
        FileObject[] localChildren = getLocalChildren();
        List<Key> childKeys = getChildKeys();
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
            GaeFileSystemManager fsManager = (GaeFileSystemManager)getFileSystem().getFileSystemManager();
            FileObject localFile = fsManager.resolveFile( "file://" + getName().getPath() );
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
            if ( newGaeFile.entity == null ) { // newfile was deleted during rename
                newGaeFile.doAttach();
            }
            List<Key> contentKeys = getContentKeys(); // copy contents to the new file
            for ( int i = 0; i < contentKeys.size(); i++  ) {
                // TODO: use Entity.setPropertiesFrom() added in SDK 1.2.2?
                writeContentEntity( copyContent( getContentEntity( i ),
                                                    newGaeFile.getContentEntity( i ) ) );
            }
            // TODO: test copying a file to one with a different block size
            newGaeFile.entity.setProperty( CONTENT_SIZE, this.entity.getProperty( CONTENT_SIZE ) );
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
            entity.removeProperty( BLOCK_SIZE ); // not needed for folders
        }
        // onChange() will be invoked after this to put the entity
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
                    entity.removeProperty( CHILD_KEYS );
                }
            }
        } else { // child being added
            if ( childKeys == null ) {
                childKeys = new ArrayList<Key>();
                childKeys.add( childKey );
                entity.setProperty( CHILD_KEYS, childKeys );
            } else if ( !childKeys.contains( childKey ) ) {
                childKeys.add( childKey );
            }
        }
        putEntity();
    }

    /**
     * Called when the type or content of this file changes, or when it is created
     * or deleted.
     */
    @Override
    protected void onChange() throws FileSystemException {
        if ( getType() == FileType.IMAGINARY ) { // file/folder is being deleted
            getFileSystem().getFileSystemManager().getFilesCache().removeFile( this );
            List<Key> contentKeys = getContentKeys();
            if ( contentKeys != null ) {
                datastore.delete( contentKeys );
            }
            datastore.delete( entity.getKey() );
            entity = null;
        } else { // file/folder is being created or modified
            putEntity();
        }
    }

    /**
     * Write the entity to the GAE datastore. Make sure the file type is set
     * correctly and update the last modified time.
     */
    private void putEntity() throws FileSystemException {
        // TODO: set a flag to see if the file has actually changed before put
        entity.setProperty( FILETYPE, getType().getName() );
        doSetLastModTime( System.currentTimeMillis() );
        datastore.put( entity );
        getFileSystem().getFileSystemManager().getFilesCache().putFile( this );
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() {
        Long lastModified = (Long)entity.getProperty( LAST_MODIFIED );
        return ( lastModified != null ? lastModified.longValue() : 0 );
    }

    /**
     * Sets the last modified time of this file.
     */
    @Override
    protected boolean doSetLastModTime( final long modtime ) {
        entity.setProperty( LAST_MODIFIED, Long.valueOf( modtime ) );
        return true;
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() {
        Long contentSize = (Long)entity.getProperty( CONTENT_SIZE );
        return ( contentSize != null ? contentSize.longValue() : 0 );
    }
    
    /**
     * Intended for use by GaeRandomAccessContent.
     */
    void updateContentSize( long newSize ) {
        updateContentSize( newSize, false );
    }
    
    void updateContentSize( long newSize, boolean force ) {
        if ( force || ( newSize > doGetContentSize() ) ) {
            entity.setProperty( CONTENT_SIZE, Long.valueOf( newSize ) );
        }
    }

    /**
     * Creates an input stream to read the file content from.
     * 
     * The returned stream does not have to be buffered.
     */
    @Override
    protected InputStream doGetInputStream() throws IOException {
        return new GaeRandomAccessContent( this, RandomAccessMode.READ ).getInputStream();
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
        return new GaeRandomAccessContent( this, mode );
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
        return new GaeRandomAccessContent( this, RandomAccessMode.READWRITE,
                                             bAppend ? doGetContentSize() : 0 );
    }
    
    /**
     * The following methods related to content entities are for use by
     * GaeRandomAccessContent.
     */
    Entity getContentEntity( int i ) throws FileSystemException {
        List<Key> contentKeys = getContentKeys();
        if ( contentKeys == null ) {
            contentKeys = new ArrayList<Key>();
            entity.setProperty( CONTENT_KEYS, contentKeys );
        } else if ( i < contentKeys.size() ) {
            try {
                return datastore.get( contentKeys.get( i ) );
            } catch ( EntityNotFoundException e ) {
                return createContentEntity( contentKeys, i );
            }
        }
        Entity contentEntity = null;
        for ( int j = contentKeys.size(); j <= i; j++ ) {
            contentEntity = createContentEntity( contentKeys, j );
        }
        return contentEntity;
    }

    private Entity createContentEntity( List<Key> contentKeys, int i ) throws FileSystemException {
        Key contentKey = KeyFactory.createKey( entity.getKey(), ENTITY_KIND,  "content." + i );
        contentKeys.add( i, contentKey );
        return new Entity( ENTITY_KIND, contentKey.getName(), contentKey.getParent() );
    }
    
    void writeContentEntity( Entity contentEntity ) throws FileSystemException {
        datastore.put( contentEntity );
        if ( !exists() ) {
            injectType( FileType.FILE );
        }
        putEntity();
    }
    
    /**
     * Truncate the content entities up to but exclusive of the specified index.
     */
    void deleteContentEntities( int stopIndex ) {
        List<Key> contentKeys = getContentKeys();
        if ( ( contentKeys != null ) && ( contentKeys.size() > ( stopIndex + 1 ) ) ) {
            List<Key> deleteKeyList = new ArrayList<Key>();
            for ( int i = contentKeys.size() - 1; i > stopIndex; i-- ) {
                deleteKeyList.add( contentKeys.remove( i ) );
            }
            datastore.delete( deleteKeyList );
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
