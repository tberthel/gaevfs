/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.commons.vfs.provider.gae;

import java.io.*;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.*;

import com.google.appengine.api.datastore.*;

/**
 * Does the work of interacting with the GAE datastore. Every "file" and "folder"  within
 * the GAE datastore is saved as an Entity; this class is basically a wrapper around the
 * Entity. The parent-child relationships of files and folders is stored within the Entity
 * keys.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 * @version $Revision: 1.5 $ $Date: 2009/06/23 20:51:29 $
 */
public class GaeFileObject extends AbstractFileObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	private static final String ENTITY_KIND = "GaeFileObject";
	
	// entity property names
	private static final String FILETYPE = "filetype";
	private static final String LAST_MODIFIED = "last-modified";
	private static final String CONTENT_BLOB = "content";
	
	private Entity entity; // the GAE datastore entity represented by this file object
	
	private transient byte[] contentBytes; // for reading from the file; access via getContents() method
	private transient ByteArrayOutputStream bytesOut; // for writing to the file

	protected GaeFileObject( FileName name, AbstractFileSystem fs ) {
		super( name, fs );
	}

	/**
     * Attaches this file object to its file resource.  This method is called
     * before any of the doBlah() or onBlah() methods.  Sub-classes can use
     * this method to perform lazy initialization.
     */
	@Override
    protected void doAttach() throws FileSystemException {
		try {
			if ( entity == null ) {
				entity = datastore.get( createKey( true ) );
			}
			injectType( getEntityFileType() );
		} catch ( EntityNotFoundException e ) {
			entity = new Entity( ENTITY_KIND, getName().getPath(), getParentKey() );
			injectType( FileType.IMAGINARY ); // file doesn't exist
		}
    }
	
	private Key createKey( boolean includeParent ) throws FileSystemException {
		return KeyFactory.createKey( includeParent ? getParentKey() : null, ENTITY_KIND, getName().getPath() );
	}
	
	/**
	 * GAE datastore keys have only a single parent, not a full hierarchy of parents. That way when
	 * we query for all the descendants of a key, we only go one level down.
	 */
	private Key getParentKey() throws FileSystemException {
		FileObject parent = getParent();
		if ( parent == null ) { // true for root file
			return null;
		}
		return ((GaeFileObject)parent).createKey( false );
	}
	
	// this is needed because FileType is not a valid property type
	private FileType getEntityFileType() {
		String typeName = (String)entity.getProperty( FILETYPE );
		if ( typeName != null ) {
			if ( typeName.equals( FileType.FILE.getName() ) ) {
				return FileType.FILE;
			}
			if ( typeName.equals( FileType.FOLDER.getName() ) ) {
				return FileType.FOLDER;
			}
			if ( typeName.equals( FileType.FILE_OR_FOLDER.getName() ) ) {
				return FileType.FILE_OR_FOLDER;
			}
		}
		return FileType.IMAGINARY;
	}
	
	/**
     * Detaches this file object from its file resource.
     * 
     * Called when this file is closed.  Note that the file object may be
     * reused later, so should be able to be reattached.
     */
	@Override
    protected void doDetach() {
		entity = null;
		contentBytes = null;
		bytesOut = null;
    }
    
	/**
	 * Returns the file type. The main use of this method is to determine if the file
	 * exists. As long as we always set the superclass type via injectType(), this
	 * method never gets invoked (which is a good thing, because it's expensive).
	 */
    @Override
	protected FileType doGetType() {
    	try {
    		// the only way to check if the entity exists is to try to read it from the datastore
			if ( ( entity != null ) && ( datastore.get( entity.getKey() ) != null ) ) {
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
		PreparedQuery childQuery = prepareChildQuery();
		String[] childNames = new String[ childQuery.countEntities() ];
		int i = 0;
		for ( Entity child : childQuery.asIterable() ) {
			childNames[ i++ ] = child.getKey().getName();
		}
		return childNames;
	}
    
    /**
     * Lists the children of this file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.<br>
     * Other than <code>doListChildren</code> you could return FileObject's to
     * e.g. reinitialize the type of the file.<br>
     * 
     * GAE note: this implementation could probably be significantly optimized,
     * but the above comment (from the superclass) indicates we don't need to
     * worry about it.
     */
	@Override
    protected FileObject[] doListChildrenResolved() throws FileSystemException {
		PreparedQuery childQuery = prepareChildQuery();
		FileObject[] localChildren = getLocalChildren();
    	FileObject[] children = new FileObject[ localChildren.length + childQuery.countEntities() ];
    	
    	if ( localChildren.length > 0 ) {
    		System.arraycopy( localChildren, 0, children, 0, localChildren.length );
    	}
		int i = localChildren.length;
		
    	for ( Entity child : childQuery.asIterable() ) {
    		children[ i++ ] = resolveFile( child.getKey().getName() );
    	}
    	return children;
	}
	
	private PreparedQuery prepareChildQuery() throws FileSystemException {
		return datastore.prepare( new Query( ENTITY_KIND, createKey( false ) ).setKeysOnly() );
	}
	
	private FileObject[] getLocalChildren() throws FileSystemException {
		GaeFileSystemManager fsManager = (GaeFileSystemManager)getFileSystem().getFileSystemManager();
		if ( fsManager.isCombinedLocal() ) {
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
    	// the real work of deleting happens in onChange(), but we need a do-nothing
    	// implementation to override the superclass, which throws an exception
    }
    
    /**
     * Renames the file. If a folder, recursively rename the children.
     */
    @Override
    protected void doRename( FileObject newfile ) throws FileSystemException {
    	if ( this.getType().hasChildren() ) {
    		for ( FileObject child : this.getChildren() ) { // rename the children
        		String newChildPath = child.getName().getPath().replace( this.getName().getPath(), newfile.getName().getPath() );
    			child.moveTo( resolveFile( newChildPath ) );
    		}
    		newfile.createFolder(); // newfile gets detached when renaming children, this makes sure it gets created
    	} else {
    		// copy contents to the new file
    		((GaeFileObject)newfile).entity.setProperty( CONTENT_BLOB, this.entity.getProperty( CONTENT_BLOB ) );
    		newfile.createFile(); // make sure parent folders get created
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
    	// an important side-effect of getType() is that it causes this object to be attached;
    	// if not attached, then onChange() doesn't get invoked
    	if ( getType() != FileType.FOLDER ) {
    		injectType( FileType.FOLDER ); // always inject type before putEntity()
    	}
    	// onChange() will be invoked after this to put the entity
    }
    
    /**
     * Called when the type or content of this file changes, or when it is created or deleted.
     */
    @Override
    protected void onChange() throws FileSystemException {
    	if ( entity == null ) { // should always attached to an entity, but be sure
    		return;
    	}
		if ( getType() == FileType.IMAGINARY ) { // file is being deleted
			getFileSystem().getFileSystemManager().getFilesCache().removeFile( this ); // remove from cache
        	datastore.delete( entity.getKey() );
        	entity = null;
        	return;
		}
		FileType entityFileType = getEntityFileType(); // returns FileType.IMAGINARY if not in datastore
		if ( entityFileType.hasChildren() ) {
			return; // contents of folders don't change
		}
		if ( ( bytesOut != null ) && ( bytesOut.size() > 0 ) ) {
			entity.setProperty( CONTENT_BLOB, new Blob( bytesOut.toByteArray() ) );
			bytesOut = null;
		} else if ( entityFileType.hasContent() ) {
			return; // don't put files if content hasn't changed
		}
		putEntity();
    }

    /**
     * Write the entity to the GAE datastore. Make sure the file type is set correctly
     * and update the last modified time.
     */
	private void putEntity() throws FileSystemException {
		entity.setProperty( FILETYPE, getType().getName() );
		doSetLastModifiedTime( System.currentTimeMillis() );
		datastore.put( entity );
		getFileSystem().getFileSystemManager().getFilesCache().putFile( this ); // update in cache
	}

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() {
        return ((Long)entity.getProperty( LAST_MODIFIED )).longValue();
    }

    /**
     * Sets the last modified time of this file.
     */
    @Override
    protected void doSetLastModifiedTime( final long modtime ) {
        entity.setProperty( LAST_MODIFIED, Long.valueOf( modtime ) );
    }
    
    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
	protected long doGetContentSize() {
		return getContents().length;
	}

    /**
     * Creates an input stream to read the file content from.
     * 
     * The returned stream does not have to be buffered.
     */
	@Override
	protected InputStream doGetInputStream() {
		return new ByteArrayInputStream( getContents() );
	}
	
	private byte[] getContents() {
		if ( contentBytes == null ) {
			contentBytes = ((Blob)entity.getProperty( CONTENT_BLOB )).getBytes();
			if ( contentBytes == null ) {
				contentBytes = new byte[ 0 ];
			}
		}
		return contentBytes;
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
    protected OutputStream doGetOutputStream( boolean bAppend ) {
		bytesOut = new ByteArrayOutputStream( 8 * 1024 ); // 8KB initial size
		return bytesOut;
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
