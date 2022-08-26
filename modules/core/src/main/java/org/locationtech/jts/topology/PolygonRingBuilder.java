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

import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Builds a valid minimal {@link LinearRing}
 * of the {@link Polygon} for a {@link TopologyFace}.
 */
class PolygonRingBuilder
{
  public static LinearRing getRing(TopologyHalfEdge de, Set<TopologyHalfEdge> edgesUsed, GeometryFactory geomFact) {
    PolygonRingBuilder polyRing = new PolygonRingBuilder(de, edgesUsed);
    LinearRing ring = polyRing.getRing( geomFact);
    return ring;
  }
  
  private TopologyHalfEdge startEdge;
  private Set<TopologyHalfEdge> edgesUsed;
  private TopologyEdgeRing edgeRingMax;

  /**
   * Creates a builder for the ring containing the given
   * {@link TopologyDirectedEdge}
   *
   * @param startDE a dir edge in the desired ring
   * @param edgesUsed 
   */
  public PolygonRingBuilder(TopologyHalfEdge startDE, Set<TopologyHalfEdge> edgesUsed) {
    this.startEdge = startDE;
    this.edgesUsed = edgesUsed;
    this.edgeRingMax = startDE.getEdgeRing();
  }

  /**
   * Get the valid {@link LinearRing}.
   * If the edges in this do not define a valid LinearRing,
   * return null.
   *
   * @param edgesUsed
   * @param geomFact
   * @return a linear ring, or <code>null</code> if not possible to build a valid ring
   */
  public LinearRing getRing( GeometryFactory geomFact)
  {
    // for now
    //return TEMPgetMaxRing();
    //*
    Coordinate[] ringPts = buildRing();
    return geomFact.createLinearRing(ringPts);
    //*/
  }

  /*
  private LinearRing TEMPgetMaxRing() {
    TopologyEdgeRing er = startDE.getEdgeRing();
    // mark all edges in ring as used
    for (Iterator<TopologyHalfEdge> i = er.iterator(); i.hasNext();) {
      edgesUsed.add(i.next());
    }
    return er.getRingTEMP();
  }
*/
  
  private Coordinate[] buildRing()
  {
    CoordinateList coordList = new CoordinateList();
    TopologyHalfEdge currDE = startEdge;
    do {
      edgesUsed.add(currDE);
      currDE = nextInMinimalRing(currDE);
      currDE.addEdgeCoordinates(coordList);
     } while (currDE != startEdge);
    return coordList.toCoordinateArray();
  }

  private TopologyHalfEdge nextInMinimalRing(TopologyHalfEdge e) {
    /**
     * The next minimal ring edge is the next edge CW around the dest node
     * of the given edge.
     * HalfEdges are only linked to traverse CCW around nodes.
     * So the next CW edge can be found as the last edge found 
     * while traversing CCW around the node.
     * This is not totally efficent, but fine for the
     * low degree expected for typical nodes.
     */
    TopologyHalfEdge start = e.symTE();
    TopologyHalfEdge next = start;
    TopologyHalfEdge nextCW = null;
    while (true) {
      next = (TopologyHalfEdge) next.oNext();
      if (next == start) break;
      // skip cut edges
      if (! next.isOuter()) continue;
      
      if (next.getEdgeRing() == edgeRingMax) {
        nextCW = next;
      }
    }
    return nextCW;
  }

}