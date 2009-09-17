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
import static com.newatlanta.nio.file.StandardOpenOption.CREATE_NEW;
import static com.newatlanta.nio.file.StandardOpenOption.READ;
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;
import com.newatlanta.nio.file.Paths;

public class GaeFileChannelTestCase extends GaeVfsTestCase {

    @Test
    public void testReadByteBuffer() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testReadByteBufferArrayIntInt() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testWriteByteBuffer() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testWriteByteBufferArrayIntInt() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testPosition() throws IOException {
        byte[] b = new byte[ 100 ];
        ByteBuffer buf = ByteBuffer.wrap( b );
        Arrays.fill( b, (byte)0xff );
        
        FileChannel fc = FileChannel.open( Paths.get( "docs/position.txt" ),
                        EnumSet.of( READ, WRITE, CREATE_NEW ), withBlockSize( 1 ) );
        assertEquals( 0, fc.size() );
        assertEquals( 0, fc.position() );
        // Setting the position to a value that is greater than the current size is legal...
        assertEquals( fc, fc.position( 100 ) );  
        assertEquals( 0, fc.size() ); // ...but does not change the size of the entity.
        assertEquals( 100, fc.position() );
        // A later attempt to read bytes at such a position will immediately return an
        // end-of-file indication
        assertEquals( -1, fc.read( buf ) );
        // A later attempt to write bytes at such a position will cause the entity to grow
        // to accommodate the new bytes
        assertEquals( 100, fc.write( buf ) );
        assertEquals( 200, fc.size() );
        assertEquals( 200, fc.position() );
        
        // set the position to the current position
        assertEquals( fc.position(), fc.position( fc.position() ).position() );
        
        // set the position beyond the current block
        assertEquals( 2560, fc.position( 2560 ).position() );
        assertEquals( 200, fc.size() );
        assertEquals( -1, fc.read( buf ) );
        assertEquals( 100, fc.write( buf ) );
        assertEquals( 2560 + 100, fc.size() );
        assertEquals( 2560 + 100, fc.position() );
        
        try {
            // attempt to set the position to a negative value
            fc.position( -10 );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // attempt to set the position for a closed channel
            fc.position( 100 );
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
        
        try {
            // attempt to read the position for a closed channel
            fc.position();
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
    }

    @Test
    public void testSize() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/size.txt" ),
                                                WRITE, CREATE_NEW );
        assertEquals( 0, fc.size() );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
        try {
            // attempt to read the size for a closed channel
            fc.size();
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
    }

    @Test
    public void testForce() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/force.txt" ),
                                                WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        
        fc.force( true );
        fc.force( false );
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // attempt to force a closed channel
            fc.force( true );
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
    }

