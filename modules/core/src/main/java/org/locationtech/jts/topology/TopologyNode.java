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

import org.locationtech.jts.geom.Coordinate;

class TopologyNode {

  private TopologyHalfEdge edge;

  public TopologyNode(TopologyHalfEdge e) {
    edge = e;
  }

  public Coordinate getCoordinate() {
    return edge.orig();
  }

  public TopologyHalfEdge getEdge() {
    return edge;
  }

}
