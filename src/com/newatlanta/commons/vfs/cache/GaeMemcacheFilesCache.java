/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.commons.vfs.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.cache.*;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.cache.LRUFilesCache;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.newatlanta.commons.vfs.provider.gae.GaeFileSystem;

public class GaeMemcacheFilesCache extends LRUFilesCache {
	
	private static ThreadLocalCache threadLocalCache = new ThreadLocalCache();
	
	private static class ThreadLocalCache extends ThreadLocal<Map<FileName, FileObject>>
	{
		protected synchronized Map<FileName, FileObject> initialValue() {
            return new HashMap<FileName, FileObject>();
        }
		
		public FileObject get( FileName name ) {
			return get().get( name );
		}
		
		public void put( FileName name, FileObject fileObject ) {
			get().put( name, fileObject );
		}
		
		public void put( FileObject fileObject ) {
			get().put( fileObject.getName(), fileObject );
		}
		
		public void remove( FileName name ) {
			get().remove( name );
		}
	};
	
	// default properties for creating new cache instances -- 20 minute expiration
	@SuppressWarnings({ "serial", "unchecked" })
	private static final Map CACHE_PROPERTIES = Collections.unmodifiableMap( new HashMap() {
													{ put( GCacheFactory.EXPIRATION_DELTA, 20 * 60 ); }
												} );
	private Cache memcache;
	
	public GaeMemcacheFilesCache() throws FileSystemException {
		try {
			memcache = CacheManager.getInstance().getCacheFactory().createCache( CACHE_PROPERTIES );
		} catch ( CacheException e ) {
			throw new FileSystemException( e );
		}
	}
	
	public void clear() {
		threadLocalCache.remove();
		
	}
	
	public void clear( FileSystem filesystem ) {
		if ( filesystem instanceof GaeFileSystem ) {
			threadLocalCache.remove();
			// don't worry about memcache -- fileObjects will expire on their own
		} else {
			super.clear( filesystem );
		}
	}

	public void close() {
		threadLocalCache.remove();
		// don't worry about memcache -- fileObjects will expire on their own
		super.close();
	}

	public FileObject getFile( FileSystem filesystem, FileName name ) {
		if ( filesystem instanceof GaeFileSystem ) {
			FileObject fileObject = threadLocalCache.get( name );
			if ( fileObject == null ) {
				fileObject = (FileObject)memcache.get( name );
				if ( fileObject != null ) {
					threadLocalCache.put( name, fileObject );
				}
			}
			return fileObject;
		} else {
			return super.getFile( filesystem, name );
		}
	}

	@SuppressWarnings("unchecked")
	public void putFile( FileObject file ) {
		if ( file.getFileSystem() instanceof GaeFileSystem ) {
			memcache.put( file.getName(), file );
			threadLocalCache.put( file );
		} else {
			super.putFile( file );
		}
	}

	public void removeFile( FileSystem filesystem, FileName name ) {
		if ( filesystem instanceof GaeFileSystem ) {
			threadLocalCache.remove( name );
			memcache.remove( name );
		} else {
			super.removeFile( filesystem, name );
		}
	}
}
