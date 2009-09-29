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
package com.newatlanta.appengine.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
import com.newatlanta.h2.store.fs.FileSystemGae;

/**
 * Handles servlet lifecycle events related to GaeVFS. Must be configured within
 * <tt>web.xml</tt>:
 * <p><code>
 * &lt;listener><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;listener-class>com.newatlanta.appengine.servlet.GaeVfsServletEventListener&lt;/listener-class><br>
 * &lt;/listener><br>
 * </code><p>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeVfsServletEventListener implements ServletContextListener {

    /**
     * Registers with H2.
     */
    public void contextInitialized( ServletContextEvent event ) {
        try {
            // use reflection in case H2 is not installed
            Class<?> fileSystemClass = Class.forName( "org.h2.store.fs.FileSystem" );
            fileSystemClass.getMethod( "register", fileSystemClass ).invoke( null,
                                            FileSystemGae.getInstance() );
            GaeVFS.log.info( "Successfully registered GaeVFS with H2" );
        } catch ( Exception e ) {
            GaeVFS.log.info( "Failed to register GaeVFS with H2: " + e );
        }
        
        // user.home is needed by H2 to locate the ".h2.server.properties" file
        String userDir = System.getProperty( "user.dir" );
        System.setProperty( "user.home", userDir + "/WEB-INF/user-home" );
        GaeVFS.log.info( "Context initialized: " + userDir );
    }

    /**
     * Closes GaeVFS when the servlet is destroyed.
     */
    public void contextDestroyed( ServletContextEvent event ) {
        GaeVFS.close();
        GaeVFS.log.info( "Context destroyed: " + System.getProperty( "user.dir" ) );
    }
}