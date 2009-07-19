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
package com.newatlanta.commons.vfs.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.cache.LRUFilesCache;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.newatlanta.commons.vfs.provider.gae.GaeFileSystem;

/**
 * An <code>org.apache.commons.vfs.FilesCache</code> implementation based on the
 * <a href="http://code.google.com/appengine/docs/java/memcache/" target="_blank">
 * GAE memcache API</a>. This is an internal GaeVFS implementation class that is
 * normally not referenced directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>.
 * <p>
 * Commons VFS expects that a <code>FilesCache</code> implementation will return
 * a reference to a cached object, not a copy of the object as when reading
 * from <code>memcache</code>. In order to satisfy the Commons VFS requirement,
 * objects are copied into a <code>ThreadLocal</code> cache when getting them from
 * <code>memcache</code> so that subsequent gets return a reference to the same
 * object.
 * <p>
 * Unfortunately, within the GAE distributed environment, there's no way to know
 * when an object in <code>memcache</code> is modified by another instance of the
 * application running in a separate JVM. Therefore, the <code>ThreadLocal</code>
 * cache must be refreshed regularly to make sure it stays in sync with
 * <code>memcache</code>, which leads to the following warning:
 * <blockquote>
 * <b>IMPORTANT!</b> Servlets that use GaeVFS must clear the ThreadLocal cache at
 * the end of every request via the
 * {@link com.newatlanta.commons.vfs.provider.gae.GaeVFS#clearFilesCache} method.
 * </blockquote>
 * Finally, non-GAE file objects are stored in an <code>LRUFilesCache</code>, which
 * this class extends.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeMemcacheFilesCache extends LRUFilesCache {

    private static ThreadLocalCache threadLocalCache = new ThreadLocalCache();

    /**
     * This class provides some convenience methods for accessing the underlying
     * HashMap; otherwise, this could have been done with an anonymous class.
     */
    private static class ThreadLocalCache extends ThreadLocal<Map<FileName, FileObject>> {

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
    @SuppressWarnings( { "serial", "unchecked" })
    private static final Map CACHE_PROPERTIES = Collections.unmodifiableMap( new HashMap() {
        {
            put( GCacheFactory.EXPIRATION_DELTA, 20 * 60 );
        }
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
            // don't worry about memcache -- fileObjects will expire automatically
        } else {
            super.clear( filesystem );
        }
    }

    public void close() {
        threadLocalCache.remove();
        // don't worry about memcache -- fileObjects will expire automatically
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
            // memcache uses a "first created, first deleted" algorithm when purging
            // so remove before put to refresh the creation time; this also serves to
            // update the start of the 20-minute expiration
            memcache.remove( file.getName() );
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
