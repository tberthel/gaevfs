# Writing Portable Code #

The primary goal of GaeVFS is to provide a portability layer that allows you to write application code to access the file system--both reads and writes--that runs unmodified in either GAE or non-GAE servlet environments.

Key to achieving application portability is the fact that Commons VFS has the ability to access the local file system as an alternative API to `java.io.*`. Also important is the GaeVFS [Combined Local option](CombinedLocalOption.md)--enabled by default--that allows you to reference either local or virtual file system resources without specifying the file system scheme (normally, `file://` for local resources and `gae://` for virtual resources).

As illustrated in the example servlet code below, the basic strategy to achieve portability is to get a static `FileSystemManager` instance either from GaeVFS (if running within GAE) or from Commons VFS (if not running within GAE), and then perform all file system reads and writes via the `FileSytemManager` and [Commons VFS API](http://commons.apache.org/vfs/api.html) instead of using `java.io.*` classes.

<font color='red'><b>IMPORTANT!</b></font> Do not use the `VFS.getManager()` method provided by Commons VFS to get a `FileSystemManager` when running within GAE. If you do, Commons VFS will fail to initialize and it will not work!

(NOTE: the Google code formatter doesn't render properly in IE, or IE doesn't render the Google code formatter properly. Whatever. Try another browser, such as Firefox, if you're having trouble reading the following code in IE.)

```
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.vfs.*;
import com.newatlanta.commons.vfs.provider.gae.GaeVFS;

public class ExampleServlet extends HttpServlet
{
    private static FileSystemManager fsManager;
    private static boolean isGoogleAppEngine;

    public void init() throws ServletException {
        // determine if we're running within GAE and get the appropriate FileSystemManager
        isGoogleAppEngine = getServletContext().getServerInfo().contains( "Google" );
        try {
            if ( isGoogleAppEngine ) {
                GaeVFS.setRootPath( getServletContext.getRealPath( "/" ) );
                fsManager = GaeVFS.getManager();
            } else {
                fsManager = VFS.getManager();
            }
        } catch ( FileSystemException fse ) {
            throw new ServletException( fse );
        }
    }

    public void doGet( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {
        try {
            String realPath = getServletContext().getRealPath( req.getRequestURI() );
            // an example of using Commons VFS (the FileObject class is similar to java.io.File)
            FileObject fileObject = fsManager.resolveFile( realPath );
            if ( !fileObject.exists() ) {
                res.sendError( HttpServletResponse.SC_NOT_FOUND ); // return 404 to client
                return;
            }
            // ...continue processing the GET request...
        } finally {
            if ( isGoogleAppEngine ) {
                GaeVFS.clearFilesCache(); // this is important!
            }
        }
    }

    public void destroy() {
        if ( isGoogleAppEngine ) {
            GaeVFS.close(); // this is not so important, but nice to do
        }
    }
}
```


---

Copyright 2009 [New Atlanta Communications, LLC](http://www.newatlanta.com/)