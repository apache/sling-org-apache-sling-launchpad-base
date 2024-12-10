/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.base.impl.bootstrapcommands;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.framework.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BootstrapCommandFileTest {
    private final Logger logger = new Logger();
    private final File nonExistentFile = new File("/nonexistent." + System.currentTimeMillis());
    private File dataFile;
    private File cmdFile;
    private BundleContext bundleContext;

    @Before
    public void setUp() throws IOException, BundleException {
        dataFile = File.createTempFile(getClass().getSimpleName(), "txt");

        final Bundle b1 = Mockito.mock(Bundle.class);
        Mockito.when(b1.getSymbolicName()).thenReturn("somebundle");
        Mockito.when(b1.getHeaders()).thenReturn(new Hashtable<String, String>());
        Mockito.when(b1.getVersion()).thenReturn(new Version("1.0.0"));

        final Bundle[] bundles = {b1};

        bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.getDataFile(Mockito.anyString())).thenReturn(dataFile);
        Mockito.when(bundleContext.getBundles()).thenReturn(bundles);
        Mockito.when(bundleContext.getServiceReference(Mockito.anyString())).thenReturn(null);

        cmdFile = File.createTempFile(getClass().getSimpleName(), "cmd");
        final PrintWriter w = new PrintWriter(new FileWriter(cmdFile));
        w.println("# Test command file, this is a comment");
        w.println("uninstall somebundle 1.0");
        w.println("#another comment");
        w.println("uninstall otherbundle 1.0");
        w.flush();
        w.close();
    }

    @After
    public void tearDown() throws IOException {
        dataFile.delete();
        cmdFile.delete();
    }

    @Test
    public void testNoFileNoExecution() {
        final BootstrapCommandFile bcf = new BootstrapCommandFile(logger, nonExistentFile);
        assertFalse("Expecting anythingToExecute false for non-existing file", bcf.anythingToExecute(bundleContext));
    }

    @Test
    public void testExecuteOnceOnly() throws IOException {
        final BootstrapCommandFile bcf = new BootstrapCommandFile(logger, cmdFile);
        assertTrue("Expecting anythingToExecute true for existing file", bcf.anythingToExecute(bundleContext));
        assertEquals("Expecting two commands to be executed", false, bcf.execute(bundleContext));
        assertFalse("Expecting anythingToExecute false after execution", bcf.anythingToExecute(bundleContext));
    }

    @Test
    public void testParsing() throws IOException {
        final BootstrapCommandFile bcf = new BootstrapCommandFile(logger, cmdFile);
        final String cmdString = "# a comment\n"
                + "uninstall symbolicname1 1.0\n"
                + "\n"
                + "# another comment\n"
                + "uninstall symbolicname1 1.0\n";
        final List<Command> c = bcf.parse(new ByteArrayInputStream(cmdString.getBytes()));
        assertEquals("Expecting two commands after parsing", 2, c.size());
        int index = 0;
        for (Command cmd : c) {
            assertTrue("Expecting an UninstallBundleCommand at index " + index, cmd instanceof UninstallBundleCommand);
            index++;
        }
    }

    @Test
    public void testSyntaxError() throws IOException {
        final BootstrapCommandFile bcf = new BootstrapCommandFile(logger, cmdFile);
        final String cmdString = "# a comment\n"
                + "uninstall only_one_field\n"
                + "\n"
                + "# another comment\n"
                + "uninstall symbolicname1 1.0\n"
                + "\n"
                + "# another comment\n"
                + "uninstall three args 1.0\n";
        try {
            bcf.parse(new ByteArrayInputStream(cmdString.getBytes()));
            fail("Expecting IOException for syntax error");
        } catch (IOException ioe) {
            assertTrue(
                    "Exception message (" + ioe.getMessage() + ") should contain command line",
                    ioe.getMessage().contains("three args"));
        }
    }

    @Test
    public void testInvalidCommand() throws IOException {
        final BootstrapCommandFile bcf = new BootstrapCommandFile(logger, cmdFile);
        final String cmdString = "foo\n";
        try {
            bcf.parse(new ByteArrayInputStream(cmdString.getBytes()));
            fail("Expecting IOException for invalid command");
        } catch (IOException ioe) {
            assertTrue(
                    "Exception message (" + ioe.getMessage() + ") should contain command line",
                    ioe.getMessage().contains("foo"));
        }
    }
}
