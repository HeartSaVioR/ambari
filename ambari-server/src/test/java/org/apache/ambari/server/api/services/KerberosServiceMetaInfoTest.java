/*
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

package org.apache.ambari.server.api.services;

import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

public class KerberosServiceMetaInfoTest {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosServiceMetaInfoTest.class);
  private ServiceInfo serviceInfo = null;

  /* ********************************************************************************************
   * In the event we want to create a superclass that can be used to execute tests on service
   * metainfo data, the following methods may be useful
   * ******************************************************************************************** */
  private void testAutoDeploy(Map<String, AutoDeployInfo> expectedAutoDeployMap) throws AmbariException {
    Assert.assertNotNull(serviceInfo);

    List<ComponentInfo> componentList = serviceInfo.getComponents();

    Assert.assertNotNull(componentList);
    Assert.assertEquals(expectedAutoDeployMap.size(), componentList.size());

    for (ComponentInfo component : componentList) {
      Assert.assertTrue(expectedAutoDeployMap.containsKey(component.getName()));

      AutoDeployInfo expectedAutoDeploy = expectedAutoDeployMap.get(component.getName());
      AutoDeployInfo componentAutoDeploy = component.getAutoDeploy();

      if (expectedAutoDeploy == null)
        Assert.assertNull(componentAutoDeploy);
      else {
        Assert.assertNotNull(componentAutoDeploy);
        Assert.assertEquals(expectedAutoDeploy.isEnabled(), componentAutoDeploy.isEnabled());
        Assert.assertEquals(expectedAutoDeploy.getCoLocate(), componentAutoDeploy.getCoLocate());
      }
    }
  }

  private void testCardinality(HashMap<String, String> expectedCardinalityMap) throws AmbariException {
    Assert.assertNotNull(serviceInfo);

    List<ComponentInfo> componentList = serviceInfo.getComponents();

    Assert.assertNotNull(componentList);
    Assert.assertEquals(expectedCardinalityMap.size(), componentList.size());

    for (ComponentInfo component : componentList) {
      Assert.assertTrue(expectedCardinalityMap.containsKey(component.getName()));
      Assert.assertEquals(expectedCardinalityMap.get(component.getName()), component.getCardinality());
    }
  }

  protected void testDependencies(Map<String, Map<String, DependencyInfo>> expectedDependenciesMap) throws AmbariException {
    Assert.assertNotNull(serviceInfo);

    List<ComponentInfo> componentList = serviceInfo.getComponents();

    Assert.assertNotNull(componentList);
    Assert.assertEquals(expectedDependenciesMap.size(), componentList.size());

    for (ComponentInfo component : componentList) {
      Assert.assertTrue(expectedDependenciesMap.containsKey(component.getName()));

      Map<String, ? extends DependencyInfo> expectedDependencyMap = expectedDependenciesMap.get(component.getName());
      List<DependencyInfo> componentDependencies = component.getDependencies();

      if (expectedDependencyMap == null)
        Assert.assertNull(componentDependencies);
      else {
        Assert.assertEquals(expectedDependencyMap.size(), componentDependencies.size());

        for (DependencyInfo componentDependency : componentDependencies) {
          DependencyInfo expectedDependency = expectedDependencyMap.get(componentDependency.getComponentName());

          Assert.assertNotNull(expectedDependency);

          AutoDeployInfo expectedDependencyAutoDeploy = expectedDependency.getAutoDeploy();
          AutoDeployInfo componentDependencyAutoDeploy = componentDependency.getAutoDeploy();

          Assert.assertEquals(expectedDependency.getName(), componentDependency.getName());
          Assert.assertEquals(expectedDependency.getServiceName(), componentDependency.getServiceName());
          Assert.assertEquals(expectedDependency.getComponentName(), componentDependency.getComponentName());
          Assert.assertEquals(expectedDependency.getScope(), componentDependency.getScope());

          if (expectedDependencyAutoDeploy == null)
            Assert.assertNull(componentDependencyAutoDeploy);
          else {
            Assert.assertNotNull(componentDependencyAutoDeploy);
            Assert.assertEquals(expectedDependencyAutoDeploy.isEnabled(), componentDependencyAutoDeploy.isEnabled());
            Assert.assertEquals(expectedDependencyAutoDeploy.getCoLocate(), componentDependencyAutoDeploy.getCoLocate());
          }
        }
      }
    }
  }

  @Before
  public void before() throws Exception {
    File stackRoot = new File("src/main/resources/stacks");
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());

    AmbariMetaInfo metaInfo = createAmbariMetaInfo(stackRoot, new File("target/version"), true);
    metaInfo.init();

    serviceInfo = metaInfo.getService("HDP", "2.2", "KERBEROS");
  }
  /* ******************************************************************************************* */

  @Test
  public void test220Cardinality() throws Exception {
    testCardinality(new HashMap<String, String>() {
      {
        put("KDC_SERVER", "0-1");
        put("KERBEROS_CLIENT", "ALL");
      }
    });
  }

  @Test
  public void test220AutoDeploy() throws Exception {
    testAutoDeploy(new HashMap<String, AutoDeployInfo>() {
      {
        put("KDC_SERVER", null);
        put("KERBEROS_CLIENT", new AutoDeployInfo() {{
          setEnabled(true);
          setCoLocate(null);
        }});
      }
    });
  }

  @Test
  public void test220Dependencies() throws Exception {
    testDependencies(new HashMap<String, Map<String, DependencyInfo>>() {
      {
        put("KDC_SERVER", new HashMap<String, DependencyInfo>() {{
              put("KERBEROS_CLIENT",
                  new DependencyInfo() {{
                    setName("KERBEROS/KERBEROS_CLIENT");
                    setAutoDeploy(new AutoDeployInfo() {{
                      setEnabled(true);
                      setCoLocate(null);
                    }});
                    setScope("cluster");
                  }});
            }}
        );

        put("KERBEROS_CLIENT", new HashMap<String, DependencyInfo>());
      }
    });
  }

  private TestAmbariMetaInfo createAmbariMetaInfo(File stackRoot, File versionFile, boolean replayMocks) throws Exception {
    TestAmbariMetaInfo metaInfo = new TestAmbariMetaInfo(stackRoot, versionFile);
    if (replayMocks) {
      metaInfo.replayAllMocks();

      try {
        metaInfo.init();
      } catch(Exception e) {
        LOG.info("Error in initializing ", e);
        throw e;
      }
      waitForAllReposToBeResolved(metaInfo);
    }

    return metaInfo;
  }

  private void waitForAllReposToBeResolved(AmbariMetaInfo metaInfo) throws Exception {
    int maxWait = 45000;
    int waitTime = 0;
    StackManager sm = metaInfo.getStackManager();
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }
  }

  private static class TestAmbariMetaInfo extends AmbariMetaInfo {

    MetainfoDAO metaInfoDAO;
    AlertDefinitionDAO alertDefinitionDAO;
    AlertDefinitionFactory alertDefinitionFactory;
    OsFamily osFamily;

    public TestAmbariMetaInfo(File stackRoot, File serverVersionFile) throws Exception {
      super(stackRoot, serverVersionFile);
      // MetainfoDAO
      metaInfoDAO = createNiceMock(MetainfoDAO.class);
      Class<?> c = getClass().getSuperclass();
      Field f = c.getDeclaredField("metaInfoDAO");
      f.setAccessible(true);
      f.set(this, metaInfoDAO);

      // ActionMetadata
      ActionMetadata actionMetadata = new ActionMetadata();
      f = c.getDeclaredField("actionMetadata");
      f.setAccessible(true);
      f.set(this, actionMetadata);

      //AlertDefinitionDAO
      alertDefinitionDAO = createNiceMock(AlertDefinitionDAO.class);
      f = c.getDeclaredField("alertDefinitionDao");
      f.setAccessible(true);
      f.set(this, alertDefinitionDAO);

      //AlertDefinitionFactory
      //alertDefinitionFactory = createNiceMock(AlertDefinitionFactory.class);
      alertDefinitionFactory = new AlertDefinitionFactory();
      f = c.getDeclaredField("alertDefinitionFactory");
      f.setAccessible(true);
      f.set(this, alertDefinitionFactory);

      //AmbariEventPublisher
      AmbariEventPublisher ambariEventPublisher = new AmbariEventPublisher();
      f = c.getDeclaredField("eventPublisher");
      f.setAccessible(true);
      f.set(this, ambariEventPublisher);

      //OSFamily
      Configuration config = createNiceMock(Configuration.class);
      expect(config.getSharedResourcesDirPath()).andReturn("./src/test/resources").anyTimes();
      replay(config);
      osFamily = new OsFamily(config);
      f = c.getDeclaredField("os_family");
      f.setAccessible(true);
      f.set(this, osFamily);
    }

    public void replayAllMocks() {
      replay(metaInfoDAO, alertDefinitionDAO);
    }
  }

}
