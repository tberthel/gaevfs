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

import static com.newatlanta.appengine.nio.attribute.GaeFileAttributes.withBlockSize;
import static com.newatlanta.nio.file.attribute.Attributes.readBasicFileAttributes;
import static com.newatlanta.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static com.newatlanta.nio.file.attribute.PosixFilePermissions.fromString;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.appengine.nio.attribute.GaeFileAttributeView;
import com.newatlanta.nio.channels.SeekableByteChannel;
import com.newatlanta.nio.file.AccessDeniedException;
import com.newatlanta.nio.file.AccessMode;
import com.newatlanta.nio.file.ClosedDirectoryStreamException;
import com.newatlanta.nio.file.DirectoryNotEmptyException;
import com.newatlanta.nio.file.DirectoryStream;
import com.newatlanta.nio.file.FileAlreadyExistsException;
import com.newatlanta.nio.file.Files;
import com.newatlanta.nio.file.NoSuchFileException;
import com.newatlanta.nio.file.OpenOption;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.Paths;
import com.newatlanta.nio.file.StandardOpenOption;
import com.newatlanta.nio.file.attribute.AclFileAttributeView;
import com.newatlanta.nio.file.attribute.BasicFileAttributeView;
import com.newatlanta.nio.file.attribute.DosFileAttributeView;
import com.newatlanta.nio.file.attribute.FileAttributeView;
import com.newatlanta.nio.file.attribute.FileOwnerAttributeView;
import com.newatlanta.nio.file.attribute.FileTime;
import com.newatlanta.nio.file.attribute.PosixFileAttributeView;
import com.newatlanta.nio.file.attribute.UserDefinedFileAttributeView;

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
        assertNotNull( Paths.get( "/" ).getFileSystem() );
        assertNotNull( Paths.get( "docs/small.txt" ).getFileSystem() );
    }

    @Test
    public void testIsAbsolute() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetRoot() {
        Path rootPath = Paths.get( "/" );
        assertFalse( rootPath.isAbsolute() );
        Path rootRootPath = rootPath.getRoot();
        assertTrue( rootRootPath.isAbsolute() );
        
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
    public void testDelete() throws IOException {
        // delete non-existing directory
        Path dirPath = Paths.get( "foo" );
        assertTrue( dirPath.notExists() );
        try {
            dirPath.delete();
        } catch ( NoSuchFileException e ) {
            assertEquals( dirPath.toString(), e.getFile() );
        }
        dirPath.deleteIfExists();
        
        try {
            // attempt to delete local directory
            getLocalDirectory().delete();
            fail( "expected DirectoryNotEmptyException" );
        } catch ( DirectoryNotEmptyException e ) {
        }
        
        // attempt to delete local file
        //getLocalFile().delete();
        
        // create gae directories and files to test
        dirPath = Paths.get( "/foo/bar" );
        Files.createDirectories( dirPath );
        assertTrue( dirPath.exists() );
        Path parent = dirPath.getParent();
        assertTrue( parent.exists() );
        Path file = dirPath.resolve( "baz.txt" );
        assertTrue( file.createFile().exists() );
        assertTrue( file.getParent().isSameFile( dirPath ) );
        
        // attempt to delete non-empty gae directories
        try {
            dirPath.delete();
            fail( "expected DirectoryNotEmptyException" );
        } catch ( DirectoryNotEmptyException e ) {
            assertTrue( dirPath.exists() );
        }
        
        try {
            parent.delete();
            fail( "expected DirectoryNotEmptyException" );
        } catch ( DirectoryNotEmptyException e ) {
            assertTrue( parent.exists() );
        }
        
        // delete gae file
        file.delete();
        assertTrue( file.notExists() );
        
        // delete empty gae directories
        dirPath.delete();
        assertTrue( dirPath.notExists() );
        parent.delete();
        assertTrue( parent.notExists() );
    }

    @Test
    public void testCreateSymbolicLink() throws IOException {
        try {
            Paths.get( "foo" ).createSymbolicLink( Paths.get( "bar" ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testCreateLink() throws IOException {
        try {
            Paths.get( "foo" ).createLink( Paths.get( "bar" ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testReadSymbolicLink() throws IOException {
        try {
            Paths.get( "foo" ).readSymbolicLink();
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
    public void testNewDirectoryStream() throws IOException {
        Path dirPath = Paths.get( "/" );
        assertTrue( dirPath.exists() );
        DirectoryStream<Path> dirStream = dirPath.newDirectoryStream();
        assertNotNull( dirStream );
        try {
            for ( Path child : dirStream ) {
                assertTrue( child.exists() );
                // TODO: if child is a local file and dirPath is a GaeVFS directory,
                // then the following assertion fails
                //assertTrue( child.getParent().isSameFile( dirPath ) );
                
                // TODO: make sure Paths are correct
            }
            
            try {
                // can't get iterator more than once
                dirStream.iterator();
                fail( "expected IllegalStateException" );
            } catch ( IllegalStateException e ) {
            }
        } finally {
            dirStream.close();
        }
        
        // TODO: this fails, probably due to Commons VFS caching of children
        dirStream = dirPath.newDirectoryStream();
        Iterator<Path> pathIter = dirStream.iterator();
        while ( pathIter.hasNext() ) {
            pathIter.next();
        }
        try {
            pathIter.next();
            fail( "expected NoSuchElementException" );
        } catch ( NoSuchElementException e ) {
        }
        
        dirStream.close();
        try {
            pathIter.hasNext();
            fail( "expected ClosedDirectoryStreamException" );
        } catch ( ClosedDirectoryStreamException e ) {
        }
        try {
            pathIter.next();
            fail( "expected ClosedDirectoryStreamException" );
        } catch ( ClosedDirectoryStreamException e ) {
        }
        try {
            pathIter.remove();
            fail( "expected ClosedDirectoryStreamException" );
        } catch ( ClosedDirectoryStreamException e ) {
        }
        
        // TODO: test pathIter.remove()
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewDirectoryStreamString() {
        fail( "Not yet implemented" );
    }
    
    @Test
    public void testNewDirectoryStreamFilterOfQsuperPath() {
        fail( "Not yet implemented" );
    }
    
    @Test
    public void testNewDirectoryStreamFilterOfQsuperPath1() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCreateWithNonExistingParent() throws IOException {
        // attempt to create a directory with a non-existing parent
        Path dirPath = Paths.get( "foo/bar" );
        assertTrue( dirPath.notExists() );
        assertTrue( dirPath.getParent().notExists() );
        try {
            dirPath.createDirectory();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // attempt to create a file with a non-existing parent
        Path filePath = dirPath.resolve( "test.txt" );
        assertTrue( filePath.notExists() );
        assertTrue( filePath.getParent().notExists() );
        try {
            filePath.createFile();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
    }
    
    @Test
    public void testCreateFromExistingLocalFile() throws IOException {
        Path localFile = getLocalFile();
        try {
            // attempt to create a directory from an existing local file
            localFile.createDirectory();
            fail( "expected IOException" );
        } catch ( IOException e ) {
        }
        try {
            // attempt to create a file from an existing local file
            localFile.createFile();
            fail( "expected IOException" );
        } catch ( IOException e ) {
        }
    }
    
    @Test
    public void testCreateFromExistingLocalDirectory() throws IOException {
        Path localDir = getLocalDirectory();
        try {
            // attempt to create a directory from an existing local directory
            localDir.createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        try {
            // attempt to create a file from an existing local directory
            localDir.createFile();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
    }
    
    @Test
    public void testCreateWithNonDirectoryLocalParent() throws IOException {
        Path localFile = getLocalFile();
        Path dirPath = localFile.resolve( "foo" );
        assertTrue( dirPath.notExists() );
        assertTrue( dirPath.getParent().isSameFile( localFile ) );
        
        try {
            // attempt to create a directory with a non-directory local parent
            dirPath.createDirectory();
            fail( "expected IOException" );
        } catch ( IOException e ) {
        }
        
        try {
            // attempt to create a file with a non-directory local parent
            dirPath.createFile();
            fail( "expected IOException" );
        } catch ( IOException e ) {
        }
    }
    
    private Path getLocalDirectory() throws IOException {
        Path imagesPath = Paths.get( "images" );
        assertTrue( imagesPath.exists() );
        assertTrue( readBasicFileAttributes( imagesPath ).isDirectory() );
        return imagesPath;
    }

    private Path getLocalFile() throws IOException {
        Path filePath = Paths.get( "images/large.jpg" );
        assertTrue( filePath.exists() );
        assertTrue( readBasicFileAttributes( filePath ).isRegularFile() );
        try {
            // junit testing doesn't enforce read-only access to the local file
            // system, so set the read-only permission manually for this file
            filePath.checkAccess( AccessMode.WRITE );
            fail( "expected AccessDeniedException" );
        } catch ( AccessDeniedException e ) {
        }
        return filePath;
    }
    
    @Test
    public void testCreateSpecifyingBlockSize() throws IOException {
        // specify blockSize attribute when creating a file
        Path filePath = Paths.get( "/images/test.png" );
        assertTrue( filePath.notExists() );
        assertTrue( filePath.createFile( withBlockSize( 8 ) ).exists() );
        assertIntegerAttr( filePath.getAttribute( "gae:blockSize" ), 8 * 1024 );
        assertBooleanAttr( filePath.getAttribute( "isRegularFile" ), true );
    }
    
    @Test
    public void testCreateDirectoriesAndFiles() throws IOException {
        try {
            // root directory should already exist
            Paths.get( "/" ).createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        Path imagesPath = getLocalDirectory();
        
        // create a directory within a local directory
        Path dirPath = assertCreateDirectory( imagesPath.resolve( "foo" ), imagesPath );
        
        // create a file within a local directory
        Path filePath = assertCreateFile( imagesPath.resolve( "test.jpg" ), imagesPath );
        
        // create a directory within a local directory
        dirPath = assertCreateDirectory( Paths.get( "images/bar" ), imagesPath );
        
        // create a file within a local directory
        filePath = assertCreateFile( Paths.get( "images/test.gif" ), imagesPath );
        
        // create a directory within a GaeVFS directory
        dirPath = assertCreateDirectory( Paths.get( "/images/bar/baz" ) );
        
        // create a file within a GaeVFS directory
        filePath = assertCreateFile( Paths.get( "/images/bar/test.gif" ) );
        
        try {
            // attempt to create a GaeVFS directory that already exists
            assertTrue( dirPath.exists() );
            dirPath.createDirectory();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        try {
            // attempt to create a GaeVFS file that already exists
            assertTrue( filePath.exists() );
            filePath.createFile();
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // attempt to create a directory with a non-directory GaeVFS parent
        dirPath = filePath.resolve( "baz" );
        assertTrue( dirPath.notExists() );
        assertTrue( dirPath.getParent().isSameFile( filePath ) );
        try {
            dirPath.createDirectory();
            fail( "expected IOExeption" );
        } catch ( IOException e ) {
        }
        
        // attempt to create a file with a non-directory GaeVFS parent
        filePath = filePath.resolve( "test.gif" );
        assertTrue( filePath.notExists() );
        try {
            filePath.createFile();
            fail( "expected IOExeption" );
        } catch ( IOException e ) {
        }
        
        // attempt to specify attributes when creating a directory
        dirPath = Paths.get( "images/fubar" );
        assertTrue( dirPath.notExists() );
        try {
            dirPath.createDirectory( asFileAttribute( fromString( "rwxr-x---" ) ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }

        // attempt to specify invalid attributes when creating a file
        filePath = Paths.get( "images/fubar.png" );
        assertTrue( filePath.notExists() );
        try {
            filePath.createFile( asFileAttribute( fromString( "rwxr-x---" ) ) );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    private Path assertCreateFile( Path file ) throws IOException {
        return assertCreateFile( file, null );
    }
    
    private Path assertCreateFile( Path file, Path parent ) throws IOException {
        assertTrue( file.notExists() );
        assertTrue( file.createFile().exists() );
        assertTrue( readBasicFileAttributes( file ).isRegularFile() );
        if ( parent != null ) {
            assertTrue( file.getParent().isSameFile( parent ) );
        }
        return file;
    }

    private Path assertCreateDirectory( Path dir ) throws IOException {
        return assertCreateDirectory( dir, null );
    }
    
    private Path assertCreateDirectory( Path dir, Path parent ) throws IOException {
        assertTrue( dir.notExists() );
        assertTrue( dir.createDirectory().exists() );
        assertTrue( readBasicFileAttributes( dir ).isDirectory() );
        if ( parent != null ) {
            assertTrue( dir.getParent().isSameFile( parent ) );
        }
        return dir;
    }

    @Test
    public void testNewByteChannel() throws IOException {
        // attempt to get a ByteChannel on a file that doesn't exist
        Path newTxt = Paths.get( "docs/new.txt" );
        assertTrue( newTxt.notExists() );
        try {
            newTxt.newByteChannel();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // unsupported options
        assertUnsupportedNewByteChannelOption( newTxt, SYNC );
        assertUnsupportedNewByteChannelOption( newTxt, DSYNC );
        assertUnsupportedNewByteChannelOption( newTxt, DELETE_ON_CLOSE );
        assertUnsupportedNewByteChannelOption( newTxt, WRITE, CREATE, SPARSE );
        
        // illegal options
        try {
            newTxt.newByteChannel( APPEND, READ );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        try {
            newTxt.newByteChannel( APPEND, TRUNCATE_EXISTING );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        // ignored options if not writing
        try {
            newTxt.newByteChannel( CREATE );
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        try {
            newTxt.newByteChannel( CREATE_NEW );
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        try {
            newTxt.newByteChannel( SPARSE );
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // CREATE_NEW option on non-existing file
        assertNotNull( newTxt.newByteChannel( WRITE, CREATE_NEW ) );
        assertTrue( newTxt.exists() );
        try {
            // CREATE_NEW option on existing file
            newTxt.newByteChannel( APPEND, CREATE_NEW ); // APPEND implies WRITE
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // CREATE option on non-existing file, with attributes
        newTxt.delete();
        assertTrue( newTxt.notExists() );
        SeekableByteChannel fc = newTxt.newByteChannel( EnumSet.of( WRITE, CREATE ),
                                                            withBlockSize( 256 ) );
        assertNotNull( fc );
        assertTrue( newTxt.exists() );
        
        // CREATE option on existing file
        fc.close();
        fc = newTxt.newByteChannel( APPEND, CREATE ); // APPEND implies WRITE
        assertNotNull( fc );
        assertTrue( newTxt.exists() );
        
        // APPEND option
        fc.close();
        fc = newTxt.newByteChannel( APPEND ); // APPEND implies WRITE
        assertNotNull( fc );
        
        // TRUNCATE_EXISTING option
        fc.close();
        fc = newTxt.newByteChannel( WRITE, TRUNCATE_EXISTING );
        assertNotNull( fc );
        
        // default options (read)
        fc.close();
        fc = newTxt.newByteChannel();
        assertNotNull( fc );
        fc.close();
        newTxt.delete();
    }

    private void assertUnsupportedNewByteChannelOption( Path newTxt, OpenOption ... options )
            throws IOException {
        try {
            newTxt.newByteChannel( options );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }

    @Test
    public void testNewOutputStream() throws IOException {
        // attempt to get an OutputStream on a file that doesn't exist
        Path newTxt = Paths.get( "docs/new.txt" );
        assertTrue( newTxt.notExists() );
        try {
            newTxt.newOutputStream();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // CREATE_NEW option on non-existing file
        assertNotNull( newTxt.newOutputStream( CREATE_NEW ) );
        assertTrue( newTxt.exists() );
        try {
            // CREATE_NEW option on existing file
            newTxt.newOutputStream( CREATE_NEW );
            fail( "expected FileAlreadyExistsException" );
        } catch ( FileAlreadyExistsException e ) {
        }
        
        // CREATE option on non-existing file
        newTxt.delete();
        assertTrue( newTxt.notExists() );
        OutputStream out = newTxt.newOutputStream( CREATE );
        assertNotNull( out );
        assertTrue( newTxt.exists() );
        
        // CREATE option on existing file
        out.close();
        out = newTxt.newOutputStream( CREATE );
        assertNotNull( out );
        assertTrue( newTxt.exists() );
        
        // APPEND option
        out.close();
        out = newTxt.newOutputStream( APPEND );
        assertNotNull( out );
        
        // default options
        out.close();
        out = newTxt.newOutputStream();
        assertNotNull( out );
        out.close();
        
        // unsupported and illegal options
        Set<OpenOption> optionSet = new HashSet<OpenOption>();
        optionSet.add( WRITE );
        optionSet.add( CREATE );
        optionSet.add( CREATE_NEW );
        optionSet.add( APPEND );
        
        for ( StandardOpenOption option: StandardOpenOption.values() ) {
            try {
                out = newTxt.newOutputStream( option );
                assertTrue( optionSet.contains( option ) );
                out.close();
            } catch ( FileAlreadyExistsException e ) {
                assertTrue( option == CREATE_NEW );
            } catch ( UnsupportedOperationException e ) {
                assertFalse( optionSet.contains( option ) );
            } catch ( IllegalArgumentException e ) {
                assertFalse( optionSet.contains( option ) );
            }
        }
    }
    
    @Test
    public void testNewInputStream() throws IOException {
        // attempt to get an InputStream on a file that doesn't exist
        Path path = Paths.get( "doesNotExist.txt" );
        assertTrue( path.notExists() );
        try {
            path.newInputStream();
            fail( "expected NoSuchFileException" );
        } catch ( NoSuchFileException e ) {
        }
        
        // attempt to get an InputStream on a directory
        path = Paths.get( "docs" );
        assertTrue( path.exists() );
        assertTrue( readBasicFileAttributes( path ).isDirectory() );
        try {
            path.newInputStream();
            fail( "expected IOException" );
        } catch ( IOException e ) {
        }
        
        // local file
        path = Paths.get( "/docs/small.txt" );
        assertTrue( path.exists() );
        assertTrue( readBasicFileAttributes( path ).isRegularFile() );
        
        // test open options
        for ( StandardOpenOption option: StandardOpenOption.values() ) {
            try {
                path.newInputStream( option );
                assertTrue( option == StandardOpenOption.READ );
            } catch ( UnsupportedOperationException e ) {
                assertTrue( option != StandardOpenOption.READ );
            }
        }
        
        // default open options
        InputStream in = path.newInputStream();
        int inSize = (int)readBasicFileAttributes( path ).size();
        byte[] inBytes = new byte[ inSize ];
        assertEquals( inSize, in.read( inBytes ) );
        assertEquals( 0, in.available() );
        in.close();
        
        Path newTxt = Paths.get( "docs/new.txt" );
        assertCreateFile( newTxt );
        
        OutputStream out = newTxt.newOutputStream();
        out.write( inBytes );
        out.close();
        
        int newSize = (int)readBasicFileAttributes( newTxt ).size();
        assertEquals( inSize, newSize );
        
        // gae file
        in = newTxt.newInputStream();
        byte[] newBytes = new byte[ newSize ];
        assertEquals( newSize, in.read( newBytes ) );
        assertEquals( 0, in.available() );
        in.close();
        
        assertTrue( Arrays.equals( inBytes, newBytes ) );
    }

    @Test
    public void testIsHidden() throws IOException {
        Path rootPath = Paths.get( "/" );
        assertTrue( rootPath.exists() );
        assertFalse( rootPath.isHidden() );
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
        // JUnit test environment does not enforce read-only access to local files,
        // so set "read-only" file system permission in order for this test to pass
        try {
            filePath.checkAccess( AccessMode.WRITE );
            fail( "expected AccessDeniedException" );
        } catch ( AccessDeniedException e ) {
        }
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
    public void testRegisterWatchServiceKindOfQArrayModifierArray() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testRegisterWatchServiceKindOfQArray() {
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

    @Test
    public void testGetAttribute() throws IOException {
        Path path = Paths.get( "/attributeTest" ).createDirectory();
        assertTrue( path.exists() );
        
        // basic attributes without specifying view name
        assertFileTimeAttr( path.getAttribute( "lastModifiedTime" ) );
        assertLongAttr( path.getAttribute( "size" ), 0 );
        assertBooleanAttr( path.getAttribute( "isRegularFile" ), false );
        assertBooleanAttr( path.getAttribute( "isDirectory" ), true );
        assertBooleanAttr( path.getAttribute( "isSymbolicLink" ), false );
        assertBooleanAttr( path.getAttribute( "isOther" ), false );
        
        // basic attributes specifying view name
        assertFileTimeAttr( path.getAttribute( "basic:lastModifiedTime" ) );
        assertLongAttr( path.getAttribute( "basic:size" ), 0 );        
        assertBooleanAttr( path.getAttribute( "basic:isRegularFile" ), false );
        assertBooleanAttr( path.getAttribute( "basic:isDirectory" ), true );
        assertBooleanAttr( path.getAttribute( "basic:isSymbolicLink" ), false );
        assertBooleanAttr( path.getAttribute( "basic:isOther" ), false );
        
        // gae attribute without specifying view name
        assertNull( path.getAttribute( "blockSize" ) );
        
        // gae attributes specifying view name
        assertIntegerAttr( path.getAttribute( "gae:blockSize" ), 0 );
        
        // unsupported basic attributes without specifying view name
        assertNull( path.getAttribute( "lastAccessTime" ) );
        assertNull( path.getAttribute( "creationTime" ) );
        assertNull( path.getAttribute( "fileKey" ) );
        
        // unsupported basic attribute specifying view name
        assertNull( path.getAttribute( "basic:lastAccessTime" ) );
        assertNull( path.getAttribute( "basic:creationTime" ) );
        assertNull( path.getAttribute( "basic:fileKey" ) );
        
        // unsupported views
        assertNull( path.getAttribute( "dos:archive" ) );
        assertNull( path.getAttribute( "posix:group" ) );
        assertNull( path.getAttribute( "acl:acl" ) );
        assertNull( path.getAttribute( "unix:uid" ) );
        assertNull( path.getAttribute( "user:test" ) );
    }

    @Test
    public void testGetFileAttributeView() {
        Path rootPath = Paths.get( "/" );
        FileAttributeView attr = rootPath.getFileAttributeView( BasicFileAttributeView.class );
        assertNotNull( attr );
        assertEquals( "basic", attr.name() );
        attr = rootPath.getFileAttributeView( GaeFileAttributeView.class );
        assertNotNull( attr );
        assertEquals( "gae", attr.name() );
        assertNull( rootPath.getFileAttributeView( AclFileAttributeView.class ) );
        assertNull( rootPath.getFileAttributeView( DosFileAttributeView.class ) );
        assertNull( rootPath.getFileAttributeView( FileOwnerAttributeView.class ) );
        assertNull( rootPath.getFileAttributeView( PosixFileAttributeView.class ) );
        assertNull( rootPath.getFileAttributeView( UserDefinedFileAttributeView.class ) ); 
    }

    @Test
    public void testReadAttributes() throws IOException {
        Path path = Paths.get( "/attributeTest" ).createDirectory();
        assertTrue( path.exists() );
        
        // all basic attributes without specifying view name
        Map<String, ?> attrMap = path.readAttributes( "*" );
        assertDirBasicAttributes( attrMap, 6 );
        
        // specified basic attributes without view name
        attrMap = path.readAttributes( "size,isDirectory,lastAccessTime,creationTime" );
        assertNotEmptyMap( attrMap, 2 );
        assertLongAttr( attrMap.get( "size" ), 0 );
        assertBooleanAttr( attrMap.get( "isDirectory" ), true );
        assertNull( attrMap.get( "lastAccessTime" ) );
        assertNull( attrMap.get( "creationTime" ) );
        
        // all basic attributes specifying view name
        attrMap = path.readAttributes( "basic:*" );
        assertDirBasicAttributes( attrMap, 6 );
        
        // specified basic attributes with view name
        attrMap = path.readAttributes( "basic:lastModifiedTime,isRegularFile,isSymbolicLink,fileKey" );
        assertNotEmptyMap( attrMap, 3 );
        assertFileTimeAttr( attrMap.get( "lastModifiedTime" ) );
        assertBooleanAttr( attrMap.get( "isRegularFile" ), false );
        assertBooleanAttr( attrMap.get( "isSymbolicLink" ), false );
        assertNull( attrMap.get( "fileKey" ) );
        
        // gae attributes without view name
        assertEmptyMap( path.readAttributes( "blockSize" ) );
        
        // all gae attributes with view name
        attrMap = path.readAttributes( "gae:*" );
        assertDirBasicAttributes( attrMap, 7 );
        assertIntegerAttr( attrMap.get( "blockSize" ), 0 );
        
        // specified gae attribute with view name
        attrMap = path.readAttributes( "gae:blockSize" );
        assertNotEmptyMap( attrMap, 1 );
        assertIntegerAttr( attrMap.get( "blockSize" ), 0 );
        
        // unsupported views
        assertEmptyMap( path.readAttributes( "dos:*" ) );
        assertEmptyMap( path.readAttributes( "posix:*" ) );
        assertEmptyMap( path.readAttributes( "user:*" ) );
    }
    
    private static void assertEmptyMap( Map<String, ?> attrMap ) {
        assertNotNull( attrMap );
        assertTrue( attrMap.isEmpty() );
    }
    
    private static void assertNotEmptyMap( Map<String, ?> attrMap, int size ) {
        assertNotNull( attrMap );
        assertFalse( attrMap.isEmpty() );
        assertEquals( size, attrMap.size() );
    }
    
    private static void assertDirBasicAttributes( Map<String, ?> attrMap, int size ) {
        assertNotNull( attrMap );
        assertFalse( attrMap.isEmpty() );
        assertEquals( size, attrMap.size() );
        
        assertFileTimeAttr( attrMap.get( "lastModifiedTime" ) );
        assertLongAttr( attrMap.get( "size" ), 0 );
        assertBooleanAttr( attrMap.get( "isRegularFile" ), false );
        assertBooleanAttr( attrMap.get( "isDirectory" ), true ); 
    }
    
    private static void assertFileTimeAttr( Object attr ) {
        assertNotNull( attr );
        assertTrue( attr instanceof FileTime );
        assertFalse( ((FileTime)attr).toMillis() == 0 );
    }
    
    private static void assertLongAttr( Object attr, long value ) {
        assertNotNull( attr );
        assertTrue( attr instanceof Long );
        assertEquals( value, ((Long)attr).longValue() );
    }
    
    private static void assertIntegerAttr( Object attr, int value ) {
        assertNotNull( attr );
        assertTrue( attr instanceof Integer );
        assertEquals( value, ((Integer)attr).intValue() );
    }
    
    private static void assertBooleanAttr( Object attr, boolean value ) {
        assertNotNull( attr );
        assertTrue( attr instanceof Boolean );
        assertEquals( value, ((Boolean)attr).booleanValue() );
    }

    @Test
    public void testSetAttribute() throws IOException {
        Path path = Paths.get( "test.txt" );
        assertTrue( path.notExists() );
        
        // supported attributes
        path.setAttribute( "gae:blockSize", 64 );
        
        // unsupported basic attributes without view
        assertUnsupportedSetAttribute( path, "lastModifiedTime" );
        assertUnsupportedSetAttribute( path, "size" );
        assertUnsupportedSetAttribute( path, "isRegularFile" );
        assertUnsupportedSetAttribute( path, "isDirectory" );
        assertUnsupportedSetAttribute( path, "lastAccessTime" );
        assertUnsupportedSetAttribute( path, "creationTime" );
        assertUnsupportedSetAttribute( path, "isSymbolicLink" );
        assertUnsupportedSetAttribute( path, "isOther" );
        assertUnsupportedSetAttribute( path, "fileKey" );
        
        // unsupported basic attributes with view
        assertUnsupportedSetAttribute( path, "basic:lastModifiedTime" );
        assertUnsupportedSetAttribute( path, "basic:size" );
        assertUnsupportedSetAttribute( path, "basic:isRegularFile" );
        assertUnsupportedSetAttribute( path, "basic:isDirectory" );
        assertUnsupportedSetAttribute( path, "basic:lastAccessTime" );
        assertUnsupportedSetAttribute( path, "basic:creationTime" );
        assertUnsupportedSetAttribute( path, "basic:isSymbolicLink" );
        assertUnsupportedSetAttribute( path, "basic:isOther" );
        assertUnsupportedSetAttribute( path, "basic:fileKey" );
    }
    
    private static void assertUnsupportedSetAttribute( Path path, String attribute )
            throws IOException {
        try {
            path.setAttribute( attribute, null );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
    }
}
