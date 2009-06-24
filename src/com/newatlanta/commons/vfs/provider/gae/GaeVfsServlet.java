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
package com.newatlanta.commons.vfs.provider.gae;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

/**
 * GaeVfsServlet uploads files into the GAE virtual file system (GaeVFS) and
 * then serves them out again. It can also be configured to optionally return
 * directory listings of GaeVFS folders. Here's sample web.xml configuration for
 * this servlet:
 * 
 *  <servlet>
 *      <servlet-name>gaevfs</servlet-name>
 *      <servlet-class>com.newatlanta.commons.vfs.provider.gae.GaeVfsServlet</servlet-class>
 *      <init-param>
 *          <param-name>dirListingAllowed</param-name>
 *          <param-value>true</param-value>
 *      </init-param>
 *      <init-param>
 *          <param-name>initDirs</param-name>
 *          <param-value>/gaevfs/images,/gaevfs/docs</param-value>
 *      </init-param>
 *      <init-param>
 *          <param-name>uploadRedirect</param-name>
 *          <param-value>/uploadComplete.jsp</param-value>
 *      </init-param>
 *  </servlet>
 *  <servlet-mapping>
 *      <servlet-name>gaevfs</servlet-name>
 *      <url-pattern>/gaevfs/*</url-pattern>
 *  </servlet-mapping>
 * 
 * The <url-pattern> within the <servlet-mapping> element is very important
 * because it determines which incoming requests get processed by this servlet.
 * When uploading a file, be sure to specify a "path" form parameter that starts
 * with this URL pattern. For example, if you upload a file named "picture.jpg"
 * with a "path" of "/gaevfs/images", then the following URL will serve it:
 * 
 *      http://www.myhost.com/gaevfs/images/picture.jpg
 * 
 * If you upload "picture.jpg" with any path that doesn't start with "/gaevfs"
 * then it will never get served because this servlet won't get invoked. You can
 * configure the <url-pattern> to be whatever you want--and even specify
 * multiple <url-pattern> elements--just make sure to specify the "path" form
 * parameter correctly when uploading the file. See additional comments on the
 * doPost() method.
 * 
 * The "dirListingAllowed" <init-param> controls whether this servlet returns
 * directory listings for GaeVFS folders. If false, then a FORBIDDEN error is
 * returned for attempted directory listings. The default value is false.
 * 
 * The "initDirs" <init-param> allows you to specify a comma-separated list of
 * folders to create when this servlet initializes. This is merely for
 * convenience and is entirely optional. (If you have directory listing enabled,
 * then it's a good idea to create the top-level directory. Otherwise, you'll
 * get a NOT_FOUND response when invoking this servlet on an empty file system,
 * which might be confusing).
 * 
 * The "uploadRedirect" <init-param> allows you to specify a page to use to
 * create the response for a file upload. The default is to do a directory
 * listing of the folder to which the file was uploaded, so you should specify
 * "uploadRedirect" if you disable directory listings.
 * 
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
@SuppressWarnings("serial")
public class GaeVfsServlet extends HttpServlet {

    private boolean dirListingAllowed;
    private String uploadRedirect;
    private int rootPathLen;

    @Override
    public void init() throws ServletException {

        rootPathLen = this.getServletContext().getRealPath( "/" ).length();
        dirListingAllowed = Boolean.parseBoolean( getInitParameter( "dirListingAllowed" ) );
        uploadRedirect = getInitParameter( "uploadRedirect" );

        try {
            String initDirs = getInitParameter( "initDirs" );
            if ( initDirs != null ) {
                StringTokenizer st = new StringTokenizer( initDirs, "," );
                while ( st.hasMoreTokens() ) {
                    FileObject fileObject = GaeVFS.resolveFile( "gae://"
                            + getServletContext().getRealPath( st.nextToken() ) );
                    if ( !fileObject.exists() ) {
                        fileObject.createFolder();
                    }
                }
            }
        } catch ( FileSystemException e ) {
            throw new ServletException( e );
        } finally {
            GaeVFS.clearFilesCache();
        }
    }

    @Override
    public void destroy() {
        GaeVFS.close();
    }

    /**
     * If a file is specified, return the file. If a folder is specified, then
     * either return a listing of the folder, or FORBIDDEN, based on
     * configuration.
     */
    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
        try {
            FileObject fileObject = GaeVFS.resolveFile( getServletContext().getRealPath( req.getRequestURI() ) );
            if ( !fileObject.exists() ) {
                res.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            if ( fileObject.getType().hasChildren() ) { // it's a directory
                if ( dirListingAllowed ) {
                    res.getWriter().write( getListHTML( fileObject ) );
                    res.flushBuffer();
                } else {
                    res.sendError( HttpServletResponse.SC_FORBIDDEN, "Directory listing not allowed" );
                }
                return;
            }

            // it's file, return it

            // the servlet MIME type is configurable via web.xml, check it first
            String contentType = getServletContext().getMimeType( fileObject.getName().getBaseName() );
            res.setContentType( contentType != null ? contentType
                    : fileObject.getContent().getContentInfo().getContentType() );

            copyAndClose( fileObject.getContent().getInputStream(), res.getOutputStream() );
            res.flushBuffer();

        } finally {
            GaeVFS.clearFilesCache();
        }
    }

    /**
     * Copied from:
     * 
     *      http://www.docjar.com/html/api/org/mortbay/util/Resource.java.html
     * 
     * Modified to support GAE virtual file system. Return the directory listing
     * for the specified GaeVFS folder.
     */
    private String getListHTML( FileObject fileObject ) throws IOException {
        String title = "Directory: " + fileObject.getName().getPath().substring( rootPathLen );

        StringBuffer buf = new StringBuffer( 4096 );
        buf.append( "<HTML><HEAD><TITLE>" );
        buf.append( title );
        buf.append( "</TITLE></HEAD><BODY>\n<H1>" );
        buf.append( title );
        buf.append( "</H1><TABLE BORDER='0' cellpadding='3'>" );

        if ( fileObject.getParent() != null ) {
            buf.append( "<TR><TD><A HREF='" );
            String parentPath = fileObject.getParent().getName().getPath().substring( rootPathLen );
            buf.append( parentPath );
            if ( !parentPath.endsWith( "/" ) ) {
                buf.append( "/" );
            }
            buf.append( "'>Parent Directory</A></TD><TD></TD><TD></TD></TR>\n" );
        }

        FileObject[] children = fileObject.getChildren();
        Arrays.sort( children );

        if ( children.length == 0 ) {
            buf.append( "<TR><TD>[empty directory]</TD></TR>\n" );
        } else {
            DateFormat dfmt = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM );
            NumberFormat nfmt = NumberFormat.getIntegerInstance();

            buf.append( "<tr><th align='left'>Name</th><th>Size</th><th aligh='left'>Type</th><th align='left'>Date modified</th></tr>" );
            for ( FileObject child : children ) {
                buf.append( "<TR><TD><A HREF=\"" );
                buf.append( child.getName().getPath().substring( rootPathLen ) );
                buf.append( "\">" );
                buf.append( StringEscapeUtils.escapeHtml( child.getName().getBaseName() ) );
                if ( child.getType().hasChildren() ) {
                    buf.append( '/' );
                }
                buf.append( "</TD><TD ALIGN=right>" );
                if ( child.getType().hasContent() ) {
                    buf.append( nfmt.format( child.getContent().getSize() ) ).append( " bytes" );
                }
                buf.append( "</TD><TD>" );
                buf.append( child.getType().getName() );
                buf.append( "</TD><TD>" );
                buf.append( dfmt.format( new Date( child.getContent().getLastModifiedTime() ) ) );
                buf.append( "</TD></TR>\n" );
            }
        }

        buf.append( "</TABLE>\n" );
        buf.append( "</BODY></HTML>\n" );

        return buf.toString();
    }

    /**
     * Copied from:
     * 
     *      http://code.google.com/appengine/kb/java.html#fileforms
     * 
     * Modified to write the uploaded file to the GAE virtual file system
     * (GaeVFS).
     * 
     * The "path" form parameter specifies a relative path within the web
     * application. The relative path is translated to a full path using
     * ServletContext.getRealPath() for storing with GaeVFS. All directories
     * within the path hierarchy are created (if they don't already exist) when
     * the file is saved.
     */
    @Override
    public void doPost( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException {
        // Check that we have a file upload request
        if ( !ServletFileUpload.isMultipartContent( req ) ) {
            res.sendError( HttpServletResponse.SC_BAD_REQUEST, "form enctype not multipart/form-data" );
        }
        try {
            String path = "/";
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iterator = upload.getItemIterator( req );

            while ( iterator.hasNext() ) {
                FileItemStream item = iterator.next();
                if ( item.isFormField() ) {
                    if ( item.getFieldName().equalsIgnoreCase( "path" ) ) {
                        path = Streams.asString( item.openStream() );
                        if ( !path.endsWith( "/" ) ) {
                            path = path + "/";
                        }
                    }
                } else {
                    FileObject fileObject = GaeVFS.resolveFile( "gae://"
                            + getServletContext().getRealPath( path + item.getName() ) );
                    copyAndClose( item.openStream(), fileObject.getContent().getOutputStream() );
                }
            }

            // redirect to the configured response, or to this servlet for a
            // directory listing
            res.sendRedirect( uploadRedirect != null ? uploadRedirect : path );

        } catch ( FileUploadException e ) {
            throw new ServletException( e );
        } finally {
            GaeVFS.clearFilesCache();
        }
    }

    /**
     * Copy the InputStream to the OutputStream then close them both.
     */
    private static void copyAndClose( InputStream in, OutputStream out ) throws IOException {
        IOUtils.copy( in, out );
        out.close();
        in.close();
    }
}
