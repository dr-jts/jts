package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.DirectedEdgeStar;
import org.locationtech.jts.planargraph.Node;

/**
 * Represents a ring of {@link TopologyDirectedEdge}s
 * around a {@link TopologyFace}.
 * The ring may be either an outer shell or a hole.
 * The ring may contain self-intersections, which represent either inverted
 * holes or linked holes.
 *
 * @version 1.4
 */
public class TopologyEdgeRing
{

  /**
   * Test whether the parent edge of a DirectedEdge
   * lies on the outer boundary of an EdgeRing.
   * (Dangling or Cut edges will have the same EdgeRing on both sides).
   *
   * @param de the DirectedEdge to test
   * @return <code>true</code> if the parent edge lies on the boundary of the ring
   */
  public static boolean isExternal(TopologyDirectedEdge de)
  {
    boolean isExternal = de.getEdgeRing() != ((TopologyDirectedEdge) de.getSym()).getEdgeRing();
    return isExternal;
  }

  /**
   * Gets the next DirectedEdge which is rightmost of this one.
   * If this dirEdge is part of a ring,
   * this is the next dirEdge in the ring.
   *
   * @param de
   */
  public static DirectedEdge getNextRight(DirectedEdge de)
  {
    DirectedEdge sym = de.getSym();
    Node node = sym.getFromNode();
    DirectedEdgeStar deStar = node.getOutEdges();
    DirectedEdge next = DirectedEdgeStarEx.nextEdge(deStar, sym, DirectedEdgeStarEx.COUNTERCLOCKWISE);
    return next;
  }

  /**
   * Gets the previous DirectedEdge which is rightmost of this one.
   * If this dirEdge is part of a ring,
   * this is the previous dirEdge in the ring.
   *
   * @param de
   */
  public static DirectedEdge getPrevRight(DirectedEdge de)
  {
    Node node = de.getFromNode();
    DirectedEdgeStar deStar = node.getOutEdges();
    DirectedEdge prevSym = DirectedEdgeStarEx.nextEdge(deStar, de, DirectedEdgeStarEx.CLOCKWISE);
    return prevSym.getSym();
  }

  public static TopologyEdgeRing buildEdgeRing(TopologyDirectedEdge startDE)
  {
    TopologyEdgeRing er = new TopologyEdgeRing();
    er.build(startDE);
    return er;
  }

  public static TopologyEdgeRing OLDbuildEdgeRing(TopologyDirectedEdge startDE)
  {
    TopologyDirectedEdge de = startDE;
    TopologyEdgeRing er = new TopologyEdgeRing();
    do {
      er.add(de);
      de.setEdgeRing(er);
      //de = de.getNext();
      de = (TopologyDirectedEdge) getNextRight(de);
      //Assert.isTrue(de != null, "found null DE in ring");
      if (de != startDE && de.isInEdgeRing())
          throw new TopologyException("found directed edge already assigned to ring");
      //Assert.isTrue(de == startDE || ! de.isInEdgeRing(), "found DE already in ring");
    } while (de != startDE);
    return er;
  }

