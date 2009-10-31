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
package com.newatlanta.appengine.taskqueue;

import static com.google.appengine.api.labs.taskqueue.QueueConstants.maxTaskSizeBytes;
import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.payload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;

/**
 * Implements deferred tasks, based on the Python implementation:
 * 
 *    http://code.google.com/appengine/articles/deferred.html
 *    
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
@SuppressWarnings("serial")
public class Deferred extends HttpServlet {
    
    private static final String QUEUE_NAME = "deferred";
    private static final String TASK_CONTENT_TYPE = "application/x-java-serialized-object";
    private static final String ENTITY_KIND = Deferred.class.getName();
    private static final String TASK_PROPERTY = "taskBytes";
    
    private static final Queue queue = QueueFactory.getQueue( QUEUE_NAME );
    private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static final Logger log = Logger.getLogger( Deferred.class.getName() );
    
    public interface Deferrable extends Serializable {
        public void doTask( Object ... args ) throws PermanentTaskFailure;
    }
    
    public class PermanentTaskFailure extends Exception {
        public PermanentTaskFailure( String message ) {
            super( message );
        }
    }
    
    public static void defer( Deferrable target, Object ... args ) throws IOException {
        // serialize the target and arguments
        byte[] taskBytes = serialize( new TaskHolder( target, args ) );
        if ( taskBytes.length <= maxTaskSizeBytes() ) {
            queue.add( payload( taskBytes, TASK_CONTENT_TYPE ) );
        } else {
            // create a datastore entity and add its key as the task payload
            Entity entity = new Entity( ENTITY_KIND );
            entity.setProperty( TASK_PROPERTY, new Blob( taskBytes ) );
            Key key = datastore.put( entity );
            log.info( "put datastore entity: " + key );
            queue.add( payload( serialize( key ), TASK_CONTENT_TYPE ) );
        } 
    }
    
    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res )
            throws ServletException {
        try {
            Object payload = deserialize( req );
            if ( payload instanceof Key ) {
                // get TaskHolder from datastore
                Blob taskBlob = (Blob)datastore.get(
                                        (Key)payload ).getProperty( TASK_PROPERTY );
                datastore.delete( (Key)payload );
                log.info( "deleted datastore entity: " + (Key)payload );
                if ( taskBlob != null ) {
                    payload = deserialize( taskBlob.getBytes() );
                }
            }
            if ( payload instanceof TaskHolder ) {
                ((TaskHolder)payload).target.doTask( ((TaskHolder)payload).args );
            }
        } catch ( EntityNotFoundException e ) {
            log.severe( e.toString() ); // don't retry task
        } catch ( PermanentTaskFailure e ) {
            log.severe( e.toString() ); // don't retry task
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
    
    private static Object deserialize( HttpServletRequest req ) {
        if ( req.getContentLength() == 0 ) {
            return null;
        }
        try {
            byte[] bytesIn = new byte[ req.getContentLength() ];
            req.getInputStream().readLine( bytesIn, 0, bytesIn.length );
            return deserialize( bytesIn );
        } catch ( IOException e ) {
            log.severe( e.toString() );
            return null; // don't retry task
        }
    }

    private static Object deserialize( byte[] bytesIn ) {
        ObjectInputStream objectIn = null;
        try {
            objectIn = new ObjectInputStream( new BufferedInputStream(
                                        new ByteArrayInputStream( bytesIn ) ) );
            return objectIn.readObject();
        } catch ( IOException e ) {
            log.severe( e.toString() );
            return null; // don't retry task
        } catch ( ClassNotFoundException e ) {
            log.severe( e.toString() );
            return null; // don't retry task
        } finally {
            try {
                if ( objectIn != null ) {
                    objectIn.close();
                }
            } catch ( IOException ignore ) {
            }
        }
    }
    
    private static class TaskHolder implements Serializable {
        private Deferrable target;
        private Object[] args;
    
        private TaskHolder( Deferrable target, Object[] args ) {
            this.target = target;
            this.args = args;
        }
    }
}
