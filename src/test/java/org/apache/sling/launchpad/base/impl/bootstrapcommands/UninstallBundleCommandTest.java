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

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.apache.felix.framework.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/** Test the UninstallBundleCommand */
public class UninstallBundleCommandTest {
    private BundleContext bundleContext;
    private final Logger logger = new Logger();

    @Before
    public void setUp() throws Exception {
        final Bundle [] b = new Bundle[3];
        for(int i=0; i < b.length; i++) {
            b[i] = Mockito.mock(Bundle.class);
        }

        // b0 is in version range, will be uninstalled
        Mockito.when(b[0].getSymbolicName()).thenReturn("testbundle");
        Mockito.when(b[0].getHeaders()).thenReturn(new Hashtable<String, String>());
        Mockito.when(b[0].getVersion()).thenReturn(new Version("1.0.0"));

        // b1 is not in version range, not uninstalled
        Mockito.when(b[1].getSymbolicName()).thenReturn("testbundle");
        Mockito.when(b[1].getHeaders()).thenReturn(new Hashtable<String, String>());
        Mockito.when(b[1].getVersion()).thenReturn(new Version("2.0.0"));

        // b2 has different symbolic name, not uninstalled
        Mockito.when(b[2].getSymbolicName()).thenReturn("otherbundle");
        Mockito.when(b[2].getHeaders()).thenReturn(new Hashtable<String, String>());
        Mockito.when(b[2].getVersion()).thenReturn(new Version("1.0.0"));

        bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.getBundles()).thenReturn(b);
    }

    @Test
    public void testExplicitVersion() throws Exception {
        final UninstallBundleCommand proto = new UninstallBundleCommand();
        // v=1.0.0 should remove 1.0.0 only, not 2.0.0
        final Command cmd = proto.parse("uninstall testbundle 1.0.0");
        assertNotNull("Expecting parsing to succeed", cmd);
        cmd.execute(logger, bundleContext);
        Mockito.verify(bundleContext.getBundles()[0]).uninstall();
    }

    @Test
    public void testVersionRange() throws Exception {
        final UninstallBundleCommand proto = new UninstallBundleCommand();
        final String from1Includedto2NotIncluded = "[1,2)";
        final Command cmd = proto.parse("uninstall testbundle " + from1Includedto2NotIncluded);
        assertNotNull("Expecting parsing to succeed", cmd);
        cmd.execute(logger, bundleContext);
        Mockito.verify(bundleContext.getBundles()[0]).uninstall();
    }
}