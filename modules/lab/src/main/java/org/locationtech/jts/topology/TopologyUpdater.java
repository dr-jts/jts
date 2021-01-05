package org.locationtech.jts.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.Node;
import org.locationtech.jts.util.Assert;


/**
 * Provides methods for updating a {@link Topology}
 */
public class TopologyUpdater {

  private Topology topology;

  public TopologyUpdater(Topology topology) {
    this.topology = topology;
  }

  /**
   * Replaces a {@link TopologyEdge} with a list of {@link CoordinateSequence}s
   * which define new edges.
   * The sequences must start and end at the same nodes as the
   * edge they replace.
   * The nodes of the sequences may be coincident with
   * nodes already existing in the topology.
   * If this is the case, the faces in the topology will change.
   * The edge data is preserved and applied to each of the
   * new edges.
   * <p>
   * WARNING!!!  Currently this method does NOT handle topology collapses
   *
   * @param edge the edge to replace
   * @param newSequences the CoordinateSequences to add
   * @return a List of the new TopologyEdges
   */
  public List replaceEdge(TopologyEdge edge, Collection newSequences)
  {
    // save the face data
    TopologyFace[] oldFace = edge.getFaces();
    Object[] oldFaceData = new Object[2];
    if (oldFace[0] != null) oldFaceData[0] = oldFace[0].getData();
    if (oldFace[1] != null) oldFaceData[1] = oldFace[1].getData();

    deleteEdge(edge);

    // add the new edges
    List newEdgeList = new ArrayList();
    // MD - would it be better to add all the edges at once?  Need an addEdges method
    for (Iterator i = newSequences.iterator(); i.hasNext(); ) {
      CoordinateSequence seq = (CoordinateSequence) i.next();
      TopologyEdge e = addEdge(seq);
      newEdgeList.add(e);
      TopologyFace[] newFace = e.getFaces();
      // set the face data for the new faces (if any)
      if (newFace[0] != null) newFace[0].setData(oldFaceData[0]);
      if (newFace[1] != null) newFace[1].setData(oldFaceData[1]);
    }
    return newEdgeList;
  }


  /**
   * Replaces a {@link TopologyEdge} with a list of {@link CoordinateSequence}s
   * which define new edges.
   * The sequences must start and end at the same nodes as the
   * edge they replace, and they must not intersect any other
   * edges in the topology.
   * The edge data is preserved and applied to each of the
   * new edges.
   * This does not change the faces in the topology.
   * <p>
   * WARNING!!!  Currently this method does NOT handle topology collapses
   *
   * @param edge the edge to replace
   * @param newSequences the CoordinateSequences to add
   * @return a List of the new TopologyEdges
   */
  public List replaceNonIntersectingEdge(TopologyEdge edge, Collection newSequences)
  {
    Object edgeData = edge.getData();
    TopologyDirectedEdge de0 = (TopologyDirectedEdge) edge.getDirEdge(0);
    TopologyEdgeRing er0 = de0.getEdgeRing();
    TopologyDirectedEdge de1 = (TopologyDirectedEdge) edge.getDirEdge(1);
    TopologyEdgeRing er1 = de1.getEdgeRing();

    Node fromNode = edge.getDirEdge(0).getFromNode();
    Node toNode = edge.getDirEdge(0).getToNode();

    topology.getGraph().remove(edge);

    List newEdges = new ArrayList();
    for (Iterator i = newSequences.iterator(); i.hasNext(); ) {
      CoordinateSequence seq = (CoordinateSequence) i.next();
      TopologyEdge e = topology.addEdge(seq);
      // save the edge data
      e.setData(edgeData);
      newEdges.add(e);
    }

    // check that new edge list has expected topology
    checkNonIntersectedContiguousEdgeList(newEdges, fromNode, toNode);

    /**
     * Rebuild the edgerings.
     * the new edges should not otherwise affect the topology
     * (i.e. the faces will stay the same)
     */
    TopologyEdge newEdge0 = (TopologyEdge) newEdges.get(0);
    er0.clearDirEdgeList();
    er1.clearDirEdgeList();
    er0.build((TopologyDirectedEdge) newEdge0.getDirEdge(0));
    er1.build((TopologyDirectedEdge) newEdge0.getDirEdge(1));

    return newEdges;
  }