  /**
   * Find the innermost enclosing shell EdgeRing containing the argument EdgeRing, if any.
   * The innermost enclosing ring is the <i>smallest</i> enclosing ring.
   * The algorithm used depends on the fact that:
   * <br>
   *  ring A contains ring B iff envelope(ring A) contains envelope(ring B)
   * <br>
   * This routine is only safe to use if the chosen point of the hole
   * is known to be properly contained in a shell
   * (which is guaranteed to be the case if the hole does not touch its shell)
   *
   * @param testER the hole edgering to assign
   * @param shellList the list of candidate shells
   * @return containing EdgeRing, if there is one
   * @return <code>null</code> if no containing EdgeRing is found
   */
  public static TopologyEdgeRing findEdgeRingContaining(TopologyEdgeRing testER, List shellList)
  {
    LinearRing testRing = testER.getRing();
    Envelope testEnv = testRing.getEnvelopeInternal();

    TopologyEdgeRing minShell = null;
    Envelope minEnv = null;
    for (Iterator it = shellList.iterator(); it.hasNext(); ) {
      TopologyEdgeRing tryShell = (TopologyEdgeRing) it.next();
      LinearRing tryShellRing = tryShell.getRing();
      Envelope tryEnv = tryShellRing.getEnvelopeInternal();
      boolean isContained = false;
      // the hole must be contained in the shell envelope
      if (! tryEnv.contains(testEnv))
        continue;
      // the hole envelope cannot equal the shell envelope
      if (tryEnv.equals(testEnv))
        continue;

      /**
       * Here the tryShell ring contains the hole ring.
       * Now we must test containment exactly by testing if a hole pt
       * is contained in the shell ring.  To do this, we must use a hole pt
       * which is guaranteed not to be in the shell ring (since the ptInRing test
       * is only valid if this is the case).
       */
      Coordinate[] tryShellPts = tryShellRing.getCoordinates();
      Coordinate testPt = ptNotInList(testRing.getCoordinates(), tryShellPts);
      if (PointLocation.isInRing(testPt, tryShellPts) )
        isContained = true;
      // check if this new containing ring is smaller than the current minimum ring
      if (isContained) {
        if (minShell == null
            || minEnv.contains(tryEnv)) {
          minShell = tryShell;
          minEnv = minShell.getRing().getEnvelopeInternal();
        }
      }
    }
    return minShell;
  }

  /**
   * Finds a point in a list of points which is not contained in another list of points
   *
   * @param testPts the {@link Coordinate}s to test
   * @param pts an array of {@link Coordinate}s to test the input points against
   * @return a {@link Coordinate} from <code>testPts</code> which is not in <code>pts</code>, '
   * or <code>null</code>
   */
  public static Coordinate ptNotInList(Coordinate[] testPts, Coordinate[] pts)
  {
    for (int i = 0; i < testPts.length; i++) {
      Coordinate testPt = testPts[i];
      if (! isInList(testPt, pts))
          return testPt;
    }
    return null;
  }

  /**
   * Tests whether a given point is in an array of points.
   * Uses a value-based test.
   *
   * @param pt a {@link Coordinate} for the test point
   * @param pts an array of {@link Coordinate}s to test
   * @return <code>true</code> if the point is in the array
   */
  public static boolean isInList(Coordinate pt, Coordinate[] pts)
  {
    for (int i = 0; i < pts.length; i++) {
        if (pt.equals(pts[i]))
            return true;
    }
    return false;
  }

  // used only to compute a ring for hole placement
  private static GeometryFactory ringFactory = new GeometryFactory();

  private TopologyFace face = null;
  private List deList = null;
  // cache the following data for efficiency
  private Coordinate[] ringPts = null;
  private LinearRing ring = null;

  public TopologyEdgeRing()
  {
  }

  public void build(TopologyDirectedEdge startDE)
  {
    deList = new ArrayList();
    ringPts = null;
    ring = null;
    TopologyDirectedEdge de = startDE;
    do {
      add(de);
      de.setEdgeRing(this);
      //de = de.getNext();
      de = (TopologyDirectedEdge) getNextRight(de);
      //Assert.isTrue(de != null, "found null DE in ring");
      if (de != startDE && de.isInEdgeRing())
          throw new TopologyException("found directed edge already assigned to ring");
      //Assert.isTrue(de == startDE || ! de.isInEdgeRing(), "found DE already in ring");
    } while (de != startDE);
  }

