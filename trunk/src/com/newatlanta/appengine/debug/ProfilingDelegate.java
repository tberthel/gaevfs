package com.newatlanta.appengine.debug;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ProfilingDelegate implements Delegate<Environment> {
    
    private static final Logger log = Logger.getLogger( ProfilingDelegate.class.getName() );
    
    private Delegate<Environment> parent;
    
    public ProfilingDelegate( Delegate<Environment> parent ) {
      this.parent = parent;
    }
    
    public byte[] makeSyncCall( Environment env, String pkg, String method, byte[] request ) {
      long start = System.nanoTime();
      byte[] result = parent.makeSyncCall( env, pkg, method, request );
      log.info( pkg + "." + method + ": " +
              TimeUnit.MILLISECONDS.convert( System.nanoTime() - start, TimeUnit.NANOSECONDS ) + "ms" );
      return result;
    }

    @Override
    public Future<byte[]> makeAsyncCall( Environment env, String pkg, String method,
            byte[] request, ApiConfig apiConfig ) {
        long start = System.nanoTime();
        Future<byte[]> result = parent.makeAsyncCall( env, pkg, method, request, apiConfig );
        log.info( pkg + "." + method + ": " +
                TimeUnit.MILLISECONDS.convert( System.nanoTime() - start, TimeUnit.NANOSECONDS ) + "ms" );
        return result;
    }
    
    public void log(Environment env, LogRecord logRec) {
        parent.log(env, logRec);
    }
}
