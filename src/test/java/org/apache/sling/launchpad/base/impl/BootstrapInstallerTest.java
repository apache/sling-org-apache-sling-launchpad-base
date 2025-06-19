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
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.api.StartupMode;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing the bootstrap installer methods
 */
public class BootstrapInstallerTest {

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#extractFileName(java.lang.String)}
     * .
     */
    @Test
    public void testExtractFileNameForwardSlash() {
        String filename = BootstrapInstaller.extractFileName("myfile.html");
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("/things/myfile.html");
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/myfile.html");
        assertEquals("myfile.html", filename);

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#extractFileName(java.lang.String)}
     * .
     */
    @Test
    public void testExtractFileNameSeparatorChar() {
        String filename = BootstrapInstaller.extractFileName("myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("/things/myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName(
                "LOTS/of/random/things/myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/".replace('/', File.separatorChar));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/".replace('/', File.separatorChar));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#copyStreamToFile(java.io.InputStream, java.io.File)}
     * .
     */
    @Test
    public void testCopyStreamToFile() {
        InputStream stream = null;
        File to = null;
        File testDir = new File("testing");
        testDir.deleteOnExit(); // cleanup
        assertTrue(testDir.mkdir());

        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("holaworld.jar");
        assertNotNull(stream); // cleanup
        to = new File(testDir, "test.jar");
        to.deleteOnExit();
        try {
            BootstrapInstaller.copyStreamToFile(stream, to);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        File copy = new File(testDir, "test.jar");
        try {
            FileInputStream copyStream = new FileInputStream(copy);
            byte[] copyData = new byte[copyStream.available()];
            copyStream.read(copyData);
            copyStream.close();
            FileInputStream origStream = new FileInputStream(copy);
            byte[] origData = new byte[origStream.available()];
            origStream.read(origData);
            origStream.close();
            assertArrayEquals(copyData, origData);
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        try {
            BootstrapInstaller.copyStreamToFile(null, to);
            fail("should have thrown exception");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            BootstrapInstaller.copyStreamToFile(stream, null);
            fail("should have thrown exception");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#isBlank(java.lang.String)}
     * .
     */
    @Test
    public void testIsBlank() {
        assertTrue(BootstrapInstaller.isBlank(null));
        assertTrue(BootstrapInstaller.isBlank(""));
        assertTrue(BootstrapInstaller.isBlank(" "));

        assertFalse(BootstrapInstaller.isBlank("Test"));
        assertFalse(BootstrapInstaller.isBlank(" asdf "));
    }

    @Test
    public void testIgnoreSameVersionSameStartLevel()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // prepare already installed bundle
        Bundle mockInstalledBundle = mock(Bundle.class);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_VERSION, "1.1.0");
        when(mockInstalledBundle.getSymbolicName()).thenReturn("test.bundle");
        when(mockInstalledBundle.getHeaders()).thenReturn(headers);

        BundleStartLevel mockBundleStartLevel = mock(BundleStartLevel.class);
        when(mockInstalledBundle.adapt(BundleStartLevel.class)).thenReturn(mockBundleStartLevel);
        when(mockBundleStartLevel.getStartLevel()).thenReturn(1);

        // new bundle to install
        Attributes mockNewBundleAttributes = mock(Attributes.class);
        when(mockNewBundleAttributes.getValue(Constants.BUNDLE_VERSION)).thenReturn("1.1.0");
        Manifest mockNewBundleManifest = mock(Manifest.class);
        when(mockNewBundleManifest.getMainAttributes()).thenReturn(mockNewBundleAttributes);

        assertTrue((Boolean) ignoreMethod.invoke(bsi, mockInstalledBundle, 1, mockNewBundleManifest));
    }

    @Test
    public void testIgnoreSnapshotVersion()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // prepare already installed bundle
        Bundle mockInstalledBundle = mock(Bundle.class);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_VERSION, "1.1.0");
        when(mockInstalledBundle.getSymbolicName()).thenReturn("test.bundle");
        when(mockInstalledBundle.getHeaders()).thenReturn(headers);

        BundleStartLevel mockBundleStartLevel = mock(BundleStartLevel.class);
        when(mockInstalledBundle.adapt(BundleStartLevel.class)).thenReturn(mockBundleStartLevel);
        when(mockBundleStartLevel.getStartLevel()).thenReturn(1);

        // new bundle to install
        Attributes mockNewBundleAttributes = mock(Attributes.class);
        when(mockNewBundleAttributes.getValue(Constants.BUNDLE_VERSION)).thenReturn("1.1.0.SNAPSHOT");
        Manifest mockNewBundleManifest = mock(Manifest.class);
        when(mockNewBundleManifest.getMainAttributes()).thenReturn(mockNewBundleAttributes);

        assertFalse((Boolean) ignoreMethod.invoke(bsi, mockInstalledBundle, 1, mockNewBundleManifest));
    }

    @Test
    public void testIgnoreDifferentStartLevel()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // prepare already installed bundle
        Bundle mockInstalledBundle = mock(Bundle.class);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_VERSION, "1.1.0");
        when(mockInstalledBundle.getSymbolicName()).thenReturn("test.bundle");
        when(mockInstalledBundle.getHeaders()).thenReturn(headers);

        BundleStartLevel mockBundleStartLevel = mock(BundleStartLevel.class);
        when(mockInstalledBundle.adapt(BundleStartLevel.class)).thenReturn(mockBundleStartLevel);
        when(mockBundleStartLevel.getStartLevel()).thenReturn(20);

        // new bundle to install
        Attributes mockNewBundleAttributes = mock(Attributes.class);
        when(mockNewBundleAttributes.getValue(Constants.BUNDLE_VERSION)).thenReturn("1.1.0");
        Manifest mockNewBundleManifest = mock(Manifest.class);
        when(mockNewBundleManifest.getMainAttributes()).thenReturn(mockNewBundleAttributes);

        assertFalse((Boolean) ignoreMethod.invoke(bsi, mockInstalledBundle, 1, mockNewBundleManifest));
    }

    @Test
    public void testIgnoreNewBundleHigherVersion()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // prepare already installed bundle
        Bundle mockInstalledBundle = mock(Bundle.class);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_VERSION, "1.1.0");
        when(mockInstalledBundle.getSymbolicName()).thenReturn("test.bundle");
        when(mockInstalledBundle.getHeaders()).thenReturn(headers);

