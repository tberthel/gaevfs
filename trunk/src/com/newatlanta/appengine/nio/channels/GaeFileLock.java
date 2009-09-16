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
import java.util.concurrent.locks.Lock;

import com.newatlanta.nio.channels.FileChannel;
import com.newatlanta.nio.channels.FileLock;

public class GaeFileLock extends FileLock {

    private Lock lock;
    
    public GaeFileLock( FileChannel fileChannel, long position, long size,
                          boolean shared, Lock lock ) {
        super( fileChannel, position, size, shared );
        this.lock = lock;
    }

    @Override
    public boolean isValid() {
        return ( lock != null );
    }

    @Override
    public void release() throws IOException {
        lock.unlock();
        lock = null;
    }
}
