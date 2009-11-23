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
package com.newatlanta.appengine.vfs.provider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileContentInfo;
import org.apache.commons.vfs.FileContentInfoFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;

/**
 * Implements <code>FileContent</code> for GaeVFS. Differs from the Commons VFS
 * <code>DefaultFileContent</code> in the following ways:
 * <ol>
 * <li>Does not automatically buffer input and output streams.
 * <li>Allows multiple output streams.
 * </ol>
 *
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileContent implements FileContent {
    
    private GaeFileObject fileObject;
    private List<Closeable> closeableList = Collections.synchronizedList(
                                                    new ArrayList<Closeable>() );
    
    private FileContentInfo fileContentInfo;
    private FileContentInfoFactory fileContentInfoFactory;
    
    GaeFileContent( GaeFileObject fileObject,
                        FileContentInfoFactory fileContentInfoFactory ) {
        this.fileObject = fileObject;
        this.fileContentInfoFactory = fileContentInfoFactory;
    }

    public void close() throws FileSystemException {
        synchronized( closeableList ) {
            // using array avoids ConcurrentModificationException
            Closeable[] closeableArray = closeableList.toArray(
                                        new Closeable[ closeableList.size() ] );
            for ( Closeable closeable : closeableArray ) {
                try {
                    closeable.close();
                } catch ( IOException e ) {
                    GaeVFS.log.warning( e.toString() );
                }
            }
        }
    }

    public Object getAttribute( String attrName ) throws FileSystemException {
        return null;
    }

    public String[] getAttributeNames() throws FileSystemException {
        return new String[ 0 ];
    }

    @SuppressWarnings("unchecked")
    public Map getAttributes() throws FileSystemException {
        return Collections.EMPTY_MAP;
    }

    public Certificate[] getCertificates() throws FileSystemException {
        return null;
    }

    public FileContentInfo getContentInfo() throws FileSystemException {
        if ( fileContentInfo == null ) {
            fileContentInfo = fileContentInfoFactory.create( this );
        }
        return fileContentInfo;
    }

    public FileObject getFile() {
        return fileObject;
    }

    public InputStream getInputStream() throws FileSystemException {
        return fileObject.getInputStream();
    }
    
    public void notifyOpen( Closeable closeable ) {
        closeableList.add( closeable );
    }
    
    public void notifyClosed( Closeable closeable ) {
        closeableList.remove( closeable );
    }

    public long getLastModifiedTime() throws FileSystemException {
        return fileObject.doGetLastModifiedTime();
    }

    public OutputStream getOutputStream() throws FileSystemException {
        return getOutputStream( false );
    }

    public OutputStream getOutputStream( boolean append ) throws FileSystemException {
        return fileObject.getOutputStream( append );
    }

    public RandomAccessContent getRandomAccessContent( RandomAccessMode mode )
            throws FileSystemException {
        return fileObject.getRandomAccessContent( mode );
    }

    public long getSize() throws FileSystemException {
        if ( !fileObject.getType().hasContent() ) {
            throw new FileSystemException( "vfs.provider/get-size-not-file.error",
                                                fileObject );
        }
        return fileObject.doGetContentSize();
    }

    public boolean hasAttribute( String attrName ) throws FileSystemException {
        return false;
    }

    public boolean isOpen() {
        return !closeableList.isEmpty();
    }

    public void removeAttribute( String attrName ) throws FileSystemException {
        throw new FileSystemException( "vfs.provider/remove-attribute-not-supported.error" );
    }

    public void setAttribute( String attrName, Object value ) throws FileSystemException {
        throw new FileSystemException( "vfs.provider/set-attribute-not-supported.error" );
    }

    public void setLastModifiedTime( long modTime ) throws FileSystemException {
        fileObject.doSetLastModTime( modTime );
    }
}
