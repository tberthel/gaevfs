package com.newatlanta.commons.vfs.provider.gae;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs.RandomAccessContent;

public class GaeOutputStream extends OutputStream {
    
    private RandomAccessContent out;
    
    public GaeOutputStream( RandomAccessContent rac, boolean append ) throws IOException {
        out = rac;
        if ( append ) {
            rac.seek( rac.length() );
        }
    }

    @Override
    public void write( int b ) throws IOException {
        out.write( b );
    }
    
    @Override
    public void write( byte[] b ) throws IOException {
        out.write( b );
    }
    
    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        out.write( b, off, len );
    }
    
    @Override
    public void flush() throws IOException {
        if ( out instanceof GaeRandomAccessContent ) {
            ((GaeRandomAccessContent)out).flush();
        }
    }
    
    @Override
    public void close() throws IOException {
        out.close();
    }
}
