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

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.newatlanta.appengine.junit.TestEnvironment;
import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;

/**
 * Acquires the specified lock, then sleeps for the specified time or until
 * interrupted.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class FileLockingThread extends Thread {
    
    private FileChannel fileChannel;
    private FileLock fileLock;
    private long sleepTime;
    
    @SuppressWarnings("unchecked")
    private Delegate delegate;
    
    public static FileLockingThread createThread( FileChannel fileChannel, long sleepTime ) {
        FileLockingThread lockThread = new FileLockingThread( fileChannel, sleepTime );
        lockThread.start();
        try {
            do {
                Thread.sleep( 100 ); // give lockThread a chance to run
            } while ( !lockThread.isAlive() );
        } catch ( InterruptedException e ) {
        }
        return lockThread;
    }
    
    private FileLockingThread( FileChannel fileChannel, long sleepTime ) {
        super( "FileLockThread" );
        this.delegate = ApiProxy.getDelegate();
        this.sleepTime = sleepTime;
        this.fileChannel = fileChannel;
    }
    
    public FileLock getFileLock() {
        return fileLock;
    }

    @Override
    public void run() {
        ApiProxy.setEnvironmentForCurrentThread( new TestEnvironment() );
        ApiProxy.setDelegate( delegate );

        try {
            fileLock = fileChannel.lock();
            try {
                sleep( sleepTime );
            } catch ( InterruptedException e ) {
            } finally {
                fileLock.release();
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