    @Test
    public void testTransferTo() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testTransferFrom() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testReadByteBufferLong() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testWriteByteBufferLong() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testLock() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testTryLock() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/truncate.txt" ),
                                                EnumSet.of( WRITE, CREATE_NEW ) );
        assertTrue( fc.isOpen() );
        
        try {
            // negative position
            fc.tryLock( -1, 100, false );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        try {
            // negative size
            fc.tryLock( 0, -100, false );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        try {
            // negative position + size
            fc.tryLock( Long.MAX_VALUE, 100, false );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        try {
            // attempt to lock a region (currently unsupported)
            fc.tryLock( 1, 100, false );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {   
        }
        
        // get an exclusive lock
        FileLock fileLock = fc.tryLock();
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertFalse( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        
        try {
            // try second get on exclusive lock
            fc.tryLock();
            fail( "expected OverlappingFileLockException" );
        } catch ( OverlappingFileLockException e ) {
        }
        
        try {
            // try get on shared lock
            fc.tryLock( 0, Long.MAX_VALUE, true );
            fail( "expected OverlappingFileLockException" );
        } catch ( OverlappingFileLockException e ) {
        }
        
        fileLock.release();
        assertFalse( fileLock.isValid() );
        fileLock.release(); // release on an invalid lock does nothing
        
        // get exclusive lock on separate thread
        FileLockingThread lockThread = FileLockingThread.createThread( fc, Long.MAX_VALUE );
        assertTrue( lockThread.isAlive() );
        assertTrue( lockThread.getFileLock().isValid() );
        assertFalse( lockThread.getFileLock().isShared() );
        
        try {
            // try second get on exclusive lock
            fc.tryLock();
            fail( "expected OverlappingFileLockException" );
        } catch ( OverlappingFileLockException e ) {
        }
        
        try {
            // try get on shared lock
            fc.tryLock( 0, Long.MAX_VALUE, true );
            fail( "expected OverlappingFileLockException" );
        } catch ( OverlappingFileLockException e ) {
        }
        
        lockThread.interrupt();
        try {
            do {
                Thread.sleep( 100 ); // give lockThread a chance to run
            } while ( lockThread.isAlive() );
        } catch ( InterruptedException e ) {
        }
        assertFalse( lockThread.isAlive() );
        assertFalse( lockThread.getFileLock().isValid() );
        
        // get and release a shared lock
        fileLock = fc.tryLock( 0, Long.MAX_VALUE, true );
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertTrue( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        fileLock.release();
        assertFalse( fileLock.isValid() );
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // attempt to lock a closed channel
            fc.tryLock();
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
        
        // attempt FileLock.release() on a closed channel
        fileLock.release();
    }

    @Test
    public void testTruncate() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/truncate.txt" ),
                            EnumSet.of( WRITE, CREATE_NEW ), withBlockSize( 1 ) );
        assertTrue( fc.isOpen() );

        ByteBuffer b = ByteBuffer.allocate( 1 ).put( (byte)0xff );
        b.rewind();
        for ( int i = 0; i < 10; i++ ) { // write out 10 blocks
            fc.position( i * 1024 );
            fc.write( b );
        }
        assertEquals( ( 1024 * 9 ) + 1, fc.size() );
        
        try {
            // size < 0
            fc.truncate( -10 );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        // size < current size, position > size
        long size = 1024 * 6;
        assertTrue( ( size < fc.size() ) && ( fc.position() > size ) );
        assertEquals( size, fc.truncate( size ).size() );
        assertEquals( size, fc.position() );
        
        long seed = new Random().nextLong();
        System.out.println( "testTruncate() random seed: " + seed );
        Random rand = new Random( seed );
        
        // size >= current size, position > size
        size = fc.size() + rand.nextInt( Integer.MAX_VALUE );
        fc.position( size + 1 + rand.nextInt( Integer.MAX_VALUE ) );
        assertTrue( ( size >= fc.size() ) && ( fc.position() > size ) );
        assertEquals( fc.size(), fc.truncate( size ).size() ); // size unmodified
        assertEquals( size, fc.position() );
        
        // size < current size, position <= size
        size = fc.size() - Math.max( 1, rand.nextInt( (int)fc.size() ) );
        fc.position( size - rand.nextInt( (int)size ) );
        assertTrue( ( size < fc.size() ) && ( fc.position() <= size ) );
        assertEquals( fc.position(), fc.truncate( size ).position() ); // position unmodified
        assertEquals( size, fc.size() );
        
        // size >= current size, position <= size
        size = fc.size() + rand.nextInt( Integer.MAX_VALUE );
        long pos = fc.position( size - rand.nextInt( (int)fc.position() ) ).position();
        assertTrue( ( size >= fc.size() ) && ( fc.position() <= size ) );
        assertEquals( fc.size(), fc.truncate( size ).size() ); // size unmodified
        assertEquals( pos, fc.position() ); // position unmodified
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // closed channel
            fc.truncate( 0 );
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
        
        // non-writable file channel
        fc = FileChannel.open( Paths.get( "docs/truncate.txt" ) );
        try {
            fc.truncate( 0 );
            fail( "expected NonWritableChannelException" );
        } catch ( NonWritableChannelException e ) {
        }
    }

    @Test
    public void testOpen() throws IOException {
        // FileChannel.open() ends up invoking GaePath.newByteChannel(), which is
        // tested by the GaePathTestCase class. 
    }

    @Test
    public void testClose() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/close.txt" ), WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        
        FileLock fileLock = fc.lock();
        assertTrue( fileLock.isValid() );
        
        fc.close();
        assertFalse( fc.isOpen() );
        assertFalse( fileLock.isValid() );
        
        fc.close(); // attempt to close a closed channel (does nothing)
    }

    @Test
    public void testIsOpen() throws IOException {
        // local file
        FileChannel fc = FileChannel.open( Paths.get( "docs/large.pdf" ) );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
        
        // gae file
        fc = FileChannel.open( Paths.get( "docs/isopen.txt" ), WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
    }
}
