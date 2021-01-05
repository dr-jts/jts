package org.locationtech.jts.topology;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geomgraph.EdgeRing;
import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.Node;

/**
 * A {@link DirectedEdge} of a {@link Topology}, which represents
 * an edge of a polygon formed by the graph.
 *
 * @version 1.4
 */
public class TopologyDirectedEdge
    extends DirectedEdge
{

  /**
   * The edge ring this dir edge is part of
   */
  private TopologyEdgeRing edgeRing = null;

  /**
   * Constructs a directed edge connecting the <code>from</code> node to the
   * <code>to</code> node.
   *
   * @param directionPt
   *                  specifies this DirectedEdge's direction (given by an imaginary
   *                  line from the <code>from</code> node to <code>directionPt</code>)
   * @param edgeDirection
   *                  whether this DirectedEdge's direction is the same as or
   *                  opposite to that of the parent Edge (if any)
   */
  public TopologyDirectedEdge(Node from, Node to, Coordinate directionPt,
      boolean edgeDirection)
  {
    super(from, to, directionPt, edgeDirection);
  }

  /**
   * Gets the {@link TopologyFace} for this directed edge.
   * Assumes that the dir edge is currently assigned to an edge ring.
   *
   * @return the face for this directed edge
   */
  public TopologyFace getFace()
  {
    if (getEdgeRing() == null) return null;
    return getEdgeRing().getFace();
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

  /**
   * Tests whether this dir edge is currently in a {@link TopologyEdgeRing).
   *
   * @return <code>true</code if this dir edge is in an edge ring
   */
  public boolean isInEdgeRing()
  {
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
    TopologyEdge edge = (TopologyEdge) getEdge();
    CoordinateSequence coords = edge.getCoordinates();
    if (getEdgeDirection()) {
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

}
