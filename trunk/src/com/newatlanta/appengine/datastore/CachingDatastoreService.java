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

import static com.google.appengine.api.datastore.KeyFactory.keyToString;
import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
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
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/**
 * Implements a <code>DatastoreService</code> that automatically caches entities
 * in memcache. Uses a write-behind cache when writing entities to the datastore.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
@SuppressWarnings("serial")
public class CachingDatastoreService extends HttpServlet implements DatastoreService {
    
    private static final String QUEUE_NAME = "write-behind-task";
    private static final String KEY_PARAM = "key";
    private static final String TASK_URL = "/" + QUEUE_NAME;
    
    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static Queue queue;
    
    private static final Logger log = Logger.getLogger( CachingDatastoreService.class.getName() );
    
    static {
        try {
            queue = QueueFactory.getQueue( QUEUE_NAME );
            queue.add( url( TASK_URL ) ); // verify queue configured
        } catch ( Exception e ) {
            log.info( e.getMessage() );
            queue = QueueFactory.getDefaultQueue(); // TODO: test default queue
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
     * gets the entity from the datastore, puts it into memcache, then returns it.
     */
    public Entity get( Key key ) throws EntityNotFoundException {
        return get( null, key );
    }
    
    public Entity get( Transaction txn, Key key ) throws EntityNotFoundException {
        String keyString = keyToString( key );
        Entity entity = (Entity)memcache.get( keyString );
        if ( entity == null ) {
            try {
                entity = datastore.get( txn, key );
            } catch ( DatastoreTimeoutException e ) {
                entity = datastore.get( txn, key );
            }
            memcache.put( keyString, entity, expiration, SetPolicy.SET_ALWAYS );
        }
        return entity;
    }
    
    /**
     * WARNING! Don't rely on the ordering of entities returned by this method.
     */
    public Map<Key, Entity> get( Iterable<Key> keys ) {
        return get( null, keys );
    }
    
    
    @SuppressWarnings("unchecked")
    public Map<Key, Entity> get( Transaction txn, Iterable<Key> keys ) {
        // TODO: this method has not been tested
        Collection<Object> keyStrings = keysToStrings( keys );
        Map<Key, Entity> keyEntityMap = mapStringToKey( (Map)memcache.getAll( keyStrings ) );
        if ( keyEntityMap.isEmpty() ) {
            return getAndCache( txn, keys );
        }
        if ( keyEntityMap.size() < keyStrings.size() ) {
            List<Key> keyList = new ArrayList<Key>();
            for ( Key key : keys ) {
                if ( !keyEntityMap.containsKey( key ) ) {
                    keyList.add( key );
                }
            }
            keyEntityMap.putAll( getAndCache( txn, keyList ) );
        }
        return keyEntityMap;
    }
    
    private static Collection<Object> keysToStrings( Iterable<Key> keys ) {
        Collection<Object> keyStrings = new ArrayList<Object>();
        for ( Key key : keys ) {
            keyStrings.add( keyToString( key ) );
        }
        return keyStrings;
    }
    
    private static Map<Key, Entity> mapStringToKey( Map<String, Entity> stringEntityMap ) {
        Map<Key, Entity> keyEntityMap = new HashMap<Key, Entity>();
        for ( Entity entity : stringEntityMap.values() ) {
            keyEntityMap.put( entity.getKey(), entity );
        }
        return keyEntityMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Key, Entity> getAndCache( Transaction txn, Iterable<Key> keys ) {
        Map<Key, Entity> keyEntityMap;
        try {
            keyEntityMap = datastore.get( txn, keys );
        } catch ( DatastoreTimeoutException e ) {
            keyEntityMap = datastore.get( txn, keys );
        }
        memcache.putAll( (Map)mapKeyToString( keyEntityMap ), expiration, SetPolicy.SET_ALWAYS );
        return keyEntityMap;
    }
    
    private static Map<String, Entity> mapKeyToString( Map<Key, Entity> keyEntityMap ) {
        Map<String, Entity> stringEntityMap = new HashMap<String, Entity>();
        for ( Entity entity : keyEntityMap.values() ) {
            stringEntityMap.put( keyToString( entity.getKey() ), entity );
        }
        return stringEntityMap;
    }
    
    public Key put( Entity entity ) {
        Key key = entity.getKey();
        if ( !key.isComplete() ) {
            // TODO: this path has not been tested
            KeyRange keyRange = datastore.allocateIds( key.getParent(), key.getKind(), 1 );
            key = keyRange.getStart();
        }
        String keyString = keyToString( key );
        memcache.put( keyString, entity, expiration, SetPolicy.SET_ALWAYS );
        try {
            queue.add( url( TASK_URL ).param( KEY_PARAM, keyString ) );
            log.info( key.toString() );
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
    
    @SuppressWarnings("unchecked")
    public List<Key> put( Iterable<Entity> entities ) {
        List<Key> keyList = new ArrayList<Key>();
        Map<String, Entity> stringEntityMap = new HashMap<String, Entity>();
        TaskOptions taskOptions = url( TASK_URL );
        int i = 0;
        for ( Entity entity : entities ) {
            Key key = entity.getKey();
            if ( !key.isComplete() ) {
                // TODO: needs to be implemented
            }
            keyList.add( key );
            String keyString = keyToString( key );
            stringEntityMap.put( keyString, entity );
            taskOptions.param( KEY_PARAM + i++, keyString );
            log.info( key.toString() );
        }
        memcache.putAll( (Map)stringEntityMap, expiration, SetPolicy.SET_ALWAYS );
        try {
            queue.add( taskOptions );
            return keyList;
        } catch ( Exception e ) {
            log.warning( e.getMessage() );
            try {
                return datastore.put( entities );
            } catch ( DatastoreTimeoutException t ) {
                return datastore.put( entities );
            }
        }
    }
    
    /**
     * Don't use write-behind cache with transactions.
     */
    public Key put( Transaction txn, Entity entity ) {
        return datastore.put( txn, entity );
    }

    public List<Key> put( Transaction txn, Iterable<Entity> entities ) {
        return datastore.put( txn, entities );
    }
    
    public void delete( Key ... keys ) {
        delete( null, Arrays.asList( keys ) );
    }
    
    public void delete( Iterable<Key> keys ) {
        delete( null, keys );
    }
    
    public void delete( Transaction txn, Key ... keys ) {
        delete( txn, Arrays.asList( keys ) ); 
    }

    public void delete( Transaction txn, Iterable<Key> keys ) {
        try {
            datastore.delete( txn, keys );
        } catch ( DatastoreTimeoutException e ) {
            datastore.delete( txn, keys );
        }
        memcache.deleteAll( keysToStrings( keys ) );
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

    // TODO: is there a way to cache query results?
    public PreparedQuery prepare( Query query ) {
        return datastore.prepare( query );
    }

    public PreparedQuery prepare( Transaction txn, Query query ) {
        return datastore.prepare( txn, query );
    }
    
    /***************************************************************************
     *                                                                         *
     *               W R I T E - B E H I N D   T A S K                         *
     *                                                                         *
     ***************************************************************************/
    
    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
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
        List<String> keys = new ArrayList<String>();
        Enumeration<String> paramNames = req.getParameterNames();
        while ( paramNames.hasMoreElements() ) {
            // parameters can have only a single value (see issue #2090)
            keys.add( req.getParameter( paramNames.nextElement() ) );
        }
        Map<String, Entity> stringEntityMap = (Map)memcache.getAll( (List)keys );
        if ( !stringEntityMap.isEmpty() ) {
            // TODO: using transactions should be optional
            Transaction txn = datastore.beginTransaction();
            try {
                datastore.put( txn, stringEntityMap.values() );
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
