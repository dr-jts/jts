package org.locationtech.jts.operation.overlaysr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.noding.NodedSegmentString;
import org.locationtech.jts.noding.SegmentString;
import org.locationtech.jts.operation.overlay.OverlayOp;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

public class OverlaySR {


  /**
   * Computes an overlay operation for 
   * the given geometry arguments.
   * 
   * @param geom0 the first geometry argument
   * @param geom1 the second geometry argument
   * @param opCode the code for the desired overlay operation
   * @return the result of the overlay operation
   */
  public static Geometry overlayOp(Geometry geom0, Geometry geom1, PrecisionModel pm, int opCode)
  {
    OverlaySR gov = new OverlaySR(geom0, geom1, pm);
    Geometry geomOv = gov.getResultGeometry(opCode);
    return geomOv;
  }

  private Geometry[] geom;
  private GeometryFactory geomFact;
  private PrecisionModel pm;

  public OverlaySR(Geometry geom0, Geometry geom1, PrecisionModel pm) {
    geom = new Geometry[] { geom0, geom1 };
    this.pm = pm;
    geomFact = geom0.getFactory();
  }  
  
  private Geometry getResultGeometry(int overlayOpCode) {
    Geometry resultGeom = computeOverlay(overlayOpCode);
    return resultGeom;
    //return TESToverlay(overlayOpCode);
  }

  private Geometry TESToverlay(int overlayOpCode) {
    Geometry gr0 = GeometryPrecisionReducer.reduce(geom[0], pm);
    Geometry gr1 = GeometryPrecisionReducer.reduce(geom[1], pm);
    if (overlayOpCode == OverlayOp.UNION) {
      
      // **********  TESTIMG ONLY  **********
      return gr0.union(gr1);
    }
    else if (overlayOpCode == OverlayOp.INTERSECTION) {
      return gr0.intersection(gr1);
    }
    // MD - will not implement other overlay ops yet
    throw new UnsupportedOperationException();
  }

  private Geometry computeOverlay(int overlayOpCode) {
    Collection<SegmentString> edges = node();
    OverlayGraph graph = buildTopology(edges);
    
    //TODO: extract included linework from graph
    
    //TODO: build geometries
    return toLines(edges, geomFact );
  }

  private OverlayGraph buildTopology(Collection<SegmentString> edges) {
    OverlayGraph graph = OverlayGraph.buildGraph( edges );
    graph.computeLabelling();
    return graph;
  }

  private Collection<SegmentString> node() {
    OverlayNoder noder = new OverlayNoder(pm);
    noder.add(geom[0], 0);
    noder.add(geom[1], 1);
    Collection<SegmentString> edges = noder.node();
    Collection<SegmentString> mergedEdges = merge(edges);
    return mergedEdges;
  }
  
  private Collection<SegmentString> merge(Collection<SegmentString> edges) {
    // TODO implement merging here
    
    //computeLabelsFromDepths();
    //replaceCollapsedEdges();
    
    return edges;
  }

  private static Geometry toLines(Collection<SegmentString> segStrings, GeometryFactory geomFact) {
    List lines = new ArrayList();
    for (SegmentString ss : segStrings ) {
      //Coordinate[] pts = getCoords(nss);
      Coordinate[] pts = ss.getCoordinates();
      
      lines.add(geomFact.createLineString(pts));
    }
    return geomFact.buildGeometry(lines);
  }

}
