/*
 * Copyright (c) 2022 Martin Davis
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.topology;

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
class PolygonBuilder
{
  private GeometryFactory geomFact;
  private Polygon poly;

  public PolygonBuilder(TopologyFace face, GeometryFactory geomFact) {
    this.geomFact = geomFact;
    build(face);
  }

  private void build(TopologyFace face) {
    List shellRings = face.getShell().getMinimalRings(geomFact);
    List<LinearRing> holeRingList = new ArrayList<LinearRing>();
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

  private List<LinearRing> getHoleRings(TopologyFace face, List<LinearRing> holeRingList)
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