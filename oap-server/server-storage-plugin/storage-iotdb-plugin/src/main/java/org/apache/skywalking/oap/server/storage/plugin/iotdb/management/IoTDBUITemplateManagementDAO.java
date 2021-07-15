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

package org.apache.skywalking.oap.server.storage.plugin.iotdb.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.management.ui.template.UITemplate;
import org.apache.skywalking.oap.server.core.query.input.DashboardSetting;
import org.apache.skywalking.oap.server.core.query.type.DashboardConfiguration;
import org.apache.skywalking.oap.server.core.query.type.TemplateChangeStatus;
import org.apache.skywalking.oap.server.core.storage.StorageData;
import org.apache.skywalking.oap.server.core.storage.StorageHashMapBuilder;
import org.apache.skywalking.oap.server.core.storage.management.UITemplateManagementDAO;
import org.apache.skywalking.oap.server.library.util.BooleanUtils;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.IoTDBClient;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.base.IoTDBInsertRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class IoTDBUITemplateManagementDAO implements UITemplateManagementDAO {
    private final IoTDBClient client;
    private final StorageHashMapBuilder<UITemplate> storageBuilder = new UITemplate.Builder();

    @Override
    public List<DashboardConfiguration> getAllTemplates(Boolean includingDisabled) throws IOException {
        StringBuilder query = new StringBuilder();
        query.append("select * from ").append(client.getStorageGroup()).append(IoTDBClient.DOT)
                .append(UITemplate.INDEX_NAME).append(" where 1=1");
        if (!includingDisabled) {
            query.append(" and ").append(UITemplate.DISABLED).append(" = ").append(BooleanUtils.FALSE);
        }

        List<DashboardConfiguration> dashboardConfigurationList = new ArrayList<>();
        List<? super StorageData> storageDataList = client.queryForList(UITemplate.INDEX_NAME, query.toString(),
                storageBuilder);
        storageDataList.forEach(storageData -> dashboardConfigurationList.add((DashboardConfiguration) storageData));
        return dashboardConfigurationList;
    }

    @Override
    public TemplateChangeStatus addTemplate(DashboardSetting setting) throws IOException {
        final UITemplate uiTemplate = setting.toEntity();

        IoTDBInsertRequest request = new IoTDBInsertRequest(UITemplate.INDEX_NAME, 1L,
                uiTemplate, storageBuilder);
        client.write(request);
        return TemplateChangeStatus.builder().status(true).build();
    }

    @Override
    public TemplateChangeStatus changeTemplate(DashboardSetting setting) throws IOException {
        final UITemplate uiTemplate = setting.toEntity();

        StringBuilder query = new StringBuilder();
        query.append("select * from ").append(client.getStorageGroup()).append(IoTDBClient.DOT)
                .append(UITemplate.INDEX_NAME).append(" where ").append(IoTDBClient.ID_COLUMN).append(" = '")
                .append(uiTemplate.id()).append("'");
        List<? super StorageData> queryResult = client.queryForList(UITemplate.INDEX_NAME, query.toString(), storageBuilder);
        if (queryResult.size() == 0) {
            return TemplateChangeStatus.builder().status(false).message("Can't find the template").build();
        } else {
            IoTDBInsertRequest request = new IoTDBInsertRequest(UITemplate.INDEX_NAME, 1L,
                    uiTemplate, storageBuilder);
            client.write(request);
            return TemplateChangeStatus.builder().status(true).build();
        }
    }

    @Override
    public TemplateChangeStatus disableTemplate(String name) throws IOException {
        StringBuilder query = new StringBuilder();
        query.append("select * from ").append(client.getStorageGroup()).append(IoTDBClient.DOT)
                .append(UITemplate.INDEX_NAME).append(" where ").append(IoTDBClient.ID_COLUMN).append(" = '")
                .append(name).append("'");

        List<? super StorageData> queryResult = client.queryForList(UITemplate.INDEX_NAME, query.toString(), storageBuilder);
        if (queryResult.size() == 0) {
            return TemplateChangeStatus.builder().status(false).message("Can't find the template").build();
        } else {
            final UITemplate uiTemplate = (UITemplate) queryResult.get(0);
            uiTemplate.setDisabled(BooleanUtils.TRUE);
            IoTDBInsertRequest request = new IoTDBInsertRequest(UITemplate.INDEX_NAME, 1L,
                    uiTemplate, storageBuilder);
            client.write(request);
            return TemplateChangeStatus.builder().status(true).build();
        }
    }
}
