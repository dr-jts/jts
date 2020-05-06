package org.locationtech.jts.operation.intarea;

import java.util.List;

import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;

public class IntersectionArea {
  
  public static double area(Geometry geom0, Geometry geom1) {
    IntersectionArea area = new IntersectionArea(geom0);
    return area.area(geom1);
  }
  
  private static LineIntersector li = new RobustLineIntersector();
  
  private Geometry geom0;
  private IndexedPointInAreaLocator locator0;
  private STRtree indexSegs;

  public IntersectionArea(Geometry geom) {
    this.geom0 = geom;
    locator0 = new IndexedPointInAreaLocator(geom);
    indexSegs = buildIndex(geom);
  }
  
  public double area(Geometry geom) {
    // TODO: for now assume poly is CW and has no holes
    
    double area = 0.0;
    
    area += areaForIntersections(geom);
    
    area += areaForInteriorVertices(geom, geom0.getEnvelopeInternal(), locator0);
    IndexedPointInAreaLocator locator1 = new IndexedPointInAreaLocator(geom);
    area += areaForInteriorVertices(geom0, geom.getEnvelopeInternal(), locator1);
    
    return area;
  }

  private double areaForIntersections(Geometry geomB) {
    double area = 0.0;
    CoordinateSequence seqB = getVertices(geomB);
    
    boolean isCCWB = Orientation.isCCW(seqB);
    
    // Compute rays for all intersections   
    for (int j = 0; j < seqB.size() - 1; j++) {
      Coordinate b0 = seqB.getCoordinate(j);
      Coordinate b1 = seqB.getCoordinate(j+1);
      if (isCCWB) {
        // flip segment orientation
        Coordinate temp = b0; b0 = b1; b1 = temp;
      }
      
      Envelope env = new Envelope(b0, b1);
      IntersectionVisitor intVisitor = new IntersectionVisitor(b0, b1);
      indexSegs.query(env, intVisitor);
      area += intVisitor.getArea();
    }
    return area;
  }

  class IntersectionVisitor implements ItemVisitor {
    double area = 0.0;
    private Coordinate b0;
    private Coordinate b1;
    
    IntersectionVisitor(Coordinate b0, Coordinate b1) {
      this.b0 = b0;
      this.b1 = b1;
    }
    
    double getArea() {
      return area;
    }
    
    public void visitItem(Object item) {
      LineSegment seg = (LineSegment) item;
      Coordinate a0 = seg.p0;
      Coordinate a1 = seg.p1;
      
      area += segmentIntArea(b0, b1, a0, a1);
    }
  }
  
