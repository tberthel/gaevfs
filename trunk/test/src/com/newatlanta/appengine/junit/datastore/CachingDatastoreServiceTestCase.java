package com.newatlanta.appengine.junit.datastore;

import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.newatlanta.appengine.datastore.CachingDatastoreService;
import com.newatlanta.appengine.datastore.CachingDatastoreService.CacheOption;
import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;

public class CachingDatastoreServiceTestCase extends GaeVfsTestCase {
    
    @Test
    public void testPutEntity() throws EntityNotFoundException {
        // put an entity with a partial key
        Entity entity = new Entity( "test" );
        entity.setProperty( "test", "test" );
        assertFalse( entity.getKey().isComplete() );
        DatastoreService datastore = new CachingDatastoreService( CacheOption.WRITE_THROUGH );
        Key key = datastore.put( entity );
        assertTrue( key.isComplete() );
        assertTrue( MemcacheServiceFactory.getMemcacheService().contains( key ) );
        entity = DatastoreServiceFactory.getDatastoreService().get( key );
    }

    @Test
    public void testPutIterableOfEntity() {
        fail( "Not yet implemented" );
    }

}
