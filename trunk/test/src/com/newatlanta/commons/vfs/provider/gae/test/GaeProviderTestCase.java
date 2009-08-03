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
package com.newatlanta.commons.vfs.provider.gae.test;

import java.io.File;

import junit.framework.Test;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FilesCache;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.cache.LRUFilesCache;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs.test.AbstractProviderTestCase;
import org.apache.commons.vfs.test.ProviderTestConfig;
import org.apache.commons.vfs.test.ProviderTestSuite;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import com.newatlanta.appengine.junit.GaeTestEnvironment;
import com.newatlanta.commons.vfs.provider.gae.GaeFileNameParser;
import com.newatlanta.commons.vfs.provider.gae.GaeFileSystemManager;

/**
 * Executes the Commons VFS ProviderTestSuite for GaeVFS.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeProviderTestCase extends AbstractProviderTestCase implements ProviderTestConfig {
	
	private static final String TEST_BASEDIR = "test/data";
	
	/**
     * Creates the test suite for the GaeVFS file system.
     */
    public static Test suite() throws Exception {
        return new ProviderTestSuite( new GaeProviderTestCase() );
    }
    
    public GaeProviderTestCase() {
    	// initialize GAE test environment
    	ApiProxy.setEnvironmentForCurrentThread( new GaeTestEnvironment() );
        ApiProxyLocalImpl proxy = new ApiProxyLocalImpl( new File( "." ) ) {};
        proxy.setProperty( LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString() );
        ApiProxy.setDelegate( proxy );
        
        // for AbstractVfsTestCase
    	System.setProperty( "test.basedir", TEST_BASEDIR );
		
		// GAE doesn't set these values; Commons VFS needs them to initialize
        System.setProperty( "os.arch", "" );
        System.setProperty( "os.version", "" );
    }

    private static FilesCache cache;
    
	public FilesCache getFilesCache() throws Exception {
		if ( cache == null) {
			cache = new LRUFilesCache();
		}
		return cache;
	}
	
	public DefaultFileSystemManager getDefaultFileSystemManager() throws Exception {
		boolean combinedLocal = Boolean.parseBoolean( System.getProperty( "combined.local" ) );
		System.out.println( "Combined Local option: " + combinedLocal );
		return new GaeFileSystemManager().setCombinedLocal( combinedLocal );
	}

	public void prepare( DefaultFileSystemManager manager ) throws Exception {
		manager.addProvider( "file", new DefaultLocalFileProvider() );
		((GaeFileSystemManager)manager).prepare( TEST_BASEDIR,
								getClass().getResource( "test-providers.xml" ) );
	}
	
	/**
     * Returns the base folder for tests. Copies test files from the local file
     * system to GaeVFS. Note that SVN (.svn) folders are not copied; if the are,
     * then the size of the LRUFilesCache created within GaeFileSystemManager.prepare()
     * must be increased to avoid testcase failures.
     */
	@Override
    public FileObject getBaseTestFolder( FileSystemManager manager ) throws Exception {
		FileObject gaeTestBaseDir = manager.getBaseFile().resolveFile( "test-data" );
		if ( !gaeTestBaseDir.exists() ) {
			FileObject localTestBaseDir = manager.resolveFile( "file://" +
							GaeFileNameParser.getRootPath( manager.getBaseFile().getName() ) +
							gaeTestBaseDir.getName().getPath() );
			gaeTestBaseDir.copyFrom( localTestBaseDir, new TestFileSelector() );
			// confirm that the correct number of files were copied
			FileObject[] testFiles = localTestBaseDir.findFiles( new TestFileSelector() );
			FileObject[] gaeFiles = gaeTestBaseDir.findFiles( Selectors.SELECT_FILES );
			assertEquals( testFiles.length, gaeFiles.length );
		}
        return gaeTestBaseDir;
    }
	
	private class TestFileSelector implements FileSelector
	{
		public boolean includeFile( FileSelectInfo fileInfo ) throws Exception {
			return fileInfo.getFile().getType() == FileType.FILE; // files only
		}

		public boolean traverseDescendents( FileSelectInfo fileInfo ) throws Exception {
			String baseName = fileInfo.getFile().getName().getBaseName();
			return !baseName.equals( ".svn" ); // skip .svn directories
		}
		
	}
}