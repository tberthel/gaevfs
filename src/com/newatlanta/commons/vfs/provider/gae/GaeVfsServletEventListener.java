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
package com.newatlanta.commons.vfs.provider.gae;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * Handles servlet lifecycle events related to GaeVFS. Must be configured within
 * <tt>web.xml</tt>:
 * <p><code>
 * &lt;listener><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;listener-class>com.newatlanta.commons.vfs.provider.gae.GaeVfsServletEventListener&lt;/listener-class><br>
 * &lt;/listener><br>
 * </code><p>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeVfsServletEventListener implements ServletContextListener, ServletRequestListener {
	
	/**
	 * Initializes GaeVFS with the webapp root path.
	 */
	public void contextInitialized( ServletContextEvent event ) {
		GaeVFS.setRootPath( event.getServletContext().getRealPath( "/" ) );
	}
	
	/**
	 * Closes GaeVFS when the servlet is destroyed.
	 */
	public void contextDestroyed( ServletContextEvent event ) {
		GaeVFS.close();
	}

	/**
	 * Does nothing.
	 */
	public void requestInitialized( ServletRequestEvent event ) {
	}
	
	/**
	 * Does nothing.
	 */
	public void requestDestroyed( ServletRequestEvent event ) {
	}
}