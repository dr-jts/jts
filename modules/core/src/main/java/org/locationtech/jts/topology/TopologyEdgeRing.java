package org.locationtech.jts.topology;

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

public class TopologyEdgeRing {
  
  public static TopologyEdgeRing create(TopologyHalfEdge startDE)
  {
    TopologyEdgeRing er = new TopologyEdgeRing();
    er.build(startDE);
    return er;
  }
  
  // used only to compute a ring for hole placement
  private static GeometryFactory ringFactory = new GeometryFactory();

  private TopologyFace face;
  private List<TopologyHalfEdge> heList;

  private Coordinate[] ringPts = null;
  private LinearRing ring = null;
  
  public boolean isValidSizeForRing() {
    getCoordinates();
    if (ringPts.length <= 3) return false;
    return true;
  }

  public boolean isHole() {
    // perhaps this could be optimized?
    getCoordinates();
    if (ringPts.length < 3)
      return false;
    return Orientation.isCCW(ringPts);
  }

  /**
   * Tests whether this edgeRing is removed.
   *
   * @return true if this edgeRing is removed
   */
  boolean isRemoved() { return heList == null; }

  // only until minimal rings can be built
  public LinearRing getRingTEMP() {
    return getRing();
  }
  
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
  
  private Coordinate[] getCoordinates() {
    if (ringPts == null) {
      CoordinateList coordList = new CoordinateList();
      for (Iterator<TopologyHalfEdge> i = iterator(); i.hasNext(); ) {
        TopologyHalfEdge de = i.next();
        de.addEdgeCoordinates(coordList);
      }
      ringPts = coordList.toCoordinateArray();
    }
    return ringPts;
  }

  /**
   * An iterator over the {@link TopologyDirectedEdge}s in this ring.
   * @return an iterator over the dir edges
   */
  public Iterator<TopologyHalfEdge> iterator() { return heList.iterator(); }

  /**
   * Sets the face containing this ring
   *
   * @param face the face containing this ring
   */
  public void setFace(TopologyFace face) {
    this.face = face;
  }

  public TopologyFace getFace() { return face; }
  
  public void build(TopologyHalfEdge start) {
    heList = new ArrayList<TopologyHalfEdge>();
    ringPts = null;
    ring = null;
    TopologyHalfEdge e = start;
    do {
      heList.add(e);
      e.setEdgeRing(this);
      e = e.nextTE();
      //Assert.isTrue(de != null, "found null DE in ring");
      if (e != start && e.isInEdgeRing())
          throw new TopologyException("found directed edge already assigned to ring");
    } while (e != start);
  }

  public String toString() {
    if (ring == null) return "TER";
    return ring.toString();
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
  
  /**
   * Computes the minimal LinearRings formed by this edgering.
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
  public List<LinearRing> getMinimalRings(GeometryFactory geomFact)
  {
    List<LinearRing> rings = new ArrayList<LinearRing>();
    Set<TopologyHalfEdge> edgesUsed = new HashSet<TopologyHalfEdge>();

    for (Iterator<TopologyHalfEdge> i = iterator(); i.hasNext(); ) {
      TopologyHalfEdge e = i.next();
      if (! edgesUsed.contains(e)) {
        if (e.isOuter()) {
          LinearRing ring = PolygonRingBuilder.getRing(e, edgesUsed, geomFact);
          if (ring != null)
            rings.add(ring);
        }
      }
    }
    return rings;
  }



}
