package org.locationtech.jts.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;


import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

public class TopologyBuilderTest extends GeometryTestCase {

  public static void main(String[] args) throws Exception
  {
    TestRunner.run(TopologyBuilderTest.class);
  }
  
  private GeometryFactory geomFact = new GeometryFactory();
  private TopologyBuilder topoBuilder;

  public TopologyBuilderTest(String name) {
    super(name);
  }

  public void testTriangleWithHole() {
    String[] wkt = {
        "LINESTRING (153 200, 64 81, 340 89, 153 200)",   //shell
        "LINESTRING (153 200, 134 149, 200 145, 153 200)",   // hole
          };
    String[] expected = {
        "POLYGON ((153 200, 340 89, 64 81, 153 200), (153 200, 134 149, 200 145, 153 200))",
        "POLYGON ((153 200, 200 145, 134 149, 153 200))"
    };
    checkTopology(wkt, expected);
  }
  
  public void testSquareWithHoles() {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (10 10, 10 20, 20 20, 20 10, 10 10)",   // hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    String[] expected = {
        "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0), (10 10, 20 10, 20 20, 10 20, 10 10), (30 10, 40 10, 40 20, 30 20, 30 10))",
        "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))",
        "POLYGON ((30 10, 30 20, 40 20, 40 10, 30 10))"
    };
    checkTopology(wkt, expected);
  }

  public void testSquareWithTouchingHoles() {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (0 0, 10 20, 20 10, 0 0)",   // touching hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    String[] expected = {
        "POLYGON ((0 0, 0 100, 100 100, 100 0, 0 0), (0 0, 20 10, 10 20, 0 0), (30 10, 40 10, 40 20, 30 20, 30 10))",
        "POLYGON ((0 0, 10 20, 20 10, 0 0))",
        "POLYGON ((30 10, 30 20, 40 20, 40 10, 30 10)))"
    };
    checkTopology(wkt, expected);
  }

  public void testTriangleWithCutEdge() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 0, 0 0)",   //shell
        "LINESTRING (0 0, 10 10)",   // cut edge
        "LINESTRING (10 10, 10 20, 20 10, 10 10)"  // hole
          };
    checkTopology(wkt);
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
    checkTopology(wkt);
  }
  
  void checkTopology(String[] wkts) {
    checkTopology(wkts, null);
  }
  
  void checkTopology(String[] wkts, String[] wktExpected)
  {
    topoBuilder = new TopologyBuilder();
    topoBuilder.addEdgesFromLineStrings(readList(wkts));

    Topology cov = topoBuilder.getTopology();
    Collection<TopologyFace> faces = cov.getFaces();
    List<Polygon> polys = new ArrayList<Polygon>();
    for (Iterator<TopologyFace> i = faces.iterator(); i.hasNext(); ) {
      TopologyFace face = (TopologyFace) i.next();
      Polygon poly = face.getPolygon(geomFact);
      polys.add(poly);
      System.out.println(poly);
    }
    
    if (wktExpected != null) {
      Geometry expected = toGeometryCollection(readList(wktExpected));
      checkEqual(expected, toGeometryCollection(polys));
    }
  }
}
