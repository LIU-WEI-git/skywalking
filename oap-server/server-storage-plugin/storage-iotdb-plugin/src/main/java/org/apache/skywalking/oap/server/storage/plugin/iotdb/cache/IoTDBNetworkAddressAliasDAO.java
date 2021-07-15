/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.storage.plugin.iotdb.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.analysis.manual.networkalias.NetworkAddressAlias;
import org.apache.skywalking.oap.server.core.storage.StorageData;
import org.apache.skywalking.oap.server.core.storage.cache.INetworkAddressAliasDAO;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.IoTDBClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IoTDBNetworkAddressAliasDAO implements INetworkAddressAliasDAO {
    private final NetworkAddressAlias.Builder storageBuilder = new NetworkAddressAlias.Builder();
    private final IoTDBClient client;

    public IoTDBNetworkAddressAliasDAO(final IoTDBClient client) {
        this.client = client;
    }

    @Override
    public List<NetworkAddressAlias> loadLastUpdate(long timeBucket) {
        StringBuilder query = new StringBuilder();
        query.append("select * from ").append(client.getStorageGroup()).append(IoTDBClient.DOT)
                .append(NetworkAddressAlias.INDEX_NAME).append(" where ")
                .append(NetworkAddressAlias.LAST_UPDATE_TIME_BUCKET).append(" >= ").append(timeBucket);

        List<NetworkAddressAlias> networkAddressAliases = new ArrayList<>();
        try {
            List<? super StorageData> storageDataList = client.queryForList(NetworkAddressAlias.INDEX_NAME,
                    query.toString(), storageBuilder);
            storageDataList.forEach(storageData -> networkAddressAliases.add((NetworkAddressAlias) storageData));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return networkAddressAliases;
    }
}
