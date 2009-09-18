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
package com.newatlanta.appengine.locks;

/**
 * A timer used for sleep loops. Doubles the amount of time each iteration until
 * the maximum is reached.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class SleepTimer {
    
    private static final long MAX_SLEEP_TIME = 128; // milliseconds
    
    private long sleepTime;
    private long maxTime;
    
    public SleepTimer() {
        this( 1, MAX_SLEEP_TIME );
    }
    
    public SleepTimer( long start, long max ) {
        if ( ( start < 0 ) || ( max < 0 ) || ( start > max ) ) {
            throw new IllegalArgumentException();
        }
        sleepTime = start;
        maxTime = max;
    }
    
    /**
     * Doubles each invocation until the maximum is reached (or exceeded).
     */
    public long nextSleepTime() {
        if ( sleepTime < maxTime ) {
            long returnValue = sleepTime;
            sleepTime <<= 1;
            return returnValue;
        }
        return maxTime;
    }
}