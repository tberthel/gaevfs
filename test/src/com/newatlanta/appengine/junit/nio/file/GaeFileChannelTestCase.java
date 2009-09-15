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
import static com.newatlanta.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.nio.channels.FileChannel;
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
                            EnumSet.of( WRITE, CREATE_NEW ), withBlockSize( 1 ) );
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
    public void testSize() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testForce() {
        fail( "Not yet implemented" );
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
    public void testLockLongLongBoolean() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testTryLockLongLongBoolean() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testTruncateLong() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testOpen() throws IOException {
        // FileChannel.open() ends up invoking GaePath.newByteChannel(), which is
        // tested by the GaePathTestCase class. 
    }

    @Test
    public void testClose() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsOpen() throws IOException {
        // local file
        FileChannel fc = FileChannel.open( Paths.get( "docs/large.pdf" ) );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
        
        // gae file
        fc = FileChannel.open( Paths.get( "docs/new.txt" ), WRITE, CREATE_NEW );
        assertTrue( fc.isOpen() );
        fc.close();
        assertFalse( fc.isOpen() );
    }
}
