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

import java.io.IOException;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.repackaged.java.nio.file.Files;
import com.newatlanta.repackaged.java.nio.file.Path;
import com.newatlanta.repackaged.java.nio.file.Paths;

public class FilesTestCase extends GaeVfsTestCase {

    @Test
    public void testProbeContentType() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testWalkFileTreePathSetOfFileVisitOptionIntFileVisitorOfQsuperPath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testWalkFileTreePathFileVisitorOfQsuperPath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testCreateDirectories() throws IOException {
        Path grandParent = Paths.get( "foo" );
        assertTrue( grandParent.notExists() );
        Path parent = Paths.get( "foo/bar" );
        assertTrue( parent.notExists() );
        assertTrue( parent.getParent().isSameFile( grandParent ) );
        Path child = Paths.get( "foo/bar/baz" );
        assertTrue( child.notExists() );
        assertTrue( child.getParent().isSameFile( parent ) );
        
        Files.createDirectories( child );
        assertTrue( grandParent.exists() );
        assertTrue( parent.exists() );
        assertTrue( child.exists() );
    }
}
