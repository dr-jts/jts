package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

/**
 * Builds a {@link Topology} from a collection of {@link Linestring}s.
 * The linestrings must be correctly noded (e.g. not touch except at endpoints).
 */
public class TopologyBuilder
{
  private Topology topology;

  public TopologyBuilder() {
    topology = new Topology();
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
   * Adds a {@link TopologyEdge} defined by a sequence of coordinates.
   *
   * @param pts the point sequence defining a edge.
   * @return the TopologyEdge created
   */
  public TopologyEdge addEdge(CoordinateSequence pts)
  {
    return topology.addEdge(pts);
  }

  /**
   * Adds {@link TopologyEdge}s defined by a collection of {@link Linestring}s.
   *
   * @param lines the edges to add
   */
  public void addEdgesFromLineStrings(Collection lines)
  {
    for (Iterator i = lines.iterator(); i.hasNext(); ) {
      LineString line = (LineString) i.next();
      addEdge(line.getCoordinateSequence());
    }
  }

  /**
   * Builds the {@link Topology}
   */
  private void build()
  {
    List faceList = computeFaces(topology.getGraph().dirEdgeIterator());
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
  static List computeFaces(Iterator dirEdgeIterator)
  {
    List edgeRingList = computeEdgeRings(dirEdgeIterator);
    List[] shellsHoles = findShellsAndHoles(edgeRingList);
    List faceList = createFaces(shellsHoles[0]);
    assignHolesToShells(shellsHoles[0], shellsHoles[1]);
    return faceList;
  }

  static List createFaces(List shells)
  {
    List faces = new ArrayList();
    for (Iterator i = shells.iterator(); i.hasNext(); ) {
      TopologyEdgeRing shell = (TopologyEdgeRing) i.next();
      TopologyFace face = new TopologyFace(shell);
      shell.setFace(face);
      faces.add(face);
    }
    return faces;
  }

  static List[] findShellsAndHoles(Collection edgeRingList)
  {
    List holeList = new ArrayList();
    List shellList = new ArrayList();
    for (Iterator i = edgeRingList.iterator(); i.hasNext(); ) {
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

  static void assignHolesToShells(List shellList, List holeList)
  {
    for (Iterator i = holeList.iterator(); i.hasNext(); ) {
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
  static List computeEdgeRings(Iterator dirEdgeIt)
  {
    // build all edgerings reachable from the input dirEdges
    List edgeRingList = new ArrayList();
    while (dirEdgeIt.hasNext()) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) dirEdgeIt.next();
      // if DE is already in ring skip it
      if (de.isInEdgeRing()) continue;
      TopologyEdgeRing edgeRing = TopologyEdgeRing.buildEdgeRing(de);
      edgeRingList.add(edgeRing);
    }
    return edgeRingList;
  }
}