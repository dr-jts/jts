package org.locationtech.jts.topold;

import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.DirectedEdgeStar;
import org.locationtech.jts.planargraph.Node;

/**
 * Builds a valid {@link LinearRing}
 * of the {@link Polygon} for a {@link TopologyFace}.
 */
public class PolygonRingBuilder
{
  private TopologyDirectedEdge startDE;

  /**
   * Creates a builder for the ring containing the given
   * {@link TopologyDirectedEdge}
   *
   * @param startDE a dir edge in the desired ring
   */
  public PolygonRingBuilder(TopologyDirectedEdge startDE) {
    this.startDE = startDE;
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
  public LinearRing getRing(Set edgesUsed, GeometryFactory geomFact)
  {
    Coordinate[] ringPts = build(edgesUsed);
    return geomFact.createLinearRing(ringPts);
  }

  private Coordinate[] build(Set edgesUsed)
  {
    CoordinateList coordList = new CoordinateList();
    TopologyEdgeRing edgeRing = startDE.getEdgeRing();
    TopologyDirectedEdge currDE = startDE;
    do {
      edgesUsed.add(currDE);
      currDE = getNextInMinimalRing((TopologyDirectedEdge) currDE, edgeRing);
      currDE.addEdgeCoordinates(coordList);
     } while (currDE != startDE);
    return coordList.toCoordinateArray();
  }

  private static TopologyDirectedEdge getNextInMinimalRing(TopologyDirectedEdge de,
      TopologyEdgeRing edgeRing)
  {
    Node node = de.getToNode();
    DirectedEdgeStar deStar = node.getOutEdges();
    return getNextInMinimalRing(deStar, (TopologyDirectedEdge) de.getSym(), edgeRing);
  }

  private static TopologyDirectedEdge getNextInMinimalRing(DirectedEdgeStar deStar,
      TopologyDirectedEdge de, final TopologyEdgeRing edgeRing)
  {
    return (TopologyDirectedEdge) DirectedEdgeStarEx.nextEdge(deStar, de, DirectedEdgeStarEx.CLOCKWISE,
        new DirectedEdgeStarEx.Condition() {
      public boolean isTrue(DirectedEdge de) {
        // only look for edges in the current edgering
        if (((TopologyDirectedEdge) de).getEdgeRing() != edgeRing)
          return false;
        // ignore dangles & cut edges
        if (! TopologyEdgeRing.isExternal((TopologyDirectedEdge) de))
          return false;
        return true;
      }
    }
    );
  }

}