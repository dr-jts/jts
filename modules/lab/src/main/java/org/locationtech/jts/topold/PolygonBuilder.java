package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;

/**
 * Builds a {@link Polygon} from a {@link TopologyFace}
 */
public class PolygonBuilder
{
  private GeometryFactory geomFact;
  private Polygon poly;

  public PolygonBuilder(TopologyFace face, GeometryFactory geomFact) {
    this.geomFact = geomFact;
    build(face);
  }

  private void build(TopologyFace face) {
    List shellRings = face.getShell().getMinimalRings(geomFact);
    List holeRingList = new ArrayList();
    LinearRing shell = null;
    // exactly one of the rings is the shell, the rest must be holes
    for (Iterator i = shellRings.iterator(); i.hasNext(); ) {
      LinearRing ring = (LinearRing) i.next();
      if (Orientation.isCCW(ring.getCoordinates())) {
          holeRingList.add(ring);
      }
      else {
        if (shell != null)
          throw new TopologyException("Found more than one shell ring");
        shell = ring;
      }
    }
    if (shell == null)
      throw new TopologyException("Unable to determine shell ring");
    // build isolated holes
    if (face.hasHoles()) {
      getHoleRings(face, holeRingList);
    }
    poly = geomFact.createPolygon(shell, GeometryFactory.toLinearRingArray(holeRingList));
  }

  private List getHoleRings(TopologyFace face, List holeRingList)
  {
    for (Iterator i = face.getHoles().iterator(); i.hasNext(); ) {
      TopologyEdgeRing holeEdgeRing = (TopologyEdgeRing) i.next();
      holeRingList.addAll(holeEdgeRing.getMinimalRings(geomFact));
    }
    return holeRingList;
  }

  public Polygon getPolygon()
  {
    return poly;
  }
}