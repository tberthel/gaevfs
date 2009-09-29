package com.newatlanta.appengine.junit.h2;

import java.security.AccessControlException;
import java.security.Permission;

import org.h2.store.fs.FileSystem;
import org.h2.test.TestAll;
import org.h2.test.TestBase;
import org.h2.test.db.TestAlter;
import org.h2.test.db.TestAutoRecompile;
import org.h2.test.db.TestBackup;
import org.h2.test.db.TestBigDb;
import org.h2.test.db.TestBigResult;
import org.h2.test.db.TestCases;
import org.h2.test.db.TestCheckpoint;
//import org.h2.test.db.TestCluster;
import org.h2.test.db.TestCompatibility;
import org.h2.test.db.TestCsv;
import org.h2.test.db.TestDeadlock;
import org.h2.test.db.TestEncryptedDb;
import org.h2.test.db.TestExclusive;
import org.h2.test.db.TestFullText;
import org.h2.test.db.TestFunctionOverload;
import org.h2.test.db.TestFunctions;
import org.h2.test.db.TestIndex;
import org.h2.test.db.TestLargeBlob;
import org.h2.test.db.TestLinkedTable;
import org.h2.test.db.TestListener;
import org.h2.test.db.TestLob;
import org.h2.test.db.TestLogFile;
import org.h2.test.db.TestMemoryUsage;
import org.h2.test.db.TestMultiConn;
import org.h2.test.db.TestMultiDimension;
import org.h2.test.db.TestMultiThread;
import org.h2.test.db.TestMultiThreadedKernel;
import org.h2.test.db.TestOpenClose;
import org.h2.test.db.TestOptimizations;
import org.h2.test.db.TestOutOfMemory;
import org.h2.test.db.TestPowerOff;
import org.h2.test.db.TestReadOnly;
import org.h2.test.db.TestRights;
import org.h2.test.db.TestRunscript;
import org.h2.test.db.TestSQLInjection;
import org.h2.test.db.TestScript;
import org.h2.test.db.TestScriptSimple;
import org.h2.test.db.TestSequence;
import org.h2.test.db.TestSessionsLocks;
import org.h2.test.db.TestSpaceReuse;
import org.h2.test.db.TestSpeed;
import org.h2.test.db.TestTempTables;
import org.h2.test.db.TestTransaction;
import org.h2.test.db.TestTriggersConstraints;
import org.h2.test.db.TestTwoPhaseCommit;
import org.h2.test.db.TestView;
import org.junit.Test;

import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;
import com.newatlanta.h2.store.fs.FileSystemGae;

public class GaeH2TestCase extends GaeVfsTestCase {
    
    private SecurityManager systemSecurityManager;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty( "h2.runFinalize", Boolean.FALSE.toString() );
        systemSecurityManager = System.getSecurityManager();
        System.setSecurityManager( new CustomSecurityManager( systemSecurityManager ) );
        FileSystem.register( FileSystemGae.getInstance() );
    }
    
    @Override
    public void tearDown() throws Exception {
        System.setSecurityManager( systemSecurityManager );
        systemSecurityManager = null;
        super.tearDown();
    }
    
    private void runTest( String className ) throws Exception {
        TestAll conf = new TestAll();
        conf.jdk14 = false;
        conf.googleAppEngine = true;
        TestBase.createCaller( className ).init( conf ).test();
    }
    
    @Test
    public void testScriptSimple() throws Exception {
        runTest( TestScriptSimple.class.getName() );
    }
    
    @Test
    public void testScript() throws Exception {
        runTest( TestScript.class.getName() );
    }
    
    @Test
    public void testAlter() throws Exception {
        runTest( TestAlter.class.getName() );
    }
    
    @Test
    public void testAutoRecompile() throws Exception {
        runTest( TestAutoRecompile.class.getName() );
    }
    
    @Test
    public void testBackup() throws Exception {
        runTest( TestBackup.class.getName() );
    }
    
    @Test
    public void testBigDb() throws Exception {
        runTest( TestBigDb.class.getName() );
    }
    
    @Test
    public void testBigResult() throws Exception {
        runTest( TestBigResult.class.getName() );
    }
    
    @Test
    public void testCases() throws Exception {
        runTest( TestCases.class.getName() );
    }
    
    @Test
    public void testCheckpoint() throws Exception {
        runTest( TestCheckpoint.class.getName() );
    }
    
