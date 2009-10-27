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
package com.newatlanta.appengine.h2.store.fs;

import static com.newatlanta.repackaged.java.nio.file.StandardOpenOption.CREATE;
import static com.newatlanta.repackaged.java.nio.file.StandardOpenOption.DSYNC;
import static com.newatlanta.repackaged.java.nio.file.StandardOpenOption.READ;
import static com.newatlanta.repackaged.java.nio.file.StandardOpenOption.SYNC;
import static com.newatlanta.repackaged.java.nio.file.StandardOpenOption.WRITE;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.h2.store.fs.FileObject;

import com.newatlanta.appengine.nio.channels.GaeFileChannel;
import com.newatlanta.repackaged.java.nio.channels.FileChannel;
import com.newatlanta.repackaged.java.nio.file.Path;
import com.newatlanta.repackaged.java.nio.file.StandardOpenOption;

public class FileObjectGae implements FileObject {
    
    private String name;
    private FileChannel channel;

    FileObjectGae( Path filePath, String mode ) throws IOException {
        name = filePath.toUri().toString();
        EnumSet<StandardOpenOption> options = EnumSet.of( READ );
        if ( mode.contains( "w" ) ) {
            options.add( WRITE );
            options.add( CREATE );
            if ( mode.contains( "s" ) ) {
                options.add( SYNC );
            }
            if ( mode.contains( "d" ) ) {
                options.add( DSYNC );
            }
        }
        channel = FileChannel.open( filePath, options );
    }
    
    public void close() throws IOException {
        channel.close();
    }

    public long getFilePointer() throws IOException {
        return channel.position();
    }

    public String getName() {
        return name;
    }

    public long length() throws IOException {
        return channel.size();
    }

    public void readFully( byte[] b, int off, int len ) throws IOException {
        if ( len == 0 ) {
            return;
        }
        if ( channel.read( ByteBuffer.wrap( b, off, len ) ) < 0 ) {
            throw new EOFException();
        }
    }

    public void seek( long pos ) throws IOException {
        channel.position( pos );
    }

    public void setFileLength( long newLength ) throws IOException {
        if ( newLength <= channel.size() ) {
            channel.truncate( newLength );
        } else if ( channel instanceof GaeFileChannel ) {
            ((GaeFileChannel)channel).setLength( newLength );
        } else {
            // extend without modifying channel position
            channel.write( ByteBuffer.allocate( 1 ), newLength - 1 );
        }
    }

    public void sync() throws IOException {
        channel.force( true );
    }

    public void write( byte[] b, int off, int len ) throws IOException {
        if ( len == 0 ) {
            return;
        }
        channel.write( ByteBuffer.wrap( b, off, len ) );
    }
}
