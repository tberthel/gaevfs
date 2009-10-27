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
package com.newatlanta.appengine.nio.channels;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.newatlanta.appengine.locks.ExclusiveLock;
import com.newatlanta.repackaged.java.nio.channels.FileLock;

/**
 * Implements {@linkplain com.newatlanta.repackaged.java.nio.channels.FileLock} for GaeVFS.
 * 
 * <p>Platform dependencies:
 * <ul>
 * <li>Shared locks are not supported; a request for a shared lock is automatically
 * converted into a request for an exclusive lock.</li>
 * 
 * <p><li>Locking of regions is not supported; locks must be requested for the entire
 * file.</li>
 * 
 * <p><li>Locks are advisory; programs must cooperatively observe the locking
 * protocol, and are not automatically prevented from violating the locks.</li>
 * </ul>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileLock extends FileLock {
    
    private static Map<String, GaeFileLock> fileLocks =
            Collections.synchronizedMap( new HashMap<String, GaeFileLock>() );

    private String name;
    private Lock lock;
    private boolean isValid;
    
    /**
     * Validates position and size arguments, makes sure file channel is open,
     * and makes sure no other thread within this JVM already owns the lock or
     * is attempting to acquire it.
     */
    GaeFileLock( GaeFileChannel fileChannel, long position, long size ) throws IOException {
        super( fileChannel, position, size, false ); // validates position and size
        if ( !isEntireFile( position, size ) ) {
            throw new UnsupportedOperationException( "Region locking is not supported." );
        } 
        fileChannel.checkOpen();
        name = fileChannel.getLockName();
        
        // make sure no other thread own the lock or is attempting to acquire it
        synchronized( fileLocks ) {
            if ( fileLocks.containsKey( name ) ) {
                throw new OverlappingFileLockException();
            }
            fileLocks.put( name, this );
        }
        
        lock = new ExclusiveLock( name );
    }
    
    public boolean isEntireFile() {
        return isEntireFile( position(), size() );
    }
    
    private static boolean isEntireFile( long position, long size ) {
        return ( ( position == 0L ) && ( size == Long.MAX_VALUE ) );
    }
    
    synchronized boolean tryLock() {
        if ( !isValid && ( lock != null ) ) {
            isValid = lock.tryLock();
        }
        return isValid();
    }

    @Override
    public synchronized boolean isValid() {
        return ( isValid && ( lock != null ) );
    }

    @Override
    public synchronized void release() {
        if ( isValid() ) {
            lock.unlock();
            isValid = false;
            lock = null;
        }
        fileLocks.remove( name );
    }
    
    static void releaseAllLocks( GaeFileChannel fileChannel ) {
        synchronized( fileLocks ) {
            for ( GaeFileLock lock : fileLocks.values() ) {
                if ( ( fileChannel == null ) ||
                        ( lock.acquiredBy() == fileChannel ) ) {
                    lock.release();
                }
            }
        }
    }
    
    public static void releaseAllLocks() {
        releaseAllLocks( null );
        fileLocks.clear();
    }
}