//    @Test
//    public void testCluster() throws Exception {
//        runTest( TestCluster.class.getName() );
//    }
    
    @Test
    public void testCompatibility() throws Exception {
        runTest( TestCompatibility.class.getName() );
    }
    
    @Test
    public void testCsv() throws Exception {
        runTest( TestCsv.class.getName() );
    }
    
    @Test
    public void testDeadlock() throws Exception {
        runTest( TestDeadlock.class.getName() );
    }
    
    @Test
    public void testEncryptedDb() throws Exception {
        runTest( TestEncryptedDb.class.getName() );
    }
    
    @Test
    public void testExclusive() throws Exception {
        runTest( TestExclusive.class.getName() );
    }
    
    @Test
    public void testFullText() throws Exception {
        runTest( TestFullText.class.getName() );
    }
    
    @Test
    public void testFunctionOverload() throws Exception {
        runTest( TestFunctionOverload.class.getName() );
    }
    
    @Test
    public void testFunctions() throws Exception {
        runTest( TestFunctions.class.getName() );
    }
    
    @Test
    public void testIndex() throws Exception {
        runTest( TestIndex.class.getName() );
    }
    
    @Test
    public void testLargeBlob() throws Exception {
        runTest( TestLargeBlob.class.getName() );
    }
    
    @Test
    public void testLinkedTable() throws Exception {
        runTest( TestLinkedTable.class.getName() );
    }
    
    @Test
    public void testListener() throws Exception {
        runTest( TestListener.class.getName() );
    }
    
    @Test
    public void testLob() throws Exception {
        runTest( TestLob.class.getName() );
    }
    
    @Test
    public void testLogFile() throws Exception {
        runTest( TestLogFile.class.getName() );
    }
    
    @Test
    public void testMemoryUsage() throws Exception {
        runTest( TestMemoryUsage.class.getName() );
    }
    
    @Test
    public void testMultiConn() throws Exception {
        runTest( TestMultiConn.class.getName() );
    }
    
    @Test
    public void testMultiDimension() throws Exception {
        runTest( TestMultiDimension.class.getName() );
    }
    
    @Test
    public void testMultiThread() throws Exception {
        runTest( TestMultiThread.class.getName() );
    }
    
    @Test
    public void testMultiThreadedKernel() throws Exception {
        runTest( TestMultiThreadedKernel.class.getName() );
    }
    
    @Test
    public void testOpenClose() throws Exception {
        runTest( TestOpenClose.class.getName() );
    }
    
    @Test
    public void testOptimizations() throws Exception {
        runTest( TestOptimizations.class.getName() );
    }
    
    @Test
    public void testOutOfMemory() throws Exception {
        runTest( TestOutOfMemory.class.getName() );
    }
    
    @Test
    public void testPowerOff() throws Exception {
        runTest( TestPowerOff.class.getName() );
    }
    
    @Test
    public void testReadOnly() throws Exception {
        runTest( TestReadOnly.class.getName() );
    }
    
    @Test
    public void testRights() throws Exception {
        runTest( TestRights.class.getName() );
    }
    
    @Test
    public void testRunscript() throws Exception {
        runTest( TestRunscript.class.getName() );
    }
    
    @Test
    public void testSQLInjection() throws Exception {
        runTest( TestSQLInjection.class.getName() );
    }
    
    @Test
    public void testSessionsLocks() throws Exception {
        runTest( TestSessionsLocks.class.getName() );
    }
    
    @Test
    public void testSequence() throws Exception {
        runTest( TestSequence.class.getName() );
    }
    
    @Test
    public void testSpaceReuse() throws Exception {
        runTest( TestSpaceReuse.class.getName() );
    }
    
    @Test
    public void testSpeed() throws Exception {
        runTest( TestSpeed.class.getName() );
    }
    
    @Test
    public void testTempTables() throws Exception {
        runTest( TestTempTables.class.getName() );
    }
    
    @Test
    public void testTransaction() throws Exception {
        runTest( TestTransaction.class.getName() );
    }
    
    @Test
    public void testTriggersConstraints() throws Exception {
        runTest( TestTriggersConstraints.class.getName() );
    }
    
    @Test
    public void testTwoPhaseCommit() throws Exception {
        runTest( TestTwoPhaseCommit.class.getName() );
    }
    
    @Test
    public void testView() throws Exception {
        runTest( TestView.class.getName() );
    }
    
    /**
     * Custom security manager that prevents the H2 log writer thread
     * (WriterThread) from running.
     *
     */
    private class CustomSecurityManager extends SecurityManager {
        
        SecurityManager securityManager;
        
        private CustomSecurityManager( SecurityManager securityManager ) {
            this.securityManager = securityManager;
        }
        
        @Override
        public void checkPermission( Permission perm ) {
            if ( securityManager != null ) {
                securityManager.checkPermission( perm );
            }
        }

        @Override
        public void checkAccess( Thread t ) {
            if ( t.getName().startsWith( "H2 Log Writer" ) ) {
                throw new AccessControlException( "H2 Log Writer" );
            }
        }
    }
}
