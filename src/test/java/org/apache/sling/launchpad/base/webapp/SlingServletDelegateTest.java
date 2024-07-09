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
package org.apache.sling.launchpad.base.webapp;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;


public class SlingServletDelegateTest {

    @Mock
    private ServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    private SlingServletDelegate slingServletDelegate;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        slingServletDelegate = new SlingServletDelegate();
    }

    @Test
    public void testServiceWithNoDelegatee() throws Exception {
        slingServletDelegate.service(servletRequest, servletResponse);
        verify(servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test(expected = RuntimeException.class)
    public void testCalculateServletPackagesForVersionLessThan5() {
        String servletVersion = "3.1";
        int majorVersion = 3;

        slingServletDelegate.calculateServletPackages(servletVersion, majorVersion);
    }

    @Test
    public void testCalculateServletPackagesForVersion5() {
        String servletVersion = "5.0";
        int majorVersion = 5;
        String expectedPackages = "jakarta.servlet;jakarta.servlet.http;jakarta.servlet.descriptor;jakarta.servlet.annotation;version=5.0";

        // Get the servlet packages
        String actualPackages = slingServletDelegate.calculateServletPackages(servletVersion, majorVersion);
        // Verify
        assertEquals(expectedPackages, actualPackages);
    }

    @Test
    public void testCalculateServletPackagesForVersion6() {
        String servletVersion = "6.0";
        int majorVersion = 6;
        String expectedPackages = "jakarta.servlet;jakarta.servlet.http;jakarta.servlet.descriptor;jakarta.servlet.annotation;version=5.0"
                + ",jakarta.servlet;jakarta.servlet.http;jakarta.servlet.descriptor;jakarta.servlet.annotation;version=6.0";

        // Get the servlet packages
        String actualPackages = slingServletDelegate.calculateServletPackages(servletVersion, majorVersion);
        // Verify
        assertEquals(expectedPackages, actualPackages);
    }

}
