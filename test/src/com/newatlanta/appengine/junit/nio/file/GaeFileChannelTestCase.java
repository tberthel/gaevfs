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
import static com.newatlanta.nio.file.StandardOpenOption.APPEND;
import static com.newatlanta.nio.file.StandardOpenOption.CREATE_NEW;
import static com.newatlanta.nio.file.StandardOpenOption.READ;
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.appengine.nio.channels.GaeFileChannel;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;
import com.newatlanta.nio.file.Path;
import com.newatlanta.nio.file.Paths;
import com.newatlanta.nio.file.ProviderMismatchException;

public class GaeFileChannelTestCase extends GaeVfsTestCase {

    @Test
    public void testReadOptions() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate( 10 );
        FileChannel fc = FileChannel.open( Paths.get( "docs/readOptions.txt" ),
                                                WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        
        try {
            // non-readable channel
            fc.read( buf );
            fail( "expected NonReadableChannelException" );
        } catch ( NonReadableChannelException e ) {
        }
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // closed channel
            fc.read( buf );
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
    }
    
    @Test
    public void testReadInt() throws IOException {
        ByteBuffer buf = getByteBuffer( 1024 * 32, (byte)'R' ); // 32KB
        
        Path filePath = Paths.get( "docs/readInt.txt" );
        FileChannel fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                                withBlockSize( 32 ) );
        assertTrue( fc.isOpen() );
        
        // read an empty file
        assertEquals( -1, ((GaeFileChannel)fc).read() );
        
        assertEquals( buf.capacity(), fc.write( buf ) );
        assertEquals( buf.capacity(), fc.write( (ByteBuffer)buf.rewind() ) );
        assertEquals( buf.capacity() * 2, fc.size() );
        fc.position( 0 );
       
        for ( int i = 0; i < ( fc.size() ); i++ ) {
            assertEquals( 'R', ((GaeFileChannel)fc).read() );
            assertEquals( i + 1, fc.position() );
        }
        
