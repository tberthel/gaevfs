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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.repackaged.java.nio.file.Path;
import com.newatlanta.repackaged.java.nio.file.Paths;
import com.newatlanta.repackaged.java.nio.file.attribute.AclEntry;
import com.newatlanta.repackaged.java.nio.file.attribute.Attributes;
import com.newatlanta.repackaged.java.nio.file.attribute.BasicFileAttributes;
import com.newatlanta.repackaged.java.nio.file.attribute.DosFileAttributes;
import com.newatlanta.repackaged.java.nio.file.attribute.FileStoreSpaceAttributes;
import com.newatlanta.repackaged.java.nio.file.attribute.FileTime;
import com.newatlanta.repackaged.java.nio.file.attribute.PosixFileAttributes;
import com.newatlanta.repackaged.java.nio.file.attribute.PosixFilePermission;
import com.newatlanta.repackaged.java.nio.file.attribute.UserPrincipal;

public class GaeFileAttributesTestCase extends GaeVfsTestCase {
    
    @Test
    public void testGaeDirectory() throws IOException {
        Path path = Paths.get( "/attributeTest" ).createDirectory();
        assertTrue( path.exists() );
        BasicFileAttributes attr = Attributes.readBasicFileAttributes( path );
        assertTrue( attr.isDirectory() );
        assertFalse( attr.isRegularFile() );
        FileTime lastModifiedTime = attr.lastModifiedTime();
        assertNotNull( lastModifiedTime );
        assertFalse( lastModifiedTime.toMillis() == 0 );
        assertEquals( 0, attr.size() );
        assertNotSupported( attr );
    }

    @Test
    public void testLocalDirectory() throws IOException {
        Path path = Paths.get( "images" );
        assertTrue( path.exists() );
        BasicFileAttributes attr = Attributes.readBasicFileAttributes( path );
        assertTrue( attr.isDirectory() );
        assertFalse( attr.isRegularFile() );
        FileTime lastModifiedTime = attr.lastModifiedTime();
        assertNotNull( lastModifiedTime );
        assertFalse( lastModifiedTime.toMillis() == 0 );
        assertEquals( 0, attr.size() );
        assertNotSupported( attr );
    }
    
    @Test
    public void testGaeFile() throws IOException {
        // TODO: this will fail until GaePath.copyTo() is supported
        Path path = Paths.get( "images/small.jpg" ).copyTo( Paths.get( "images/test.jpg" ) );
        assertTrue( path.exists() );
        BasicFileAttributes attr = Attributes.readBasicFileAttributes( path );
        assertFalse( attr.isDirectory() );
        assertTrue( attr.isRegularFile() );
        FileTime lastModifiedTime = attr.lastModifiedTime();
        assertNotNull( lastModifiedTime );
        assertFalse( lastModifiedTime.toMillis() == 0 );
        assertEquals( 28359, attr.size() );
        assertNotSupported( attr );
    }

    @Test
    public void testLocalFile() throws IOException {
        Path path = Paths.get( "images/small.jpg" );
        assertTrue( path.exists() );
        BasicFileAttributes attr = Attributes.readBasicFileAttributes( path );
        assertFalse( attr.isDirectory() );
        assertTrue( attr.isRegularFile() );
        FileTime lastModifiedTime = attr.lastModifiedTime();
        assertNotNull( lastModifiedTime );
        assertFalse( lastModifiedTime.toMillis() == 0 );
        assertEquals( 28359, attr.size() );
        assertNotSupported( attr );
    }
    
    @Test
    public void testNonExistingPath() throws IOException {
        Path path = Paths.get( "doesNotExist" );
        assertFalse( path.exists() );
        BasicFileAttributes attr = Attributes.readBasicFileAttributes( path );
        assertFalse( attr.isDirectory() );
        assertFalse( attr.isRegularFile() );
        assertNull( attr.lastModifiedTime() );
        assertEquals( 0, attr.size() );
        assertNotSupported( attr );
    }
    
    private static void assertNotSupported( BasicFileAttributes attr ) {
        assertNull( attr.creationTime() );
        assertNull( attr.fileKey() );
        assertFalse( attr.isOther() );
        assertFalse( attr.isSymbolicLink() );
        assertNull( attr.lastAccessTime() );
    }
    
    @Test
    public void testSetTimes() throws IOException {
        Path path = Paths.get( "test.txt" ).createFile();
        long currentTime = System.currentTimeMillis();
        Attributes.setLastModifiedTime( path, FileTime.fromMillis( currentTime ) );
        assertEquals( currentTime, Attributes.readBasicFileAttributes( path ).lastModifiedTime().toMillis() );
        Attributes.setLastAccessTime( path, FileTime.fromMillis( currentTime ) ); // does nothing
    }
    
    @Test
    public void testAttributesClass() throws IOException {
        Path path = Paths.get( "test.txt" ).createFile();
        
        try {
            @SuppressWarnings("unused")
            List<AclEntry> aclList = Attributes.getAcl( path );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            Attributes.setAcl( path, new ArrayList<AclEntry>() );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            @SuppressWarnings("unused")
            UserPrincipal owner = Attributes.getOwner( path );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            Attributes.setOwner( path, null );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            @SuppressWarnings("unused")
            DosFileAttributes dosAttrs = Attributes.readDosFileAttributes( path );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            @SuppressWarnings("unused")
            PosixFileAttributes posixAttrs = Attributes.readPosixFileAttributes( path );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        try {
            Attributes.setPosixFilePermissions( path, new HashSet<PosixFilePermission>() );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        // this one should actually work, needs GaePath.getFileStore() to be implemented
        FileStoreSpaceAttributes fsAttrs = Attributes.readFileStoreSpaceAttributes( path.getFileStore() );
    }
}
