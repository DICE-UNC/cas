/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
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
package org.jasig.cas.monitor;

import org.jasig.cas.adaptors.ldap.AbstractLdapTests;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Unit test for {@link PooledConnectionFactoryMonitor} class.
 * @author Marvin S. Addison
 * @since 4.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/ldap-context.xml", "/ldap-poolmonitor-test.xml"})
public class PooledConnectionFactoryMonitorTests extends AbstractLdapTests {

    @Autowired
    @Qualifier("pooledLdapConnectionFactoryMonitor")
    private PooledConnectionFactoryMonitor monitor;

    @BeforeClass
    public static void bootstrap() throws Exception {
        initDirectoryServer();
    }

    @Test
    public void verifyObserve() throws Exception {
        assertEquals(StatusCode.OK, monitor.observe().getCode());
    }
}
