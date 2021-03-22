package org.locationtech.jts.topold;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.planargraph.Edge;

/**
 * An edge of a polygonization graph.
 *
 * @version 1.4
 */
public class TopologyEdge
    extends Edge
{
  private CoordinateSequence pts;

  public TopologyEdge(CoordinateSequence pts)
  {
    this.pts = pts;
  }

  public CoordinateSequence getCoordinates() { return pts; }

  public TopologyFace[] getFaces()
  {
    TopologyFace[] face = new TopologyFace[2];
    face[0] = ((TopologyDirectedEdge) getDirEdge(0)).getEdgeRing().getFace();
    face[1] = ((TopologyDirectedEdge) getDirEdge(1)).getEdgeRing().getFace();
    return face;
  }

  public TopologyFace getFace(int i)
  {
    return ((TopologyDirectedEdge) getDirEdge(0)).getEdgeRing().getFace();
  }

  /**
   * Removes this edge from its containing topology.
   */
  void remove()
  {
    pts = null;
  }

  /**
   * Tests whether this edge has been removed from the topology.
   *
   * @return <code>true</code> if this edge has been removed
   */
  public boolean isRemoved()
  {
    return pts == null;
  }
}
