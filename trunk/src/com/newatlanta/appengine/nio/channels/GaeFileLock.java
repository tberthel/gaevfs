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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;

public class GaeFileLock extends FileLock {
    
    private static Map<String, List<GaeFileLock>> fileLocks =
        Collections.synchronizedMap( new HashMap<String, List<GaeFileLock>>() );

    private Lock lock;
    private String name;
    
    GaeFileLock( GaeFileChannel fileChannel, long position, long size,
                    boolean shared ) throws IOException
    {
        super( fileChannel, position, size, shared ); // validates position and size
        fileChannel.checkOpen();
        name = fileChannel.getLockName();
        
        // multiple locks--whether exclusive or shared--must not overlap
        synchronized ( fileLocks ) {
            List<GaeFileLock> lockList = fileLocks.get( name );
            if ( lockList != null ) {
                for ( GaeFileLock lock : lockList ) {
                    if ( lock.overlaps( position, size ) ) {
                        throw new OverlappingFileLockException();
                    }
                }
            } else {
                lockList = new ArrayList<GaeFileLock>();
                fileLocks.put( name, lockList );
            }
            lockList.add( this );
        }
    }
    
    boolean isEntireFile() {
        return ( ( position() == 0L ) && ( size() == Long.MAX_VALUE ) );
    }
    
    FileLock acquired( Lock lock ) {
        this.lock = lock;
        return this;
    }

    @Override
    public boolean isValid() {
        return ( lock != null );
    }

    @Override
    public void release() throws IOException {
        synchronized ( fileLocks ) {
            List<GaeFileLock> lockList = fileLocks.get( name );
            if ( lockList != null ) {
                lockList.remove( this );
                if ( lockList.isEmpty() ) {
                    fileLocks.remove( name );
                }
            }
        }
        if ( lock != null ) {
            lock.unlock();
            lock = null;
        }
    }
    
    static void releaseAll( FileChannel fileChannel ) throws IOException {
        synchronized( fileLocks ) {
            for ( List<GaeFileLock> lockList : fileLocks.values() ) {
                // use array to avoid ConcurrentModificationException
                GaeFileLock[] locks = lockList.toArray( new GaeFileLock[ lockList.size() ] );
                for ( GaeFileLock fileLock : locks ) {
                    if ( ( fileChannel == null ) ||
                            ( fileLock.acquiredBy() == fileChannel ) ) {
                        fileLock.release();
                    }
                }
            }
        }
    }
    
    public static void releaseAll() throws IOException {
        releaseAll( null );
        fileLocks.clear();
    }
}
