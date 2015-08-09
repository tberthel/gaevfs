# Getting Started #

Before using GaeVFS you should review the [Commons VFS API documentation](http://commons.apache.org/vfs/api.html). The GaeVFS jar file contains a `vfs-providers.xml` configuration file that binds GaeVFS to the scheme `gae`. The URI format supported by GaeVFS is:

> gae://_path_

Where _path_ is a UNIX-style (or URI-style) absolute or relative path. Paths that do not start with "/" are interpreted as relative paths from the webapp root directory. Paths that start with "/" are interpreted (initially) as full absolute paths.

Absolute paths must specify sub-directories of the webapp root directory. Any absolute path that does not specify such a sub-directory is interpreted to be a relative path from the webapp root directory, regardless of the fact that it starts with "/".

It's probably easiest to just use relative paths and let GaeVFS handle the path translations transparently. The exception might be in cases where you're [writing portable code](ApplicationPortability.md) to run in both GAE and non-GAE environments.

Examples:

  * gae://myfile.zip
  * gae://images/picture.jpg
  * gae://docs/mydocument.pdf

**NOTE:** the [Combined Local Option](CombinedLocalOption.md) allows you to access GaeVFS file system resources by specifying URIs that omit the `gae://` scheme.

## Initializing GaeVFS ##

In order to use GaeVFS, you **must** first set the webapp root directory path via the `GaeVFS.setRootPath()` static method as illustrated below. It's best to do this within your servlet's `init()` method. If you don't specify the correct webapp root directory, you may get unexpected errors.

After setting the webapp root directory path, invoke the `GaeVFS.getManager()` static method to obtain a `FileSystemManager` instance. Then use the [Commons VFS API](http://commons.apache.org/vfs/api.html) as you normally would; for example:

```
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileObject;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
    .
    .
    GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
    .
    .
    FileSystemManager fsManager = GaeVFS.getManager();
    FileObject tmpFolder = fsManager.resolveFile( "gae://WEB-INF/tmp" );
    if ( !tmpFolder.exists() ) {
        tmpFolder.createFolder();
    }
    .
    .
```

<font color='red'><b>IMPORTANT!</b></font> Do not use the `VFS.getManager()` method provided by Commons VFS to get a `FileSystemManager` when running within GAE. If you do, Commons VFS will fail to initialize and it will not work!

## Clearing the File Cache ##

It's very important that you clear the GaeVFS file cache at the end of every servlet request via the `GaeVFS.clearFilesCache()` method, best placed within a `finally` clause:

```
public void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
    try {
        // ...process the GET request...
    } finally {
        GaeVFS.clearFilesCache(); // this is important!
    }
}
```

See the comments within the [GaeMemcacheFilesCache source code](http://code.google.com/p/gaevfs/source/browse/trunk/src/com/newatlanta/commons/vfs/cache/GaeMemcacheFilesCache.java) if you're interested in details regarding why this is important.

## Closing GaeVFS ##

It's good practice, though not strictly necessary, to close GaeVFS when your servlet is destroyed to aid in clean-up of GaeVFS resources:

```
public void destroy() {
    GaeVFS.close(); // this is not strictly required, but good practice
}
```


---

Copyright 2009 [New Atlanta Communications, LLC](http://www.newatlanta.com/)