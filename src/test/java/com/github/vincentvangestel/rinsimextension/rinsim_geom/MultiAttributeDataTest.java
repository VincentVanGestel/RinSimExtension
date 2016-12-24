package com.github.vincentvangestel.rinsimextension.rinsim_geom;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;

public class MultiAttributeDataTest {
  static final double DELTA = 0.0001;

  Graph<MultiAttributeData> graph;
  Class<Graph<MultiAttributeData>> graphType;

  @Before
  public void setUp() throws InstantiationException, IllegalAccessException {
    graph = new TableGraph<>();
  }

  @Test
  public void asyncSpeedTest() {
    final Point A = new Point(0, 0), B = new Point(0, 1);
    graph.addConnection(A, B,
      MultiAttributeData.builder().setMaxSpeed(1).build());
    graph.addConnection(B, A,
      MultiAttributeData.builder().setMaxSpeed(2).build());

    assertEquals(1d, graph.connectionData(A, B).get().getMaxSpeed().get(),
      DELTA);
    assertEquals(2d, graph.connectionData(B, A).get().getMaxSpeed().get(),
      DELTA);
  }

}
