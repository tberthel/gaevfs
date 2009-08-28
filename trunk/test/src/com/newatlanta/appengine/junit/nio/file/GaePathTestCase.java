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
package com.newatlanta.appengine.junit.nio.file;

import java.io.IOException;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.nio.file.AccessDeniedException;
import com.newatlanta.nio.file.AccessMode;
import com.newatlanta.nio.file.FileAlreadyExistsException;
import com.newatlanta.nio.file.NoSuchFileException;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.Paths;
import com.newatlanta.nio.file.attribute.Attributes;

/**
 * Tests <code>com.newatlanta.appengine.nio.file.GaePath</code>.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaePathTestCase extends GaeVfsTestCase {
    
    @Test
    public void testHashCode() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetFileSystem() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsAbsolute() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetRoot() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetName() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetParent() throws IOException {
        Path rootPath = Paths.get( "/" );
        assertNull( rootPath.getParent() );
        
        // check an existing directory
        Path dirPath = Paths.get( "images" );
        Path parentPath = dirPath.getParent();
        assertNotNull( parentPath );
        assertTrue( parentPath.equals( rootPath ) );
        assertTrue( parentPath.isSameFile( rootPath ) );
        assertEquals( 0, parentPath.compareTo( rootPath ) );
        
        // check a non-existing directory with non-existing parent
        dirPath = Paths.get( "abc/def" );
        parentPath = dirPath.getParent();
        
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetNameCount() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetNameInt() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testSubpath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testStartsWith() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testEndsWith() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNormalize() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testResolvePath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testResolveString() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRelativize() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testDelete() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testDeleteIfExists() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCreateSymbolicLink() throws IOException {
        try {
            Paths.get( "abc" ).createSymbolicLink( Paths.get( "def" ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testCreateLink() throws IOException {
        try {
            Paths.get( "abc" ).createLink( Paths.get( "def" ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testReadSymbolicLink() throws IOException {
        try {
            Paths.get( "abc" ).readSymbolicLink();
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testToUri() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testToAbsolutePath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testToRealPath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCopyTo() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testMoveTo() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewDirectoryStream() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewDirectoryStreamString() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCreateFile() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCreateDirectory() throws IOException {
        // attempt to create a directory with a non-existing parent
        Path dirPath = Paths.get( "abc/def" );
        assertFalse( dirPath.exists() );
        try {
            dirPath.createDirectory();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // attempt to create a directory within a non-directory parent (?)
//        Path filePath = Paths.get( "images/large.jpg" );
//        dirPath = filePath.resolve( "newDir" ); // TODO: finish when Path.resolve() supported
//        dirPath.createDirectory();
        
        // create a directory within a local directory
        Path imagesPath = Paths.get( "images" );
//        dirPath = imagesPath.resolve( "test1" ); // TODO: finish when Path.resolve() supported
//        dirPath = dirPath.createDirectory();
//        assertNotNull( dirPath );
//        assertTrue( Attributes.readBasicFileAttributes( dirPath ).isDirectory() );
        // check parent: equals? isSameFile? compareTo?
        
        // create a directory within a local directory
        dirPath = Paths.get( "images/test2" );
        dirPath = dirPath.createDirectory();
        assertNotNull( dirPath );
        assertTrue( dirPath.exists() );
        //assertTrue( Attributes.readBasicFileAttributes( dirPath ).isDirectory() );
        // check parent: equals? isSameFile? compareTo?
        
        // create a directory within a GaeVFS directory
        dirPath = Paths.get( "/images/test2/test3" );
        dirPath = dirPath.createDirectory();
        assertNotNull( dirPath );
        assertTrue( dirPath.exists() );
        // check parent: equals? isSameFile? compareTo?
        
        // attempt to create a local directory that already exists
        try {
            assertTrue( imagesPath.exists() );
            imagesPath.createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // attempt to create a GaeVFS directory that already exists
        try {
            assertTrue( dirPath.exists() );
            dirPath.createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // attempt to create a directory from an existing local file
        try {
            Path filePath = Paths.get( "images/large.jpg" );
            assertTrue( filePath.exists() );
            filePath.createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // attempt to create a directory from an existing GaeVFS file
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewByteChannelSetOfQextendsOpenOptionFileAttributeOfQArray() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewByteChannelOpenOptionArray() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewOutputStream() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsHidden() {
        fail( "Not yet implemented" );
    }

    /**
     * Also test exists() and notExists(), which invoke checkAccess().
     */
    @Test
    public void testCheckAccess() throws IOException {
        // check the root path
        Path rootPath = Paths.get( "/" );
        assertTrue( rootPath.exists() );
        assertFalse( rootPath.notExists() );
        rootPath.checkAccess();
        rootPath.checkAccess( AccessMode.READ );
        rootPath.checkAccess( AccessMode.WRITE );
        rootPath.checkAccess( AccessMode.READ, AccessMode.WRITE );
        try {
            rootPath.checkAccess( AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE );
            fail( "expected AccessDeniedException" );
        } catch ( AccessDeniedException e ) {
        }
        
        // check an existing directory
        Path dirPath = Paths.get( "docs" );
        assertTrue( dirPath.exists() );
        assertFalse( dirPath.notExists() );
        dirPath.checkAccess();
        dirPath.checkAccess( AccessMode.READ );
        dirPath.checkAccess( AccessMode.WRITE );
        dirPath.checkAccess( AccessMode.READ, AccessMode.WRITE );
        try {
            dirPath.checkAccess( AccessMode.READ, AccessMode.EXECUTE );
            fail( "expected AccessDeniedException" );
        } catch ( AccessDeniedException e ) {
        }
        
        // check an existing file
        Path filePath = Paths.get( "docs/large.pdf" );
        assertTrue( filePath.exists() );
        assertFalse( filePath.notExists() );
        filePath.checkAccess( AccessMode.READ );
        // JUnit test environment does not enforce read-only access to local files
//        try {
//            filePath.checkAccess( AccessMode.WRITE );
//            fail( "expected AccessDeniedException" );
//        } catch ( AccessDeniedException e ) {
//        }
        try {
            filePath.checkAccess( AccessMode.EXECUTE );
            fail( "expected AccessDeniedException" );
        } catch ( AccessDeniedException e ) {
        }
        
        // check a non-existing directory
        dirPath = Paths.get( "doesNotExist" );
        assertFalse( dirPath.exists() );
        assertTrue( dirPath.notExists() );
        try {
            dirPath.checkAccess();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // check a non-existing file
        filePath = Paths.get( "docs/doesNotExist.pdf" );
        assertFalse( filePath.exists() );
        assertTrue( filePath.notExists() );
        try {
            filePath.checkAccess();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
    }

    @Test
    public void testGetFileStore() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIterator() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCompareTo() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsSameFile() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testEqualsObject() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testToString() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewDirectoryStreamFilterOfQsuperPath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRegisterWatchServiceKindOfQArrayModifierArray() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRegisterWatchServiceKindOfQArray() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetAttribute() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetFileAttributeView() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewInputStream() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testReadAttributes() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testSetAttribute() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewDirectoryStreamFilterOfQsuperPath1() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRegisterWatchServiceKindOfQArrayModifierArray1() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRegisterWatchServiceKindOfQArray1() {
        fail( "Not yet implemented" );
    }

}
