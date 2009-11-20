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
import org.h2.test.jaqu.AliasMapTest;
import org.h2.test.jaqu.SamplesTest;
import org.h2.test.jaqu.UpdateTest;
import org.h2.test.jdbc.TestBatchUpdates;
import org.h2.test.jdbc.TestCallableStatement;
import org.h2.test.jdbc.TestCancel;
import org.h2.test.jdbc.TestDatabaseEventListener;
import org.h2.test.jdbc.TestDriver;
import org.h2.test.jdbc.TestManyJdbcObjects;
import org.h2.test.jdbc.TestMetaData;
import org.h2.test.jdbc.TestNativeSQL;
import org.h2.test.jdbc.TestPreparedStatement;
import org.h2.test.jdbc.TestResultSet;
import org.h2.test.jdbc.TestStatement;
import org.h2.test.jdbc.TestTransactionIsolation;
import org.h2.test.jdbc.TestUpdatableResultSet;
import org.h2.test.jdbc.TestZloty;
import org.h2.test.jdbcx.TestConnectionPool;
import org.h2.test.jdbcx.TestDataSource;
import org.h2.test.jdbcx.TestXA;
import org.h2.test.jdbcx.TestXASimple;
import org.h2.test.mvcc.TestMvcc1;
import org.h2.test.mvcc.TestMvcc2;
import org.h2.test.mvcc.TestMvcc3;
import org.h2.test.mvcc.TestMvccMultiThreaded;
import org.h2.test.rowlock.TestRowLocks;
import org.h2.test.server.TestNestedLoop;
import org.h2.test.synth.TestBtreeIndex;
import org.h2.test.synth.TestCrashAPI;
import org.h2.test.synth.TestFuzzOptimizations;
import org.h2.test.synth.TestMultiThreaded;
import org.h2.test.synth.TestRandomSQL;
import org.h2.test.unit.TestBitField;
import org.h2.test.unit.TestCache;
import org.h2.test.unit.TestClearReferences;
import org.h2.test.unit.TestCompress;
import org.h2.test.unit.TestDataPage;
import org.h2.test.unit.TestDate;
import org.h2.test.unit.TestDateIso8601;
import org.h2.test.unit.TestFile;
import org.h2.test.unit.TestIntArray;
import org.h2.test.unit.TestIntIntHashMap;
import org.h2.test.unit.TestMathUtils;
import org.h2.test.unit.TestOverflow;
import org.h2.test.unit.TestPattern;
import org.h2.test.unit.TestRecovery;
import org.h2.test.unit.TestScriptReader;
import org.h2.test.unit.TestSecurity;
import org.h2.test.unit.TestStringCache;
import org.h2.test.unit.TestStringUtils;
import org.h2.test.unit.TestValue;
import org.h2.test.unit.TestValueHashMap;
import org.junit.Test;

