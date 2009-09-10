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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;
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
     * WARNING! Don't rely on the ordering of entities returned by this method.
     */
    public Map<Key, Entity> get( Iterable<Key> keys ) {
        return get( null, keys );
    }
    
    
    @SuppressWarnings("unchecked")
    public Map<Key, Entity> get( Transaction txn, Iterable<Key> keys ) {
        // TODO: this method has not been tested
        Map<Key, Entity> entities = (Map)memcache.getAll( (Collection)keys );
        if ( entities.isEmpty() ) {
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
        memcache.put( key, entity, expiration, SetPolicy.SET_ALWAYS );
        if ( cacheOption == CacheOption.WRITE_BEHIND ) {
            try {
                byte[] objectBytes = serialize( key );
                // TODO: fix this when issue #2097 is resolved
//                String contentType = "application/x-java-serialized-object";
//                queue.add( url( TASK_URL ).payload( objectBytes, contentType ) );
                queue.add( url( TASK_URL ).method( Method.GET ).param( "payload", objectBytes ) );
                log.info( key.toString() );
                return key;
            } catch ( Exception e ) {
                log.warning( e.getMessage() );
            }
        }
        // WRITE_THROUGH, or failed to queue write-behind task
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
        memcache.putAll( (Map)entityMap, expiration, SetPolicy.SET_ALWAYS );
        if ( cacheOption == CacheOption.WRITE_BEHIND ) {
            try {
                // serialize key set for task options payload
                List<Key> keyList = new ArrayList<Key>( entityMap.keySet() );
                byte[] objectBytes = serialize( keyList );
                // TODO: fix this when issue #2097 is resolved
//                String contentType = "application/x-java-serialized-object";
//                queue.add( url( TASK_URL ).payload( objectBytes, contentType ) );
                queue.add( url( TASK_URL ).method( Method.GET ).param( "payload", objectBytes ) );
                return keyList;
            } catch ( Exception e ) {
                log.warning( e.getMessage() );
            }
        }
        // WRITE_THROUGH, or failed to queue write-behind task
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
        Object payload = null;
        try {
            payload = deserialize( req );
            if ( payload == null ) {
                return;
            }
        } catch ( Exception e ) {
            log.warning( e.getMessage() );
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
        if ( !entityMap.isEmpty() ) {
            // TODO: using transactions should be optional
            Transaction txn = datastore.beginTransaction();
            try {
                datastore.put( txn, entityMap.values() );
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
    
    private static Object deserialize( HttpServletRequest req ) throws IOException {
       // TODO: fix this when issue #2097 is resolved
//        byte[] bytesIn = new byte[ req.getContentLength() ];
//        req.getInputStream().readLine( bytesIn, 0, bytesIn.length );
        String qs = req.getQueryString();
        if ( qs == null ) {
            return null;
        }
        byte[] bytesIn = decode( qs.substring( "payload=".length() ) );
        ObjectInputStream objectIn = new ObjectInputStream( new BufferedInputStream( 
                                        new ByteArrayInputStream( bytesIn ) ) );
        try {
            return objectIn.readObject();
        } catch ( ClassNotFoundException e ) {
            log.warning( e.getMessage() );
            return null;
        } finally {
            objectIn.close();
        }
    }
    
    /*
     * This method copied from java.net.URLDecoder and modified to return a byte
     * array instead of a string. Also, it only works for strings with *every* 
     * character is hex-encoded.
     * 
     * Copyright 1998-2006 Sun Microsystems, Inc.  All Rights Reserved.
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
     *
     * This code is free software; you can redistribute it and/or modify it
     * under the terms of the GNU General Public License version 2 only, as
     * published by the Free Software Foundation.  Sun designates this
     * particular file as subject to the "Classpath" exception as provided
     * by Sun in the LICENSE file that accompanied this code.
     *
     * This code is distributed in the hope that it will be useful, but WITHOUT
     * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
     * version 2 for more details (a copy is included in the LICENSE file that
     * accompanied this code).
     *
     * You should have received a copy of the GNU General Public License version
     * 2 along with this work; if not, write to the Free Software Foundation,
     * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
     *
     * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
     * CA 95054 USA or visit www.sun.com if you need additional information or
     * have any questions.
     */
    public static byte[] decode( String s ) {
        int numChars = s.length();
        ByteBuffer buff = ByteBuffer.allocate( numChars / 3 );
        int i = 0;
        char c = s.charAt( i );
        /*
         * Starting with this instance of %, process all consecutive substrings of
         * the form %xy. Each substring %xy will yield a byte.
         */
        try {
            // (numChars-i)/3 is an upper bound for the number of remaining bytes
            while ( ( ( i + 2 ) < numChars ) && ( c == '%' ) ) {
                buff.put( (byte)Integer.parseInt( s.substring( i + 1, i + 3 ), 16 ) );
                i += 3;
                if ( i < numChars ) {
                    c = s.charAt( i );
                }
            }
            // A trailing, incomplete byte encoding such as "%x" will cause an exception to be thrown
            if ( ( i < numChars ) && ( c == '%' ) ) {
                throw new IllegalArgumentException( "URLDecoder: Incomplete trailing escape (%) pattern" );
            }
        } catch ( NumberFormatException e ) {
            throw new IllegalArgumentException( "URLDecoder: Illegal hex characters in escape (%) pattern - "
                    + e.getMessage() );
        }
        return buff.array();
    }
}
