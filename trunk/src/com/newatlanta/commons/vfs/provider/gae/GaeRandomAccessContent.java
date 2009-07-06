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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;

/**
 * Implements random access to a byte array by:
 * 
 *  1) Extending java.io.ByteArrayOutputStream and manipulating its "count" field
 *     when writing so that it writes from the file pointer position.
 *     
 *  2) Creating a java.io.ByteArrayInputStream that wraps the superclass "buf"
 *     field for reading.
 *     
 *  The class mimics java.io.RandomAccessFile, which has this description:
 *  
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
 *  
 *    "It is generally true of all the reading routines in this class that if
 *  end-of-file is reached before the desired number of bytes has been read, an
 *  EOFException (which is a kind of IOException) is thrown. If any byte cannot
 *  be read for any reason other than end-of-file, an IOException other than
 *  EOFException is thrown. In particular, an IOException may be thrown if the
 *  stream has been closed." 
 *     
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeRandomAccessContent extends ByteArrayOutputStream implements RandomAccessContent {

    private RandomAccessMode mode;
    private long filePointer;
    private InputStream in;
    
    /**
     * if given an empty buffer, initialize to an 8KB buffer; otherwise,
     * start with the buffer that was passed in
     */
    public GaeRandomAccessContent( byte[] b, RandomAccessMode m ) {
        super( b.length > 0 ? 0 : 8 * 1024 );
        if ( b.length > 0 ) {
            super.buf = b;
            super.count = b.length;
        }
        mode = m;
    }
    
    public long getFilePointer() {
        return filePointer;
    }

    /**
     * Notice: If you use seek(long) you must re-get the InputStream.
     */
    public synchronized InputStream getInputStream() throws IOException {
        if ( in == null ) {
            in = new ByteArrayInputStream( super.buf );
            in.skip( filePointer );
        }
        return in;
    }

    public long length() {
        return super.count;
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs.  The offset may be
     * set beyond the end of the file. Setting the offset beyond the end
     * of the file does not change the file length.  The file length will
     * change only by writing after the offset has been set beyond the end
     * of the file.
     * 
     * Notice: If you use getInputStream() you must re-get the InputStream
     * after calling seek(long)
     */
    public synchronized void seek( long pos ) {
        filePointer = pos;
        in = null;
    }
    
    /**
     * This method mimics java.io.RandomAccessFile.setLength(), which has this
     * description:
     * 
     *   "If the present length of the file as returned by the length method is
     * greater than the newLength argument then the file will be truncated. In this
     * case, if the file offset as returned by the getFilePointer method is greater
     * than newLength then after this method returns the offset will be equal to
     * newLength.
     * 
     *   "If the present length of the file as returned by the length method is
     * smaller than the newLength argument then the file will be extended. In this
     * case, the contents of the extended portion of the file are not defined." 
     */
    public synchronized void setLength( long newLength ) throws IOException {
        if ( super.count > newLength ) { // truncate
            Arrays.fill( super.buf, (int)newLength, super.count, (byte)0 );
            super.count = (int)newLength;
            if ( filePointer > newLength ) {
                filePointer = newLength;
                in = null;
            }
        } else if ( super.count < newLength ) { // extend
            int len = (int)newLength - super.count;
            super.write( new byte[ len ], 0, len );
            // leave file pointer where it is
        }
    }

    /**
     * All writes must be done via the following two methods.
     * 
     * Trick the base class into writing at the file pointer by setting its count
     * field; then set it back after the write.
     */
    @Override
    public synchronized void write( int b ) {
        int origCount = doBeforeWrite();
        super.write( b );
        doAfterWrite( origCount, 1 );
    }
    
    @Override
    public synchronized void write( byte[] b, int off, int len ) {
        int origCount = doBeforeWrite();
        super.write( b, off, len );
        doAfterWrite( origCount, len );
    }
    
    private synchronized int doBeforeWrite() {
        if ( !mode.requestWrite() ) {
            throw new UnsupportedOperationException( "file opened read-only" );
        }
        int origCount = super.count;
        super.count = (int)filePointer;
        return origCount;
    }
    
    private synchronized void doAfterWrite( int origCount, int len ) {
        filePointer += len;
        super.count = (int)( filePointer < origCount ? origCount : filePointer );
        if ( in != null ) {
            try {
                in.skip( len );
            } catch ( IOException e ) {
                // this should never happen
                e.printStackTrace( System.err );
            }
        }
    }
    
    public void writeBoolean( boolean v ) {
        write( v ? 1 : 0 );
    }

    public void writeByte( int v ) {
        write( v );
    }

    public void writeBytes( String s ) throws IOException {
        write( s.getBytes() );
    }

    public void writeChar( int v ) {
        write( v );
    }

    public void writeChars( String s ) throws IOException {
        write( s.getBytes() );
    }

    public void writeDouble( double v ) {
        write( (int)v );
    }

    public void writeFloat( float v ) {
        write( (int)v );
    }

    public void writeInt( int v ) {
        write( v );
    }

    public void writeLong( long v ) {
        write( (int)v );
    }

    public void writeShort( int v ) {
        write( v );
    }

    public void writeUTF( String str ) throws IOException {
        write( str.getBytes() );
    }

    /**
     * All reads must be done via the following two methods.
     */
    public synchronized int readInt() throws IOException {
        int c = getInputStream().read();
        filePointer++;
        return c;
    }
    
    public synchronized void readFully( byte[] b, int off, int len ) throws IOException {
        getInputStream().read( b, off, len );
        filePointer += len;
    }
    
    public void readFully( byte[] b ) throws IOException {
        readFully( b, 0, b.length );
    }
    
    public boolean readBoolean() throws IOException {
        return ( readInt() == 0 ? false : true );
    }

    public byte readByte() throws IOException {
        return (byte)readInt();
    }

    public char readChar() throws IOException {
        return (char)readInt();
    }

    public double readDouble() throws IOException {
        return readInt();
    }

    public float readFloat() throws IOException {
        return readInt();
    }

    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    public long readLong() throws IOException {
        return readInt();
    }

    public short readShort() throws IOException {
        return (short)readInt();
    }

    public String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }

    public int readUnsignedByte() throws IOException {
        return readInt();
    }

    public int readUnsignedShort() throws IOException {
        return readInt();
    }

    public synchronized int skipBytes( int n ) throws IOException {
        long skipped = getInputStream().skip( n );
        filePointer += skipped;
        return (int)skipped;
    }
}
