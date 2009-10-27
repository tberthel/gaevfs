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

import org.apache.commons.vfs.FileObject;

import com.newatlanta.repackaged.java.nio.file.attribute.BasicFileAttributeView;
import com.newatlanta.repackaged.java.nio.file.attribute.FileTime;

public class GaeFileAttributeView implements BasicFileAttributeView {

    private String name;
    private FileObject fileObject;
    
    public GaeFileAttributeView( String name, FileObject fileObject ) {
        this.name = name;
        this.fileObject = fileObject;
    }
    
    public String name() {
        return name;
    }

    public GaeFileAttributes readAttributes() throws IOException {
        return new GaeFileAttributes( fileObject );
    }

    public void setTimes( FileTime lastModifiedTime, FileTime lastAccessTime,
                            FileTime createTime ) throws IOException {
        if ( lastModifiedTime != null ) {
            fileObject.getContent().setLastModifiedTime( lastModifiedTime.toMillis() );
        }
    }
}
