/*
 * Copyright (c) 2022 Martin Davis
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.topology;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.io.WKTWriter;

public class TopologyEdge {

  public static TopologyEdge create(CoordinateSequence pts) {
    TopologyEdge edge = new TopologyEdge(pts);
    TopologyHalfEdge he0 = createEdgePair(edge);
    edge.setHalfEdge(he0);
    return edge;
  }
  
  private static TopologyHalfEdge createEdgePair(TopologyEdge edge)
  {
    TopologyHalfEdge e0 = createEdge(edge, true);
    TopologyHalfEdge e1 = createEdge(edge, false);
    e0.link(e1);
    return e0;
  }
  
  private static TopologyHalfEdge createEdge(TopologyEdge edge, boolean direction)
  {
    CoordinateSequence seq = edge.getCoordinateSequence();
    Coordinate origin;
    Coordinate dirPt;
    if (direction) {
      origin = seq.getCoordinate(0);
      dirPt = seq.getCoordinate(1);
    }
    else {
      int ilast = seq.size() - 1;
      origin = seq.getCoordinate(ilast);
      dirPt = seq.getCoordinate(ilast-1);
    }
    return new TopologyHalfEdge(origin, dirPt, direction, edge);
  }

  private TopologyHalfEdge hedge;
  private CoordinateSequence seq;

  private TopologyEdge(CoordinateSequence pts) {
    this.seq = pts;
  }

  private void setHalfEdge(TopologyHalfEdge e) {
    hedge = e;
  }

  public TopologyHalfEdge getHalfEdge() {
    return hedge;
  }

  public CoordinateSequence getCoordinateSequence() {
    return seq;
  }
  
  public int size() {
    return seq.size();
  }

  public String toString() {
    return WKTWriter.toLineString(seq.toCoordinateArray());
  }
}
