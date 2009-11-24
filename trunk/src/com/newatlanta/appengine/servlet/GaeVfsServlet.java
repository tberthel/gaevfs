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
package com.newatlanta.appengine.servlet;

import static com.newatlanta.appengine.nio.file.attribute.GaeFileAttributes.withBlockSize;
import static com.newatlanta.repackaged.java.nio.file.Files.createDirectories;
import static com.newatlanta.repackaged.java.nio.file.attribute.Attributes.readBasicFileAttributes;
import static org.apache.commons.fileupload.util.Streams.asString;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.newatlanta.repackaged.java.nio.file.Path;
import com.newatlanta.repackaged.java.nio.file.Paths;
import com.newatlanta.repackaged.java.nio.file.attribute.BasicFileAttributes;

/**
 * <code>GaeVfsServlet</code> uploads files into the GAE virtual file system (GaeVFS)
 * and then serves them out again. It can also be configured to optionally return
 * directory listings of GaeVFS folders. Here's sample <code>web.xml</code> configuration
 * for this servlet:
 * <p><code>
 * &lt;listener><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;listener-class>com.newatlanta.appengine.servlet.GaeVfsServletEventListener&lt;/listener-class><br>
 * &lt;/listener><br>
 * &lt;servlet><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-name>gaevfs&lt;/servlet-name><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-class>com.newatlanta.appengine.servlet.GaeVfsServlet&lt;/servlet-class><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;init-param><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-name>dirListingAllowed&lt;/param-name><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-value>true&lt;/param-value><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/init-param><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;init-param><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-name>initDirs&lt;/param-name><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-value>/gaevfs/images,/gaevfs/docs&lt;/param-value><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/init-param><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;init-param><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-name>uploadRedirect&lt;/param-name><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;param-value>/uploadComplete.jsp&lt;/param-value><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/init-param><br>
 * &lt;/servlet><br>
 * &lt;servlet-mapping><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;servlet-name>gaevfs&lt;/servlet-name><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;url-pattern>/gaevfs/*&lt;/url-pattern><br>
 * &lt;/servlet-mapping><br>
 * </code>
 * <p>
 * The <code>&lt;url-pattern></code> within the <code>&lt;servlet-mapping></code>
 * element is very important
 * because it determines which incoming requests get processed by this servlet.
 * When uploading a file, be sure to specify a "path" form parameter that starts
 * with this URL pattern. For example, if you upload a file named "picture.jpg"
 * with a "path" of "/gaevfs/images", then the following URL will serve it:
 * <blockquote><code>
 *      http://www.myhost.com/gaevfs/images/picture.jpg
 * </code></blockquote>
 * If you upload "picture.jpg" with any path that doesn't start with "/gaevfs"
 * then it will never get served because this servlet won't get invoked. You can
 * configure the <code>&lt;url-pattern></code> to be whatever you want--and even
 * specify multiple <code>&lt;url-pattern></code> elements--just make sure to
 * specify the "path" form parameter correctly when uploading the file. See additional
 * comments on the <code>doPost()</code> method.
 * <p>
 * The "dirListingAllowed" <code>&lt;init-param></code> controls whether this servlet
 * returns directory listings for GaeVFS folders. If false, then a <code>FORBIDDEN</code>
 * error is returned for attempted directory listings. The default value is false.
 * <p>
 * The "initDirs" <code>&lt;init-param></code> allows you to specify a comma-separated
 * list of folders to create when this servlet initializes. This is merely for
 * convenience and is entirely optional. (If you have directory listing enabled,
 * then it's a good idea to create the top-level directory. Otherwise, you'll
 * get a <code>NOT_FOUND</code> response when invoking this servlet on an empty
 * file system, which might be confusing).
 * <p>
 * The "uploadRedirect" <code>&lt;init-param></code> allows you to specify a page
 * to use to create the response for a file upload. The default is to do a directory
 * listing of the folder to which the file was uploaded, so you should specify
 * "uploadRedirect" if you disable directory listings.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
@SuppressWarnings("serial")
public class GaeVfsServlet extends HttpServlet {

    private boolean dirListingAllowed;
    private String uploadRedirect;

    /**
     * Initializes GaeVFS and processes <code>&lt;init-param></code> elements from
     * <code>web.xml</code>.
     */
    @Override
    public void init() throws ServletException {
        dirListingAllowed = Boolean.parseBoolean( getInitParameter( "dirListingAllowed" ) );
        uploadRedirect = getInitParameter( "uploadRedirect" );
        
        try {
            String initDirs = getInitParameter( "initDirs" );
            if ( initDirs != null ) {
                StringTokenizer st = new StringTokenizer( initDirs, "," );
                while ( st.hasMoreTokens() ) {
                    Path dirPath = Paths.get( st.nextToken() );
                    if ( dirPath.notExists() ) {
                        createDirectories( dirPath );
                    }
                }
            }
        } catch ( IOException e ) {
            throw new ServletException( e );
        }
    }

    /**
     * If a file is specified, return the file; if a folder is specified, then
     * either return a listing of the folder, or <code>FORBIDDEN</code>, based on
     * configuration.
     */
    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res )
            throws ServletException, IOException {

        Path path = Paths.get( req.getRequestURI() );
        if ( path.notExists() ) {
            res.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        if ( readBasicFileAttributes( path ).isDirectory() ) {
            if ( dirListingAllowed ) {
                res.getWriter().write( getListHTML( path ) );
                res.flushBuffer();
            } else {
                res.sendError( HttpServletResponse.SC_FORBIDDEN,
                                             "Directory listing not allowed" );
            }
            return;
        }

        // it's file, return it
        // TODO: add support for If-Modified-Since header? caching headers?

        // the servlet MIME type is configurable via web.xml
        String contentType = getServletContext().getMimeType( path.getName().toString() );
        if ( contentType != null ) {
            res.setContentType( contentType );
        }
        copyAndClose( path.newInputStream(), res.getOutputStream() );
        res.flushBuffer();
    }

    /**
     * Return the directory listing for the specified GaeVFS folder. Copied from:
     * 
     *      http://www.docjar.com/html/api/org/mortbay/util/Resource.java.html
     * 
     * Modified to support GAE virtual file system. 
     */
    private String getListHTML( Path path ) throws IOException {
        String title = "Directory: " + path.toString();

        StringBuffer buf = new StringBuffer( 4096 );
        buf.append( "<HTML><HEAD><TITLE>" );
        buf.append( title );
        buf.append( "</TITLE></HEAD><BODY>\n<H1>" );
        buf.append( title );
        buf.append( "</H1><TABLE BORDER='0' cellpadding='3'>" );

        if ( path.getParent() != null ) {
            buf.append( "<TR><TD><A HREF='" );
            String parentPath = path.getParent().toString();
            buf.append( parentPath );
            if ( !parentPath.endsWith( "/" ) ) {
                buf.append( "/" );
            }
            buf.append( "'>Parent Directory</A></TD><TD></TD><TD></TD></TR>\n" );
        }

        Iterator<Path> children = path.newDirectoryStream().iterator();
        if ( !children.hasNext() ) {
            buf.append( "<TR><TD>[empty directory]</TD></TR>\n" );
        } else {
            NumberFormat nfmt = NumberFormat.getIntegerInstance();
            buf.append( "<tr><th align='left'>Name</th><th>Size</th>" +
                "<th aligh='left'>Type</th><th align='left'>Date modified</th></tr>" );
            while ( children.hasNext() ) {
                Path child = children.next();
                buf.append( "<TR><TD><A HREF=\"" ).append( child ).append( "\">" );
                buf.append( escapeHtml( child.getName().toString() ) );
                BasicFileAttributes childAttrs = readBasicFileAttributes( child );
                if ( childAttrs.isDirectory() ) {
                    buf.append( '/' );
                }
                buf.append( "</TD><TD ALIGN=right>" );
                if ( childAttrs.isRegularFile() ) {
                    buf.append( nfmt.format( childAttrs.size() ) ).append( " bytes" );
                }
                buf.append( "</TD><TD>" );
                buf.append( childAttrs.isDirectory() ? "directory" : "file" );
                buf.append( "</TD><TD>" );
                buf.append( childAttrs.lastModifiedTime() );
                buf.append( "</TD></TR>\n" );
            }
        }

        buf.append( "</TABLE>\n" );
        buf.append( "</BODY></HTML>\n" );

        return buf.toString();
    }

    /**
     * Writes the uploaded file to the GAE virtual file system (GaeVFS). Copied from:
     * 
     *      http://code.google.com/appengine/kb/java.html#fileforms
     * 
     * The "path" form parameter specifies a <a href="http://code.google.com/p/gaevfs/wiki/CombinedLocalOption"
     * target="_blank">GaeVFS path</a>. All directories within the path hierarchy
     * are created (if they don't already exist) when the file is saved.
     */
    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res )
    		throws ServletException, IOException {
        // Check that we have a file upload request
        if ( !ServletFileUpload.isMultipartContent( req ) ) {
            res.sendError( HttpServletResponse.SC_BAD_REQUEST,
                                        "form enctype not multipart/form-data" );
        }
        try {
            String path = "/";
            int blockSize = 0;
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iterator = upload.getItemIterator( req );

            while ( iterator.hasNext() ) {
                FileItemStream item = iterator.next();
                if ( item.isFormField() ) {
                    if ( item.getFieldName().equalsIgnoreCase( "path" ) ) {
                        path = asString( item.openStream() );
                        if ( !path.endsWith( "/" ) ) {
                            path = path + "/";
                        }
                    } else if ( item.getFieldName().equalsIgnoreCase( "blocksize" ) ) {
                        String s = asString( item.openStream() );
                        if ( s.length() > 0 ) {
                            blockSize = Integer.parseInt( s );
                        }
                    }
                } else {
                    Path filePath = Paths.get( path + item.getName() );
                    Path parent = filePath.getParent();
                    if ( parent.notExists() ) {
                        createDirectories( parent );
                    }
                    if ( blockSize > 0 ) {
                        filePath.createFile( withBlockSize( blockSize ) );
                    } else {
                        filePath.createFile();
                    }
                    copyAndClose( item.openStream(), filePath.newOutputStream() );
                }
            }

            // redirect to the configured response, or to this servlet for a
            // directory listing
            res.sendRedirect( uploadRedirect != null ? uploadRedirect : path );

        } catch ( FileUploadException e ) {
            throw new ServletException( e );
        }
    }

    /**
     * Copy the InputStream to the OutputStream then close them both.
     */
    private static int BUFF_SIZE = 64 * 1024; // 64KB
    
    private static void copyAndClose( InputStream in, OutputStream out )
            throws IOException {
        // TODO what is the optimal buffer size? does it make sense to buffer
        // both streams? different buffer sizes for upload and download? does
        // the GaeVFS block size matter?
        in = new BufferedInputStream( in, BUFF_SIZE );
        out = new BufferedOutputStream( out, BUFF_SIZE );
        IOUtils.copy( in, out );
        IOUtils.closeQuietly( out );
        IOUtils.closeQuietly( in );
    }
}
