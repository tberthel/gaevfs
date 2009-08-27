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

import java.io.File;

import org.apache.commons.vfs.FileObject;

import com.newatlanta.appengine.junit.LocalDatastoreTestCase;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;

/**
 * The base class for a GaeVFS junit testcases.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public abstract class GaeVfsTestCase extends LocalDatastoreTestCase {
    
    static {
        System.setProperty( "user.dir", new File( "test/data" ).getAbsolutePath() );
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    public void tearDown() throws Exception {
        GaeVFS.getManager().getFilesCache().close();
        super.tearDown();
    }
    
    public static void assertEquals( File file, FileObject fileObject ) throws Exception {
    	assertEqualPaths( file, fileObject );
		assertEquals( file.canRead(), fileObject.isReadable() );
		assertEquals( file.canWrite(), fileObject.isWriteable() );
		assertEquals( file.exists(), fileObject.exists() );
		if ( file.getParentFile() == null ) {
			assertNull( fileObject.getParent() );
		} else {
			assertEqualPaths( file.getParentFile(), fileObject.getParent() );
		}
		assertEquals( file.isDirectory(), fileObject.getType().hasChildren() );
		assertEquals( file.isFile(), fileObject.getType().hasContent() );
		assertEquals( file.isHidden(), fileObject.isHidden() );
		if ( file.isFile() ) {
			assertEquals( file.length(), fileObject.getContent().getSize() );
		}
		if ( file.isDirectory() ) { // same children
			File[] childFiles = file.listFiles();
			FileObject[] childObjects = fileObject.getChildren();
			assertEquals( childFiles.length, childObjects.length );
			for ( int i = 0; i < childFiles.length; i++ ) {
				assertEqualPaths( childFiles[ i ], childObjects[ i ] );
			}
		}
    }
    
    public static void assertEqualPaths( File file, FileObject fileObject ) throws Exception {
    	String fileObjectPath = new File( fileObject.getName().getPath() ).getAbsolutePath();
    	assertEquals( file.getAbsolutePath(), fileObjectPath ); 
    	assertEquals( file.getName(), fileObject.getName().getBaseName() );
    }
}