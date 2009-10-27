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
package com.newatlanta.appengine.nio.file.attribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import com.newatlanta.commons.vfs.provider.gae.GaeFileObject;
import com.newatlanta.repackaged.java.nio.file.attribute.BasicFileAttributes;
import com.newatlanta.repackaged.java.nio.file.attribute.FileAttribute;
import com.newatlanta.repackaged.java.nio.file.attribute.FileTime;

public class GaeFileAttributes implements BasicFileAttributes {

    // supported view names
    public static final String BASIC_VIEW = "basic";
    public static final String GAE_VIEW = "gae";
    
    // supported basic attribute names
    public static final String LAST_MODIFIED_TIME = "lastModifiedTime";
    public static final String SIZE = "size";
    public static final String IS_REGULAR_FILE = "isRegularFile";
    public static final String IS_DIRECTORY = "isDirectory";
    public static final String IS_SYMBOLIC_LINK = "isSymbolicLink";
    public static final String IS_OTHER = "isOther";
    
    // supported gae attribute names
    public static final String BLOCK_SIZE = "blockSize";
    
    // currently unsupported basic attribute names
    public static final String LAST_ACCESS_TIME = "lastAccessTime";
    public static final String CREATION_TIME = "creationTime";
    public static final String FILE_KEY = "fileKey";
    
    private FileObject fileObject;
    
    GaeFileAttributes( FileObject fileObject ) {
        this.fileObject = fileObject;
    }
    
    public FileTime creationTime() {
        return null; // not supported
    }

    /**
     * Used by Files.walkFileTree() to detect cycles created by symbolic links.
     */
    public Object fileKey() {
        return null; // not supported
    }

    public boolean isDirectory() {
        try {
            return fileObject.getType().hasChildren();
        } catch ( FileSystemException e ) {
            return false;
        }
    }

    public boolean isOther() {
        return false;
    }

    public boolean isRegularFile() {
        try {
            return fileObject.getType().hasContent();
        } catch ( FileSystemException e ) {
            return false;
        }
    }

    public boolean isSymbolicLink() {
        return false;
    }

    public FileTime lastAccessTime() {
        return null; // not supported
    }

    public FileTime lastModifiedTime() {
        try {
            return FileTime.fromMillis( fileObject.getContent().getLastModifiedTime() );
        } catch ( FileSystemException e ) {
            return null;
        }
    }

    public long size() {
        try {
            return fileObject.getContent().getSize();
        } catch ( FileSystemException e ) {
            return 0;
        }
    }
    
    public int blockSize() {
        try {
            if ( fileObject instanceof GaeFileObject ) {
                return ((GaeFileObject)fileObject).getBlockSize();
            }
        } catch ( FileSystemException e ) {
        }
        return 0;
    }
    
    public void setBlockSize( int blockSize ) throws IOException {
        if ( fileObject instanceof GaeFileObject ) {
            ((GaeFileObject)fileObject).setBlockSize( blockSize );
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public static Set<String> supportedFileAttributeViews() {
        Set<String> viewSet = new HashSet<String>();
        viewSet.add( BASIC_VIEW );
        viewSet.add( GAE_VIEW );
        return viewSet;
    }
    
    public Map<String, ?> getSupportedAttributes( String viewName ) {
        Map<String, Object> attrMap = new HashMap<String, Object>();
        attrMap.put( LAST_MODIFIED_TIME, lastModifiedTime() );
        attrMap.put( SIZE, size() );
        attrMap.put( IS_REGULAR_FILE, isRegularFile() );
        attrMap.put( IS_DIRECTORY, isDirectory() );
        attrMap.put( IS_SYMBOLIC_LINK, Boolean.FALSE );
        attrMap.put( IS_OTHER, Boolean.FALSE );
        if ( GAE_VIEW.equals( viewName ) ) {
            attrMap.put( BLOCK_SIZE, blockSize() );
        }
        return attrMap;
    }
    
    public Object getAttribute( String viewName, String attrName ) {
        if ( LAST_MODIFIED_TIME.equals( attrName ) ) {
            return lastModifiedTime();
        } else if ( SIZE.equals( attrName ) ) {
            return size();
        } else if ( IS_REGULAR_FILE.equals( attrName ) ) {
            return isRegularFile();
        } else if ( IS_DIRECTORY.equals( attrName ) ) {
            return isDirectory();
        } else if ( IS_SYMBOLIC_LINK.equals( attrName ) ) {
            return Boolean.FALSE;
        } else if ( IS_OTHER.endsWith( attrName ) ) {
            return Boolean.FALSE;
        } else if ( GAE_VIEW.equals( viewName ) ) {
            // may support other gae attributes in the future
            if ( BLOCK_SIZE.equals( attrName ) ) {
                return blockSize();
            }
        }
        return null;
    }
    
    public void setAttribute( String viewName, String attrName, Object attrValue )
            throws IOException
    {
        if ( GAE_VIEW.equals( viewName ) ) {
            // may support other gae attributes in the future
            if ( BLOCK_SIZE.equals( attrName ) ) {
                setBlockSize( ((Integer)attrValue).intValue() );
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Convenience method for use with Path.createFile():
     * 
     *     Path filePath = Paths.get( "myFile.txt" );
     *     filePath.createFile( withBlockSize( 8 ) );
     */
    public static GaeBlockSizeAttribute withBlockSize( int size ) {
        return new GaeBlockSizeAttribute( size );
    }
    
    private static class GaeBlockSizeAttribute implements FileAttribute<Integer> {

        private int blockSize;
        
        private GaeBlockSizeAttribute( int size ) {
            blockSize = size;
        }
        
        public String name() {
            return BLOCK_SIZE;
        }

        public Integer value() {
            return blockSize;
        }
    }
}
