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
package org.apache.hadoop.net;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.NET_TOPOLOGY_CONFIG_BUCKET_KEY;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

public class TestFdpConfdMapping {
    private String hostName1 = "10.32.10.245";
    private String hostName2 = "10.32.102.1158";

    @Test
    public void testConfigBucket() throws IOException {

        String bucketName = "prod-fdp-smd-mapping";
        FdpConfdMapping mapping = new FdpConfdMapping();

        Configuration conf = new Configuration();
        conf.set(NET_TOPOLOGY_CONFIG_BUCKET_KEY, bucketName);
        mapping.setConf(conf);

        List<String> names = new ArrayList<String>();
        names.add(hostName1);
        names.add(hostName2);

        List<String> result = mapping.resolve(names);
        System.out.println(result);
        assertEquals(names.size(), result.size());
        assertEquals("/ch/smd-1", result.get(0));
        assertEquals("/ch/smd-unassigned", result.get(1));


    }

    @Test
    public void testEmptyConfigBucket() throws IOException {

        String bucketName = "";
        FdpConfdMapping mapping = new FdpConfdMapping();

        Configuration conf = new Configuration();
        conf.set(NET_TOPOLOGY_CONFIG_BUCKET_KEY, bucketName);
        mapping.setConf(conf);

        List<String> names = new ArrayList<String>();
        names.add(hostName1);
        names.add(hostName2);

        List<String> result = mapping.resolve(names);
        System.out.println(result);
        assertEquals(names.size(), result.size());
        assertEquals("/ch/smd-unassigned", result.get(0));
        assertEquals("/ch/smd-unassigned", result.get(1));


    }

    @Test
    public void testWrongConfigBucket() throws IOException {

        String bucketName = "prod-tets";
        FdpConfdMapping mapping = new FdpConfdMapping();

        Configuration conf = new Configuration();
        conf.set(NET_TOPOLOGY_CONFIG_BUCKET_KEY, bucketName);
        mapping.setConf(conf);

        List<String> names = new ArrayList<String>();
        names.add(hostName1);
        names.add(hostName2);

        List<String> result = mapping.resolve(names);
        System.out.println(result);
        assertEquals(names.size(), result.size());
        assertEquals("/ch/smd-unassigned", result.get(0));
        assertEquals("/ch/smd-unassigned", result.get(1));


    }
}
