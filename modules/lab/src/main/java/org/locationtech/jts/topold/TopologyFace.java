package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.planargraph.GraphComponent;
import org.locationtech.jts.planargraph.Node;
import org.locationtech.jts.util.Assert;

/**
 * Represents a face of a {@link Topology}.
 * Faces contain a single enclosing {@link TopologyEdgeRing} (oriented CW) and possibly
 * some disjoint internal hole {@link TopologyEdgeRing}s (oriented CCW).
 */
public class TopologyFace extends GraphComponent{
  private TopologyEdgeRing shell;
  private List holes;  // List<TopologyEdgeRing>

  public TopologyFace(TopologyEdgeRing shell) {
    this.shell = shell;
  }

/**
   * Removes this face from its containing {@link Topology}.
   * This causes all associated {@link TopologyEdgeRing}s
   * to be removed as well.
   */
  void remove()
  {
    if (isRemoved()) return;
    shell.remove();
    if (holes != null) {
      for (Iterator i = holes.iterator(); i.hasNext(); ) {
        TopologyEdgeRing holeER = (TopologyEdgeRing) i.next();
        holeER.remove();
      }
    }
    shell = null;
    holes = null;
  }

  /**
   * Tests whether this face has been removed from its containing Topology
   * @return
   */
  public boolean isRemoved() { return shell == null; }

  public TopologyEdgeRing getShell() { return shell; }

  public boolean hasHoles()
  {
    boolean noHoles = (holes == null || holes.isEmpty());
    return ! noHoles;
  }

  /**
   * Returns a List of {@link TopologyEdgeRing}s, if any.
   *
   * @return a list of the hole edgerings in this face, or <code>null</code> if none
   */
  public List getHoles() { return holes; }

  void addHole(TopologyEdgeRing hole)
  {
    if (holes == null)
      holes = new ArrayList();
    holes.add(hole);
  }

  /**
   * Gets all the {@link TopologyDirectedEdge}s in this face's edgerings
   *
   * @return a list of dir edges
   */
  public List getDirEdges()
  {
    List dirEdges = new ArrayList();
    for (Iterator it = shell.iterator(); it.hasNext(); ) {
      dirEdges.add(it.next());
    }
    if (holes != null)  {
      for (Iterator holeIt = holes.iterator(); holeIt.hasNext(); ) {
        TopologyEdgeRing hole = (TopologyEdgeRing) holeIt.next();
        for (Iterator it = hole.iterator(); it.hasNext(); ) {
          dirEdges.add(it.next());
        }
      }
    }
    return dirEdges;
  }

  /**
   * Gets an {@link Iterator} over the {@link TopologyDirectedEdge}s
   * around this face.
   *
   * @return
   */
  public Iterator iterator()
  {
    // MD - testing only
//    Iterator dirEdgeIt = new OLDTopologyFaceDirEdgeIterator(this);
//    checkIterator(dirEdgeIt);
    return new TopologyFaceDirEdgeIterator(this);
  }

  /**
   * Tests that an iterator produces the correct list of dirEdges
   */
  private void checkIterator(Iterator dirEdgeIt)
  {
    List dirEdges = getDirEdges();
    List itDirEdges = new ArrayList();
    while (dirEdgeIt.hasNext()) {
      itDirEdges.add(dirEdgeIt.next());
    }
    if (dirEdges.size() != itDirEdges.size()) {
      Assert.shouldNeverReachHere("sizes do not match");
    }
    for (int i = 0; i < dirEdges.size(); i++) {
      if (dirEdges.get(i) != itDirEdges.get(i)) {
        Assert.shouldNeverReachHere("dir edge mismatch");
      }
    }
  }

  /**
   * Gets the {@link Polygon} geometry determined by this face.
   *
   * @param geomFact the GeometryFactory to use to build the polygon
   * @return the polygon for the face
   */
  public Polygon getPolygon(GeometryFactory geomFact)
  {
    PolygonBuilder builder = new PolygonBuilder(this, geomFact);
    return builder.getPolygon();
  }

