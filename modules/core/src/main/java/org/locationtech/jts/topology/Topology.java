package org.locationtech.jts.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;


public class Topology {

  private Map<Coordinate, TopologyNode> nodeMap = new HashMap<Coordinate, TopologyNode>();
  private List<TopologyEdge> edges = new ArrayList<TopologyEdge>();
  private List<TopologyFace> faces;
  
  /**
   * Adds an edge defined by a sequence of points.
   *
   * @param pts the point sequence defining a edge.
   */
  TopologyEdge addEdge(CoordinateSequence pts)
  {
    // sanity checks
    if (pts.size() < 2) { return null; }

    /*
    TopologyEdge edge = new TopologyEdge(pts);
    TopologyHalfEdge he0 = TopologyHalfEdge.createEdgePair(pts, edge);
    edge.setHalfEdges(he0);
    */
    
    /*
    Coordinate startPt = pts.getCoordinate(0);
    Coordinate endPt = pts.getCoordinate(pts.size() - 1);
    Node nStart = getNode(startPt);
    Node nEnd = getNode(endPt);
*/
    TopologyEdge edge = TopologyEdge.create(pts);
    edges.add(edge);
    add(edge.getHalfEdge());
    add(edge.getHalfEdge().symTE());
    return edge;
  }

  private void add(TopologyHalfEdge e) {
    /**
     * If the edge origin node is already in the graph, 
     * insert the edge into the star of edges around the node.
     * Otherwise, add a new node for the origin.
     */
    TopologyNode node = nodeMap.get(e.orig());
    if (node != null) {
      node.getEdge().insert(e);
    }
    else {
      node = new TopologyNode(e);
      nodeMap.put(e.orig(), node);
    }  
  }

  private TopologyNode getNode(Coordinate pt)
  {
    return nodeMap.get(pt);
  }

  public Iterator<TopologyEdge> edgeIterator() {
    return edges.iterator();
  }

  public void setFaces(List<TopologyFace> faces) {
    this.faces = faces;
  }

  public Collection<TopologyFace> getFaces() {
    return faces;
  }

}