        BundleStartLevel mockBundleStartLevel = mock(BundleStartLevel.class);
        when(mockInstalledBundle.adapt(BundleStartLevel.class)).thenReturn(mockBundleStartLevel);
        when(mockBundleStartLevel.getStartLevel()).thenReturn(1);

        // new bundle to install
        Attributes mockNewBundleAttributes = mock(Attributes.class);
        when(mockNewBundleAttributes.getValue(Constants.BUNDLE_VERSION)).thenReturn("1.2.0");
        Manifest mockNewBundleManifest = mock(Manifest.class);
        when(mockNewBundleManifest.getMainAttributes()).thenReturn(mockNewBundleAttributes);

        assertFalse((Boolean) ignoreMethod.invoke(bsi, mockInstalledBundle, 1, mockNewBundleManifest));
    }

    @Test
    public void testIgnoreNewBundleLowerVersion()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // prepare already installed bundle
        Bundle mockInstalledBundle = mock(Bundle.class);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_VERSION, "1.1.0");
        when(mockInstalledBundle.getSymbolicName()).thenReturn("test.bundle");
        when(mockInstalledBundle.getHeaders()).thenReturn(headers);

        BundleStartLevel mockBundleStartLevel = mock(BundleStartLevel.class);
        when(mockInstalledBundle.adapt(BundleStartLevel.class)).thenReturn(mockBundleStartLevel);
        when(mockBundleStartLevel.getStartLevel()).thenReturn(1);

        // new bundle to install
        Attributes mockNewBundleAttributes = mock(Attributes.class);
        when(mockNewBundleAttributes.getValue(Constants.BUNDLE_VERSION)).thenReturn("1.0.0");
        Manifest mockNewBundleManifest = mock(Manifest.class);
        when(mockNewBundleManifest.getMainAttributes()).thenReturn(mockNewBundleAttributes);

        assertTrue((Boolean) ignoreMethod.invoke(bsi, mockInstalledBundle, 1, mockNewBundleManifest));
    }

    @Test
    public void testIgnoreNewBundleInstallation()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // prepare for invoking the installBundle method
        BundleContext mockBundleContext = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);
        LaunchpadContentProvider mockLaunchpadContentProvider = mock(LaunchpadContentProvider.class);
        BootstrapInstaller bsi =
                new BootstrapInstaller(mockBundleContext, mockLogger, mockLaunchpadContentProvider, StartupMode.UPDATE);

        Method ignoreMethod =
                BootstrapInstaller.class.getDeclaredMethod("ignore", Bundle.class, int.class, Manifest.class);
        ignoreMethod.setAccessible(true);

        // new bundle to install
        Manifest mockNewBundleManifest = mock(Manifest.class);

        assertFalse((Boolean) ignoreMethod.invoke(bsi, null, 1, mockNewBundleManifest));
    }

    // TODO eventually add in tests that create a context so we can test more
    // things in detail

}
