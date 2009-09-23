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
package com.newatlanta.h2.store.fs;

import java.io.IOException;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;
import org.h2.store.fs.FileObject;

public class FileObjectGae implements FileObject {
    
    // TODO re-implement based on java.nio (com.newatlanta.nio)
    private org.apache.commons.vfs.FileObject fileObject;
    private RandomAccessContent content;

    public FileObjectGae(org.apache.commons.vfs.FileObject _fileObject, String mode) throws FileSystemException {
        fileObject = _fileObject;
        RandomAccessMode raMode;
        if ( mode.equals( "r" ) ) {
            raMode = RandomAccessMode.READ;
        } else if ( mode.equals( "rw" ) || mode.equals( "rws" ) || mode.equals( "rwd" ) ) {
            raMode = RandomAccessMode.READWRITE;
        } else {
            throw new IllegalArgumentException( "invalid mode: " + mode );
        }
        content = fileObject.getContent().getRandomAccessContent(raMode);
    }
    
    public void close() throws IOException {
        content.close();
    }

    public long getFilePointer() throws IOException {
        return content.getFilePointer();
    }

    public String getName() {
        return fileObject.getName().getURI();
    }

    public long length() throws IOException {
        return content.length();
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        content.readFully(b, off, len);
    }

    public void seek(long pos) throws IOException {
        content.seek(pos);
    }

    public void setFileLength(long newLength) throws IOException {
//        content.setLength(newLength);
    }

    /**
     * Force changes to the physical location.
     */
    public void sync() throws IOException {
        fileObject.refresh();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        content.write(b, off, len);
    }
}
