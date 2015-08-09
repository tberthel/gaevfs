# Google App Engine Virtual File System (GaeVFS) #

GaeVFS is an [Apache Commons VFS](http://commons.apache.org/vfs/) plug-in that implements a distributed, writeable virtual file system for [Google App Engine (GAE) for Java](http://code.google.com/appengine/docs/java/overview.html). GaeVFS is implemented using the GAE [datastore](http://code.google.com/appengine/docs/java/datastore/overview.html) and [memcache](http://code.google.com/appengine/docs/java/memcache/overview.html) APIs. The primary goal of GaeVFS is to provide a portability layer that allows you to write application code to access the file system--both reads and writes--that runs unmodified in either GAE or non-GAE servlet environments.

Start with the [Using GaeVFS](UsingGaeVFS.md) Wiki page to learn more about GaeVFS.

## News ##

<font color='red'><b>IMPORTANT!</b></font> The datastore representation used by GaeVFS 0.3 is different and incompatible with the ones used by versions 0.2 and 0.1, and there is no automatic conversion mechanism. You must delete all of the GaeFileObject entities created by GaeVFS 0.1 or 0.2 before upgrading to version 0.3.

  * Version 0.4 (not released yet)
    * added GaeVfsServletEventListener to handle GaeVFS initialization automatically
    * reworked metadata cache to no longer require thread local cache (negate issue `#`2); also, removed requirement for Commons VFS serialization patch
    * moved H2 support to the gaevfs.jar and now register GaeVFS as an H2 file system plug-in rather than modifying H2 itself (a patch for H2 is still needed to prevent creation of the WriterThread)
    * GaeFileObject.getPath() now returns a relative path from the webapp root
    * added junit test cases
  * **July 21, 2009.** Version 0.3 released.
    * added support for files larger than 1 megabyte (fixed issue `#`1)
    * added support for random access content and append file content capabilities
    * don't use parent keys to model file/folder hierarchical relationships (revert back to original implementation using key lists within properties)
    * GaeVFS.resolveFile() should not throw ServletException
    * fixed issue `#`3: invoking FileObject.getChildren() on root directory includes the root directory in the returned array
    * fixed issue `#`5: NPE within AbstractFileObject.finalize()
    * switch to MANUAL caching strategy (versus ON\_RESOLVE)
    * added or updated javadocs for all source files
  * **June 26, 2009.** Version 0.2 released.
    * added support for the [Combined Local Option](CombinedLocalOption.md)
    * added memcached-based metadata cache
    * switched to parent-key implementation to manage hierarchical relationships (rather than storing child key list as parent property)
    * fixed recursive deletes of directories
    * fixed renaming of directories
  * **May 29, 2009.** Version 0.1 released (first public release).

## Download ##

The latest version of GaeVFS can be found by clicking the [Downloads](http://code.google.com/p/gaevfs/downloads/list) tab.

<font color='red'><b>IMPORTANT!</b></font> GaeVFS requires a modified version of Commons VFS 2.0, which is included with the GaeVFS download. The [patch](http://code.google.com/p/gaevfs/source/browse/trunk/src/VFS-abstractfilename-abstractfileobject-serialization.patch) that implements these changes has been submitted for inclusion in the Commons VFS 2.0 release.

GaeVFS also requires the following libraries (get the latest released versions unless otherwise noted):

| **Dependency** | **Required By** |
|:---------------|:----------------|
| Commons VFS 2.0 (see important note above) | GaeVFS          |
| [Commons Logging](http://commons.apache.org/logging/) | Commons VFS     |
| [Commons Collections](http://commons.apache.org/collections/) | Commons VFS     |
| [Commons FileUpload](http://commons.apache.org/fileupload/) | GaeVfsServlet   |
| [Commons IO](http://commons.apache.org/io/) | GaeVfsServlet   |
| [Commons Lang](http://commons.apache.org/lang/) | GaeVfsServlet   |


## GaeVfsServlet ##

The `GaeVfsServlet`, included within the GaeVFS jar file, demonstrates use of GaeVFS and the Commons VFS API, and provides some useful functionality of its own, including the ability to:

  * upload files into GaeVFS
  * serve files from GaeVFS
  * (optionally) perform GaeVFS directory listings

See the [GaeVfsServlet javadocs](http://gaevfs.googlecode.com/svn/trunk/docs/javadoc/com/newatlanta/commons/vfs/provider/gae/GaeVfsServlet.html) or [GaeVfsServlet source code ](http://code.google.com/p/gaevfs/source/browse/trunk/src/com/newatlanta/appengine/servlet/GaeVfsServlet.java)for instructions on its use. The GaeVFS download package also contains an `upload.html` file that demonstrates uploading files via `GaeVfsServlet`.

## memcache ##

GaeVFS uses `memcache` to cache virtual file system metadata, but currently does not cache any file content. Use of `memcache` for file content caching is planned as a future enhancement.

## Limitations ##

There are no file size limitations beginning with GaeVFS 0.3, which supports essentially unlimited file sizes. However, Google App Engine limits file uploads to 10 megabytes.

Only a one thread at a time within a JVM is allowed to open a file for write access, however, there is currently no mechanism for locking files across multiple JVMs in a distributed environment. Therefore, it is possible that multiple threads within separate JVMs could be granted write access to the same file at the same time.

## GAE Datastore Indexes ##

GaeVFS does not require defining any datastore indexes.


---

Copyright 2009 [New Atlanta Communications, LLC](http://www.newatlanta.com/)