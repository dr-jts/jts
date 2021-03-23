package org.locationtech.jts.topology;

import org.locationtech.jts.edgegraph.HalfEdge;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.io.WKTWriter;

public class TopologyHalfEdge extends HalfEdge {

  public static TopologyHalfEdge createEdge(CoordinateSequence pts, TopologyEdge edge, boolean direction)
  {
    Coordinate origin;
    Coordinate dirPt;
    if (direction) {
      origin = pts.getCoordinate(0);
      dirPt = pts.getCoordinate(1);
    }
    else {
      int ilast = pts.size() - 1;
      origin = pts.getCoordinate(ilast);
      dirPt = pts.getCoordinate(ilast-1);
    }
    return new TopologyHalfEdge(origin, dirPt, direction, edge);
  }

  public static TopologyHalfEdge createEdgePair(CoordinateSequence pts, TopologyEdge edge)
  {
    TopologyHalfEdge e0 = TopologyHalfEdge.createEdge(pts, edge, true);
    TopologyHalfEdge e1 = TopologyHalfEdge.createEdge(pts, edge, false);
    e0.link(e1);
    return e0;
  }
  
  private Coordinate dirPt;
  private boolean direction;
  private TopologyEdge edge;
  private org.locationtech.jts.topology.TopologyEdgeRing edgeRing;

  public TopologyHalfEdge(Coordinate orig, Coordinate dirPt, boolean direction, TopologyEdge edge) {
    super(orig);
    this.dirPt = dirPt;
    this.direction = direction;
    this.edge = edge;
  }

  public TopologyHalfEdge symTE() {
    return (TopologyHalfEdge) sym();
  }

  public boolean isForward() {
    return direction;
  }
  public Coordinate directionPt() {
    return dirPt;
  }
  
  /**
   * Sets the {@link TopologyEdgeRing} that this directed edge is
   * a member of.
   *
   * @param edgeRing the edge ring containing this dir edge
   */
  public void setEdgeRing(TopologyEdgeRing edgeRing)
  {
      this.edgeRing = edgeRing;
  }
  /**
   * Returns the ring of directed edges that this directed edge is
   * a member of, or null if the ring has not been set.
   *
   * @return <code>true</code> if this dir edge is part of an edge ring
   * @see #setRing(EdgeRing)
   */
  public TopologyEdgeRing getEdgeRing() { return edgeRing; }

  public boolean isInEdgeRing() {
    return edgeRing != null && ! edgeRing.isRemoved();
  }

  /**
   * Adds the coordinates of the parent {@link TopologyEdge} to
   * a {@link CoordinateList}, oriented
   * correctly in the direction of this dir edge.
   * Repeated coordinates are removed.
   *
   * @param coordList the coordinate list to add to
   */
  public void addEdgeCoordinates(CoordinateList coordList)
  {
    CoordinateSequence coords = edge.getCoordinateSequence();
    if (direction) {
      for (int i = 0; i < coords.size(); i++) {
        coordList.add(coords.getCoordinate(i), false);
      }
    }
    else {
      for (int i = coords.size() - 1; i >= 0; i--) {
        coordList.add(coords.getCoordinate(i), false);
      }
    }
  }

  public String toString() {
    Coordinate orig = orig();
    Coordinate dest = dest();
    String dirPtStr = (edge.size() > 2)
        ? ", " + WKTWriter.format(directionPt())
            : "";

    return "THE( "+ WKTWriter.format(orig)
        + dirPtStr
        + " .. " + WKTWriter.format(dest)
        + " ) " 
        ;
  }
}
