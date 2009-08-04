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
package com.newatlanta.h2.store.fs;

import static com.newatlanta.commons.vfs.provider.gae.GaeVFS.resolveFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.provider.UriParser;
import org.h2.store.fs.FileObject;
import org.h2.store.fs.FileSystem;

public class FileSystemGae extends FileSystem {

    private static final FileSystemGae INSTANCE = new FileSystemGae();

    public static FileSystemGae getInstance() {
        return INSTANCE;
    }
    
    /**
     * Check if the file system is responsible for this file name.
     *
     * @param fileName the file name
     * @return true if it is
     */
    @Override
    protected boolean accepts(String fileName) {
    	String scheme = UriParser.extractScheme( fileName );
        return ( ( scheme != null ) && scheme.equals( "gae" ) );
    }

    @Override
    public boolean canWrite(String fileName) {
        try {
            return resolveFile(fileName).isWriteable();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public void copy(String original, String copy) throws SQLException {
        try {
            resolveFile(copy).copyFrom(resolveFile(original),Selectors.SELECT_SELF);
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Create all required directories that are required for this file.
     *
     * @param fileName the file name (not directory name)
     */
    @Override
    public void createDirs(String fileName) throws SQLException {
        try {
            resolveFile(fileName).getParent().createFolder();
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public boolean createNewFile(String fileName) throws SQLException {
        try {
            resolveFile(fileName).createFile();
            return true;
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Create a new temporary file.
     *
     * @param prefix the prefix of the file name (including directory name if
     *            required)
     * @param suffix the suffix
     * @param deleteOnExit if the file should be deleted when the virtual
     *            machine exists
     * @param inTempDir if the file should be stored in the temporary directory
     * @return the name of the created file
     */
    private static AtomicInteger counter = new AtomicInteger(new Random().nextInt() & 0xffff);

    @Override
    public String createTempFile(String prefix, String suffix, boolean deleteOnExit, boolean inTempDir)
            throws IOException {
        org.apache.commons.vfs.FileObject tempDir;
        if (inTempDir) {
            tempDir = resolveFile("WEB-INF/temp");
        } else {
            tempDir = resolveFile(prefix).getParent();
        }
        tempDir.createFolder();

        return tempDir.resolveFile(prefix + Integer.toString(counter.getAndIncrement())
                + (suffix == null ? ".tmp" : suffix)).getName().getURI();
    }

    @Override
    public void delete(String fileName) throws SQLException {
        try {
            resolveFile(fileName).delete();
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public void deleteRecursive(String directory) throws SQLException {
        try {
            resolveFile(directory).delete(Selectors.SELECT_ALL);
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public boolean exists(String fileName) {
        try {
            return resolveFile(fileName).exists();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean fileStartsWith(String fileName, String prefix) {
        return fileName.startsWith(prefix);
    }

    @Override
    public String getAbsolutePath(String fileName) {
        try {
            return resolveFile(fileName).getName().getURI();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return fileName;
        }
    }

    /**
     * Get the file name (without directory part).
     *
     * @param name the directory and file name
     * @return just the file name
     */
    @Override
    public String getFileName(String name) throws SQLException {
        try {
            return resolveFile(name).getName().getBaseName();
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public long getLastModified(String fileName) {
        try {
            return resolveFile(fileName).getContent().getLastModifiedTime();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return 0;
        }
    }

    @Override
    public String getParent(String fileName) {
        try {
            return resolveFile(fileName).getParent().getName().getURI();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            int i = fileName.lastIndexOf('/');
            return (i > 0 ? fileName.substring(0, i) : "");
        }
    }

    @Override
    public boolean isAbsolute(String fileName) {
        return fileName.equals(getAbsolutePath(fileName));
    }

    @Override
    public boolean isDirectory(String fileName) {
        try {
            return resolveFile(fileName).getType().hasChildren();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean isReadOnly(String fileName) {
        try {
            return !resolveFile(fileName).isWriteable();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public long length(String fileName) {
        try {
            return resolveFile(fileName).getContent().getSize();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return 0;
        }
    }

    @Override
    public String[] listFiles(String directory) throws SQLException {
        try {
        	org.apache.commons.vfs.FileObject dirObject = resolveFile(directory);
        	if (!dirObject.exists()) {
        		return new String[0];
        	}
            org.apache.commons.vfs.FileObject[] children = dirObject.getChildren();
            String[] files = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                files[i] = children[i].getName().getURI();
            }
            return files;
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public String normalize(String fileName) throws SQLException {
        try {
            return resolveFile(fileName).getName().getURI();
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public InputStream openFileInputStream(String fileName) throws IOException {
        return resolveFile(fileName).getContent().getInputStream();
    }

    /**
     * Open a random access file object.
     *
     * @param fileName the file name
     * @param mode the access mode. Supported are r, rw, rws, rwd
     * @return the file object
     */
    @Override
    public FileObject openFileObject(String fileName, String mode) throws IOException {
        org.apache.commons.vfs.FileObject fileObject = resolveFile(fileName);
        fileObject.createFile(); // does nothing if file exists
        return new FileObjectGae(fileObject,mode);
    }

    @Override
    public OutputStream openFileOutputStream(String fileName, boolean append) throws SQLException {
        try {
            return resolveFile(fileName).getContent().getOutputStream(append);
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public void rename(String oldName, String newName) throws SQLException {
        try {
            resolveFile(oldName).moveTo(resolveFile(newName));
        } catch (IOException e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public boolean tryDelete(String fileName) {
        try {
            org.apache.commons.vfs.FileObject fileObject = resolveFile(fileName);
            fileObject.delete();
            return fileObject.exists();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
        
    }
}
