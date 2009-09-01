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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.vfs.FileSystemException;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.newatlanta.commons.vfs.provider.gae.GaeFileObject;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;

public class GaeFileChannel extends FileChannel {
    
    private static final String CONTENT_BLOB = "content-blob"; // property key
    
    private GaeFileObject fileObject; // parent file
    
    private Entity block;   // the current block
    private int index;      // index of the current block
    private boolean dirty;  // current block needs to be written?
    
    private long position; // absolute position within the file
    
    private byte[] buffer;  // current block buffer
    private int offset;     // relative position of filePointer within buffer
    private int blockSize;

    public GaeFileChannel( GaeFileObject gfo, int _blockSize ) {
        fileObject = gfo;
        blockSize = _blockSize;
    }
    
    @Override
    public void force( boolean metaData ) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public FileLock lock( long position, long size, boolean shared ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public synchronized GaeFileChannel position( long newPosition ) throws IOException {
        if ( position == newPosition ) {
            return this;
        }
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException( "invalid position: " + newPosition );
        }
        int newIndex = calcBlockIndex( newPosition );
        if ( newIndex != index ) {
            close();
            index = newIndex;
        }
        position = newPosition;
        offset = calcBlockOffset( position );
        return this;
    }
    
    /**
     * Given an absolute position within the file, calculate the block index.
     */
    private int calcBlockIndex( long i ) {
        return (int)( i / blockSize );
    }
    
    /**
     * Given an absolute position within the file, calculate the offset within
     * the current block.
     */
    private int calcBlockOffset( long i ) {
        return (int)( i - ( index * blockSize ) );
    }

    @Override
    public long read( ByteBuffer[] dsts, int offset, int length ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int read( ByteBuffer dst, long position ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public int read( ByteBuffer dst ) throws IOException {
        int len = dst.remaining();
        if ( len == 0 ) {
            return 0;
        }
        long fileLen = size();
        if ( position >= fileLen ) {
            return -1;
        }
        if ( position + len > fileLen ) {
            len = (int)( fileLen - position );
        }
        if ( len <= 0 ) {
            return 0;
        }
        return internalRead( dst, len );
    }
    
    private synchronized int internalRead( ByteBuffer dst, int len ) throws IOException {
        // len is always less than or equal to the file length, so we're
        // always going to read exactly len number of bytes
        initBuffer();
        if ( calcBlockIndex( position + len ) == index ) { // within current block
            dst.put( buffer, offset, len );
            position( position + len );
            return len; // recursive reads always end here
        } else {
            // read to the end of the current buffer
            int bytesAvailable = buffer.length - offset;
            dst.put( buffer, offset, bytesAvailable );

            // move file pointer to beginning of next buffer
            position( position + bytesAvailable );

            // recursively read the rest of the input
            internalRead( dst, len - bytesAvailable );
            return len;
        }
    }

    @Override
    public long size()throws IOException {
        return fileObject.getContent().getSize();
    }

    @Override
    public long transferFrom( ReadableByteChannel src, long position, long count ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long transferTo( long position, long count, WritableByteChannel target ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public GaeFileChannel truncate( long size ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileLock tryLock( long position, long size, boolean shared ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized long write( ByteBuffer[] srcs, int offset, int length ) throws IOException {
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
    public synchronized int write( ByteBuffer src, long writePos ) throws IOException {
        long origPosition = position;
        try {
            return position( writePos ).write( src );
        } finally {
            position( origPosition );
        }
    }
    
    @Override
    public synchronized int write( ByteBuffer src ) throws IOException {
        int len = src.remaining();
        if ( len > 0 ) {
            internalWrite( src, len );
        }
        return len;
    }
    
    private synchronized void internalWrite( ByteBuffer src, int len ) throws IOException {
        initBuffer();
        if ( calcBlockIndex( position + len ) == index ) { // within current block
            writeBuffer( src, len );
            moveFilePointer( position + len );
        } else {
            // fill the current buffer
            int bytesAvailable = blockSize - offset;
            writeBuffer( src, bytesAvailable );
            moveFilePointer( position + bytesAvailable );

            // recursively write the rest of the output
            internalWrite( src, len - bytesAvailable );
        }
    }
    
    private synchronized void writeBuffer( ByteBuffer src, int len ) throws FileSystemException {
        if ( ( offset + len ) > buffer.length ) {
            extendBuffer( offset + len );
        }
        src.get( buffer, offset, len );
        dirty = true;
    }
    
    /**
     * The preferred extended buffer size is twice the current size, but it must be:
     *      - at least as large as len
     *      - at least as large as the minimum buffer size
     *      - no larger than the file block size
     */
    private synchronized void extendBuffer( int len ) throws FileSystemException {
        byte[] tempBuf = buffer;
        // twice the current size, but at least as large as len
        int newSize = Math.max( buffer.length << 1, len );
        // at least as large as the minimum size
        newSize = Math.max( newSize, getMinBufferSize() );
        // no larger than the block size
        buffer = new byte[ Math.min( newSize, blockSize ) ];
        System.arraycopy( tempBuf, 0, buffer, 0, tempBuf.length );
    }

    private synchronized void moveFilePointer( long newPos ) throws IOException {
        fileObject.updateContentSize( newPos );
        position( newPos );
    }
    
    private synchronized void initBuffer() throws FileSystemException {
        if ( buffer != null ) {
            return;
        }
        if ( block == null ) {
            block = fileObject.getBlock( index );
            dirty = false;
        }
        Blob contentBlob = (Blob)block.getProperty( CONTENT_BLOB );
        buffer = ( contentBlob != null ? contentBlob.getBytes()
                                       : new byte[ getMinBufferSize() ] );
    }
    
    private int getMinBufferSize() throws FileSystemException {
        if ( blockSize <= ( 1024 * 8 ) ) {
            return blockSize;
        } else if ( blockSize <= ( 1024 * 32 ) ) {
            return 1024 * 8;
        } else if ( blockSize >= ( 1024 * 256 ) ) {
            return 1024 * 64;
        } else {
            return blockSize >> 2; // one-fourth block size
        }
    }

    @Override
    protected synchronized void implCloseChannel() throws IOException {
        if ( dirty && ( block != null ) && ( buffer != null ) ) {
            boolean setBlobProperty = true;
            long fileLen = size();
            // if this is the last block for the file, and the buffer is less than
            // half full, only write out the actual number of bytes
            if ( calcBlockIndex( fileLen ) == index ) {
                // the EOF offset could be larger than buffer.length if setLength()
                // is used to set the file length larger than buffer.length, but no
                // bytes get written past buffer.length
                int eofoffset = calcBlockOffset( fileLen );
                if ( eofoffset < ( buffer.length >> 1 ) ) { // less than half full
                    byte[] outbuf = new byte[ eofoffset ];
                    System.arraycopy( buffer, 0, outbuf, 0, outbuf.length );
                    block.setProperty( CONTENT_BLOB, new Blob( outbuf ) );
                    setBlobProperty = false;
                }
            }
            if ( setBlobProperty ) {
                block.setProperty( CONTENT_BLOB, new Blob( buffer ) );
            }
            fileObject.putBlock( block );
            dirty = false;
        }
        block = null;
        buffer = null;
        index = 0;
        position = 0;
        offset = 0;
    }
}
