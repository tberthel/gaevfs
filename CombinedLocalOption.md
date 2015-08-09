# Introduction #

Apache Commons VFS treats all file names as URIs. The scheme `file://` is used to reference files on the local file system; however, this is also the default scheme, so it may be omitted when referencing local file system resources. Examples of local file system URIs are (from the [Commons VFS documentation](http://commons.apache.org/vfs/filesystems.html)):

  * `file:///home/someuser/somedir`
  * `file:///C:/Documents and Settings`
  * `file://///somehost/someshare/afile.txt`
  * `/home/someuser/somedir`
  * `c:\program files\some dir`
  * `c:/program files/some dir`

The scheme `gae://` is used by GaeVFS to reference virtual file system resources. Examples of GaeVFS file system URIs are:

  * `gae:///home/someuser/somedir`
  * `gae:///C:/Documents and Settings`
  * `gae://///somehost/someshare/afile.txt`

The Combined Local option--which is enabled by default--combines the local and GaeVFS file systems to allow you to treat them as a single file system. Specifically, the Combined Local option:

  1. allows you to reference either local or GaeVFS file system resources without specifying the `file://` or `gae://` schemes
  1. allows both local and GaeVFS file systems resources to use the same parent directory paths
  1. treats local file system resources as read-only, while providing both read and write access to GaeVFS file system resources
  1. allows you to explicitly reference a file system by specifying the `file://` or `gae://` scheme within URIs

In summary, the Combined Local option of GaeVFS--by allowing you to omit the `file://` or `gae://` schemes--permits you to write code that references both local and GaeVFS file system resources without knowing or caring which file system actually contains the resources.

# Details #

The GaeVFS file system is initialized by invoking the `GaeVFS.getManager()` method, which returns a `GaeFileSystemManager` instance that implements the `FileSystemManager` interface. (You must first set the webapp root path via the `GaeVFS.setRootPath()` method before invoking `GaeVFS.getManager()` the first time. You only need to invoke `GaeVFS.setRootPath()` once, probably within your servlet's `init()` method.) References to `GaeVFS.getManager()` always return the same static `GaeFileSystemManager` instance:

```
import org.apache.commons.vfs.*;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;
    .
    .
    GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
    .
    .
    FileSystemManager fsManager = GaeVFS.getManager();
    .
    .
```

The Combined Local option can be enabled or disabled at any time via the `GaeFileSystemManager.setCombinedLocal( boolean )` method, which return the `GaeFileSystemManager` instance to allow for method chaining.

The `FileSystemManager.resolveFile()` method, in it's various overloaded forms, is used to locate file system resources. If the Combined Local option is disabled, the `FileSystemManager.resolveFile()` method behaves exactly as described in the [Commons VFS API documentation](http://commons.apache.org/vfs/apidocs/index.html). That is, if the Combined Local option is disabled, you must specify the `gae://` scheme to reference GaeVFS file system resources, and may either specify or omit the `file://` scheme to reference local file system resources. For example:

```
    GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
    FileSystemManager fsManager = GaeVFS.getManager().setCombinedLocal( false );
    FileObject gaeFile = fsManager.resolveFile( "gae:///home/someuser/somedir/gfile.txt" ):
    FileObject localFile = fsManager.resolveFile( "file:///home/someuser/somedir/afile.txt" );
    FileObject anotherLocalFile = fsManager.resolveFile( "/home/someuser/somedir/bfile.txt" );
```

If the Combined Local option is enabled--which is the default--then you may omit the scheme and `FileSystemManager.resolveFile()` will search both the local file system and the GaeVFS file system when searching for resources. If you don't specify the scheme, you won't know whether the `FileObject` returned by `FileSystemManager.resolveFile()` is a local or GaeVFS resource, except that local file system resources are read-only. Specifically:

  * if `FileObject.exists()` returns `true`, it may be either a local or GaeVFS resource
  * if `FileObject.exists()` returns `false`, then invoking `FileObject.createFile()` or `FileObject.createFolder()` creates a GaeVFS resource
  * the `FileObject.isWriteable()` method returns `true` for GaeVFS resources, and `false` for local file system resources
  * the `FileObject.delete()` method throws an exception for local file system resources
  * all other `FileObject` methods work as you'd expect them to

For example:

```
    GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
    FileSystemManager fsManager = GaeVFS.getManager(); // Combined Local is true by default
    FileObject fileObject = fsManager.resolveFile( "/home/someuser/somedir/somefile.txt" );
    if ( !fileObject.exists() ) {
        fileObject.createFile(); // create GaeVFS file
    }
```

Local and GaeVFS resources may share directory paths. For example, a file on the local file system can be specified by the URI `file:///home/someuser/somedir/localfile.txt` and a file on the GaeVFS file system may be specified by `gae:///home/someuser/somedir/gaefile.txt`. If the Combined Local option is enabled, then listing the contents of the `/home/someuser/somedir` directory lists both the local and GaeVFS resources with that parent directory path. For example, consider the following code:

```
    GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
    FileSystemManager fsManager = GaeVFS.getManager(); // Combined Local is true by default
    FileObject fileObject = fsManager.resolveFile( "/home/someuser/somedir" );
    if ( fileObject.exists() ) {
        FileObject[] children = fileObject.listChildren();
        System.out.println( "number of children: " + children.length );
        for ( FileObject child : children ) {
            System.out.println( child.getName().getBaseName() );
        }
    }
```

The output of the above code is:

```
    number of children: 2
    localfile.txt
    gaefile.txt
```

An example use of the Combined Local option might be to create a `/images` directory for your GAE-hosted web application that is populated with an initial set of image files on the local file system. You could then provide a mechanism for uploading additional images--maybe using the GaeVfsServlet--which would be stored within the GaeVFS file system. You could then serve both local and GaeVFS images from the same directory path--again, possibly using the GaeVfsServlet--without having to differentiate between the two file systems. Of course, uploaded image files cannot overwrite or replace images on the local file system, which are read-only and cannot be renamed or deleted.

Finally, if the Combined Local option is enabled, you may still specify the scheme to restrict `FileSystemManager.resolveFile()` to the local or GaeVFS file system (or any file system supported by Commons VFS).


---

Copyright 2009 [New Atlanta Communications, LLC](http://www.newatlanta.com/)