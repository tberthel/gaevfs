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

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.labs.taskqueue.QueueConstants.maxTaskSizeBytes;
import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;
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
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;

/**
 * Implements deferred tasks for GAE/J, based on the
 * <a href="http://code.google.com/appengine/articles/deferred.html">Python deferred
 * library</a>.
 *    
 * First, the deferred task handler (servlet) needs to be configured within
 * <code>web.xml</code>:
 * <pre>
 * &lt;servlet>
 *     &lt;servlet-name>Deferred&lt;/servlet-name>
 *     &lt;servlet-class>com.newatlanta.appengine.taskqueue.Deferred&lt;/servlet-class>
 * &lt;/servlet>
 * &lt;servlet-mapping>
 *     &lt;servlet-name>Deferred&lt;/servlet-name>
 *     &lt;url-pattern>/_ah/queue/deferred&lt;/url-pattern>
 * &lt;/servlet-mapping>
 * </pre>
 * <p>
 * Second, the "deferred" queue needs to be configured within <code>queue.xml</code>
 * (use whatever rate you want):
 * <pre>
 * &lt;queue>
 *     &lt;name>deferred&lt;/name>
 *     &lt;rate>10/s&lt;/rate>
 * &lt;/queue>
 * </pre>
 * <p>
 * Next, create a class that implements the
 * <code>com.newatlanta.appengine.taskqueue.Deferred.Deferrable</code> interface;
 * the <code>doTask</code> method of this class is where you implement your task
 * logic.
 * <p>
 * Finally, invoke the <code>Deferred.defer</code> method to queue up your task:
 * <pre>
 * DeferredTask task = new DeferredTask(); // implements Deferrable
 * Deferred.defer( task );
 * </pre>
 * <p>
 * If the task size exceeds 10KB, the task options are stored within a datastore
 * entity, which is deleted when the task is executed.
 * <p>
 * Your <code>doTask</code> method can throw a <code>PermanentTaskFailure</code>
 * exception to halt retries; any other exceptions cause the task to be retried.
 *    
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
@SuppressWarnings("serial")
public class Deferred extends HttpServlet {
    
    private static final String QUEUE_NAME = "deferred";
    private static final String TASK_CONTENT_TYPE = "application/x-java-serialized-object";
    private static final String ENTITY_KIND = Deferred.class.getName();
    private static final String TASK_PROPERTY = "taskBytes";
    
    private static final Logger log = Logger.getLogger( Deferred.class.getName() );
    
    /**
     * The <code>Deferrable</code> should be implemented by any class whose instances
     * are intended to be executed as background tasks. The class must define a method
     * of no arguments called <code>doTask</code>.
     */
    public interface Deferrable extends Serializable {
        /**
         * Invoked to perform the background task.
         * 
         * @throws PermanentTaskFailure To indicate that the task should not be
         * retried. These exceptions are logged.
         * 
         * @throws ServletException To indicate that the task should be retried.
         * These exceptions are not logged.
         * 
         * @throws IOException To indicate that the task should be retried. These
         * exceptions are not logged.
         */
        public void doTask() throws ServletException, IOException;
    }
    
    /**
     * Used to indicate that a task should not be retried.
     */
    public class PermanentTaskFailure extends ServletException {
        /**
         * Constructs a new exception with the specified detail message.
         * 
         * @param message The detailed message.
         */
        public PermanentTaskFailure( String message ) {
            super( message );
        }
    }
    
    /**
     * Queue a task for background execution using the "deferred" queue.
     * 
     * @param task The task to be executed.
     * @throws IOException If an error occurs serializing the task.
     */
    public static void defer( Deferrable task ) throws IOException {
        defer( task, QUEUE_NAME );
    }
    
    /**
     * Queue a task for background execution using the specified queue.
     * 
     * @param task The task to be executed.
     * @param queueName The queue to be used.
     * @throws IOException If an error occurs serializing the task.
     */
    public static void defer( Deferrable task, String queueName ) throws IOException {
        byte[] taskBytes = serialize( task );
        if ( taskBytes.length <= maxTaskSizeBytes() ) {
            try {
                queueTask( taskBytes, queueName );
                return;
            } catch ( IllegalArgumentException e ) {
                log.warning( e.getMessage() + ": " + taskBytes.length );
                // task size too large, fall through
            }
        }
        // create a datastore entity and add its key as the task payload
        Entity entity = new Entity( ENTITY_KIND );
        entity.setProperty( TASK_PROPERTY, new Blob( taskBytes ) );
        Key key = getDatastoreService().put( entity );
        log.info( "put datastore key: " + key );
        try {
            queueTask( serialize( key ), queueName );
        } catch ( IOException e ) {
            deleteEntity( key ); // delete entity if error queuing task
            throw e;
        } catch ( RuntimeException e ) {
            deleteEntity( key ); // delete entity if error queuing task
            throw e;
        }
    }
    
    /**
     * Add a task to the queue.
     * 
     * @param taskBytes The payload for the task.
     */
    private static void queueTask( byte[] taskBytes, String queueName ) {
        getQueue( queueName ).add( payload( taskBytes, TASK_CONTENT_TYPE ) );
    }
    
    /**
     * Execute the background task.
     * 
     * The task payload is either type Deferrable or Key; in the latter case,
     * retrieve (then delete) the Deferrable instance from the datastore.
     */
    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        try {
            Object payload = deserialize( req );
            if ( payload instanceof Key ) {
                // get Deferrable from datastore
                Blob taskBlob = (Blob)getDatastoreService().get(
                                    (Key)payload ).getProperty( TASK_PROPERTY );
                deleteEntity( (Key)payload );
                if ( taskBlob != null ) {
                    payload = deserialize( taskBlob.getBytes() );
                }
            }
            if ( payload instanceof Deferrable ) {
                ((Deferrable)payload).doTask();
            } else if ( payload != null ) {
                log.severe( "invalid payload type: " + payload.getClass().getName() );
                // don't retry task
            }
        } catch ( EntityNotFoundException e ) {
            log.severe( e.toString() ); // don't retry task
        } catch ( PermanentTaskFailure e ) {
            log.severe( e.toString() ); // don't retry task
        } 
    }
    
    /**
     * Delete a datastore entity.
     * 
     * @param key The key of the entity to delete.
     */
    private static void deleteEntity( Key key ) {
        try {
            getDatastoreService().delete( key );
            log.info( "deleted datastore key: " + key );
        } catch ( DatastoreFailureException e ) {
            log.warning( "failed to delete datastore key: " + key );
            log.warning( e.toString() );
        }
    }
    
    /**
     * Serialize an object into a byte array.
     * 
     * @param obj An object to be serialized.
     * @return A byte array containing the serialized object
     * @throws IOException If an I/O error occurs during the serialization process.
     */
    private static byte[] serialize( Object obj ) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream( 
                                            new BufferedOutputStream( bytesOut ) );
        objectOut.writeObject( obj );
        objectOut.close();
        return bytesOut.toByteArray();
    }
    
    /**
     * Deserialize an object from an HttpServletRequest input stream. Does not
     * throw any exceptions; instead, exceptions are logged and null is returned.
     * 
     * @param req An HttpServletRequest that contains a serialized object.
     * @return An object instance, or null if an exception occurred.
     */
    private static Object deserialize( HttpServletRequest req ) {
        if ( req.getContentLength() == 0 ) {
            log.severe( "request content length is 0" );
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

    /**
     * Deserialize an object from a byte array. Does not throw any exceptions;
     * instead, exceptions are logged and null is returned.
     * 
     * @param bytesIn A byte array containing a previously serialized object.
     * @return An object instance, or null if an exception occurred.
     */
    private static Object deserialize( byte[] bytesIn ) {
        ObjectInputStream objectIn = null;
        try {
            objectIn = new ObjectInputStream( new BufferedInputStream(
                                        new ByteArrayInputStream( bytesIn ) ) );
            return objectIn.readObject();
        } catch ( IOException e ) {
            log.severe( e.toString() );
            return null;
        } catch ( ClassNotFoundException e ) {
            log.severe( e.toString() );
            return null;
        } finally {
            try {
                if ( objectIn != null ) {
                    objectIn.close();
                }
            } catch ( IOException ignore ) {
            }
        }
    }
}