  /**
   * Clears this edgeRing from its directed edges.
   * Used only during rebuilding the edgering after an edge has
   * been replaced by a set of edges with identical topology
   */
  void clearDirEdgeList()
  {
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      de.setEdgeRing(null);
    }
    deList = null;
  }

  /**
   * Adds a {@link DirectedEdge} which is known to form part of this ring.
   * @param de the {@link DirectedEdge} to add.
   */
  private void add(DirectedEdge de)
  {
    deList.add(de);
  }


  /**
   * An iterator over the {@link TopologyDirectedEdge}s in this ring.
   * @return an iterator over the dir edges
   */
  public Iterator iterator() { return deList.iterator(); }

  /**
   * Sets the face containing this ring
   *
   * @param face the face containing this ring
   */
  public void setFace(TopologyFace face) {
    this.face = face;
  }

  public TopologyFace getFace() { return face; }

  /**
   * Tests whether this ring is a hole.
   * Due to the way the edges in the graph are linked,
   * a ring is a hole if it is oriented counter-clockwise.
   *
   * @return <code>true</code> if this ring is a hole
   */
  public boolean isHole()
  {
    // perhaps this could be optimized?
    getCoordinates();
    if (ringPts.length < 3)
      return false;
    return Orientation.isCCW(ringPts);
  }

  /**
   * Tests if the {@link LinearRing} formed by this edge ring is topologically valid.
   * @return <code>true</code> if the ring is valid
   */
  public boolean isValid()
  {
    getCoordinates();
    if (ringPts.length <= 3) return false;
    getRing();
    return ring.isValid();
  }

  /**
   * Tests if the ring formed by the edges is big enough to be a valid {@link LinearRing}.
   * E.g. single lines will not form valid rings.
   *
   * @return <code>true</code> if there are enough points for a valid ring
   */
  public boolean isValidSizeForRing()
  {
    getCoordinates();
    if (ringPts.length <= 3) return false;
    return true;
  }

  /**
   * Computes the list of coordinates which are contained in this ring.
   * The coordinatea are computed once only and cached.
   *
   * @return an array of the {@link Coordinate}s in this ring
   */
  private Coordinate[] getCoordinates()
  {
    if (ringPts == null) {
      CoordinateList coordList = new CoordinateList();
      for (Iterator i = iterator(); i.hasNext(); ) {
        TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
        de.addEdgeCoordinates(coordList);
      }
      ringPts = coordList.toCoordinateArray();
    }
    return ringPts;
  }

  /**
   * Removes this edgeRing from the topology.
   * This causes all associated dirEdges to have their edgering cleared
   */
  void remove()
  {
    if (isRemoved()) return;
    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      de.setEdgeRing(null);
    }
    deList = null;
    face = null;
  }

  /**
   * Tests whether this edgeRing is removed.
   *
   * @return true if this edgeRing is removed
   */
  boolean isRemoved() { return deList == null; }

  /**
   * Returns this ring as a {@link LinearRing}, or null if an Exception occurs while
   * creating it (such as a topology problem). Details of problems are written to
   * standard output.
   *
   * @return the LinearRing computed
   */
  private LinearRing getRing()
  {
    if (ring != null) return ring;
    getCoordinates();
    if (ringPts.length < 3)
      System.out.println("less than 3 pts in ring: " + ringPts[0]);
    try {
      ring = ringFactory.createLinearRing(ringPts);
    }
    catch (Exception ex) {
      System.out.println("less than 3 pts in ring: " + ringPts[0]);
    }
    return ring;
  }

  /**
   * Computes the minimal LinearRingss forming this edgering.
   * The set of rings returned will be either a shell and some holes
   * (if this ring is the shell of a {@link TopologyFace} -
   * a shell will generate hole rings if it is self-intersecting),
   * or a set of holes (if this ring is a hole of a {@link TopologyFace}
   * The caller is responsible for determining whether they are a shell or a hole
   * (e.g. by checking their orientation).
   *
   * @param geomFact the factory to use when constructing the coordinate sequences
   * @return a list of {@link LinearRing}s
   */
  public List getMinimalRings(GeometryFactory geomFact)
  {
    List rings = new ArrayList();
    Set edgesUsed = new HashSet();

    for (Iterator i = iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      if (! edgesUsed.contains(de)) {
        if (isExternal(de)) {
          PolygonRingBuilder polyRing = new PolygonRingBuilder(de);
          LinearRing ring = polyRing.getRing(edgesUsed, geomFact);
          if (ring != null)
            rings.add(ring);
        }
      }
    }
    return rings;
  }
}
