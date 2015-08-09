# Block Size #

GaeVFS stores files as a series of blocks. Each block corresponds to a Google App Engine datastore entity and therefore has a maximum size of 1 megabyte (due to entity overhead, the actual limit is 1023 `*` 1024 = 1,047,552 bytes). The default block size is 128KB (131,072 bytes); this is an arbitrary number that may not be optimal for any particular application. Testing and experimentation will be required to determine optimal block sizes for different application scenarios.

The block size can be set programmatically via the GaeVFS API. It's possible to change the default block size, and to set the block size on a per-file basis using the static `GaeVFS.setBlockSize( int )` methods:

```
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
    .
    .
    GaeVFS.setBlockSize( 64 ); // set default block size to 64KB
    .
    .
```

All files are created using the default block size unless a block size is specifically set for the file prior to the file being created:

```
import org.apache.commons.vfs.FileObject;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
    .
    .
    FileObject tempFile = GaeVFS.resolveFile( "/temp/myfile.tmp" );
    GaeVFS.setBlockSize( tempFile, 8 ); // set block size for tempFile to 8KB
    tempFile.createFile();
    .
    .
```

After a file is created it's not possible to change its block size. Attempting to set the block size for a file after the file is created will result in an `IOException`.

The `size` parameter for both `GaeVFS.setBlockSize()` methods is an integer between 1 and 1023, which specifies units of KB (the value is multiplied by 1024 to determine the actual block size). If an number less than or equal to 0 is specified, an `IllegalArgumentException` is thrown; if a number larger than 1023 is specifed, the value 1023 is used.

When persisting a block to a Google App Engine datastore entity, only the actual number of bytes written to the file are persisted; that is, a partial block is persisted, not the entire block. Also, when creating a new block for writing, only a partial block is initially allocated, which is then extended as needed as bytes are written to the file. This means that small files do not suffer a penalty of either storage space in the datastore or application memory by specifying a large block size.

GaeVFS supports random access (read/write) to files via an API that is similar to `java.io.RandomAccessFile`. The Commons VFS API calls required to open a file for random access are illustrated below:

```
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.util.FileObjectUtils;
import org.apache.commons.vfs.util.RandomAccessMode;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
    .
    .
    FileObject tempFile = GaeVFS.resolveFile( "/temp/myfile.tmp" );
    RandomAccessContent content = tempFile.getContent().getRandomAccessContent( RandomAccessMode.READWRITE );
    .
    .
```

When a file is opened for reading, writing, or random access, only a single block is retained in memory at a time. Currently, entity blocks are not saved in memcache, but are read directly from the datastore; memcache support is planned as a future enhancement to GaeVFS.

See the [GaeRandomAccessContent source code](http://code.google.com/p/gaevfs/source/browse/trunk/src/com/newatlanta/commons/vfs/provider/gae/GaeRandomAccessContent.java) for details of the GaeVFS implementation of file blocks (the `GaeRandomAccessContent` class is used for reading and writing files, in addition to providing random access).


---

Copyright 2009 [New Atlanta Communications, LLC](http://www.newatlanta.com/)