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

package org.apache.hadoop.hive.ql.optimizer.graph;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import org.apache.hadoop.hive.ql.exec.Operator;
import org.apache.hadoop.hive.ql.exec.TableScanOperator;
import org.apache.hadoop.hive.ql.optimizer.graph.OperatorGraph.Cluster;
import org.apache.hadoop.hive.ql.optimizer.graph.OperatorGraph.OpEdge;
import org.apache.hadoop.hive.ql.plan.JoinDesc;
import org.apache.hadoop.hive.ql.plan.OperatorDesc;
import org.apache.hadoop.hive.ql.plan.TableScanDesc;

public class DotExporter {

  private OperatorGraph operatorGraph;

  public DotExporter(OperatorGraph operatorGraph) {
    this.operatorGraph = operatorGraph;
  }

  public void write(File outFile) throws Exception {
    Map<Operator<?>, Cluster> nodeCluster = operatorGraph.nodeCluster;
    DagGraph<Operator<?>, OpEdge> g = operatorGraph.g;
    PrintWriter writer = new PrintWriter(outFile);
    writer.println("digraph G");
    writer.println("{");
    HashSet<Cluster> clusters = new HashSet<>(nodeCluster.values());
    int idx = 0;
    for (Cluster cluster : clusters) {
      idx++;
      writer.printf("subgraph cluster_%d {", idx);
      for (Operator<?> member : cluster.members) {
        writer.printf("%s;", nodeName(member));
      }
      writer.printf("label = \"cluster %d\";", idx);
      writer.printf("}");
    }
    Set<Operator<?>> nodes = g.nodes();
    for (Operator<?> n : nodes) {
      writer.printf("%s[shape=record,label=\"%s\",%s];", nodeName(n), nodeLabel(n), style(n));
      Set<Operator<?>> succ = g.successors(n);
      for (Operator<?> s : succ) {
        writer.printf("%s->%s;", nodeName(n), nodeName(s));
      }
    }

    writer.println("}");
    writer.close();
  }

  private String style(Operator<?> n) {
    String fillColor = "white";
    OperatorDesc c = n.getConf();
    if (n instanceof TableScanOperator) {
      fillColor = "#ccffcc";
    }
    if (c instanceof JoinDesc) {
      fillColor = "#ffcccc";
    }
    return String.format("style=filled,fillcolor=\"%s\"", fillColor);
  }

  private String nodeLabel(Operator<?> n) {
    List<String> rows = new ArrayList<String>();

    rows.add(nodeName0(n));
    if ((n instanceof TableScanOperator)) {
      TableScanOperator ts = (TableScanOperator) n;
      TableScanDesc conf = ts.getConf();
      rows.add(vBox(conf.getTableName(), conf.getAlias()));
    }
    return vBox(rows);
  }

  private String hBox(List<String> rows) {
    return "" + Joiner.on("|").join(rows) + "";
  }

  private String hBox(String... rows) {
    return "" + Joiner.on("|").join(rows) + "";
  }

  private String vBox(List<String> rows) {
    return "{ " + hBox(rows) + "}";
  }

  private String vBox(String... strings) {
    return "{ " + hBox(strings) + "}";
  }

  private String nodeName(Operator<?> member) {
    return String.format("\"%s\"", member);
  }

  private String nodeName0(Operator<?> member) {
    return member.toString();
  }


}
