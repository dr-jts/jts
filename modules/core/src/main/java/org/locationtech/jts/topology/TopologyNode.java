package org.locationtech.jts.topology;

import org.locationtech.jts.geom.Coordinate;

public class TopologyNode {

  private TopologyHalfEdge edge;

  public TopologyNode(TopologyHalfEdge e) {
    edge = e;
  }

  public Coordinate getCoordinate() {
    return edge.orig();
  }

  public TopologyHalfEdge getEdge() {
    return edge;
  }

}
