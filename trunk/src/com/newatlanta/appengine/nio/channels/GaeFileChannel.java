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
package com.newatlanta.appengine.nio.channels;

import static com.newatlanta.appengine.nio.channels.GaeFileLock.releaseAllLocks;
import static com.newatlanta.nio.file.StandardOpenOption.APPEND;
import static com.newatlanta.nio.file.StandardOpenOption.READ;
import static com.newatlanta.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

import org.apache.commons.vfs.FileSystemException;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.newatlanta.appengine.locks.SleepTimer;
import com.newatlanta.commons.vfs.provider.gae.GaeFileContent;
import com.newatlanta.commons.vfs.provider.gae.GaeFileObject;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;
import com.newatlanta.nio.file.OpenOption;

/**
 * Implements {@linkplain com.newatlanta.nio.channels.FileChannel} for GaeVFS.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileChannel extends FileChannel {
    
    // property keys
    private static final String CONTENT_BLOB = "content-blob";
    private static final String DIRTY = "dirty";
    
    private GaeFileObject fileObject;
    private Set<? extends OpenOption> options;
    
    private long position; // absolute position within the file
    private int index; // index of the current block
    
    private Entity block; // current block
    private int blockSize;
    
    private ByteBuffer buffer; // wraps the current block contents
    
    public GaeFileChannel( GaeFileObject fileObject, Set<? extends OpenOption> options )
            throws IOException {
        this.fileObject = fileObject;
        this.blockSize = fileObject.getBlockSize();
        this.options = options;
        if ( options.contains( TRUNCATE_EXISTING ) ) {
            truncate( 0 );
        }
        ((GaeFileContent)fileObject.getContent()).notifyOpen( this );
    }
    
    private GaeFileChannel() {
    }
    
    /**
     * Creates a new channel that shares this channel's contents, but with
     * independent position.
     * 
     * Intended for internal use to implement read() and write() methods that
     * don't modify the position. If this is ever exposed publicly, then the
     * GaeFileContent.notify() method should be invoked for the new channel.
     */
    private GaeFileChannel duplicate() {
        GaeFileChannel duplicate = new GaeFileChannel();
        duplicate.fileObject = this.fileObject;
        duplicate.options = this.options;
        duplicate.position = this.position;
        duplicate.index = this.index;
        duplicate.block = this.block;
        duplicate.blockSize = this.blockSize;
        if ( buffer != null ) {
            duplicate.buffer = buffer.duplicate();
        }
        return duplicate;
    }
    
    @Override
    public synchronized void force( boolean metaData ) throws IOException {
        checkOpen();
        flush();
        fileObject.putContent( metaData, true );
    }

    public synchronized void flush() throws FileSystemException, IOException {
        if ( ( block != null ) && ( buffer != null ) && isDirty() ) {
            int eofoffset = calcBlockOffset( doGetSize() );
            if ( eofoffset < ( blockSize >> 1 ) ) {
                // this is the last block for the file, and the block is less than half
                // full, so only write out the actual number of bytes in the buffer
                eofoffset = Math.min( eofoffset, buffer.capacity() );
                block.setProperty( CONTENT_BLOB, new Blob( ByteBuffer.allocate( eofoffset ).put(
                    (ByteBuffer)buffer.duplicate().position( 0 ).limit( eofoffset ) ).array() ) );
            } else {
                block.setProperty( CONTENT_BLOB, new Blob( buffer.array() ) );
            }
        }
    }

    @Override
    public FileLock lock( long position, long size, boolean shared ) throws IOException {
        checkLockOptions( shared );
        // GaeFileLock constructor validates arguments
        GaeFileLock fileLock = new GaeFileLock( this, position, size );
        try {
            try {
                SleepTimer timer = new SleepTimer();
                while ( !fileLock.tryLock() ) {
                    Thread.sleep( timer.nextSleepTime() );
                    if ( !isOpen() ) { // another thread closed the channel
                        throw new AsynchronousCloseException();
                    }
                }
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt(); // set interrupted flag
                throw new FileLockInterruptionException();
            }
            return fileLock;
        } finally {
            if ( !fileLock.isValid() ) {
                fileLock.release();
            }
        }
    }
    
    @Override
    public FileLock tryLock( long position, long size, boolean shared ) throws IOException {
        checkLockOptions( shared );
        // GaeFileLock constructor validates arguments
        GaeFileLock fileLock = new GaeFileLock( this, position, size );
        try {
            if ( !fileLock.tryLock() ) {
                return null;
            }
            return fileLock;
        } finally {
            if ( !fileLock.isValid() ) {
                fileLock.release();
            }
        }
    }
    
    private void checkLockOptions( boolean shared ) {
        if ( shared ) {
            if ( !options.contains( READ ) ) {
                throw new NonReadableChannelException();
            }
        } else if ( !options.contains( WRITE ) ) {
            throw new NonWritableChannelException();
        }
    }
    
    public String getLockName() {
        return fileObject.getName().getPath()+ ".GaeFileChannel.lock";
    }
    
    /**
     * Sets the file length without modifying the contents or position.
     */
    public void setLength( long newLength ) throws IOException {
        fileObject.updateContentSize( newLength );
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return position;
    }

    @Override
    public synchronized GaeFileChannel position( long newPosition ) throws IOException {
        if ( position == newPosition ) {
            return this;
        }
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException( "Negative newPosition" );
        }
        checkOpen();
        positionInternal( newPosition, true );
        return this;
    }
    
    private void positionInternal( long newPosition, boolean updateBuffer )
            throws IOException {
        int newIndex = calcBlockIndex( newPosition );
        if ( newIndex != index ) {
            closeBlock();
            index = newIndex;
        }
        position = newPosition;
        if ( updateBuffer && ( buffer != null ) ) {
            setBufferPosition();
        }
    }
    
    private void setBufferPosition() throws FileSystemException {
        int offset = calcBlockOffset( position );
        if ( offset > buffer.capacity() ) {
            extendBuffer( offset );
        } else {
            buffer.position( offset );
        }
    }
    
    private synchronized void closeBlock() throws IOException {
        flush();
        block = null;
        buffer = null;
        index = 0;
        position = 0;
    }
    
    /**
     * Given an absolute position within the file, calculate the block index.
     */
    private int calcBlockIndex( long i ) throws FileSystemException {
        return (int)( i / blockSize );
    }
    
    /**
     * Given an absolute position within the file, calculate the offset within
     * the current block.
     */
    private int calcBlockOffset( long i ) throws FileSystemException {
        return (int)( i - ( index * blockSize ) );
    }

    @Override
    public long read( ByteBuffer[] dsts, int offset, int length ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int read( ByteBuffer dst, long position ) throws IOException {
        // TODO this method has not been tested
        return this.duplicate().position( position ).read( dst );
    }
    
    public synchronized int read() throws IOException {
        checkReadOptions();
        if ( position >= doGetSize() ) {
            return -1;
        }
        initBuffer();
        int c = buffer.get() & 0xff;
        positionInternal( position + 1, false );
        return c;
    }
    
    @Override
    public synchronized int read( ByteBuffer dst ) throws IOException {
        checkReadOptions();
        long fileLen = doGetSize();
        if ( position >= fileLen ) {
            return -1;
        }
        int totalBytesRead = 0;
        while ( dst.hasRemaining() && ( position < fileLen ) ) {
            initBuffer();
            int r = dst.remaining();
            if ( calcBlockIndex( position + r - 1 ) == index ) { 
                // within current block, read until dst is full or to EOF
                int eofoffset = calcBlockOffset( fileLen );
                int limit = Math.min( buffer.position() + r, eofoffset );
                if ( limit > buffer.capacity() ) {
                    buffer.limit( buffer.capacity() );
                    dst.put( buffer );
                    dst.position( Math.min( limit - buffer.capacity(), dst.capacity() ) );
                } else {
                    buffer.limit( limit );
                    dst.put( buffer );
                    buffer.limit( buffer.capacity() ); // restore original limit
                }
                int bytesRead = ( r - dst.remaining() );
                totalBytesRead += bytesRead;
                positionInternal( position + bytesRead, false );
            } else {
                // read to the end of the current buffer
                r = buffer.remaining();
                dst.put( buffer );
                totalBytesRead += r;
    
                // move position to beginning of next buffer, repeat loop
                positionInternal( position + r, false );
            }
        }
        return totalBytesRead;
    }

    private void checkReadOptions() throws ClosedChannelException {
        checkOpen();
        if ( !options.contains( READ ) ) {
            throw new NonReadableChannelException();
        }
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return doGetSize();
    }
    
    private long doGetSize() throws IOException {
        return fileObject.doGetContentSize();
    }

    @Override
    public long transferFrom( ReadableByteChannel src, long position, long count )
            throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long transferTo( long position, long count, WritableByteChannel target )
            throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public synchronized GaeFileChannel truncate( long size ) throws IOException {
        if ( size < 0 ) {
            throw new IllegalArgumentException( "Negative size" );
        }
        checkOpen();
        if ( !options.contains( WRITE ) ) {
            throw new NonWritableChannelException();
        }
        if ( position > size ) {
            positionInternal( size, true );
        }
        if ( size < doGetSize() ) {
            fileObject.deleteBlocks( calcBlockIndex( size - 1 ) + 1 );
            fileObject.updateContentSize( size, true );
            fileObject.putContent( true, false );
        }
        return this;
    }
    
    public static void setDirty( Entity entity, boolean dirty ) {
        entity.setProperty( DIRTY, Boolean.valueOf( dirty ) );
    }
    
    private void setDirty( boolean dirty ) {
        setDirty( block, dirty );
    }
    
    public static boolean isDirty( Entity block, boolean removeProperty ) {
        Boolean dirty = (Boolean)block.getProperty( DIRTY );
        if ( removeProperty ) {
            block.removeProperty( DIRTY );
        }
        return ( ( dirty != null ) && dirty.booleanValue() );
    }
    
    private boolean isDirty() {
        return isDirty( block, false );
    }

    @Override
    public synchronized long write( ByteBuffer[] srcs, int offset, int length )
            throws IOException {
        checkWriteOptions();
        // TODO this method has not been tested
        if ( ( offset < 0 ) || ( offset > srcs.length ) ||
                ( length < 0 ) || ( length > ( srcs.length - offset ) ) ) {
            throw new IllegalArgumentException();
        }
        int bytesWritten = 0;
        for ( int i = offset; i < ( offset + length ); i++ ) {
            bytesWritten += write( srcs[ i ] );
        }
        return bytesWritten;
    }

    @Override
    public int write( ByteBuffer src, long writePos ) throws IOException {
        // TODO this method has not been tested
        return this.duplicate().position( writePos ).write( src );
    }
    
    public synchronized void write( int b ) throws IOException {
        checkWriteOptions();
        initBuffer();
        if ( !buffer.hasRemaining() ) {
            extendBuffer( 1 );
        }
        buffer.put( (byte)b );
        finishWrite( position + 1 ); // update content size, set dirty, position
    }
    
    @Override
    public synchronized int write( ByteBuffer src ) throws IOException {
        checkWriteOptions();
        int bytesWritten = 0;
        while ( src.hasRemaining() ) {
            initBuffer();
            if ( calcBlockIndex( position + src.remaining() - 1 ) == index ) {
                // writing entirely within current block
                bytesWritten += writeBuffer( src );
            } else {
                // fill the current block then repeat loop
                int limit = src.limit();
                src.limit( src.position() + ( blockSize - buffer.position() ) );
                bytesWritten += writeBuffer( src );
                src.limit( limit );
            }
        }
        return bytesWritten;
    }
    
    private synchronized int writeBuffer( ByteBuffer src ) throws IOException {
        int n = src.remaining();
        if ( n > buffer.remaining() ) {
            extendBuffer( buffer.position() + n );
        }
        buffer.put( src );
        finishWrite( position + n ); // update content size, set dirty, position
        return n;
    }
    
    private synchronized void initBuffer() throws FileSystemException {
        if ( buffer != null ) {
            return;
        }
        if ( block == null ) {
            block = fileObject.getBlock( index );
        }
        Blob contentBlob = (Blob)block.getProperty( CONTENT_BLOB );
        buffer = ( contentBlob != null ? ByteBuffer.wrap( contentBlob.getBytes() )
                                       : ByteBuffer.allocate( getMinBufferSize() ) );
        setBufferPosition();
    }
    
    /**
     * The preferred minimum buffer size is one-fourth the block size, but will be:
     *      - equal to the block size, if the block size is less than 8K
     *      - no smaller than 8K (unless the block size is smaller than 8K)
     *      - no larger than 64K
     */
    private int getMinBufferSize() throws FileSystemException {
        if ( blockSize <= ( 1024 * 8 ) ) {
            return blockSize;
        } else if ( blockSize <= ( 1024 * 32 ) ) {
            return 1024 * 8;
        } else if ( blockSize >= ( 1024 * 256 ) ) {
            return 1024 * 64;
        } else {
            return blockSize >> 2; // one-fourth the block size
        }
    }
    
    /**
     * The preferred extended buffer size is twice the current size, but it must be:
     *      - at least as large as len
     *      - at least as large as the minimum buffer size
     *      - no larger than the file block size
     */
    private synchronized void extendBuffer( int len ) throws FileSystemException {
        ByteBuffer oldbuf = (ByteBuffer)buffer.rewind();
        // twice the current size, but at least as large as len
        int newSize = Math.max( buffer.capacity() << 1, len );
        // at least as large as the minimum size
        newSize = Math.max( newSize, getMinBufferSize() );
        // no larger than the block size
        buffer = ByteBuffer.allocate( Math.min( newSize, blockSize ) );
        buffer.put( oldbuf ).position( calcBlockOffset( position ) );
    }
    
    private synchronized void finishWrite( long newPos ) throws IOException {
        fileObject.updateContentSize( newPos );
        setDirty( true );
        positionInternal( newPos, false );
    }

    private void checkWriteOptions() throws IOException {
        checkOpen();
        if ( !options.contains( WRITE ) ) {
            throw new NonWritableChannelException();
        }
        if ( options.contains( APPEND ) ) {
            // advance position to end of file before write
            positionInternal( doGetSize(), true );
        }
    }
    
    void checkOpen() throws ClosedChannelException {
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
    }

    @Override
    protected void implCloseChannel() throws IOException {
        flush();
        if ( options.contains( WRITE ) ) {
            fileObject.endOutput();
        }
        ((GaeFileContent)fileObject.getContent()).notifyClosed( this );
        releaseAllLocks( this ); // release all locks acquired by this channel
    }
}
