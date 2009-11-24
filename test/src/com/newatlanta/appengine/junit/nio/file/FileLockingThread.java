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
package com.newatlanta.appengine.junit.nio.file;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import com.newatlanta.appengine.locks.ExclusiveLock;
import com.newatlanta.appengine.nio.channels.GaeFileChannel;

/**
 * Simulates lock acquisition by a thread in a different JVM.
 * 
 * Acquires the specified lock, then sleeps for the specified time or until
 * interrupted. Upon resuming, either closes the channel or releases the lock.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class FileLockingThread extends Thread {
    
    private GaeFileChannel fileChannel;
    private long sleepTime;
    private boolean closeChannel;
    private Thread interruptThread;
    
    public static FileLockingThread createThread( GaeFileChannel fileChannel ) {
        return createThread( fileChannel, Long.MAX_VALUE, false, null );
    }
    
    public static FileLockingThread createThread( GaeFileChannel fileChannel,
            long sleepTime, Thread interruptThread ) {
        return createThread( fileChannel, sleepTime, false, interruptThread );
    }
    
    public static FileLockingThread createThread( GaeFileChannel fileChannel,
            long sleepTime, boolean closeChannel ) {
        return createThread( fileChannel, sleepTime, closeChannel, null );
    }
    
    public static FileLockingThread createThread( GaeFileChannel fileChannel,
            long sleepTime, boolean closeChannel, Thread interruptThread )
    {
        FileLockingThread lockThread = new FileLockingThread( fileChannel,
                                        sleepTime, closeChannel, interruptThread );
        lockThread.start();
        try {
            do {
                Thread.sleep( 100 ); // give lockThread a chance to run
            } while ( !lockThread.isAlive() );
        } catch ( InterruptedException e ) {
        }
        return lockThread;
    }
    
    private FileLockingThread( GaeFileChannel fileChannel, long sleepTime,
                                    boolean closeChannel, Thread interruptThread ) {
        super( "FileLockThread" );
        this.fileChannel = fileChannel;
        this.sleepTime = sleepTime;
        this.closeChannel = closeChannel;
        this.interruptThread = interruptThread;
    }

    @Override
    public void run() {
        try {
            Lock lock = new ExclusiveLock( fileChannel.getLockName() );
            lock.lock();
            try {
                sleep( sleepTime );
            } catch ( InterruptedException e ) {
            } finally {
                if ( interruptThread != null ) {
                    interruptThread.interrupt();
                }
                if ( closeChannel ) {
                    fileChannel.close();
                }
                lock.unlock();
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
