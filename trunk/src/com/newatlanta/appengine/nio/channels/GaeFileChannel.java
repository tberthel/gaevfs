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

import com.newatlanta.commons.vfs.provider.gae.GaeRandomAccessContent;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;

public class GaeFileChannel extends FileChannel {
    
    private GaeRandomAccessContent rac;

    public GaeFileChannel( GaeRandomAccessContent rac ) {
        this.rac = rac;
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
        return rac.getFilePointer();
    }

    @Override
    public synchronized GaeFileChannel position( long newPosition ) throws IOException {
        rac.seek( newPosition );
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
        int len = dst.remaining();
        if ( dst.hasArray() ) {
            return rac.read( dst.array(), dst.arrayOffset(), dst.remaining() );
        } else {
            byte[] b = new byte[ len ];
            int bytesRead = rac.read( b, 0, len );
            dst.put( b );
            return bytesRead;
        }
    }

    @Override
    public long size()throws IOException {
        return rac.length();
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
        rac.setLength( size );
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
        long origPosition = position();
        try {
            return position( writePos ).write( src );
        } finally {
            position( origPosition );
        }
    }
    
    @Override
    public synchronized int write( ByteBuffer src ) throws IOException {
        int len = src.remaining();
        if ( src.hasArray() ) { 
            rac.write( src.array(), src.arrayOffset(), len );
        } else {
            byte[] b = new byte[ len ];
            src.get( b );
            rac.write( b );
        }
        return len;
    }

    @Override
    protected synchronized void implCloseChannel() throws IOException {
        rac.close();
    }
}
