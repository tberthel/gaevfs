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

import static com.newatlanta.nio.file.StandardOpenOption.APPEND;
import static com.newatlanta.nio.file.StandardOpenOption.READ;
import static com.newatlanta.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;

import com.newatlanta.commons.vfs.provider.gae.GaeFileObject;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;
import com.newatlanta.nio.file.OpenOption;

public class GaeFileChannel extends FileChannel {
    
    private FileObject fileObject;
    private RandomAccessContent content;
    private Set<? extends OpenOption> options;
    
    public GaeFileChannel( FileObject fileObject, Set<? extends OpenOption> options )
            throws IOException {
        this.fileObject = fileObject;
        RandomAccessMode mode = options.contains( WRITE ) ? RandomAccessMode.READWRITE
                                                          : RandomAccessMode.READ;
        this.content = fileObject.getContent().getRandomAccessContent( mode );
        this.options = options;
        if ( options.contains( TRUNCATE_EXISTING ) ) {
            content.setLength( 0 );
        }
    }
    
    @Override
    public void force( boolean metaData ) throws IOException {
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        if ( fileObject instanceof GaeFileObject ) {
            ((GaeFileObject)fileObject).persist( metaData, true );
        }
    }

    @Override
    public FileLock lock( long position, long size, boolean shared ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public FileLock tryLock( long position, long size, boolean shared ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long position() throws IOException {
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        return content.getFilePointer();
    }

    @Override
    public synchronized GaeFileChannel position( long newPosition ) throws IOException {
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException( "newPosition cannot be negative: " +
                                                    newPosition );
        }
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        content.seek( newPosition );
        return this;
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
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        if ( !options.contains( READ ) ) {
            throw new NonReadableChannelException();
        }
        try {
            int len = dst.remaining();
            if ( dst.hasArray() ) {
                content.readFully( dst.array(), dst.arrayOffset(), dst.remaining() );
            } else {
                byte[] b = new byte[ len ];
                content.readFully( b, 0, len );
                dst.put( b );
            }
            return len - dst.remaining();
        } catch ( EOFException e ) {
            return -1;
        }
    }

    @Override
    public long size()throws IOException {
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        return content.length();
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
    public synchronized GaeFileChannel truncate( long size ) throws IOException {
        if ( size < 0 ) {
            throw new IllegalArgumentException( "size cannot be negative: " + size );
        }
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        if ( !options.contains( WRITE ) ) {
            throw new NonWritableChannelException();
        }
        if ( size < content.length() ) { // if ( size < size() )
            content.setLength( size );
        } else if ( content.getFilePointer() > size ) { // if ( position() > size )
            content.seek( size ); // position( size );
        }
        return this;
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
    public int write( ByteBuffer src, long writePos ) throws IOException {
        // TODO: this is wrong! need to allow other concurrent operations, if
        // not changing file size (not allowed to modify file pointer)
//        long origPosition = position();
//        try {
//            return position( writePos ).write( src );
//        } finally {
//            position( origPosition );
//        }
        return 0;
    }
    
    @Override
    public synchronized int write( ByteBuffer src ) throws IOException {
        if ( !isOpen() ) {
            throw new ClosedChannelException();
        }
        if ( !options.contains( WRITE ) ) {
            throw new NonWritableChannelException();
        }
        if ( options.contains( APPEND ) ) {
            content.seek( content.length() ); // advance file pointer to end of file
        }
        int len = src.remaining();
        if ( src.hasArray() ) { 
            content.write( src.array(), src.arrayOffset(), len );
        } else {
            byte[] b = new byte[ len ];
            src.get( b );
            content.write( b );
        }
        return len;
    }

    @Override
    protected synchronized void implCloseChannel() throws IOException {
        content.close();
    }
}
