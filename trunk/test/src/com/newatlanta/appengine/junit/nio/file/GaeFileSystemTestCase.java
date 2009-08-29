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

import java.util.Set;

import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.nio.file.Paths;

public class GaeFileSystemTestCase extends GaeVfsTestCase {

    @Test
    public void testProvider() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testClose() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsOpen() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testIsReadOnly() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetSeparator() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetRootDirectories() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetFileStores() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testSupportedFileAttributeViews() {
        Set<String> views = Paths.get( "/" ).getFileSystem().supportedFileAttributeViews();
        assertNotNull( views );
        assertFalse( views.isEmpty() );
        assertEquals( 2, views.size() );
        assertTrue( views.contains( "basic" ) );
        assertTrue( views.contains( "gae" ) );
    }

    @Test
    public void testGetPath() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetPathMatcher() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGetUserPrincipalLookupService() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testNewWatchService() {
        fail( "Not yet implemented" );
    }

    @Test
    public void testGaeFileSystem() {
        fail( "Not yet implemented" );
    }

}