  private double segmentIntArea(Coordinate b0, Coordinate b1, Coordinate a0, Coordinate a1) {
    // can assume segment envelopes interact
    
    // TODO: can the intersection computation be optimized?
    li.computeIntersection(a0, a1, b0, b1);
    if (! li.hasIntersection()) return 0.0;
    
    /**
     * With both rings oriented CW (effectively)
     * There are two situations for segment intersections:
     * 
     * 1) A entering B, B exiting A => rays are IP-A1:R, IP-B0:L
     * 2) A exiting B, B entering A => rays are IP-A0:L, IP-B1:R
     * (where :L/R indicates result is to the Left or Right).
     * 
     * Use full edge to compute direction, for accuracy.
     */
    Coordinate intPt = li.getIntersection(0);
    
    boolean isAenteringB = Orientation.COUNTERCLOCKWISE == Orientation.index(a0, a1, b1);
    
    if ( isAenteringB ) {
      return EdgeRay.areaTerm(intPt, a0, a1, true)
        + EdgeRay.areaTerm(intPt, b1, b0, false);
    }
    else {
      return EdgeRay.areaTerm(intPt, a1, a0, false)
       + EdgeRay.areaTerm(intPt, b0, b1, true);
    }
  }

  
  private double addIntersectionsOLD(Geometry geomB) {
    double area = 0.0;
    CoordinateSequence seqA = getVertices(geom0);
    CoordinateSequence seqB = getVertices(geomB);
    
    boolean isCCWA = Orientation.isCCW(seqA);
    boolean isCCWB = Orientation.isCCW(seqB);
    
    Envelope envB = geomB.getEnvelopeInternal();
    // Compute rays for all intersections
    LineIntersector li = new RobustLineIntersector();
    
    for (int i = 0; i < seqA.size() - 1; i++) {
      Coordinate a0 = seqA.getCoordinate(i);
      Coordinate a1 = seqA.getCoordinate(i+1);
      
      if (! envB.intersects(a0, a1)) continue;
      
      if (isCCWA) {
        // flip segment orientation
        Coordinate temp = a0; a0 = a1; a1 = temp;
      }
      
      Envelope envSeg0 = new Envelope(a0, a1);
      
      for (int j = 0; j < seqB.size() - 1; j++) {
        Coordinate b0 = seqB.getCoordinate(j);
        Coordinate b1 = seqB.getCoordinate(j+1);
        
        if (! envSeg0.intersects(b0, b1)) continue;
        
        if (isCCWB) {
          // flip segment orientation
          Coordinate temp = b0; b0 = b1; b1 = temp;
        }
        
        li.computeIntersection(a0, a1, b0, b1);
        if (li.hasIntersection()) {
          //isIntersected0[i] = true;
          //isIntersected1[j] = true;
          
          /**
           * With both rings oriented CW (effectively)
           * There are two situations for segment intersections:
           * 
           * 1) A entering B, B exiting A => rays are IP-A1:R, IP-B0:L
           * 2) A exiting B, B entering A => rays are IP-A0:L, IP-B1:R
           * (where :L/R indicates result is to the Left or Right).
           * 
           * Use full edge to compute direction, for accuracy.
           */
          Coordinate intPt = li.getIntersection(0);
          
          boolean isAenteringB = Orientation.COUNTERCLOCKWISE == Orientation.index(a0, a1, b1);
          
          if ( isAenteringB ) {
            area += EdgeRay.areaTerm(intPt, a0, a1, true);
            area += EdgeRay.areaTerm(intPt, b1, b0, false);
          }
          else {
            area += EdgeRay.areaTerm(intPt, a1, a0, false);
            area += EdgeRay.areaTerm(intPt, b0, b1, true);
          }
        }
      }
    }
    return area;
  }
    
  private double areaForInteriorVertices(Geometry geom, Envelope env, IndexedPointInAreaLocator locator) {
    /**
     * Compute rays originating at vertices inside the intersection result
     * (i.e. A vertices inside B, and B vertices inside A)
     */
    double area = 0.0;
    CoordinateSequence seq = getVertices(geom);
    boolean isCW = ! Orientation.isCCW(seq);
    
    for (int i = 0; i < seq.size()-1; i++) {
      Coordinate v = seq.getCoordinate(i);
      if (! env.contains(v)) continue;
      if (Location.INTERIOR == locator.locate(v)) {
        Coordinate vPrev = i == 0 ? seq.getCoordinate(seq.size()-2) : seq.getCoordinate(i-1);
        Coordinate vNext = seq.getCoordinate(i+1);
        area += EdgeRay.areaTerm(v, vPrev, ! isCW);
        area += EdgeRay.areaTerm(v, vNext, isCW);
      }
    }
    return area;
  }
  
  private CoordinateSequence getVertices(Geometry geom) {
    Polygon poly = (Polygon) geom;
    CoordinateSequence seq = poly.getExteriorRing().getCoordinateSequence();
    return seq;
  }
  
  private STRtree buildIndex(Geometry geom) {
    Coordinate[] coords = geom.getCoordinates();
    
    boolean isCCWA = Orientation.isCCW(coords);
    STRtree index = new STRtree();
    for (int i = 0; i < coords.length - 1; i++) {
      Coordinate a0 = coords[i];
      Coordinate a1 = coords[i+1];
      LineSegment seg = new LineSegment(a0, a1);
      if (isCCWA) {
        seg = new LineSegment(a1, a0);
      }
      Envelope env = new Envelope(a0, a1);
      index.insert(env, seg);
    }
    return index;
  }
}
