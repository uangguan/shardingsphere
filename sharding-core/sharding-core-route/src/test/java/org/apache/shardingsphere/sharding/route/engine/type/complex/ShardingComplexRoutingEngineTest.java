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
 */

package org.apache.shardingsphere.sharding.route.engine.type.complex;

import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.route.fixture.AbstractRoutingEngineTest;
import org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.projection.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.underlying.common.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.underlying.common.exception.ShardingSphereException;
import org.apache.shardingsphere.underlying.route.context.RouteResult;
import org.apache.shardingsphere.underlying.route.context.RouteUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public final class ShardingComplexRoutingEngineTest extends AbstractRoutingEngineTest {
    
    @Test
    public void assertRoutingForBindingTables() {
        SelectStatementContext selectStatementContext = new SelectStatementContext(new SelectStatement(),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false), 
                new ProjectionsContext(0, 0, false, Collections.emptyList()),
                new PaginationContext(null, null, Collections.emptyList()));
        ShardingComplexRoutingEngine complexRoutingEngine = new ShardingComplexRoutingEngine(Arrays.asList("t_order", "t_order_item"), selectStatementContext,
                createShardingConditions("t_order"), new ConfigurationProperties(new Properties()));
        RouteResult routeResult = complexRoutingEngine.route(createBindingShardingRule());
        List<RouteUnit> routeUnits = new ArrayList<>(routeResult.getRouteUnits());
        assertThat(routeResult, instanceOf(RouteResult.class));
        assertThat(routeResult.getRouteUnits().size(), is(1));
        assertThat(routeUnits.get(0).getDataSourceMapper().getActualName(), is("ds_1"));
        assertThat(routeUnits.get(0).getTableMappers().size(), is(1));
        assertThat(routeUnits.get(0).getTableMappers().iterator().next().getActualName(), is("t_order_1"));
        assertThat(routeUnits.get(0).getTableMappers().iterator().next().getLogicName(), is("t_order"));
    }
    
    @Test
    public void assertRoutingForShardingTableJoinBroadcastTable() {
        SelectStatementContext selectStatementContext = new SelectStatementContext(new SelectStatement(),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false), 
                new ProjectionsContext(0, 0, false, Collections.emptyList()),
                new PaginationContext(null, null, Collections.emptyList()));
        ShardingComplexRoutingEngine complexRoutingEngine = new ShardingComplexRoutingEngine(Arrays.asList("t_order", "t_config"), selectStatementContext,
                createShardingConditions("t_order"), new ConfigurationProperties(new Properties()));
        RouteResult routeResult = complexRoutingEngine.route(createBroadcastShardingRule());
        List<RouteUnit> routeUnits = new ArrayList<>(routeResult.getRouteUnits());
        assertThat(routeResult, instanceOf(RouteResult.class));
        assertThat(routeResult.getRouteUnits().size(), is(1));
        assertThat(routeUnits.get(0).getDataSourceMapper().getActualName(), is("ds_1"));
        assertThat(routeUnits.get(0).getTableMappers().size(), is(1));
        assertThat(routeUnits.get(0).getTableMappers().iterator().next().getActualName(), is("t_order_1"));
        assertThat(routeUnits.get(0).getTableMappers().iterator().next().getLogicName(), is("t_order"));
    }
    
    @Test(expected = ShardingSphereException.class)
    public void assertRoutingForNonLogicTable() {
        ShardingComplexRoutingEngine complexRoutingEngine = new ShardingComplexRoutingEngine(Collections.emptyList(), null,
                createShardingConditions("t_order"), new ConfigurationProperties(new Properties()));
        complexRoutingEngine.route(mock(ShardingRule.class));
    }
}
