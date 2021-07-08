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
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flipkart.kloud.config.ConfigClient;
import com.flipkart.kloud.config.DynamicBucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: amithsha.s@flipkart.com (10/Mar)
 * Simple class to map the ip and SMD No this returns the rack name.
 * </p>
 * <p>
 * This class uses the configuration parameter {@code
 * {net.topology.confd.bucket.name} to locate the bucket.
 * </p>
 * <p>
 * Calls to {@link #resolve(List)} will look up the address as defined in the
 * mapping file. If no entry corresponding to the address is found, the value
 * {@code /default-rack} is returned.
 * </p>
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class FdpConfdMapping extends CachedDNSToSwitchMapping {

    private static final Logger LOG = LoggerFactory.getLogger(FdpConfdMapping.class);

    public FdpConfdMapping() {
        super(new RawFdpConfdMapping());
    }

    private RawFdpConfdMapping getRawMapping() {
        return (RawFdpConfdMapping) rawMapping;
    }

    @Override
    public Configuration getConf() {
        return getRawMapping().getConf();
    }

    @Override
    public void setConf(Configuration conf) {
        super.setConf(conf);
        getRawMapping().setConf(conf);
    }

    @Override
    public void reloadCachedMappings() {
        super.reloadCachedMappings();
        getRawMapping().reloadCachedMappings();
    }

    private static JSONObject readConfigBucket(String bucketName) throws IOException {
        ConfigClient client = new ConfigClient("10.47.0.101", 80, 1);
        JSONObject j = new JSONObject();
        JSONObject configJson = new JSONObject();

        try {
            DynamicBucket b = client.getDynamicBucket(bucketName);
            j = new JSONObject(b.getBucketJSON().toString());
            configJson = new JSONObject(j.get("keys").toString());
        }

        catch (Exception e) {
            System.out.println(e);
        }
        return configJson;
    }


    private static final class RawFdpConfdMapping extends Configured
            implements DNSToSwitchMapping {

        private Map<String, String> map;

        private Map<String, String> load() {
            Map<String, String> loadMap = new HashMap<String, String>();
            try {
                String bucket = getConf().get(NET_TOPOLOGY_CONFIG_BUCKET_KEY, null);
                System.out.println(bucket);
                if (bucket == null || bucket.length() == 0) {
                    LOG.warn(NET_TOPOLOGY_CONFIG_BUCKET_KEY + " not configured properly. ");
                    return null;
                }
                else {
                    try {
                        JSONObject confdOut = FdpConfdMapping.readConfigBucket(bucket);
                        JSONArray key = confdOut.names();
                        if (key != null) {
                            for (int i = 0; i < key.length(); i++) {
                                String ckey = key.getString(i);
                                String value = confdOut.getString(ckey);
                                loadMap.put(ckey, value);
                            }
                        }
                    }
                    catch (NullPointerException e){
                        LOG.warn("Curling the bucket failed", e);
                    }

                }
            }
            catch (Exception e){
                LOG.warn("Curling the bucket failed", e);
                return null;
            }
            return loadMap;
        }

        @Override
        public synchronized List<String> resolve(List<String> names) {
            if (map == null) {
                map = load();
                if (map == null) {
                    LOG.warn("Failed to read FDP Confd Bucket Mapping. " +
                            "/ch/smd-unassigned" + " will be used for all nodes.");
                    map = new HashMap<String, String>();
                }
            }
            List<String> results = new ArrayList<String>(names.size());
            for (String name : names) {
                String result = map.get(name);
                if (result != null) {
                    String smdName = String.format("/ch/smd-%s", result);
                    results.add(smdName);
                } else {
                    results.add("/ch/smd-unassigned");
                }
            }
            return results;
        }

        @Override
        public void reloadCachedMappings() {
            Map<String, String> newMap = load();
            if (newMap == null) {
                LOG.error("Failed to reload the FDP Confd Bucket Mapping.  The cached " +
                        "mappings will not be cleared.");
            } else {
                synchronized(this) {
                    map = newMap;
                }
            }
        }

        @Override
        public void reloadCachedMappings(List<String> names) {
            // FdpConfdMapping has to reload all mappings at once, so no chance to
            // reload mappings on specific nodes
            reloadCachedMappings();
        }
    }
}
