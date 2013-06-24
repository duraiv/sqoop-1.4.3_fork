/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.sqoop.hbase;

import java.io.IOException;

import org.junit.Test;

/**
 * Test imports of tables into HBase.
 */
public class HBaseImportTest extends HBaseTestCase {

  @Test
  public void testBasicUsage() throws IOException {
    // Create the HBase table in Sqoop as we run the job.
    String [] argv = getArgv(true, "BasicUsage", "BasicColFam", true, null);
    String [] types = { "INT", "INT" };
    String [] vals = { "0", "1" };
    createTableWithColTypes(types, vals);
    runImport(argv);
    verifyHBaseCell("BasicUsage", "0", "BasicColFam", getColName(1), "1");
  }

  @Test
  public void testMissingTableFails() throws IOException {
    // Test that if the table doesn't exist, we fail unless we
    // explicitly create the table.
    String [] argv = getArgv(true, "MissingTable", "MissingFam", false, null);
    String [] types = { "INT", "INT" };
    String [] vals = { "0", "1" };
    createTableWithColTypes(types, vals);
    try {
      runImport(argv);
      fail("Expected IOException");
    } catch (IOException ioe) {
      LOG.info("Got exception -- ok; we expected that job to fail.");
    }
  }

  @Test
  public void testOverwriteSucceeds() throws IOException {
    // Test that we can create a table and then import immediately
    // back on top of it without problem.
    String [] argv = getArgv(true, "OverwriteT", "OverwriteF", true, null);
    String [] types = { "INT", "INT" };
    String [] vals = { "0", "1" };
    createTableWithColTypes(types, vals);
    runImport(argv);
    verifyHBaseCell("OverwriteT", "0", "OverwriteF", getColName(1), "1");
    // Run a second time.
    runImport(argv);
    verifyHBaseCell("OverwriteT", "0", "OverwriteF", getColName(1), "1");
  }

  @Test
  public void testStrings() throws IOException {
    String [] argv = getArgv(true, "stringT", "stringF", true, null);
    String [] types = { "INT", "VARCHAR(32)" };
    String [] vals = { "0", "'abc'" };
    createTableWithColTypes(types, vals);
    runImport(argv);
    verifyHBaseCell("stringT", "0", "stringF", getColName(1), "abc");
  }

  @Test
  public void testNulls() throws IOException {
    String [] argv = getArgv(true, "nullT", "nullF", true, null);
    String [] types = { "INT", "INT", "INT" };
    String [] vals = { "0", "42", "null" };
    createTableWithColTypes(types, vals);
    runImport(argv);

    // This cell should import correctly.
    verifyHBaseCell("nullT", "0", "nullF", getColName(1), "42");

    // This cell should not be placed in the results..
    verifyHBaseCell("nullT", "0", "nullF", getColName(2), null);
  }

  @Test
  public void testExitFailure() throws IOException {
    String [] types = { "INT", "INT", "INT" };
    String [] vals = { "0", "42", "43" };
    createTableWithColTypes(types, vals);

    String [] argv = getArgv(true, "NoHBaseT", "NoHBaseF", true, null);
    try {
      HBaseUtil.setAlwaysNoHBaseJarMode(true);
      runImport(argv);
    } catch (IOException e)  {
      return;
    } finally {
      HBaseUtil.setAlwaysNoHBaseJarMode(false);
    }

    fail("should have gotten exception");
  }

  @Test
  public void testNullRow() throws IOException {
    String [] argv = getArgv(true, "nullRowT", "nullRowF", true, null);
    String [] types = { "INT", "INT" };
    String [] vals = { "0", "null" };
    createTableWithColTypes(types, vals);
    runImport(argv);

    // This cell should not be placed in the results..
    verifyHBaseCell("nullRowT", "0", "nullRowF", getColName(1), null);

    int rowCount = countHBaseTable("nullRowT", "nullRowF");
    assertEquals(0, rowCount);
  }

  @Test
  public void testAddRowKey() throws IOException {
    String[] types = { "INT", "INT" };
    String[] vals = { "0", "1" };
    createTableWithColTypes(types, vals);

    String[] otherArg = getArgv(true, "addRowKeyT", "addRowKeyF", true, null);
    String[] argv = new String[otherArg.length + 2];
    argv[0] = "-D";
    argv[1] = "sqoop.hbase.add.row.key=true";
    System.arraycopy(otherArg, 0, argv, 2, otherArg.length);

    runImport(argv);

    // Row key should have been added
    verifyHBaseCell("addRowKeyT", "0", "addRowKeyF", getColName(0), "0");
    verifyHBaseCell("addRowKeyT", "0", "addRowKeyF", getColName(1), "1");
  }

  @Test
  public void testAddRowKeyDefault() throws IOException {
    String[] types = { "INT", "INT" };
    String[] vals = { "0", "1" };
    createTableWithColTypes(types, vals);

    String[] argv = getArgv(true, "addRowKeyDfT", "addRowKeyDfF", true, null);

    runImport(argv);

    // Row key should not be added by default
    verifyHBaseCell("addRowKeyDfT", "0", "addRowKeyDfF", getColName(0), null);
    verifyHBaseCell("addRowKeyDfT", "0", "addRowKeyDfF", getColName(1), "1");
  }
}
