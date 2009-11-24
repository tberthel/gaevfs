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
package com.newatlanta.appengine.taskqueue;

import java.util.logging.Logger;

import com.newatlanta.appengine.taskqueue.Deferred.Deferrable;

public class TestDeferred implements Deferrable {

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = Logger.getLogger( TestDeferred.class.getName() );
    
    private byte[] arg;
    
    public TestDeferred() {
        this( 0 );
    }
    
    public TestDeferred( int size ) {
        arg = new byte[ size ];
    }

    @Override
    public void doTask() {
        log.info( "arg size = " + arg.length );
    }
}