  /**
   * Computes the {@link TopologyFace}s which edge-adjacent to this face.
   * Two faces are edge-adjacent if they share an edge.
   *
   * @return a collection of the adjacent faces (if any)
   */
  public Collection getAdjacentFaces()
  {
    Set adjFaces = new HashSet();
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      TopologyFace adjFace = ((TopologyDirectedEdge) de.getSym()).getFace();
      if (adjFace != null)
        adjFaces.add(adjFace);
    }
    return adjFaces;
  }

  /**
   * Computes the {@link TopologyFace}s which touch this one.
   * Two faces touch if they share an edge or a node
   *
   * @return a collection of the adjacent faces (if any)
   */
  public Collection getTouchingFaces()
  {
    Set touchFaces = new HashSet();
    // add edge-adjacent faces
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      TopologyFace adjFace = ((TopologyDirectedEdge) de.getSym()).getFace();
      if (adjFace != null && adjFace != this)
        touchFaces.add(adjFace);
    }

    // add node-adjacent faces
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      Node node = de.getFromNode();
      for (Iterator nodeDEIt = node.getOutEdges().iterator(); nodeDEIt.hasNext(); ) {
        TopologyDirectedEdge nodeDE = (TopologyDirectedEdge) nodeDEIt.next();
        TopologyFace face = nodeDE.getFace();
        if (face != null && face != this)
          touchFaces.add(face);
      }
    }
    return touchFaces;
  }

  /**
   * Computes the common {@link TopologyEdge}s between two {@link TopologyFace}s.
   *
   * @param face the adjacent face
   * @return a collection containing the common edges (if any)
   */
  public Collection getCommonEdges(TopologyFace face)
  {
    List commonEdges = new ArrayList();
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      TopologyFace adjFace = ((TopologyDirectedEdge) de.getSym()).getFace();
      if (adjFace == face)
        commonEdges.add(de.getEdge());
    }
    return commonEdges;
  }

  /**
   * An {@link Iterator} over all {TopologyDirectedEdge}s
   * forming the shell and hole edgerings of this face.
   *
   * This implementation builds an explicit list of the
   * rings in the face.
   */
  private class TopologyFaceDirEdgeIterator
      implements Iterator
  {
    private List edgeRings = new ArrayList();
    private int erIndex = 0;
    private Iterator it = null;

    TopologyFaceDirEdgeIterator(TopologyFace face)
    {
      edgeRings.add(face.getShell());
      List holeList = face.getHoles();
      if (holeList != null)
        edgeRings.addAll(holeList);
      nextIterator();
    }

    public void remove()
    {
      throw new UnsupportedOperationException("not implemented");
    }

    public boolean hasNext()
    {
      if (it == null) {
        return false;
      }
      while (it != null && ! it.hasNext()) {
        nextIterator();
      }
      if (it != null)
        return it.hasNext();
      return false;
    }

    public Object next()
    {
      while (it != null && ! it.hasNext())
        nextIterator();
      if (it != null)
        return it.next();
      return null;
    }

    private void nextIterator()
    {
      if (erIndex >= edgeRings.size()) {
        it = null;
        return;
      }
      TopologyEdgeRing er = (TopologyEdgeRing) edgeRings.get(erIndex++);
      if (er == null)
        System.out.println("at null");
      it = er.iterator();
    }
  }

  private class OLDTopologyFaceDirEdgeIterator
      implements Iterator
  {
    private TopologyFace face;
    private Iterator it = null;
    private Iterator holeIt = null;

    OLDTopologyFaceDirEdgeIterator(TopologyFace face)
    {
      this.face = face;
      List holeList = face.getHoles();
      if (holeList != null)
        holeIt = holeList.iterator();
    }

    public void remove()
    {
      throw new UnsupportedOperationException("not implemented");
    }

    public boolean hasNext()
    {
      if (it == null) {
        it = nextIterator();
      }
      if (it == null) return false;
      return it.hasNext();
    }

    public Object next()
    {
      if (it != null)
        return it.next();
      return null;
    }

    private Iterator nextIterator()
    {
      if (it == null) {
        it = face.getShell().iterator();
        return it;
      }
      if (holeIt != null && holeIt.hasNext()) {
        it = ((TopologyEdgeRing) holeIt.next()).iterator();
        return it;
      }
      holeIt = null;
      return null;
    }
  }
}