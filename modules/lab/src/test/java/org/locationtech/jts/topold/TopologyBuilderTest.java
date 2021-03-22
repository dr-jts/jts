package org.locationtech.jts.topold;

import java.util.Collection;
import java.util.Iterator;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.topold.Topology;
import org.locationtech.jts.topold.TopologyBuilder;
import org.locationtech.jts.topold.TopologyFace;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

/**
 * Test building {@link Topology}s
 */
public class TopologyBuilderTest extends GeometryTestCase {

  public static void main(String[] args) throws Exception
  {
    TestRunner.run(TopologyBuilderTest.class);
  }
  
  private GeometryFactory geomFact = new GeometryFactory();
  private WKTReader reader = new WKTReader();
  private TopologyBuilder topoBuilder;

  public TopologyBuilderTest(String name) {
    super(name);
  }

  public void testTriangleWithHole() throws Exception {
    String[] wkt = {
        "LINESTRING (153 200, 64 81, 340 89, 153 200)",   //shell
        "LINESTRING (153 200, 134 149, 200 145, 153 200)",   // hole
          };
    run(wkt);
  }

  public void testSquareWithHoles() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (10 10, 10 20, 20 20, 20 10, 10 10)",   // hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    run(wkt);
  }

  public void testSquareWithTouchingHoles() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (0 0, 10 20, 20 10, 0 0)",   // touching hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    run(wkt);
  }

  public void testTriangleWithCutEdge() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 0, 0 0)",   //shell
        "LINESTRING (0 0, 10 10)",   // cut edge
        "LINESTRING (10 10, 10 20, 20 10, 10 10)"  // hole
          };
    run(wkt);
  }

  public void testFigure8WithDangle() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0 , 10 10)",   // isolated edge
        "LINESTRING (185 221, 100 100)",   //dangling edge
        "LINESTRING (185 221, 88 275, 180 316)",
        "LINESTRING (185 221, 292 281, 180 316)",
        "LINESTRING (189 98, 83 187, 185 221)",
        "LINESTRING (189 98, 325 168, 185 221)"
          };
    run(wkt);
  }
  
  void run(String[] wkts)
      throws Exception
  {
    topoBuilder = new TopologyBuilder();
    topoBuilder.addEdgesFromLineStrings(readList(wkts));

    Topology cov = topoBuilder.getTopology();
    Collection faces = cov.getFaces();
    for (Iterator i = faces.iterator(); i.hasNext(); ) {
      TopologyFace face = (TopologyFace) i.next();
      Polygon poly = face.getPolygon(geomFact);
      System.out.println(poly);
    }
  }




}