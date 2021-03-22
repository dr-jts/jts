package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.Node;
import org.locationtech.jts.planargraph.PlanarGraph;

/**
 * Represents a planar graph of edges that can be used to compute a
 * polygonization, and implements the algorithms to compute the
 * {@link EdgeRings} formed by the graph.
 * <p>
 * The marked flag on {@link DirectedEdge}s is used to indicate that a directed edge
 * has be logically deleted from the graph.
 *
 * @version 1.4
 */
public class TopologyGraph
    extends PlanarGraph
{

  /**
   * Gets a list of the directed edges for an edge
   *
   * @param edge
   * @return
   */
  public static List getDirEdges(TopologyEdge edge) {
    List dirEdges = new ArrayList();
    dirEdges.add(edge.getDirEdge(0));
    dirEdges.add(edge.getDirEdge(1));
    return dirEdges;
  }

  /**
   * Gets a collection of the unique faces for an edge.
   * Null faces are not returned.
   *
   * @param edge the edge to retrieve the faces for
   * @return a Collction of unique faces (which can contain nulls)
   */
// MD - maybe this should just return an array, containing only the actual faces?
  // MD maybe this should be a method on a TopologyEdge?
  public static Collection getFaces(TopologyEdge edge) {
    List faceList = new ArrayList();
    TopologyFace[] face = edge.getFaces();
    if (face[0] != null)
      faceList.add(face[0]);
    if (face[1] != null && face[1] != face[0])
      faceList.add(face[1]);
    return faceList;
  }

  static void clearEdgeRings(Collection dirEdges) {
    for (Iterator i = dirEdges.iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      de.setEdgeRing(null);
    }
  }

  /**
   * Create a new polygonization graph.
   */
  public TopologyGraph()
  {
  }

  /**
   * Adds an edge defined by a sequence of points.
   *
   * @param pts the point sequence defining a edge.
   */
  TopologyEdge addEdge(CoordinateSequence pts)
  {
    // sanity checks
    if (pts.size() < 2) { return null; }

    Coordinate startPt = pts.getCoordinate(0);
    Coordinate endPt = pts.getCoordinate(pts.size() - 1);

    Node nStart = getNode(startPt);
    Node nEnd = getNode(endPt);

    DirectedEdge de0 = new TopologyDirectedEdge(nStart, nEnd, pts.getCoordinate(1), true);
    DirectedEdge de1 = new TopologyDirectedEdge(nEnd, nStart, pts.getCoordinate(pts.size() - 2), false);
    TopologyEdge edge = new TopologyEdge(pts);
    edge.setDirectedEdges(de0, de1);
    add(edge);
    return edge;
  }

  private Node getNode(Coordinate pt)
  {
    Node node = findNode(pt);
    if (node == null) {
      node = new Node(pt);
      // ensure node is only added once to graph
      add(node);
    }
    return node;
  }
}

