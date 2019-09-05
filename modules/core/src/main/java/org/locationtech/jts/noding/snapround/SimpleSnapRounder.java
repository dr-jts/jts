
/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.noding.snapround;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.noding.InteriorIntersectionFinderAdder;
import org.locationtech.jts.noding.MCIndexNoder;
import org.locationtech.jts.noding.NodedSegmentString;
import org.locationtech.jts.noding.Noder;
import org.locationtech.jts.noding.NodingValidator;
import org.locationtech.jts.noding.SegmentString;
import org.locationtech.jts.noding.SinglePassNoder;

/**
 * Uses Snap Rounding to compute a rounded,
 * fully noded arrangement from a set of {@link SegmentString}s.
 * Implements the Snap Rounding technique described in 
 * the papers by Hobby, Guibas &amp; Marimont, and Goodrich et al.
 * Snap Rounding assumes that all vertices lie on a uniform grid;
 * hence the precision model of the input must be fixed precision,
 * and all the input vertices must be rounded to that precision.
 * <p>
 * This implementation uses simple iteration over the line segments.
 * This is not the most efficient approach for large sets of segments.
 * <p>
 * This implementation appears to be fully robust using an integer precision model.
 * It will function with non-integer precision models, but the
 * results are not 100% guaranteed to be correctly noded.
 *
 * @version 1.7
 */
public class SimpleSnapRounder
    implements Noder
{
  private final PrecisionModel pm;
  private LineIntersector li;
  private final double scaleFactor;
  private Collection nodedSegStrings;
  private Map<Coordinate, HotPixel> hotPixelMap = new HashMap<Coordinate, HotPixel>();
  private List<HotPixel> hotPixels;
  
  private List<SegmentString> snapped;

  public SimpleSnapRounder(PrecisionModel pm) {
    this.pm = pm;
    li = new RobustLineIntersector();
    li.setPrecisionModel(pm);
    scaleFactor = pm.getScale();
  }

  /**
	 * @return a Collection of NodedSegmentStrings representing the substrings
	 * 
	 */
  public Collection getNodedSubstrings()
  {
    return NodedSegmentString.getNodedSubstrings(snapped);
  }

  /**
   * @param inputSegmentStrings a Collection of NodedSegmentStrings
   */
  public void computeNodes(Collection inputSegmentStrings)
  {
    this.nodedSegStrings = inputSegmentStrings;
    snapRound(inputSegmentStrings, li);

    // testing purposes only - remove in final version
    //checkCorrectness(inputSegmentStrings);
  }

  private void checkCorrectness(Collection inputSegmentStrings)
  {
    Collection resultSegStrings = NodedSegmentString.getNodedSubstrings(inputSegmentStrings);
    NodingValidator nv = new NodingValidator(resultSegStrings);
    try {
      nv.checkValid();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  private List<SegmentString> snapRound(Collection<NodedSegmentString> segStrings, LineIntersector li)
  {
    List<Coordinate> intersections = findInteriorIntersections(segStrings, li);
    addHotPixels(intersections);
    
    addVertexPixels(segStrings);
    
    hotPixels = new ArrayList<HotPixel>(hotPixelMap.values());

    snapped = computeSnaps(segStrings);
    return snapped;
  }

  private void addVertexPixels(Collection<NodedSegmentString> segStrings) {
    for (NodedSegmentString nss : segStrings) {
      Coordinate[] pts = nss.getCoordinates();
      addHotPixels(pts);
    }
  }

  private boolean hasHotPixel(Coordinate pt) {
    return hotPixelMap.containsKey(pt);
  }

  private void addHotPixels(Coordinate[] pts) {
    for (Coordinate pt : pts) {
      createHotPixel( round(pt) );
    }
  }
  private void addHotPixels(List<Coordinate> pts) {
    for (Coordinate pt : pts) {
      createHotPixel( round(pt) );
    }
  }

  private Coordinate round(Coordinate pt) {
    Coordinate p2 = pt.copy();
    pm.makePrecise(p2);
    return p2;
  }

  private HotPixel createHotPixel(Coordinate p) {
    HotPixel hp = hotPixelMap.get(p);
    if (hp != null) return hp;
    hp = new HotPixel(p, scaleFactor, li);
    hotPixelMap.put(p,  hp);
    return hp;
  }

  /**
   * Computes all interior intersections in the collection of {@link SegmentString}s,
   * and returns their {@link Coordinate}s.
   *
   * Does NOT node the segStrings.
   *
   * @return a list of Coordinates for the intersections
   */
  private List<Coordinate> findInteriorIntersections(Collection segStrings, LineIntersector li)
  {
    InteriorIntersectionFinderAdder intFinderAdder = new InteriorIntersectionFinderAdder(li);
    SinglePassNoder noder = new MCIndexNoder();
    noder.setSegmentIntersector(intFinderAdder);
    noder.computeNodes(segStrings);
    return intFinderAdder.getInteriorIntersections();
  }


  /**
   * Computes nodes introduced as a result of snapping segments to snap points (hot pixels)
   * @param li
   * @return 
   */
  private List<SegmentString> computeSnaps(Collection<NodedSegmentString> segStrings)
  {
    List<SegmentString> snapped = new ArrayList<SegmentString>();
    for (NodedSegmentString ss : segStrings ) {
      SegmentString snappedSS = computeSnaps(ss, snapped);
      snapped.add(snappedSS);
    }
    return snapped;
  }

  private SegmentString computeSnaps(NodedSegmentString ss, List<SegmentString> snapped)
  {
    CoordinateList snapPts = new CoordinateList();
    Coordinate[] pts = ss.getCoordinates();
    
    NodedSegmentString snapSS = round(pts, ss.getData());
    
    int snapSSIndex = 0;
    for (int i = 0; i < pts.length - 1; i++ ) {
      Coordinate currSnap = snapSS.getCoordinate(snapSSIndex);

      /**
       * If this segment has collapsed completely, skip it
       */
      Coordinate p0 = pts[i];
      Coordinate p1 = pts[i+1];
      Coordinate p1Round = round(p1);
      if (p1Round.equals2D(currSnap))
        continue;
      
      /**
       * Add any HP intersections to corresponding snapSS segment
       */
      
      snapSegment( p0, p1, snapSS, snapSSIndex);
        
      snapSSIndex++;
    }
    return snapSS;
  }

  private NodedSegmentString round(Coordinate[] pts, Object data) {
    CoordinateList roundPts = new CoordinateList();
    
    for (int i = 0; i < pts.length; i++ ) {
      roundPts.add( round( pts[i] ), false);
    }
    return new NodedSegmentString( roundPts.toCoordinateArray(), data );

  }

  /**
   * This is where all the work of snapping to hot pixels gets done
   * (in a very inefficient brute-force way).
   * 
   * @param coordinate
   * @param coordinate2
   * @param snapPts
   */
  private void snapSegment(Coordinate p0, Coordinate p1, NodedSegmentString ss, int segIndex) {
    for (HotPixel hp : hotPixels) {
      snapSegment(p0, p1, hp, ss, segIndex);
    }
  }

  private void snapSegment(Coordinate p0, Coordinate p1, HotPixel hp, NodedSegmentString ss, int segIndex) {
    if (hp.intersects(p0, p1)) {
      ss.addIntersection( hp.getCoordinate(), segIndex );
    }
  }



}