import com.newatlanta.appengine.h2.store.fs.FileSystemGae;
import com.newatlanta.appengine.junit.vfs.gae.GaeVfsTestCase;

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
        conf.googleAppEngine = true;
        //conf.big = true;
        //conf.traceTest = true;
        TestBase.createCaller( className ).init( conf ).test();
    }
    
    // db

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
    public void testExclusive() throws Exception {
        runTest( TestExclusive.class.getName() );
    }
    
    @Test
    public void testEncryptedDb() throws Exception {
        runTest( TestEncryptedDb.class.getName() );
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
        // if the TestLob.testLobDelete() test fails, it's probably because the
        // file lastModifiedTime isn't being set properly; H2 assumes this is set
        // only when the file is opened for writing
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
    
    // jaqu
    
    @Test
    public void testAliasMap() throws Exception {
        runTest( AliasMapTest.class.getName() );
    }
    
    @Test
    public void testSamples() throws Exception {
        runTest( SamplesTest.class.getName() );
    }
    
    @Test
    public void testUpdate() throws Exception {
        runTest( UpdateTest.class.getName() );
    }

    // jdbc
    
    @Test
    public void testBatchUpdates() throws Exception {
        runTest( TestBatchUpdates.class.getName() );
    }
    
    @Test
    public void testCallableStatement() throws Exception {
        runTest( TestCallableStatement.class.getName() );
    }
    
    @Test
    public void testCancel() throws Exception {
        runTest( TestCancel.class.getName() );
    }
    
    @Test
    public void testDatabaseEventListener() throws Exception {
        runTest( TestDatabaseEventListener.class.getName() );
    }
    
    @Test
    public void testDriver() throws Exception {
        runTest( TestDriver.class.getName() );
    }
    
    @Test
    public void testManyJdbcObjects() throws Exception {
        runTest( TestManyJdbcObjects.class.getName() );
    }
    
    @Test
    public void testMetaData() throws Exception {
        runTest( TestMetaData.class.getName() );
    }
    
    @Test
    public void testNativeSQL() throws Exception {
        runTest( TestNativeSQL.class.getName() );
    }
    
    @Test
    public void testPreparedStatement() throws Exception {
        runTest( TestPreparedStatement.class.getName() );
    }
    
    @Test
    public void testResultSet() throws Exception {
        runTest( TestResultSet.class.getName() );
    }
    
    @Test
    public void testStatement() throws Exception {
        runTest( TestStatement.class.getName() );
    }
    
    @Test
    public void testTransactionIsolation() throws Exception {
        runTest( TestTransactionIsolation.class.getName() );
    }
    
    @Test
    public void testUpdatableResultSet() throws Exception {
        runTest( TestUpdatableResultSet.class.getName() );
    }
    
    @Test
    public void testZloty() throws Exception {
        runTest( TestZloty.class.getName() );
    }

    // jdbcx
    
    @Test
    public void testConnectionPool() throws Exception {
        runTest( TestConnectionPool.class.getName() );
    }
    
    @Test
    public void testDataSource() throws Exception {
        runTest( TestDataSource.class.getName() );
    }
    
    @Test
    public void testXA() throws Exception {
        runTest( TestXA.class.getName() );
    }
    
    @Test
    public void testXASimple() throws Exception {
        runTest( TestXASimple.class.getName() );
    }

    // server
    
    @Test
    public void testNestedLoop() throws Exception {
        runTest( TestNestedLoop.class.getName() );
    }

    // mvcc & row level locking
    
    @Test
    public void testMvcc1() throws Exception {
        runTest( TestMvcc1.class.getName() );
    }
    
    @Test
    public void testMvcc2() throws Exception {
        runTest( TestMvcc2.class.getName() );
    }
    
    @Test
    public void testMvcc3() throws Exception {
        runTest( TestMvcc3.class.getName() );
    }
    
    @Test
    public void testMvccMultiThreaded() throws Exception {
        runTest( TestMvccMultiThreaded.class.getName() );
    }
    
    @Test
    public void testRowLocks() throws Exception {
        runTest( TestRowLocks.class.getName() );
    }

    // synth
    
    @Test
    public void testBtreeIndex() throws Exception {
        runTest( TestBtreeIndex.class.getName() );
    }
    
    @Test
    public void testCrashAPI() throws Exception {
        runTest( TestCrashAPI.class.getName() );
    }
    
    @Test
    public void testFuzzOptimizations() throws Exception {
        runTest( TestFuzzOptimizations.class.getName() );
    }
    
    @Test
    public void testRandomSQL() throws Exception {
        runTest( TestRandomSQL.class.getName() );
    }
    
    @Test
    public void testMultiThreaded() throws Exception {
        runTest( TestMultiThreaded.class.getName() );
    }
    
    // testUnit
    
    @Test
    public void testBitField() throws Exception {
        runTest( TestBitField.class.getName() );
    }
    
    @Test
    public void testCache() throws Exception {
        runTest( TestCache.class.getName() );
    }
    
    @Test
    public void testClearReferences() throws Exception {
        runTest( TestClearReferences.class.getName() );
    }
    
    @Test
    public void testCompress() throws Exception {
        runTest( TestCompress.class.getName() );
    }
    
    @Test
    public void testDataPage() throws Exception {
        runTest( TestDataPage.class.getName() );
    }
    
    @Test
    public void testDate() throws Exception {
        runTest( TestDate.class.getName() );
    }
    
    @Test
    public void testDateIso8601() throws Exception {
        runTest( TestDateIso8601.class.getName() );
    }
    
    @Test
    public void testFile() throws Exception {
        runTest( TestFile.class.getName() );
    }
    
    @Test
    public void testIntArray() throws Exception {
        runTest( TestIntArray.class.getName() );
    }
    
    @Test
    public void testIntIntHashMap() throws Exception {
        runTest( TestIntIntHashMap.class.getName() );
    }
    
    @Test
    public void testMathUtils() throws Exception {
        runTest( TestMathUtils.class.getName() );
    }
    
    @Test
    public void testOverflow() throws Exception {
        runTest( TestOverflow.class.getName() );
    }
    
    @Test
    public void testPattern() throws Exception {
        runTest( TestPattern.class.getName() );
    }
    
    @Test
    public void testRecovery() throws Exception {
        runTest( TestRecovery.class.getName() );
    }
    
    @Test
    public void testScriptReader() throws Exception {
        runTest( TestScriptReader.class.getName() );
    }
    
    @Test
    public void testSecurity() throws Exception {
        runTest( TestSecurity.class.getName() );
    }
    
    @Test
    public void testStringCache() throws Exception {
        runTest( TestStringCache.class.getName() );
    }
    
    @Test
    public void testStringUtils() throws Exception {
        runTest( TestStringUtils.class.getName() );
    }
    
    @Test
    public void testValue() throws Exception {
        runTest( TestValue.class.getName() );
    }
    
    @Test
    public void testValueHashMap() throws Exception {
        runTest( TestValueHashMap.class.getName() );
    }
    
    @Test
    public void testValueMemory() throws Exception {
        runTest( TestRandomSQL.class.getName() );
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
