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
package com.newatlanta.appengine.junit.vfs.gae;

import java.io.File;

import com.newatlanta.appengine.junit.LocalDatastoreTestCase;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;

/**
 * The base class for a GaeVFS junit testcases.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public abstract class GaeVfsTestCase extends LocalDatastoreTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        GaeVFS.setRootPath( new File( "test/data" ).getAbsolutePath() );
    }
    
    @Override
    public void tearDown() throws Exception {
        GaeVFS.clearFilesCache();
        super.tearDown();
    }
}