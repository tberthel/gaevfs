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
package com.newatlanta.appengine.junit.vfs.gae;

import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.Selectors;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.newatlanta.appengine.datastore.CachingDatastoreService;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;

/**
 * Tests the ability to list, create, delete, and rename directories.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFolderTestCase extends GaeVfsTestCase {
	
	private static String[] rootChildren = { "/.svn", "/docs", "/images", "/temp", "/test-data", "/testFolder" };
	
	private DatastoreService datastore;
	
	@Override
    public void setUp() throws Exception {
        super.setUp();
        datastore = new CachingDatastoreService();
    }
	
	public void testFolders() throws Exception {
		// root object
		FileObject rootObject = testRootObject();
		
		// create folder
		FileObject testFolder = testCreateTestFolder( rootObject );
		
		// create sub-folders
		FileObject subFolder = testFolder.resolveFile( "abc/def/ghi" );
		assertFalse( subFolder.exists() );
		subFolder.createFolder();
		assertFolder( subFolder );
		assertSubFolders( testFolder, new String[] { "abc/def/ghi", "abc/def", "abc" } );
		assertEntity( rootObject );

		// rename
		FileObject srcFolder = testFolder.resolveFile( "abc/def" );
		FileObject destFolder = testFolder.resolveFile( "abc/xyz" );
		assertTrue( testFolder.canRenameTo( destFolder ) );
		srcFolder.moveTo( destFolder );
		assertFolder( destFolder );
		assertFalse( srcFolder.exists() );
		assertSubFolders( testFolder, new String[] { "abc/xyz/ghi", "abc/xyz", "abc" } );
		
		// TODO: test moving a folder to a different parent

		// delete
		assertTrue( testFolder.exists() );
		testFolder.delete( Selectors.SELECT_ALL );
		assertFalse( testFolder.exists() );
	}

	private FileObject testRootObject() throws Exception {
		FileObject rootObject = GaeVFS.resolveFile( "/" );
		assertTrue( rootObject.exists() );
		assertEquals( rootObject.getName().getScheme(), "gae" );
		assertFolder( rootObject );
		assertNull( rootObject.getParent() );
		assertChildren( rootObject, rootChildren );
		
		// TODO: test rootObject.getName() and FileName methods

		assertEntity( rootObject );
		return rootObject;
	}

	private static void assertChildren( FileObject fileObject, String[] childNames ) throws Exception {
		FileObject[] children = fileObject.getChildren();
		for ( int i = 0; i < children.length; i++ ) {
			assertTrue( children[ i ].getName().getPath().endsWith( childNames[ i ] ) );
		}
	}
	
	private static void assertFolder( FileObject folder ) throws Exception {
		assertTrue( folder.exists() );
		assertTrue( folder.isAttached() ); // exists() causes attach
		assertTrue( folder.isReadable() );
		assertTrue( folder.isWriteable() );
		assertFalse( folder.isHidden() );
		assertTrue( folder.getType().hasChildren() );
		assertFalse( folder.getType().hasContent() );
		
		// TODO: with combined local option, local children of GAE folder will
		// have a different parent; maybe we can compare paths when GaeFileName
		// is modified to store abspath like LocalFileName
//		FileObject[] children = folder.getChildren();
//		for ( FileObject child : children ) {
//			assertEquals( folder, child.getParent() );
//			FileObject childObject = folder.getChild( child.getName().getURI() );
//			assertEquals( child, childObject );
//			assertEquals( folder, childObject.getParent() );
//		}
		
		// TODO: use folder.findFiles( Selectors.SELECT_ALL) to get all
		// descendants, then test FileName.isDescendant() and isAncestor()
	}
	
	private FileObject testCreateTestFolder( FileObject rootObject ) throws Exception {
		FileObject testFolder = GaeVFS.resolveFile( "testFolder" );
		assertFalse( testFolder.exists() );
		testFolder.createFolder();
		assertFolder( testFolder );
		assertEquals( rootObject, testFolder.getParent() );
		assertTrue( testFolder.getName().isAncestor( rootObject.getName() ) );
		assertTrue( rootObject.getName().isDescendent( testFolder.getName() ) );
		assertChildren( rootObject, rootChildren );
		assertEntity( rootObject );
		return testFolder;
	}
	
	private static void assertSubFolders( FileObject testFolder, String[] subFolderNames ) throws Exception {
		FileName rootName = testFolder.getFileSystem().getRootName();
		FileObject[] subFolders = testFolder.findFiles( Selectors.EXCLUDE_SELF );
		assertEquals( subFolders.length, subFolderNames.length );
		for ( int i = 0; i < subFolders.length; i++ ) {
			FileObject subObject = subFolders[ i ];
			assertTrue( subObject.getName().getPath().endsWith( subFolderNames[ i ] ) );
			assertFolder( subObject );
			assertEquals( subObject.getParent(), i == subFolders.length-1 ? testFolder : subFolders[ i+1 ] );
			assertTrue( rootName.isDescendent( subObject.getName() ) );
			assertTrue( subObject.getName().isAncestor( rootName ) );
		}
	}

	private void testFindFiles( FileObject rootObject ) throws Exception {
		FileObject[] findFiles = rootObject.findFiles( Selectors.EXCLUDE_SELF );
		findFiles = rootObject.findFiles( Selectors.SELECT_ALL );
		findFiles = rootObject.findFiles( Selectors.SELECT_CHILDREN );
		findFiles = rootObject.findFiles( Selectors.SELECT_FILES );
		findFiles = rootObject.findFiles( Selectors.SELECT_FOLDERS );
		findFiles = rootObject.findFiles( Selectors.SELECT_SELF );
		findFiles = rootObject.findFiles( Selectors.SELECT_SELF_AND_CHILDREN );
	}
	
	private void assertEntity( FileObject fileObject ) throws Exception {
		Key key = KeyFactory.createKey( "GaeFileObject", fileObject.getName().getPath() );
		assertEntity( datastore.get( key ) );
	}
	
	@SuppressWarnings("unchecked")
	private void assertEntity( Entity entity ) throws Exception {
		assertNotNull( entity );
		assertTrue( entity.hasProperty( "last-modified" ) );
		assertTrue( entity.hasProperty( "filetype" ) );
		
		String typeName = entity.getProperty( "filetype" ).toString();
		if ( typeName.equals( "folder" ) ) {
			assertFalse( entity.hasProperty( "block-size" ) );
			assertFalse( entity.hasProperty( "block-keys" ) );
			assertFalse( entity.hasProperty( "content-size" ) );
			if ( entity.hasProperty( "child-keys" ) ) {
				List<Key> childKeys = (List<Key>)entity.getProperty( "child-keys" );
				Map<Key, Entity> children = datastore.get( childKeys );
				assertEquals( childKeys.size(), children.size() );
				for ( Entity child : children.values() ) {
					assertEntity( child );
				}
			}
		} else if ( typeName.equals( "file" ) ) {
			assertFalse( entity.hasProperty( "child-keys" ) );
			assertTrue( entity.hasProperty( "content-size" ) );
			assertTrue( entity.hasProperty( "block-size" ) );
			assertTrue( entity.hasProperty( "block-keys" ) );
			List<Key> blockKeys = (List<Key>)entity.getProperty( "block-keys" );
			Map<Key, Entity> blocks = datastore.get( blockKeys );
			assertEquals( blockKeys.size(), blocks.size() );
			for ( Entity block : blocks.values() ) {
				assertFalse( block.hasProperty( "last-modified" ) );
				assertFalse( block.hasProperty( "filetype" ) );
				assertFalse( block.hasProperty( "child-keys" ) );
				assertFalse( block.hasProperty( "block-size" ) );
				assertFalse( block.hasProperty( "block-keys" ) );
				assertFalse( block.hasProperty( "content-size" ) );
				assertTrue( block.hasProperty( "content-blob" ) );
				assertTrue( block.getProperty( "content-blob" ) instanceof Blob );
			}
		} else {
			fail( typeName );
		}
	}
}
