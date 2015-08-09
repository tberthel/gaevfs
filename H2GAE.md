# H2 Database on GAE #

H2-GAE is an _experimental_ port of the [H2 Database Engine](http://www.h2database.com/html/main.html) to [Google App Engine](http://code.google.com/appengine/docs/java/overview.html) (GAE) using GaeVFS. H2-GAE is currently **not** suitable for production use.

As of July 21, 2009 the current status is:

  * Basic operations such as CREATE TABLE, DROP TABLE, INSERT, and SELECT are working.
  * INSERT is very, very slow.
  * SELECT seems to perform reasonably well.
  * There is currently no distributed locking mechanism that works across multiple GAE application instances running in separate JVMs. This could result in corruption of database files.

## Download and Installation ##

H2-GAE requires GaeVFS 0.3 and its dependencies. Copy `h2-gae-EXPERIMENTAL.jar`, `gaevfs-0.3.jar`, `commons-vfs-2.0-SNAPSHOT.jar`, and the required dependencies into your project's `WEB-INF/lib` directory. H2-GAE only works in **Embedded Mode** as described in the [H2 tutorial](http://www.h2database.com/html/tutorial.html#web_applications).

## JDBC Driver Class ##

The JDBC Driver Class for H2-GAE is: `org.h2.Driver`

## JDBC URLs ##

JDBC URLs for H2-GAE take the following format:

> `jdbc:h2:gae://<partial-path>;FILE_LOCK=NO;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE`


Where `<partial-path>` is a partial GaeVFS path specification. The path is "partial" because it does not specify a complete file name, but instead specifies a database name that is used by H2 to generate file names. For example, the following JDBC URL specifies a database named `test`:

> `jdbc:h2:gae://test;FILE_LOCK=NO;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE`

When processing this URL, H2-GAE creates the following files--among others--within the webapp root (virtual) directory:

```
test.data.db
test.index.db
test.trace.db
```

The JDBC URL can specify directories within the partial path, for example:

> `jdbc:h2:gae://WEB-INF/data/test;FILE_LOCK=NO;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE`

The above JDBC URL will result in the following files (among others):

```
WEB-INF/data/test.data.db
WEB-INF/data/test.index.db
WEB-INF/data/test.trace.db
```

The [GaeVfsServlet](http://gaevfs.googlecode.com/svn/trunk/docs/javadoc/com/newatlanta/commons/vfs/provider/gae/GaeVfsServlet.html) can be used to download these files.

Specifying the `FILE_LOCK=NO`, `AUTO_SERVER=FALSE` and `DB_CLOSE_ON_EXIT=FALSE` parameters is required; if omitted or set to different values, H2-GAE will throw exceptions and fail to startup.

## Source Code ##

[Source code patches](http://code.google.com/p/gaevfs/source/browse/trunk/src/) to the H2 Database Engine and Apache Commons VFS required to support H2-GAE are available within the [SVN repository](http://code.google.com/p/gaevfs/source/browse/trunk/src/) on this web site.