package org.locationtech.jts.topold;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

/**
 * Represents a planar topological graph containing faces, edges and nodes.
 * A Topology can model linear edges (which include disconnected lines, cut edges,
 * and dangling edges).  Linear edges have the same face on each side.
 * One implication of this is that faces can contain cut edges, which
 * will appear as a TopologyEdge which occurs more than once in a face.
 * Cut edges are ignored when building the polygon for a face.
 */
public class Topology
{
  private TopologyGraph topoGraph;
  private Set topoFaces;

  /**
   * Creates a new topology.
   */
  public Topology()
  {
    topoGraph = new TopologyGraph();
  }

  /**
   * Gets the {@link TopologyGraph} for this topology.
   *
   * @return
   */
  public TopologyGraph getGraph() { return topoGraph; }

  /**
   * The faces in the list are assumed to be correct and complete.
   *
   * @param faces
   */
  void setFaces(Collection faces)
  {
    topoFaces = new HashSet();
    topoFaces.addAll(faces);
  }

  void addFaces(Collection faces)
  {
    topoFaces.addAll(faces);
  }

  /**
   * Removes the given faces from the topology.
   * These faces are no longer valid for use
   *
   * @param faces the faces to remove
   */
  void removeFaces(Collection faces)
  {
    topoFaces.removeAll(faces);
    // kill off the faces
    for (Iterator i = faces.iterator(); i.hasNext(); ) {
      TopologyFace face = (TopologyFace) i.next();
      face.remove();
    }
  }

  public Collection getFaces() { return topoFaces; }

  public boolean isFace(TopologyFace face)
  {
    if (face.isRemoved()) return false;
    // Assert: if it is not removed, face should be contained in the faces set
    return topoFaces.contains(face);
  }

  /**
   * Add a {@link LineString} forming an edge of the polygon graph.
   *
   * @param line the line to add
   */
  TopologyEdge addEdge(LineString line)
  {
    // probably should require this to be done externally
    //Coordinate[] pts = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());
    return addEdge(line.getCoordinateSequence());
  }

  /**
   * Adds an edge defined by a sequence of points.
   *
   * @param pts the point sequence defining a edge.
   */
  TopologyEdge addEdge(CoordinateSequence pts)
  {
    return topoGraph.addEdge(pts);
  }

  /**
   * Tests whether the directed edge is in an outside hole
   *
   * @param de the directed edge to test
   * @return <code>true</code> if the edge is in an outside hole
   */
  public boolean isExternal(TopologyDirectedEdge de)
  {
    return de.getEdgeRing().getFace() == null;
  }
}

