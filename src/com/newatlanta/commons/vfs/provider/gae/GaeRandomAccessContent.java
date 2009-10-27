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
package com.newatlanta.commons.vfs.provider.gae;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.util.EnumSet;

import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;

import com.newatlanta.appengine.nio.channels.GaeFileChannel;
import com.newatlanta.repackaged.java.nio.file.StandardOpenOption;

/**
 * The class mimics <code>java.io.RandomAccessFile</code>, which has this description:
 * <blockquote> 
 *    "Instances of this class support both reading and writing to a random access
 *  file. A random access file behaves like a large array of bytes stored in the
 *  file system. There is a kind of cursor, or index into the implied array, called
 *  the file pointer; input operations read bytes starting at the file pointer and
 *  advance the file pointer past the bytes read. If the random access file is
 *  created in read/write mode, then output operations are also available; output
 *  operations write bytes starting at the file pointer and advance the file pointer
 *  past the bytes written. Output operations that write past the current end of
 *  the implied array cause the array to be extended. The file pointer can be read
 *  by the getFilePointer method and set by the seek method.
 * </blockquote><blockquote>
 *    "It is generally true of all the reading routines in this class that if
 *  end-of-file is reached before the desired number of bytes has been read, an
 *  EOFException (which is a kind of IOException) is thrown. If any byte cannot
 *  be read for any reason other than end-of-file, an IOException other than
 *  EOFException is thrown. In particular, an IOException may be thrown if the
 *  stream has been closed." 
 * </blockquote>
 * This is an internal GaeVFS implementation class that is normally not referenced
 * directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>. See {@link GaeVFS} as the entry point for application
 * code that interacts with GaeVFS.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeRandomAccessContent implements RandomAccessContent {

    private GaeFileChannel fileChannel;
    private DataOutputStream dataOutput;
    private DataInputStream dataInput;
    
    GaeRandomAccessContent( GaeFileObject gfo, RandomAccessMode m, boolean append )
            throws IOException {
        EnumSet<StandardOpenOption> options = EnumSet.of( StandardOpenOption.READ );
        if ( m == RandomAccessMode.READWRITE ) {
            options.add( StandardOpenOption.WRITE );
            gfo.doSetLastModTime( System.currentTimeMillis() );
        }
        if ( append ) {
            options.add( StandardOpenOption.APPEND );
        }
        fileChannel = new GaeFileChannel( gfo, options );
        dataOutput = new DataOutputStream( Channels.newOutputStream( fileChannel ) );
        dataInput = new GaeDataInputStream( Channels.newInputStream( fileChannel ) );
    }
    
    public long getFilePointer() throws IOException {
        return fileChannel.position();
    }
    
    public void close() throws IOException {
        fileChannel.close();
    }

    public InputStream getInputStream() {
        return dataInput;
    }
    
    public OutputStream getOutputStream() {
        return dataOutput;
    }

    public long length() throws IOException {
        return fileChannel.size();
    }
    
    public void seek( long pos ) throws IOException {
        fileChannel.position( pos );
    }

    public void write( int b ) throws IOException {
        dataOutput.write( b );
    }
    
    public void write( byte[] b ) throws IOException {
        dataOutput.write( b );
    }
    
    public synchronized void write( byte[] b, int off, int len ) throws IOException {
        dataOutput.write( b, off, len );
    }
    
    public void writeBoolean( boolean v ) throws IOException {
        dataOutput.writeBoolean( v );
    }

    public void writeByte( int v ) throws IOException {
        dataOutput.writeByte( v );
    }

    public void writeBytes( String s ) throws IOException {
        dataOutput.writeBytes( s );
    }

    public void writeChar( int v ) throws IOException {
        dataOutput.writeChar( v );
    }

    public void writeChars( String s ) throws IOException {
        dataOutput.writeChars( s );
    }

    public void writeDouble( double v ) throws IOException {
        dataOutput.writeDouble( v );
    }

    public void writeFloat( float v ) throws IOException {
        dataOutput.writeFloat( v );
    }

    public void writeInt( int v ) throws IOException {
        dataOutput.writeInt( v );
    }

    public void writeLong( long v ) throws IOException {
        dataOutput.writeLong( v );
    }

    public void writeShort( int v ) throws IOException {
        dataOutput.writeShort( v );
    }

    public void writeUTF( String str ) throws IOException {
        dataOutput.writeUTF( str );
    }
    
    public void readFully( byte[] b ) throws IOException {
        dataInput.readFully( b );
    }
    
    public void readFully( byte[] b, int off, int len ) throws IOException {
        dataInput.readFully( b, off, len );
    }
    
    public boolean readBoolean() throws IOException {
        return dataInput.readBoolean();
    }

    public byte readByte() throws IOException {
        return dataInput.readByte();
    }

    public char readChar() throws IOException {
        return dataInput.readChar();
    }

    public double readDouble() throws IOException {
        return dataInput.readDouble();
    }

    public float readFloat() throws IOException {
        return dataInput.readFloat();
    }

    @Deprecated
    public String readLine() throws IOException {
        return dataInput.readLine();
    }
    
    public int readInt() throws IOException {
        return dataInput.readInt();
    }

    public long readLong() throws IOException {
        return dataInput.readLong();
    }

    public short readShort() throws IOException {
        return dataInput.readShort();
    }

    public String readUTF() throws IOException {
        return dataInput.readUTF();
    }

    public int readUnsignedByte() throws IOException {
        return dataInput.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return dataInput.readUnsignedShort();
    }

    public synchronized int skipBytes( int n ) throws IOException {
        return dataInput.skipBytes( n );
    }
    
    private class GaeDataInputStream extends DataInputStream {

        private GaeDataInputStream( InputStream in ) {
            super( in );
        }
        
        /**
         * Returns -1 rather than throwing IOException if channel is closed, in
         * order to satisfy Commons VFS testcase.
         */
        @Override
        public int read() throws IOException {
            try {
                return in.read();
            } catch ( ClosedChannelException e ) {
                return -1;
            }
        }
    }
}
