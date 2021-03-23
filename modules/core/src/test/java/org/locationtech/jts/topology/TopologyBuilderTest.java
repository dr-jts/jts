package org.locationtech.jts.topology;

import java.util.Collection;
import java.util.Iterator;

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

  public void testTriangleWithHole() throws Exception {
    String[] wkt = {
        "LINESTRING (153 200, 64 81, 340 89, 153 200)",   //shell
        "LINESTRING (153 200, 134 149, 200 145, 153 200)",   // hole
          };
    checkTopology(wkt);
  }
  
  public void testSquareWithHoles() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (10 10, 10 20, 20 20, 20 10, 10 10)",   // hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    checkTopology(wkt);
  }

  public void testSquareWithTouchingHoles() throws Exception {
    String[] wkt = {
        "LINESTRING (0 0, 0 100, 100 100, 100 0, 0 0)",   //shell
        "LINESTRING (0 0, 10 20, 20 10, 0 0)",   // touching hole
        "LINESTRING (30 10, 30 20, 40 20, 40 10, 30 10)"  // hole
          };
    checkTopology(wkt);
  }

  
  void checkTopology(String[] wkts)
      throws Exception
  {
    topoBuilder = new TopologyBuilder();
    topoBuilder.addEdgesFromLineStrings(readList(wkts));

    Topology cov = topoBuilder.getTopology();
    Collection<TopologyFace> faces = cov.getFaces();
    for (Iterator<TopologyFace> i = faces.iterator(); i.hasNext(); ) {
      TopologyFace face = (TopologyFace) i.next();
      Polygon poly = face.getPolygon(geomFact);
      System.out.println(poly);
    }
  }
}
