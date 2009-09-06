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
package com.newatlanta.appengine.datastore;

import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.KeyFactory.keyToString;
import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

@SuppressWarnings("serial")
public class CachingDatastoreService extends HttpServlet implements DatastoreService {
    
    private static final String QUEUE_NAME = "write-behind-task";
    private static final String KEY_PARAM = "key";
    private static final TaskOptions TASK_URL = url( "/" + QUEUE_NAME );
    
    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static Queue queue;
    
    private static final Logger log = Logger.getLogger( CachingDatastoreService.class.getName() );
    
    static {
        try {
            queue = QueueFactory.getQueue( QUEUE_NAME );
            queue.add( TASK_URL ); // test queue configuration
        } catch ( Exception e ) {
            log.info( e.getMessage() );
            queue = QueueFactory.getDefaultQueue();
        }
    }
    
    private Expiration expiration;
    
    public CachingDatastoreService() {
        this( null );
    }
    
    public CachingDatastoreService( Expiration expiration ) {
        this.expiration = expiration;
    }
    
    /**
     * Gets an entity. Returns the entity from memcache, if it exists; otherwise,
     * gets the entity from the datastore, writes it to memcache, then returns it.
     */
    public Entity get( Key key ) throws EntityNotFoundException {
        Entity entity = (Entity)memcache.get( key );
        if ( entity == null ) {
            try {
                entity = datastore.get( key );
            } catch ( DatastoreTimeoutException e ) {
                entity = datastore.get( key );
            }
            memcache.put( key, entity, expiration, SetPolicy.SET_ALWAYS );
        }
        return entity;
    }
    
    public Map<Key, Entity> get( Iterable<Key> keys ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Entity get( Transaction txn, Key key ) throws EntityNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<Key, Entity> get( Transaction txn, Iterable<Key> keys ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Key put( Entity entity ) {
        Key key = entity.getKey();
        if ( !key.isComplete() ) {
            // TODO: is this really better than just writing the entity to the datastore?
            KeyRange keyRange = datastore.allocateIds( key.getParent(), key.getKind(), 1 );
            key = createKey( key.getParent(), key.getKind(), keyRange.getStart().getId() );
        }
        memcache.put( key, entity );
        try {
            String keyString = keyToString( key );
            queue.add( TASK_URL.param( KEY_PARAM, keyString ).method( Method.GET ) );
            log.info( key + " " + keyString );
        } catch ( Exception e ) {
            log.warning( e.getMessage() );
            try {
                key = datastore.put( entity );
            } catch ( DatastoreTimeoutException t ) {
                key = datastore.put( entity );
            }
        }
        return key;
    }
    
    public List<Key> put( Iterable<Entity> entities ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Key put( Transaction txn, Entity entity ) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Key> put( Transaction txn, Iterable<Entity> entities ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void delete( Key ... keys ) {
        delete( Arrays.asList( keys ) );
    }
    
    @SuppressWarnings("unchecked")
    public void delete( Iterable<Key> keys ) {
        try {
            datastore.delete( keys );
        } catch ( DatastoreTimeoutException e ) {
            datastore.delete( keys );
        }
        memcache.deleteAll( (Collection)keys ); // TODO: test to insure this cast really works
    }
    
    public void delete( Transaction txn, Key ... keys ) {
        // TODO Auto-generated method stub 
    }

    public void delete( Transaction txn, Iterable<Key> keys ) {
        // TODO Auto-generated method stub
    }

    public KeyRange allocateIds( String kind, long num ) {
        return datastore.allocateIds( kind, num );
    }

    public KeyRange allocateIds( Key parent, String kind, long num ) {
        return datastore.allocateIds( parent, kind, num );
    }

    public Transaction beginTransaction() {
        return datastore.beginTransaction();
    }

    public Collection<Transaction> getActiveTransactions() {
        return datastore.getActiveTransactions();
    }

    public Transaction getCurrentTransaction() {
        return datastore.getCurrentTransaction();
    }

    public Transaction getCurrentTransaction( Transaction returnedIfNoTxn ) {
        return datastore.getCurrentTransaction( returnedIfNoTxn );
    }

    public PreparedQuery prepare( Query query ) {
        return datastore.prepare( query );
    }

    public PreparedQuery prepare( Transaction txn, Query query ) {
        return datastore.prepare( txn, query );
    }
    
    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        log.info( req.getQueryString() );
        doWriteBehindTask( req, res );
    }

    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        doWriteBehindTask( req, res );
    }

    @SuppressWarnings("unchecked")
    private static void doWriteBehindTask( HttpServletRequest req, HttpServletResponse res )
            throws IOException {
        String[] keyArray = req.getParameterValues( KEY_PARAM );
        if ( ( keyArray != null ) && ( keyArray.length > 0 ) ) {
            List<Object> keyList = new ArrayList<Object>( keyArray.length );
            for ( String keyString : keyArray ) {
                keyList.add( KeyFactory.stringToKey( keyString ) );
            }
            Map<Key, Entity> entityMap = (Map)memcache.getAll( keyList );
            if ( entityMap.size() > 0 ) {
                Collection<Entity> entities = entityMap.values();
                // TODO: use of transactions should be optional/configurable.
                Transaction txn = datastore.beginTransaction();
                try {
                    datastore.put( txn, entities );
                    txn.commit();
                } catch ( DatastoreTimeoutException e ) { // retry task
                    res.sendError( HttpServletResponse.SC_REQUEST_TIMEOUT );
                } catch ( Exception e ) {
                    log.warning( e.getMessage() ); // don't retry
                } finally {
                    if ( txn.isActive() ) {
                        txn.rollback();
                    }
                }
            }
        }
    }
}
