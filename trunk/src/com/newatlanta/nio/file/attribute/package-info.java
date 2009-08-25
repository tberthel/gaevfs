/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/**
 * Interfaces and classes providing access to file and file system attributes.
 *
 * <blockquote><table cellspacing=1 cellpadding=0 summary="Attribute views">
 * <tr><th><p align="left">Attribute views</p></th><th><p align="left">Description</p></th></tr>
 * <tr><td valign=top><tt><i>{@link com.newatlanta.nio.file.attribute.AttributeView}</i></tt></td>
 *     <td>Can read or update non-opaque values associated with objects in a file system</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.FileAttributeView}</i></tt></td>
 *     <td>Can read or update file attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.BasicFileAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update a basic set of file attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.PosixFileAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update POSIX defined file attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.DosFileAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update FAT file attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp<i>{@link com.newatlanta.nio.file.attribute.FileOwnerAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update the owner of a file</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.AclFileAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update Access Control Lists</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.UserDefinedFileAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read or update user-defined file attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.FileStoreAttributeView}</i></tt></td>
 *     <td>Can read or update file system attributes</td></tr>
 * <tr><td valign=top><tt>&nbsp;&nbsp;&nbsp;&nbsp;<i>{@link com.newatlanta.nio.file.attribute.FileStoreSpaceAttributeView}&nbsp;&nbsp;</i></tt></td>
 *     <td>Can read file system <em>space usage</em> related attributes</td></tr>
 * </table></blockquote>
 *
 * <p> An attribute view provides a read-only or updatable view of the non-opaque
 * values, or <em>metadata</em>, associated with objects in a file system.
 * The {@link com.newatlanta.nio.file.attribute.FileAttributeView} interface is
 * extended by several other interfaces that that views to specific sets of file
 * attributes. {@code FileAttributeViews} are selected by invoking the {@link
 * com.newatlanta.nio.file.FileRef#getFileAttributeView} method with a
 * <em>type-token</em> to identify the required view. Views can also be identified
 * by name. The {@link com.newatlanta.nio.file.attribute.FileStoreAttributeView} interface
 * provides access to file store attributes. A {@code FileStoreAttributeView} of
 * a given type is obtained by invoking the {@link
 * com.newatlanta.nio.file.FileStore#getFileStoreAttributeView} method.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.BasicFileAttributeView}
 * class defines methods to read and update a <em>basic</em> set of file
 * attributes that are common to many file systems.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.PosixFileAttributeView}
 * interface extends {@code BasicFileAttributeView} by defining methods
 * to access the file attributes commonly used by file systems and operating systems
 * that implement the Portable Operating System Interface (POSIX) family of
 * standards.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.DosFileAttributeView}
 * class extends {@code BasicFileAttributeView} by defining methods to
 * access the legacy "DOS" file attributes supported on file systems such as File
 * Allocation Tabl (FAT), commonly used in consumer devices.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.AclFileAttributeView}
 * class defines methods to read and write the Access Control List (ACL)
 * file attribute. The ACL model used by this file attribute view is based
 * on the model defined by <a href="http://www.ietf.org/rfc/rfc3530.txt">
 * <i>RFC&nbsp;3530: Network File System (NFS) version 4 Protocol</i></a>.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.FileStoreSpaceAttributeView} class
 * defines methods to read file system space usage related attributes of a file system.
 *
 * <p> The {@link com.newatlanta.nio.file.attribute.Attributes} utility class defines
 * static methods to access file or file system attribute using the above
 * attribute views.
 *
 * <p> In addition to attribute views, this package also defines classes and
 * interfaces that are used when accessing attributes:
 *
 * <ul>
 *
 *   <p><li> The {@link com.newatlanta.nio.file.attribute.UserPrincipal} and
 *   {@link com.newatlanta.nio.file.attribute.GroupPrincipal} interfaces represent an
 *   identity or group identity. </li>
 *
 *   <p><li> The {@link com.newatlanta.nio.file.attribute.UserPrincipalLookupService}
 *   interface defines methods to lookup user or group principals. </li>
 *
 *   <p><li> The {@link com.newatlanta.nio.file.attribute.FileAttribute} interface
 *   represents the value of an attribute for cases where the attribute value is
 *   required to be set atomically when creating an object in the file system. </li>
 *
 * </ul>
 *
 *
 * <p> Unless otherwise noted, passing a <tt>null</tt> argument to a constructor
 * or method in any class or interface in this package will cause a {@link
 * java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.7
 */

package com.newatlanta.nio.file.attribute;