        // read one past EOF
        assertEquals( -1, ((GaeFileChannel)fc).read() );
    }
    
    @Test
    public void testReadByteBuffer() throws IOException {
        ByteBuffer dst = getByteBuffer( 1024 * 32, (byte)'R' ); // 32KB
        
        Path filePath = Paths.get( "docs/read.txt" );
        FileChannel fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                                withBlockSize( 8 ) );
        assertTrue( filePath.exists() );
        assertTrue( fc.isOpen() );
        
        // read an empty file
        assertEquals( -1, fc.read( dst ) );
        
        assertEquals( dst.capacity(), fc.write( dst ) );
        assertEquals( dst.capacity(), fc.write( (ByteBuffer)dst.rewind() ) );
        assertEquals( dst.capacity() * 2, fc.size() );
        fc.position( 0 );
        
        // within current block, buffer.remaining() > dst.remaining
        assertReadByteBuffer( fc, (ByteBuffer)dst.rewind().limit( 1024 * 2 ) );
        
        // more than current block
        assertReadByteBuffer( fc, (ByteBuffer)dst.limit( dst.capacity() ) );
        
        // read with empty buf
        assertEquals( 0, fc.read( dst ) );
        
        // read to EOF
        assertReadByteBuffer( fc, (ByteBuffer)dst.rewind() );
        
        // read past EOF
        assertEquals( -1, fc.read( (ByteBuffer)dst.position( 0 ) ) );
        
        // within current block, buffer.remaining() <= dst.remaining
        dst.position( 0 ).limit( 1024 );
        assertEquals( 1024, fc.write( dst ) );
        fc.position( fc.size() - 1024 );
        dst.limit( 2048 );
        assertReadByteBuffer( fc, dst );
        
        // dst.remaining larger than EOF
        fc.position( fc.size() - 100 );
        assertReadByteBuffer( fc, (ByteBuffer)dst.rewind() );
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        filePath.delete();
        assertTrue( filePath.notExists() );
        
        fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                    withBlockSize( 32 ) );
        assertTrue( filePath.exists() );
        assertTrue( fc.isOpen() );
        
        assertEquals( 1, fc.write( ByteBuffer.allocate( 1 ) ) );
        assertEquals( 1, fc.size() );
        long newLength = 32 * 1024;
        ((GaeFileChannel)fc).setLength( newLength ); // equal to one block
        assertEquals( newLength, fc.size() );
        fc.position( 0 );
        
        dst = ByteBuffer.allocate( 4096 );
        long totalBytes = 0;
        int bytesRead = 0;
        while ( ( bytesRead = fc.read( (ByteBuffer)dst.rewind() ) ) > 0 ) {
            assertEquals( 4096, bytesRead );
            totalBytes += bytesRead;
        }
        assertEquals( totalBytes, newLength );
        
        newLength += 1024;
        fc.write( ByteBuffer.allocate( 1 ), newLength - 1 );
        assertEquals( newLength, fc.size() );
        
        fc.position( 0 );
        totalBytes = 0;
        while ( ( bytesRead = fc.read( (ByteBuffer)dst.rewind() ) ) > 0 ) {
            totalBytes += bytesRead;
        }
        assertEquals( totalBytes, newLength );
    }

    private static void assertReadByteBuffer( FileChannel fc, ByteBuffer dst )
            throws IOException {
        int l = dst.limit();
        int rdst = dst.remaining();
        int pdst = dst.position();
        long rpfc = fc.size() - fc.position();
        long pfc = fc.position();
        int n = fc.read( dst);
        assertEquals( n, Math.min( rdst, rpfc ) );
        assertEquals( pdst + n, dst.position() );
        assertEquals( l, dst.limit() ); // dst limit unchanged
        assertEquals( pfc + n, fc.position() );
    }

    @Test
    public void testReadByteBufferArrayIntInt() {
        fail( "Not yet implemented" );
    }
    
    @Test
    public void testReadByteBufferLong() {
        fail( "Not yet implemented" );
    }
    
    
    private ByteBuffer getByteBuffer( int size, byte b ) {
        byte[] barray = new byte[ size ];
        Arrays.fill( barray, b );
        return ByteBuffer.wrap( barray );
    }

    @Test
    public void testWriteOptions() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate( 10 );
        Path filePath = Paths.get( "docs/writeOptions.txt" ).createFile();
        assertTrue( filePath.exists() );
        FileChannel fc = FileChannel.open( filePath, READ, CREATE_NEW );
        assertTrue( fc.isOpen() );
        
        try {
            // non-writable channel
            fc.write( buf );
            fail( "expected NonWritableChannelException" );
        } catch ( NonWritableChannelException e ) {
        }
        
        fc.close();
        assertFalse( fc.isOpen() );
        
        try {
            // closed channel
            fc.write( buf );
            fail( "expected ClosedChannelException" );
        } catch ( ClosedChannelException e ) {
        }
    }
    
    @Test
    public void testWriteInt() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/writeInt.txt" ),
                    EnumSet.of( READ, WRITE, CREATE_NEW ), withBlockSize( 32 ) );
        assertTrue( fc.isOpen() );
        
        // write one byte at the first position
        ((GaeFileChannel)fc).write( 'X' );
        assertEquals( 1, fc.size() );
        assertEquals( 1, fc.position() );
        
        // write 32KB bytes, one at a time (one past first block)
        for ( int i = 0; i < ( 32 * 1024 ); i++ ) {
            ((GaeFileChannel)fc).write( 'Y' );
            assertEquals( i + 2, fc.size() );
            assertEquals( i + 2, fc.position() );
        }
        
        fc.close();
    }
    
    @Test
    public void testWriteByteBuffer() throws IOException {
        ByteBuffer src = getByteBuffer( 1024 * 32, (byte)'W' ); // 32KB
        
        Path filePath = Paths.get( "docs/write.txt" );
        FileChannel fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                                withBlockSize( 32 ) );
        assertTrue( fc.isOpen() );
        
        // write 1KB
        int pbuf = src.position( 1024 ).position();
        int l = src.limit( 2048 ).limit();
        int r = src.remaining();
        assertEquals( 1024, r );
        long pfc = fc.position();
        int n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
        
        // write 8KB (causes buffer to be extended, but within same block)
        pbuf = src.position();
        l = src.limit( pbuf + ( 1024 * 8 ) ).limit();
        r = src.remaining();
        assertEquals( 1024 * 8, r );
        pfc = fc.position();
        n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
        
        // write 32KB (into next block)
        pbuf = src.position( 0 ).position();
        l = src.limit( src.capacity() ).limit();
        r = src.remaining();
        assertEquals( src.capacity(), r );
        pfc = fc.position();
        n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
        
        // write 23KB (exactly on block boundary)
        pbuf = src.position( 0 ).position();
        l = src.limit( 1024 * 23 ).limit();
        r = src.remaining();
        assertEquals( 1024 * 23, r );
        pfc = fc.position();
        n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
        
        // write 1KB (into next block)
        pbuf = src.position();
        l = src.limit( pbuf + 1024 ).limit();
        r = src.remaining();
        assertEquals( 1024, r );
        pfc = fc.position();
        n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
        
        // append option
        fc.close();
        assertFalse( fc.isOpen() );
        fc = FileChannel.open( filePath, APPEND );
        assertTrue( fc.isOpen() );
        
        pbuf = src.position();
        l = src.limit( pbuf + 1024 ).limit();
        r = src.remaining();
        assertEquals( 1024, r );
        assertEquals( 0, fc.position() );
        pfc = fc.size();
        n = fc.write( src );
        assertPostWrite( src, fc, pbuf, l, r, pfc, n );
    }

    private void assertPostWrite( ByteBuffer buf, FileChannel fc, int pbuf, int l,
                                    int r, long pfc, int n ) throws IOException {
        assertEquals( n, r ); // all bytes were written
        assertEquals( 0, buf.remaining() );
        assertEquals( pbuf + n, buf.position() );
        assertEquals( l, buf.limit() ); // limit did not change
        assertEquals( pfc + n, fc.position() );
    }

    @Test
    public void testWriteByteBufferArrayIntInt() {
        fail( "Not yet implemented" );
    }
    
    @Test
    public void testWriteByteBufferLong() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testPosition() throws IOException {
        ByteBuffer buf = getByteBuffer( 100, (byte)'P' );
        
        Path filePath = Paths.get( "docs/position.txt" );
        FileChannel fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                                withBlockSize( 8 ) );
        assertTrue( filePath.exists() );
        assertTrue( fc.isOpen() );
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
        int p = ( 8 * 1024 ) + 256;
        assertEquals( p, fc.position( p ).position() );
        assertEquals( 200, fc.size() );
        assertEquals( -1, fc.read( (ByteBuffer)buf.rewind() ) );
        assertEquals( 100, fc.write( (ByteBuffer)buf.rewind() ) );
        assertEquals( p + 100, fc.size() );
        assertEquals( p + 100, fc.position() );
        
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
        
        // set the position beyond the buffer capacity within a single block
        filePath.delete();
        assertTrue( filePath.notExists() );
        fc = FileChannel.open( filePath, EnumSet.of( READ, WRITE, CREATE_NEW ),
                                    withBlockSize( 64 ) );
        assertTrue( filePath.exists() );
        assertTrue( fc.isOpen() );
        // with 32KB block size, minimum buffer size is 16KB
        fc.write( buf );
        fc.position( 16 * 1024 );
        assertEquals( -1, fc.read( buf ) );
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
    public void testLock() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/lock.txt" ),
                                        EnumSet.of( READ, WRITE, CREATE_NEW ) );
        assertTrue( fc.isOpen() );
        
        // create a thread that sleeps for 200ms then closes the channel
        Thread lockThread = FileLockingThread.createThread( (GaeFileChannel)fc, 200, true );
        assertTrue( lockThread.isAlive() );
        try {
            fc.lock();
            fail( "expected AsynchronousCloseException" );
        } catch ( AsynchronousCloseException e ) {
        }
        assertFalse( lockThread.isAlive() );
        
        // re-open the channel
        fc = FileChannel.open( Paths.get( "docs/lock.txt" ), READ, WRITE );
        assertTrue( fc.isOpen() );
        
        // create a thread that sleeps for 200ms then interrupts this thread
        lockThread = FileLockingThread.createThread( (GaeFileChannel)fc, 200,
                                                        Thread.currentThread() );
        assertTrue( lockThread.isAlive() );
        try {
            fc.lock();
            fail( "expected FileLockInterruptionException" );
        } catch ( FileLockInterruptionException e ) {
            assertTrue( Thread.interrupted() );
        }
        
        // get and release an exclusive lock
        FileLock fileLock = fc.lock();
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertFalse( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        fileLock.release();
        assertFalse( fileLock.isValid() );
        fileLock.release(); // release on an invalid lock does nothing

        // get and release a shared lock (automatically converted to exclusive)
        fileLock = fc.lock( 0, Long.MAX_VALUE, true );
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertFalse( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        fileLock.release();
        assertFalse( fileLock.isValid() );
        fileLock.release(); // release on an invalid lock does nothing
    }

    @Test
    public void testTryLock() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/tryLock.txt" ),
                                                EnumSet.of( READ, WRITE, CREATE_NEW ) );
        assertTrue( fc.isOpen() );
        
        // get and release an exclusive lock
        FileLock fileLock = fc.tryLock();
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertFalse( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        fileLock.release();
        assertFalse( fileLock.isValid() );
        fileLock.release(); // release on an invalid lock does nothing
        
        // get lock on separate thread (simulating separate JVM)
        Thread lockThread = FileLockingThread.createThread( (GaeFileChannel)fc );
        assertTrue( lockThread.isAlive() );
        assertNull( fc.tryLock() ); // verify can't acquire lock
        lockThread.interrupt();    
        do {
            try {
                Thread.sleep( 100 ); // give lockThread a chance to run
            } catch ( InterruptedException e ) {
            }
        } while ( lockThread.isAlive() );
        assertFalse( lockThread.isAlive() );
        
        // get and release a shared lock (automatically converted to exclusive)
        fileLock = fc.tryLock( 0, Long.MAX_VALUE, true );
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        assertFalse( fileLock.isShared() );
        assertEquals( 0, fileLock.position() );
        assertEquals( Long.MAX_VALUE, fileLock.size() );
        assertEquals( fc, fileLock.acquiredBy() );
        fileLock.release();
        assertFalse( fileLock.isValid() );
        fileLock.release(); // release on an invalid lock does nothing
    }
    
    @Test
    public void testLockOptions() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/lockOptions.txt" ),
                                            EnumSet.of( WRITE, CREATE_NEW ) );
        assertTrue( fc.isOpen() );
        
        try {
            fc.tryLock( 0L, Long.MAX_VALUE, true ); // shared
            fail( "expected NonReadableChannelException" );
        } catch ( NonReadableChannelException e ) {
        }
        
        fc.close();
        fc = FileChannel.open( Paths.get( "docs/lockOptions.txt" ), READ );
        assertTrue( fc.isOpen() );
        
        try {
            fc.tryLock( 0L, Long.MAX_VALUE, false ); // not shared
            fail( "expected NonWritableChannelException" );
        } catch ( NonWritableChannelException e ) {
        }
        
        fc.close();
    }
    
    @Test
    public void testFileLock() throws IOException {
        FileChannel fc = FileChannel.open( Paths.get( "docs/fileLock.txt" ),
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
            fc.lock( 0, -100, false );
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
            fc.lock( 1, 100, false );
            fail( "expected UnsupportedOperationException" );
        } catch ( UnsupportedOperationException e ) {
        }
        
        FileLock fileLock = fc.tryLock();
        assertNotNull( fileLock );
        assertTrue( fileLock.isValid() );
        
        try {
            // try second get on exclusive lock
            fc.lock();
            fail( "expected OverlappingFileLockException" );
        } catch ( OverlappingFileLockException e ) {
        }
        
        fc.close();
        assertFalse( fc.isOpen() );
        assertFalse( fileLock.isValid() );
        
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
                            EnumSet.of( WRITE, CREATE_NEW ), withBlockSize( 8 ) );
        assertTrue( fc.isOpen() );

        ByteBuffer b = ByteBuffer.allocate( 1 ).put( (byte)0xff );
        for ( int i = 0; i < 10; i++ ) { // write out 10 blocks
            fc.position( i * 8 * 1024 );
            fc.write( (ByteBuffer)b.rewind() );
        }
        fc.position( fc.position() + 512 ).write( (ByteBuffer )b.rewind() );
        
        try {
            // size < 0
            fc.truncate( -10 );
            fail( "expected IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }
        
        // size < current size, position > size, within current block
        long size = fc.size() - 256;
        assertTrue( ( size < fc.size() ) && ( fc.position() > size ) );
        assertEquals( size, fc.truncate( size ).size() );
        assertEquals( size, fc.position() );
        
        // size < current size, position > size, truncate blocks
        size = ( 1024 * 8 * 6 ) - 256;
        assertTrue( ( size < fc.size() ) && ( fc.position() > size ) );
        assertEquals( size, fc.truncate( size ).size() );
        assertEquals( size, fc.position() );
        
        // size >= current size, position > size
        size = fc.size() + 1024;
        fc.position( size + 512 );
        assertTrue( ( size >= fc.size() ) && ( fc.position() > size ) );
        assertEquals( fc.size(), fc.truncate( size ).size() ); // size unmodified
        assertEquals( size, fc.position() );
        
        // size < current size, position <= size
        size = fc.size() - 2012;
        fc.position( size - 300 );
        assertTrue( ( size < fc.size() ) && ( fc.position() <= size ) );
        assertEquals( fc.position(), fc.truncate( size ).position() ); // position unmodified
        assertEquals( size, fc.size() );
        
        // size >= current size, position <= size
        size = fc.size() + 979;
        long pos = fc.position( size - 355 ).position();
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
        try {
            FileChannel.open( Paths.get( "docs/large.pdf" ) );
            fail( "expected ProviderMismatchException" );
        } catch ( ProviderMismatchException e ) {
        }
        
        // gae file
        FileChannel fc = FileChannel.open( Paths.get( "docs/isopen.txt" ), WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
    }
}
