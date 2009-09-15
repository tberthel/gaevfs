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

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.google.appengine.api.labs.taskqueue.TaskAlreadyExistsException;
import com.google.appengine.api.labs.taskqueue.TaskHandle;
import com.google.appengine.api.labs.taskqueue.TransientFailureException;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.StrictErrorHandler;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/**
 * Implements a <code>DatastoreService</code> that automatically caches entities
 * in memcache. Can be configured to use either a write-through or write-behind
 * strategy; write-behind is based on task queues.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
@SuppressWarnings("serial")
public class CachingDatastoreService extends HttpServlet implements DatastoreService {
    
    private static final String QUEUE_NAME = "write-behind-task";
    private static final String TASK_URL = "/_ah/queue/" + QUEUE_NAME;
    private static final String TASK_CONTENT_TYPE = "application/x-java-serialized-object";
    private static final String WATCHDOG_KEY = "CachingDatastoreService.watchdog";
    
    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static Queue queue;
    
    private static final ErrorHandler STRICT_ERROR_HANDLER = new StrictErrorHandler();
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = memcache.getErrorHandler();
    
    private static final Logger log = Logger.getLogger( CachingDatastoreService.class.getName() );
    
    static {
        try {
            queue = QueueFactory.getQueue( QUEUE_NAME );
            queueWatchDogTask( 0, UUID.randomUUID().toString(),
                                                UUID.randomUUID().toString() );
        } catch ( Exception e ) {
            log.warning( e.getMessage() + " " + QUEUE_NAME );
        }
    }
    
    public enum CacheOption {
        WRITE_THROUGH, WRITE_BEHIND 
    }
    
    private CacheOption cacheOption;
    private Expiration expiration;
    
    public CachingDatastoreService() {
        this( CacheOption.WRITE_BEHIND, null );
    }
    
    public CachingDatastoreService( CacheOption cacheOption, Expiration expiration ) {
        this.expiration = expiration;
        this.cacheOption = cacheOption;
    }
    
    /**
     * Gets an entity. Returns the entity from memcache, if it exists; otherwise,
     * gets the entity from the datastore, puts it into memcache, then returns it.
     */
    public Entity get( Key key ) throws EntityNotFoundException {
        return get( null, key );
    }
    
    public Entity get( Transaction txn, Key key ) throws EntityNotFoundException {
        Entity entity = (Entity)memcache.get( key );
        if ( entity == null ) {
            try {
                entity = datastore.get( txn, key );
            } catch ( DatastoreTimeoutException e ) {
                entity = datastore.get( txn, key );
            }
            memcache.put( key, entity, expiration, SetPolicy.SET_ALWAYS );
        }
        return entity;
    }
    
    /**
     * Don't rely on the ordering of entities returned by this method.
     */
    public Map<Key, Entity> get( Iterable<Key> keys ) {
        return get( null, keys );
    }
    
    @SuppressWarnings("unchecked")
    public Map<Key, Entity> get( Transaction txn, Iterable<Key> keys ) {
        Map<Key, Entity> entities = (Map)memcache.getAll( (Collection)keys );
        if ( ( entities == null ) || entities.isEmpty() ) {
            return getAndCache( txn, keys );
        }
        if ( entities.size() < ((Collection)keys).size() ) {
            List<Key> keyList = new ArrayList<Key>();
            for ( Key key : keys ) {
                if ( !entities.containsKey( key ) ) {
                    keyList.add( key );
                }
            }
            entities.putAll( getAndCache( txn, keyList ) );
        }
        return entities;
    }

    @SuppressWarnings("unchecked")
    private Map<Key, Entity> getAndCache( Transaction txn, Iterable<Key> keys ) {
        Map<Key, Entity> entities;
        try {
            entities = datastore.get( txn, keys );
        } catch ( DatastoreTimeoutException e ) {
            entities = datastore.get( txn, keys );
        }
        memcache.putAll( (Map)entities, expiration, SetPolicy.SET_ALWAYS );
        return entities;
    }
    
    public Key put( Entity entity ) {
        Key key = entity.getKey();
        if ( !key.isComplete() ) {
            // TODO: this path has not been tested
            KeyRange keyRange = datastore.allocateIds( key.getParent(), key.getKind(), 1 );
            key = keyRange.getStart();
        }
        try {
            memcache.setErrorHandler( STRICT_ERROR_HANDLER );
            memcache.put( key, entity, expiration, SetPolicy.SET_ALWAYS );
            if ( ( cacheOption == CacheOption.WRITE_BEHIND ) && watchDogIsAlive() ) {
                queue.add( url( TASK_URL ).payload( serialize( key ), TASK_CONTENT_TYPE ) );
                log.info( key.toString() );
                return key;
            }
        } catch ( Exception e ) {
            log.warning( e.getCause() != null ? e.getCause().getMessage() 
                                              : e.getMessage() );
        } finally {
            memcache.setErrorHandler( DEFAULT_ERROR_HANDLER );
        }
        // if WRITE_THROUGH, or failed to write memcache, or failed to queue
        // write-behind task, then write directly to datastore
        try {
            return datastore.put( entity );
        } catch ( DatastoreTimeoutException t ) {
            return datastore.put( entity );
        }
    }

    @SuppressWarnings("unchecked")
    public List<Key> put( Iterable<Entity> entities ) {
        // create Map<Key, Entity> for memcache putAll()
        Map<Key, Entity> entityMap = new HashMap<Key, Entity>();
        for ( Entity entity : entities ) {
            Key key = entity.getKey();
            if ( !key.isComplete() ) {
                // TODO: to be implemented
            }
            entityMap.put( key, entity );
            log.info( key.toString() );
        }
        try {
            memcache.setErrorHandler( STRICT_ERROR_HANDLER );
            memcache.putAll( (Map)entityMap, expiration, SetPolicy.SET_ALWAYS );
            if ( ( cacheOption == CacheOption.WRITE_BEHIND ) && watchDogIsAlive() ) {
                List<Key> keyList = new ArrayList<Key>( entityMap.keySet() );
                queue.add( url( TASK_URL ).payload( serialize( keyList ), TASK_CONTENT_TYPE ) );
                return keyList;
            }
        } catch ( Exception e ) {
            log.warning( e.getCause() != null ? e.getCause().getMessage() 
                                              : e.getMessage() );
        } finally {
            memcache.setErrorHandler( DEFAULT_ERROR_HANDLER );
        }
        // if WRITE_THROUGH, or failed to write memcache, or failed to queue
        // write-behind task, then write directly to datastore
        try {
            return datastore.put( entities );
        } catch ( DatastoreTimeoutException t ) {
            return datastore.put( entities );
        }
    }
    
    private static byte[] serialize( Object object ) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream( 
                                            new BufferedOutputStream( bytesOut ) );
        objectOut.writeObject( object );
        objectOut.close();
        return bytesOut.toByteArray();
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

    @SuppressWarnings("unchecked")
    public void delete( Transaction txn, Iterable<Key> keys ) {
        try {
            datastore.delete( txn, keys );
        } catch ( DatastoreTimeoutException e ) {
            datastore.delete( txn, keys );
        }
        memcache.deleteAll( (Collection)keys );
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

    // TODO: cache query results?
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
    
    private static int WATCHDOG_SECONDS = 60; // TODO: make configurable
    
    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        /**
         * The watchdog task creates a memcache key that expires at twice the
         * interval at which the task itself runs. If the key doesn't exist,
         * the task isn't running.
         */
        String urlToken = req.getParameter( "watchdog" );
        if ( urlToken != null ) {
            // make sure this task owns the token; if not, terminate
            Object memcacheToken = memcache.get( WATCHDOG_KEY );
            if ( ( memcacheToken == null ) || urlToken.equals( memcacheToken ) ) {
                String nextToken = UUID.randomUUID().toString(); // for next task
                // use the previous token as the next task name to prevent multiple
                // tasks from being queued; use a new token for the next task
                queueWatchDogTask( WATCHDOG_SECONDS * 1000, urlToken, nextToken );
                memcache.delete( WATCHDOG_KEY ); // reset the timer
                memcache.put( WATCHDOG_KEY, nextToken, Expiration.byDeltaSeconds(
                                                        WATCHDOG_SECONDS * 2 ) );
                log.info( "watchdog is alive" );
            }
        } else {
            doWriteBehindTask( req, res );
        }
    }
    
    private static TaskHandle queueWatchDogTask( long countdownMillis, String taskName,
                                                    String nextToken ) {
        for ( int i = 0; i < 100; i++ ) {
            try {
                return queue.add( url( TASK_URL ).method( Method.GET ).taskName(
                        taskName ).param( "watchdog", nextToken ).countdownMillis(
                                countdownMillis ) );
            } catch ( TaskAlreadyExistsException e ) {
                log.info( e.getMessage() + " " + taskName );
                return null;
            } catch ( TransientFailureException e ) {
                log.warning( e.getMessage() + " " + taskName );
                // repeat loop
            }
        }
        return null;
    }
    
    private static boolean watchDogIsAlive() {
        if ( !memcache.contains( WATCHDOG_KEY ) ) {
            log.warning( "write-behind task not alive" );
            return false;
        }
        return true;
    }

    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        doWriteBehindTask( req, res );
    }

    @SuppressWarnings("unchecked")
    private static void doWriteBehindTask( HttpServletRequest req, HttpServletResponse res )
            throws IOException {
        Object payload = null;
        try {
            payload = deserialize( req );
            if ( payload == null ) {
                return;
            }
        } catch ( Exception e ) {
            log.warning( e.toString() );
            return;
        }
        List<Key> keys;
        if ( payload instanceof Key ) {
            keys = new ArrayList<Key>();
            keys.add( (Key)payload );
        } else if ( payload instanceof List ) {
            keys = (List)payload;
        } else {
            log.warning( payload.getClass().getName() );
            return;
        }
        Map<String, Entity> entityMap = (Map)memcache.getAll( (List)keys );
        if ( ( entityMap != null ) && !entityMap.isEmpty() ) {
            try {
                if ( datastore.put( entityMap.values() ).size() != entityMap.size() ) {
                    log.info( "failed to write all entities - retrying" );
                    res.sendError( HttpServletResponse.SC_PARTIAL_CONTENT );
                }
            } catch ( DatastoreTimeoutException e ) { // retry task
                log.info( e.getMessage() );
                res.sendError( HttpServletResponse.SC_REQUEST_TIMEOUT );
            } catch ( ConcurrentModificationException e ) { // retry task
                log.info( e.getMessage() );
                res.sendError( HttpServletResponse.SC_CONFLICT );
            } catch ( Exception e ) { // don't retry
                log.warning( e.toString() );
            }
        }
    }
    
    private static Object deserialize( HttpServletRequest req ) throws Exception {
        if ( req.getContentLength() == 0 ) {
            return null;
        }
        byte[] bytesIn = new byte[ req.getContentLength() ];
        req.getInputStream().readLine( bytesIn, 0, bytesIn.length );
        ObjectInputStream objectIn = new ObjectInputStream( new BufferedInputStream( 
                                        new ByteArrayInputStream( bytesIn ) ) );
        try {
            return objectIn.readObject();
        } finally {
            objectIn.close();
        }
    }
}
