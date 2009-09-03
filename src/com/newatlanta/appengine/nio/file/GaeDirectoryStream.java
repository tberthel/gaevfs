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
package com.newatlanta.appengine.nio.file;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.vfs.FileObject;

import com.newatlanta.nio.file.ClosedDirectoryStreamException;
import com.newatlanta.nio.file.DirectoryStream;
import com.newatlanta.nio.file.FileSystem;
import com.newatlanta.nio.file.Path;

public class GaeDirectoryStream implements DirectoryStream<Path> {
    
    private static final int CLOSED = Integer.MIN_VALUE;

    private FileSystem fileSystem;
    private FileObject[] children;
    private int index = CLOSED;
    
    public GaeDirectoryStream( FileSystem fileSystem, FileObject[] children ) {
        this.fileSystem = fileSystem;
        this.children = children;
    }

    public synchronized Iterator<Path> iterator() {
        if ( ( index != CLOSED ) || ( children == null ) ) {
            throw new IllegalStateException();
        }
        index = -1;
        return new GaeDirectoryStreamIterator();
    }

    public void close() throws IOException {
        index = CLOSED;
        fileSystem = null;
        children = null;
    }
    
    private class GaeDirectoryStreamIterator implements Iterator<Path> {

        public boolean hasNext() {
            checkOpen();
            return ( ( index + 1 ) < children.length );
        }

        public Path next() {
            checkOpen();
            if ( ( index++ ) >= children.length ) {
                throw new NoSuchElementException();
            }
            // TODO: "The Path objects are obtained as if by resolving the name
            // of the directory entry against this path." Not sure exactly what
            // this means.
            return new GaePath( fileSystem, children[ index ] );
        }

        public void remove() {
            checkOpen();
            if ( ( index < 0 ) || ( children[ index ] == null ) ) {
                throw new IllegalStateException();
            }
            try {
                children[ index ].delete();
            } catch ( IOException e ) {
                throw new ConcurrentModificationException( e.getMessage() );
            }
            children[ index ] = null;
        }
        
        private void checkOpen() {
            if ( ( index == CLOSED ) || ( children == null ) ) {
                throw new ClosedDirectoryStreamException();
            }
        }
    }
}
