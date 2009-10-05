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
package com.newatlanta.h2.store.fs;

import static com.newatlanta.nio.file.AccessMode.WRITE;
import static com.newatlanta.nio.file.Files.createDirectories;
import static com.newatlanta.nio.file.Files.walkFileTree;
import static com.newatlanta.nio.file.Paths.get;
import static com.newatlanta.nio.file.StandardOpenOption.APPEND;
import static com.newatlanta.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.h2.message.Message;
import org.h2.store.fs.FileObject;
import org.h2.store.fs.FileSystem;

import com.newatlanta.nio.file.FileAlreadyExistsException;
import com.newatlanta.nio.file.FileVisitResult;
import com.newatlanta.nio.file.NoSuchFileException;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.SimpleFileVisitor;
import com.newatlanta.nio.file.attribute.Attributes;
import com.newatlanta.nio.file.attribute.BasicFileAttributes;

public class FileSystemGae extends FileSystem {

    private static final FileSystemGae INSTANCE = new FileSystemGae();

    public static FileSystemGae getInstance() {
        return INSTANCE;
    }
    
    /**
     * Check if the file system is responsible for this file name.
     *
     * @param fileName the file name
     * @return true if it is
     */
    @Override
    protected boolean accepts( String fileName ) {
        String scheme = get( fileName ).toUri().getScheme();
        return ( ( scheme != null ) && scheme.equals( "gae" ) );
    }

    @Override
    public boolean canWrite( String fileName ) {
        try {
            get( fileName ).checkAccess( WRITE );
            return true;
        } catch ( IOException e ) {
            return false;
        }
    }

    @Override
    public void copy( String original, String copy ) throws SQLException {
        try {
            get( original ).copyTo( get( copy ) );
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    /**
     * Create all required directories that are required for this file.
     *
     * @param fileName the file name (not directory name)
     */
    @Override
    public void createDirs( String fileName ) throws SQLException {
        try {
            createDirs( get( fileName ) );
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }
    
    private void createDirs( Path filePath ) throws IOException {
        Path parent = filePath.getParent();
        if ( parent.notExists() ) {
            createDirectories( parent );
        }
    }

    @Override
    public boolean createNewFile( String fileName ) throws SQLException {
        try {
            return get( fileName ).createFile().exists();
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    /**
     * Create a new temporary file.
     *
     * @param prefix the prefix of the file name (including directory name if
     *            required)
     * @param suffix the suffix
     * @param deleteOnExit if the file should be deleted when the virtual
     *            machine exists
     * @param inTempDir if the file should be stored in the temporary directory
     * @return the name of the created file
     */
    private static Random random = new Random();

    @Override
    public String createTempFile( String prefix, String suffix, boolean deleteOnExit,
                                    boolean inTempDir ) throws IOException {
        // TODO this method needs to be tested
        Path tempDir = inTempDir ? get( "WEB-INF/temp" ) : get( prefix ).getParent();
        if ( tempDir.notExists() ) {
            createDirectories( tempDir );
        }
        while ( true ) {
            String fileName = prefix + Integer.toString( random.nextInt() & 0xffffff )
                                        + ( suffix == null ? ".tmp" : suffix );
            try {
                return tempDir.resolve( fileName ).createFile().toUri().toString();
            } catch ( FileAlreadyExistsException e ) { // repeat loop
            }
        }
    }

    @Override
    public void delete( String fileName ) throws SQLException {
        try {
            get( fileName ).deleteIfExists();
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    @Override
    public void deleteRecursive( String directory, final boolean tryOnly )
            throws SQLException {
        // http://download.java.net/jdk7/docs/api/java/nio/file/FileVisitor.html
        // TODO this implementation needs to be examined carefully
        walkFileTree( get( directory ), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
                try {
                    file.delete();
                } catch ( IOException e ) {
                    if ( !tryOnly ) {
                        // TODO is this correct?
                        return FileVisitResult.TERMINATE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException e ) {
                if ( e == null ) {
                    try {
                        dir.delete();
                    } catch ( IOException exc ) {
                        // TODO failed to delete, do error handling here
                    }
                } else {
                    // TODO directory iteration failed
                }
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    @Override
    public boolean exists( String fileName ) {
        return get( fileName ).exists();
    }

    @Override
    public boolean fileStartsWith( String fileName, String prefix ) {
        return fileName.startsWith( prefix );
    }

    @Override
    public String getAbsolutePath( String fileName ) {
        return get( fileName ).toAbsolutePath().toString();
    }

    /**
     * Get the file name (without directory part).
     *
     * @param name the directory and file name
     * @return just the file name
     */
    @Override
    public String getFileName( String name ) throws SQLException {
        return get( name ).getName().toString();
    }

    @Override
    public long getLastModified( String fileName ) {
        try {
            return readBasicFileAttributes( fileName ).lastModifiedTime().toMillis();
        } catch ( IOException e ) {
            return 0;
        }
    }
    
    private static BasicFileAttributes readBasicFileAttributes( String fileName )
            throws IOException {
        return Attributes.readBasicFileAttributes( get( fileName ) );
    }

    @Override
    public String getParent( String fileName ) {
        return get( fileName ).getParent().toUri().toString();
    }

    @Override
    public boolean isAbsolute( String fileName ) {
        return get( fileName ).isAbsolute();
    }

    @Override
    public boolean isDirectory( String fileName ) {
        try {
            return readBasicFileAttributes( fileName ).isDirectory();
        } catch ( IOException e ) {
            return false;
        }
    }

    @Override
    public boolean isReadOnly( String fileName ) {
        return !canWrite( fileName );
    }

    @Override
    public long length( String fileName ) {
        try {
            return readBasicFileAttributes( fileName ).size();
        } catch ( IOException e ) {
            return 0;
        }
    }

    @Override
    public String[] listFiles( String directory ) throws SQLException {
        try {
            List<String> files = new ArrayList<String>();
            for ( Path path : get( directory ).newDirectoryStream() ) {
                files.add( path.toUri().toString() );
            }
            return files.toArray( new String[ files.size() ] );
        } catch ( NoSuchFileException e ) {
            return new String[ 0 ];
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    @Override
    public String normalize( String fileName ) throws SQLException {
        return get( fileName ).toUri().toString();
    }

    @Override
    public InputStream openFileInputStream( String fileName ) throws IOException {
        return get( fileName ).newInputStream();
    }

    /**
     * Open a random access file object.
     *
     * @param fileName the file name
     * @param mode the access mode. Supported are r, rw, rws, rwd
     * @return the file object
     */
    @Override
    public FileObject openFileObject( String fileName, String mode ) throws IOException {
        return new FileObjectGae( get( fileName ), mode );
    }

    @Override
    public OutputStream openFileOutputStream( String fileName, boolean append )
            throws SQLException {
        try {
            Path filePath = get( fileName );
            createDirs( filePath );
            return filePath.newOutputStream( CREATE, append ? APPEND : null );
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    @Override
    public void rename( String oldName, String newName ) throws SQLException {
        try {
            Path oldFile = get( oldName );
            Path newFile = get( newName );
            if ( oldFile.isSameFile( newFile ) ) {
                Message.throwInternalError( "rename file old=new" );
            }
            oldFile.moveTo( newFile );
        } catch ( IOException e ) {
            throw new SQLException( e );
        }
    }

    @Override
    public boolean tryDelete( String fileName ) {
        Path path = get( fileName );
        try {
            path.deleteIfExists();
        } catch ( IOException e ) {
        }
        return path.exists();
    }
}
