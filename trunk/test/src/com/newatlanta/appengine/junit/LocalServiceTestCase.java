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
package com.newatlanta.appengine.junit;

import java.io.File;

import junit.framework.TestCase;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

/**
 * http://code.google.com/appengine/docs/java/howto/unittesting.html
 */
public abstract class LocalServiceTestCase extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ApiProxy.setEnvironmentForCurrentThread( new TestEnvironment() );
        ApiProxy.setDelegate( new ApiProxyLocalImpl( new File( "." ) ) {} );
    }

    @Override
    public void tearDown() throws Exception {
        // not strictly necessary to null these out but there's no harm either
        ApiProxy.setDelegate( null );
        ApiProxy.clearEnvironmentForCurrentThread();
        super.tearDown();
    }
}