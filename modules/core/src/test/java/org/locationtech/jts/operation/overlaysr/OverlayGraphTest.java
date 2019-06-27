/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlaysr;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.noding.BasicSegmentString;
import org.locationtech.jts.noding.SegmentString;
import org.locationtech.jts.util.Assert;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

public class OverlayGraphTest extends GeometryTestCase {

  public static void main(String args[]) {
    TestRunner.run(OverlayGraphTest.class);
  }

  public OverlayGraphTest(String name) { super(name); }
  
  /**
   * This test produced an error using the old HalfEdge sorting algorithm
   * (in {@link HalfEdge#insert(HalfEdge)}).
   */
  public void testCCWAfterInserts() {
    OverlayGraph graph = new OverlayGraph();
    OverlayEdge e1 = addEdge(graph, 50, 39, 35, 42, 37, 30);
    addEdge(graph, 50, 39, 50, 60, 20, 60);
    addEdge(graph, 50, 39, 68, 35);
    checkCCW(e1);
  }

  private void checkCCW(OverlayEdge e) {
    boolean isCCW = e.isCCW();
    assertTrue("Found non-CCW edges around node " + e.toStringNode(), isCCW); 
  }

  private OverlayEdge addEdge(OverlayGraph graph, double... ord) {
    Coordinate[] pts = toCoordinates(ord);
    return graph.addEdge(pts, new OverlayLabel());
  }

  private Coordinate[] toCoordinates(double[] ord) {
    Coordinate[] pts = new Coordinate[ord.length / 2];
    for (int i = 0; i < pts.length; i++) {
      pts[i] = new Coordinate(ord[2*i], ord[2*i+1]);
    }
    return pts;
  }
}
