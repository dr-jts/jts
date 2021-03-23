package org.locationtech.jts.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

public class TopologyBuilder {
  private Topology topology;

  public TopologyBuilder() {
    topology = new Topology();
  }
  
  /**
   * Adds a {@link TopologyEdge} defined by a sequence of coordinates.
   *
   * @param pts the point sequence defining a edge.
   * @return the TopologyEdge created
   */
  public TopologyEdge addEdge(CoordinateSequence pts)
  {
    return topology.addEdge(pts);
  }

  public void addEdgesFromLineStrings(List<LineString> lines) {
    for (LineString line : lines) {
      addEdge(line.getCoordinateSequence());
    }
  }

  /**
   * Build the Topology graph and return it.
   *
   * @return
   */
  public Topology getTopology()
  {
    build();
    return topology;
  }
  
  /**
   * Builds the {@link Topology}
   */
  private void build()
  {
    List<TopologyFace> faceList = computeFaces(topology.edgeIterator());
    topology.setFaces(faceList);
  }

  /**
   * Computes the edgerings and faces for the given list of dirEdges.
   * The dirEdges must have their edge rings cleared
   * (either <tt>null</tt> or removed).
   *
   * @param dirEdgeIterator an iterator of directed edges
   * @return a list of faces
   */
  static List<TopologyFace> computeFaces(Iterator<TopologyEdge> edgeIterator)
  {
    List<TopologyEdgeRing> edgeRingList = computeEdgeRings(edgeIterator);
    List<TopologyEdgeRing>[] shellsHoles = findShellsAndHoles(edgeRingList);
    List<TopologyFace> faceList = createFaces(shellsHoles[0]);
    assignHolesToShells(shellsHoles[0], shellsHoles[1]);
    return faceList;
  }

  static List<TopologyFace> createFaces(List<TopologyEdgeRing> shells)
  {
    List<TopologyFace> faces = new ArrayList<TopologyFace>();
    for (Iterator<TopologyEdgeRing> i = shells.iterator(); i.hasNext(); ) {
      TopologyEdgeRing shell = i.next();
      TopologyFace face = new TopologyFace(shell);
      shell.setFace(face);
      faces.add(face);
    }
    return faces;
  }

  @SuppressWarnings("unchecked")
  static List<TopologyEdgeRing>[] findShellsAndHoles(Collection<TopologyEdgeRing> edgeRingList)
  {
    List<TopologyEdgeRing> holeList = new ArrayList<TopologyEdgeRing>();
    List<TopologyEdgeRing> shellList = new ArrayList<TopologyEdgeRing>();
    for (Iterator<TopologyEdgeRing> i = edgeRingList.iterator(); i.hasNext(); ) {
      TopologyEdgeRing er = (TopologyEdgeRing) i.next();
      // filter out single line edges (isolated dangles)
      if (er.isValidSizeForRing()) {
        if (er.isHole())
          holeList.add(er);
        else
          shellList.add(er);
      }
    }
    return new List[] { shellList, holeList };
  }

  static void assignHolesToShells(List<TopologyEdgeRing> shellList, List<TopologyEdgeRing> holeList)
  {
    for (Iterator<TopologyEdgeRing> i = holeList.iterator(); i.hasNext(); ) {
      TopologyEdgeRing holeER = (TopologyEdgeRing) i.next();
      TopologyEdgeRing shell = TopologyEdgeRing.findEdgeRingContaining(holeER, shellList);
      if (shell != null) {
        TopologyFace face = shell.getFace();
        face.addHole(holeER);
        holeER.setFace(face);
      }
    }
  }

  /**
   * Computes the EdgeRings formed by the dirEdges in the given iterator.
   *
   * @return a list of the {@link TopologyEdgeRing}s created.
   */
  static List<TopologyEdgeRing> computeEdgeRings(Iterator<TopologyEdge> edgeIt)
  {
    // build all edgerings reachable from the input dirEdges
    List<TopologyEdgeRing> edgeRingList = new ArrayList<TopologyEdgeRing>();
    while (edgeIt.hasNext()) {
      TopologyEdge e = (TopologyEdge) edgeIt.next();
      computeEdgeRing(e.getHalfEdge(), edgeRingList);
      computeEdgeRing(e.getHalfEdge().symTE(), edgeRingList);
    }
    return edgeRingList;
  }

  private static void computeEdgeRing(TopologyHalfEdge de, List<TopologyEdgeRing> edgeRingList) {
    // if DE is already in ring skip it
    if (de.isInEdgeRing()) return;
    TopologyEdgeRing edgeRing = TopologyEdgeRing.create(de);
    edgeRingList.add(edgeRing);
  }

}
