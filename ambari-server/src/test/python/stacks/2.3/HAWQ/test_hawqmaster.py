#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
import  resource_management.libraries.functions

@patch.object(resource_management.libraries.functions, 'check_process_status', new = MagicMock())
class TestHawqMaster(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = 'HAWQ/2.0.0/package'
  STACK_VERSION = '2.3'
  GPADMIN = 'gpadmin'
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def __asserts_for_configure(self):

    self.assertResourceCalled('Group', self.GPADMIN,
        ignore_failures = True
        )

    self.assertResourceCalled('User', self.GPADMIN,
        gid = self.GPADMIN,
        groups = ['gpadmin', u'hadoop'],
        ignore_failures = True,
        password = 'saNIJ3hOyqasU'
        )

    self.assertResourceCalled('Execute', 'chown -R gpadmin:gpadmin /usr/local/hawq/',
        timeout = 600
        )

    self.assertResourceCalled('XmlConfig', 'hdfs-client.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['hdfs-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'yarn-client.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['yarn-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['yarn-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'hawq-site.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['hawq-site'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hawq-site'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('File', '/usr/local/hawq/etc/hawq_check.cnf',
        content = self.getConfig()['configurations']['hawq-check-env']['content'],
        owner = self.GPADMIN,
        group = self.GPADMIN,
        mode=0644
        )

    self.assertResourceCalled('File', '/usr/local/hawq/etc/slaves',
        content = InlineTemplate('c6401.ambari.apache.org\nc6402.ambari.apache.org\nc6403.ambari.apache.org\n\n'),
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('Directory', '/data/hawq/master',
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )

    self.assertResourceCalled('Execute', 'chmod 700 /data/hawq/master',
        user = 'root',
        timeout =  600
        )

    self.assertResourceCalled('Directory', '/tmp/hawq/master',
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )


  @patch ('hawqmaster.common.__set_osparams')
  def test_configure_default(self, set_osparams_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'configure',
        config_file ='hawq_default.json',
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  def test_install_default(self, set_osparams_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'install',
        config_file ='hawq_default.json',
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  def test_start_default(self, set_osparams_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'start',
        config_file ='hawq_default.json',
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()

    self.assertResourceCalled('HdfsResource', '/hawq_default',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        type = 'directory',
        action = ['create_on_execute'],
        owner = self.GPADMIN,
        group = self.GPADMIN,
        user = u'hdfs',
        mode = 493,
        security_enabled = False,
        kinit_path_local = '/usr/bin/kinit',
        recursive_chown = True,
        keytab = UnknownConfigurationMock(),
        principal_name = UnknownConfigurationMock(),
        )

    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        action = ['execute'],
        user = u'hdfs',
        security_enabled = False,
        kinit_path_local = '/usr/bin/kinit',
        keytab = UnknownConfigurationMock(),
        principal_name = UnknownConfigurationMock()
        )

    self.assertResourceCalled('Execute', 'source /usr/local/hawq/greenplum_path.sh && hawq init master -a -v',
        logoutput = True, 
        not_if = None, 
        only_if = None, 
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  def test_stop_default(self, set_osparams_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'stop',
        config_file ='hawq_default.json',
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', 'source /usr/local/hawq/greenplum_path.sh && hawq stop master -M fast -a -v',
        logoutput = True, 
        not_if = None, 
        only_if = "netstat -tupln | egrep ':5432\\s' | egrep postgres",
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()
