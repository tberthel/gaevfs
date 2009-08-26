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
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

public class GaeFileChannel /*extends FileChannel*/ implements SeekableByteChannel {

    public GaeFileChannel() {
        // TODO Auto-generated constructor stub
    }

    public void force( boolean arg0 ) throws IOException {
        // TODO Auto-generated method stub

    }

    public FileLock lock( long arg0, long arg1, boolean arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

//    public MappedByteBuffer map( MapMode arg0, long arg1, long arg2 ) throws IOException {
//        // TODO Auto-generated method stub
//        return null;
//    }

    public long position() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public SeekableByteChannel position( long arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public int read( ByteBuffer arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public int read( ByteBuffer arg0, long arg1 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long read( ByteBuffer[] arg0, int arg1, int arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long size() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long transferFrom( ReadableByteChannel arg0, long arg1, long arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long transferTo( long arg0, long arg1, WritableByteChannel arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public SeekableByteChannel truncate( long arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public FileLock tryLock( long arg0, long arg1, boolean arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public int write( ByteBuffer arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public int write( ByteBuffer arg0, long arg1 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long write( ByteBuffer[] arg0, int arg1, int arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    protected void implCloseChannel() throws IOException {
        // TODO Auto-generated method stub
    }

    public void close() throws IOException {
        // TODO Auto-generated method stub   
    }

    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }
}