  /**
   * This checks that a list of edges which replaces an single edge
   * has identical topology:
   * <ul>
   * <li>the list is contiguous
   * <li>all interior nodes have degree = 2
   * <ul>
   *
   * @param edges the edges to check
   */
  private void checkNonIntersectedContiguousEdgeList(List edges, Node fromNode, Node toNode)
  {
    // Check that first and last node equal orignal
    TopologyEdge e0 = (TopologyEdge) edges.get(0);
    Assert.isTrue(e0.getDirEdge(0).getFromNode() == fromNode, "Replacing from-node different to original");
    TopologyEdge en = (TopologyEdge) edges.get(edges.size()- 1);
    Assert.isTrue(en.getDirEdge(0).getToNode() == toNode, "Replacing to-node different to original");

    for (int i = 1; i < edges.size(); i++) {
      TopologyEdge edge = (TopologyEdge) edges.get(i);
      DirectedEdge de = edge.getDirEdge(0);
      Node node = de.getFromNode();
      Assert.isTrue(node.getDegree() == 2, "found replaced edge node of degree != 2");

      DirectedEdge prevDE = TopologyEdgeRing.getPrevRight(de);
      Assert.isTrue(prevDE.getEdge() == edges.get(i - 1));
    }
  }

  /**
   * Deletes a {@link TopologyEdge},
   * along with all {@link TopologyFace}s associated with it.
   * One or more new {@link TopologyFace}s will be created
   * to represent the new topology.
   *
   * @param edge the edge to delete
   * @return the {@link TopologyFace}(s) created
   */
  public Collection deleteEdge(TopologyEdge edge)
  {
    List edges = new ArrayList();
    edges.add(edge);
    return deleteEdges(edges);
  }

// MD - this is incorrect (old code)
  /*
  public List OLDdeleteEdge(TopologyEdge edge)
  {
    Collection deletedFaces = TopologyGraph.getFaces(edge);

    // delete faces - clear face edgerings, then remove face
    List dirEdges = TopologyGraph.getDirEdges(edge);
    TopologyGraph.clearEdgeRings(dirEdges);
    topology.removeFaces(deletedFaces);

    // now remove edges
    topology.getGraph().remove(edge);

    // can maybe just use the list of dirEdges instead of every dirEdge?
    List newFaces = TopologyBuilder.computeFaces(topology.getGraph().dirEdgeIterator());
    topology.addFaces(newFaces);
    return newFaces;
  }
*/

  /**
   * Deletes a set of {@link TopologyEdge}s from the {@link Topology},
   * along with all {@link TopologyFace}s associated with them.
   *
   * @param edges the edges to delete
   * @return any new {@link TopologyFace}(s) created
   */
  public Collection deleteEdges(Collection edges)
  {
    Set facesToDelete = new HashSet();
    for (Iterator it = edges.iterator(); it.hasNext(); ) {
      TopologyEdge edge = (TopologyEdge) it.next();
      Collection edgeFaces = TopologyGraph.getFaces(edge);
      facesToDelete.addAll(edgeFaces);
    }
    List dirtyDirEdges = getFaceDirEdges(facesToDelete);

    // have to include exterior edgerings (which don't have faces)
    Collection exteriorDirEdges = getExteriorDirEdges(edges);
    dirtyDirEdges.addAll(exteriorDirEdges);

    Set liveDirtyDirEdges = new HashSet();
    liveDirtyDirEdges.addAll(dirtyDirEdges);

    TopologyGraph.clearEdgeRings(dirtyDirEdges);
    topology.removeFaces(facesToDelete);

    // now remove edges
    for (Iterator it = edges.iterator(); it.hasNext(); ) {
      TopologyEdge edge = (TopologyEdge) it.next();
      liveDirtyDirEdges.remove(edge.getDirEdge(0));
      liveDirtyDirEdges.remove(edge.getDirEdge(1));
      topology.getGraph().remove(edge);
      edge.remove();
    }

    // compute new faces
    // for efficiency, only scan the list of impacted diredges
    List newFaces = TopologyBuilder.computeFaces(liveDirtyDirEdges.iterator());
    topology.addFaces(newFaces);
    return newFaces;
  }

  private static List getFaceDirEdges(Collection faces)
  {
    List dirEdges = new ArrayList();
    for (Iterator faceIt = faces.iterator(); faceIt.hasNext(); ) {
      TopologyFace face = (TopologyFace) faceIt.next();
      addAll(face.iterator(), dirEdges);
    }
    return dirEdges;
  }

