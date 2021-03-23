package org.locationtech.jts.topology;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class TopologyFace {

  private TopologyEdgeRing shell;
  private List<TopologyEdgeRing> holes;

  public TopologyFace(TopologyEdgeRing shell) {
    this.shell = shell;
  }


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
  public List<TopologyEdgeRing> getHoles() { return holes; }
  
  public void addHole(TopologyEdgeRing holeER) {
    if (holes == null)
      holes = new ArrayList<TopologyEdgeRing>();
    holes.add(holeER);
  }

  public Polygon getPolygon(GeometryFactory geomFact) {
    PolygonBuilder builder = new PolygonBuilder(this, geomFact);
    return builder.getPolygon();
  }

  public String toString() {
    return getPolygon(new GeometryFactory()).toString();
  }
}