  private static Collection getExteriorDirEdges(Collection edges)
  {
    Collection extEdgeRings = getExteriorEdgeRings(edges);
    List dirEdges = new ArrayList();
    for (Iterator it = extEdgeRings.iterator(); it.hasNext(); ) {
      TopologyEdgeRing er = (TopologyEdgeRing) it.next();
      for (Iterator deIt = er.iterator(); deIt.hasNext(); ) {
        dirEdges.add(deIt.next());
      }
    }
    return dirEdges;
  }

  private static Collection getExteriorEdgeRings(Collection edges)
  {
    // there could be more than one exterior edgering
    Set extEdgeRings = new HashSet();
    for (Iterator it = edges.iterator(); it.hasNext(); ) {
      TopologyEdge edge = (TopologyEdge) it.next();
      for (int i = 0; i < 2; i++) {
        TopologyEdgeRing er = ((TopologyDirectedEdge) edge.getDirEdge(0)).getEdgeRing();
        if (er.getFace() == null)
          extEdgeRings.add(er);
      }
    }
    return extEdgeRings;
  }

  private static void addAll(Iterator it, Collection coll)
  {
    while (it.hasNext()) {
      coll.add(it.next());
    }
  }

  private static List toList(Iterator it)
  {
    List list = new ArrayList();
    while (it.hasNext()) {
      list.add(it.next());
    }
    return list;
  }

  /**
   * Adds a new {@link TopologyEdge} defined by a {@link CoordinateSequence}.
   *
   * @param seq the coordinate sequence defining the edge
   * @return the new {@link TopologyEdge} created by adding this edge
   */
  public TopologyEdge addEdge(CoordinateSequence seq)
  {
    TopologyEdge newEdge = topology.addEdge(seq);

    // delete and rebuild edgerings and faces
    TopologyDirectedEdge[] adjDE = new TopologyDirectedEdge[4];
    adjDE[0] = (TopologyDirectedEdge) TopologyEdgeRing.getNextRight(newEdge.getDirEdge(0));
    adjDE[1] = (TopologyDirectedEdge) TopologyEdgeRing.getPrevRight(newEdge.getDirEdge(0));
    adjDE[2] = (TopologyDirectedEdge) TopologyEdgeRing.getNextRight(newEdge.getDirEdge(1));
    adjDE[3] = (TopologyDirectedEdge) TopologyEdgeRing.getPrevRight(newEdge.getDirEdge(1));

    // get the affected faces
    Set facesToDelete = new HashSet();
    for (int i = 0; i < 4; i++) {
      if (adjDE[i] != null) {
        TopologyFace face = adjDE[i].getFace();
        if (face != null)
          facesToDelete.add(face);
      }
    }
    Assert.isTrue(facesToDelete.size() <= 2, "too many faces being deleted");

    // get the affected dirEdges
    List deToUpdate = new ArrayList();
    for (Iterator faceIt = facesToDelete.iterator(); faceIt.hasNext(); ) {
      TopologyFace face = (TopologyFace) faceIt.next();
      addAll(face.iterator(), deToUpdate);
    }
    deToUpdate.add(newEdge.getDirEdge(0));
    deToUpdate.add(newEdge.getDirEdge(1));

    // remove the affected edgerings
    for (int i = 0; i < 4; i++) {
      if (adjDE[i] != null) {
        TopologyEdgeRing er = adjDE[i].getEdgeRing();
        if (er != null)
          adjDE[i].getEdgeRing().remove();
      }
    }

    // remove the affected faces
    topology.removeFaces(facesToDelete);

    // compute new edgerings and faces for the affected diredges
    List newFaces = TopologyBuilder.computeFaces(deToUpdate.iterator());
    Assert.isTrue(newFaces.size() <= 2, "too many faces being created");

    topology.addFaces(newFaces);
    return newEdge;
  }

  /**
   * Merges the given {@link TopologyFace} into the destination face.
   * The new face takes on the data of the second face.
   * Assumes the faces are adjacent - if they are not, no action is taken.
   *
   * @param mergee the face to merge
   * @param destFace the face to merge into
   * @return the new face created
   */
  public TopologyFace mergeFace(TopologyFace mergee, TopologyFace destFace)
  {
    Collection commonEdges = mergee.getCommonEdges(destFace);
    if (commonEdges.size() == 0)
      return null;
    Collection newFaces = deleteEdges(commonEdges);
    if (newFaces.size() != 1) {
      throw new TopologyException("Found more than one result face from merge of adjacent faces");
    }
    TopologyFace newFace = (TopologyFace) newFaces.iterator().next();
    newFace.setData(destFace.getData());
    return newFace;
  }


}