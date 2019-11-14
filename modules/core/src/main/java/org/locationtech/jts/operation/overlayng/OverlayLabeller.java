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
package org.locationtech.jts.operation.overlayng;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geomgraph.Position;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.Debug;

class OverlayLabeller {
  private OverlayGraph graph;
  private InputGeometry inputGeometry;
  private Collection<OverlayEdge> edges;
  
  public OverlayLabeller(OverlayGraph graph, InputGeometry inputGeometry) {
    this.graph = graph;
    this.inputGeometry = inputGeometry;
    edges = graph.getEdges();
  }

  /**
   * Computes the topological labelling for the edges in the graph.
   * 
   */
  public void computeLabelling() {
    Collection<OverlayEdge> nodes = graph.getNodeEdges();
    labelAreaNodeEdges(nodes);
    
    //TODO: is there a way to avoid scanning all edges in these steps?
    /**
     * At this point collapsed edges labeled with location UNKNOWN
     * must be disconnected from the area edges of the parent.
     * They can be located based on their parent ring role (shell or hole).
     */
    labelCollapsedEdges();
    
    labelDisconnectedEdges();
  }


  /**
   * Labels node edges based on the arrangement
   * of boundary edges incident on them.
   * Also propagates the labelling to connected linear edges.
   *  
   * @param nodes the nodes to label
   */
  private void labelAreaNodeEdges(Collection<OverlayEdge> nodes) {
    for (OverlayEdge nodeEdge : nodes) {
      propagateAreaLocations(nodeEdge, 0);
      if (inputGeometry.hasEdges(1)) {
        propagateAreaLocations(nodeEdge, 1);
      }
    }
    labelConnectedLinearEdges();
  }

  /**
   * Scans around a node CCW, propagating the side labels
   * for a given area geometry to all edges (and their sym)
   * with unknown locations for that geometry.
   * @param e2 
   * 
   * @param geomIndex the geometry to propagate locations for
   */
  public static void propagateAreaLocations(OverlayEdge nodeEdge, int geomIndex) {
    /**
     * This handles dangling edges created by overlap limiting
     */
    if (nodeEdge.degree() == 1) return;
    
    OverlayEdge eStart = findPropagationStartEdge(nodeEdge, geomIndex);
    // no labelled edge found, so nothing to propagate
    if ( eStart == null )
      return;
    
    // initialize currLoc to location of L side
    int currLoc = eStart.getLocation(geomIndex, Position.LEFT);
    OverlayEdge e = eStart.oNextOE();

    //Debug.println("\npropagateSideLabels geomIndex = " + geomIndex + " : " + eStart);
    //Debug.print("BEFORE: " + toString(eStart));
    
    do {
      OverlayLabel label = e.getLabel();
      if ( ! label.isBoundary(geomIndex) ) {
      /**
       * If this is not a Boundary edge for this input area, 
       * its location is now known relative to this input area
       */
        label.setLocationLine(geomIndex, currLoc);
      }
      else {
        Assert.isTrue(label.hasSides(geomIndex));
        /**
         *  This is a boundary edge for the input area geom.
         *  Update the current location from its labels.
         *  Also check for topological consistency.
         */
        int locRight = e.getLocation(geomIndex, Position.RIGHT);
        if (locRight != currLoc) {
          /*
          Debug.println("side location conflict: index= " + geomIndex + " R loc " 
        + Location.toLocationSymbol(locRight) + " <>  curr loc " + Location.toLocationSymbol(currLoc) 
        + " for " + e);
        //*/
          throw new TopologyException("side location conflict: arg " + geomIndex, e.getCoordinate());
        }
        int locLeft = e.getLocation(geomIndex, Position.LEFT);
        if (locLeft == Location.NONE) {
          Assert.shouldNeverReachHere("found single null side at " + e);
        }
        currLoc = locLeft;
      }
      e = e.oNextOE();
    } while (e != eStart);
    //Debug.print("AFTER: " + toString(eStart));
  }

  /**
   * Finds a boundary edge for this geom, if one exists
   * 
   * @param nodeEdge an edge for this node
   * @param geomIndex the parent geometry index
   * @return a boundary edge, or null if no boundary edge exists
   */
  private static OverlayEdge findPropagationStartEdge(OverlayEdge nodeEdge, int geomIndex) {
    OverlayEdge eStart = nodeEdge;
    do {
      OverlayLabel label = eStart.getLabel();
      if (label.isBoundary(geomIndex)) {
        Assert.isTrue(label.hasSides(geomIndex));
        return eStart;
      }
      eStart = (OverlayEdge) eStart.oNext();
    } while (eStart != nodeEdge);
    return null;
  }
  
  /**
   * At this point collapsed edges with unknown location
   * must be disconnected from the boundary edges of the parent
   * (because otherwise the location would have
   * been propagated from them).
   * They can be now located based on their parent ring role (shell or hole).
   * (This cannot be done earlier, because the location
   * based on the boundary edges must take precedence.
   * There are situations where a collapsed edge has a location 
   * which is different to its ring role - 
   * e.g. a narrow gore in a polygon, which is in 
   * the interior of the reduced polygon, but whose
   * ring role would imply the location EXTERIOR.)
   * 
   * Note that collapsed edges can NOT have location determined via a PIP location check,
   * because that is done against the unreduced input geometry,
   * which may give an invalid result due to topology collapse.
   * 
   * The labelling is propagated to other connected edges, 
   * since there may be NOT_PART edges which are connected, 
   * and they need to be labelled in the same way.
   */
  private void labelCollapsedEdges() {
    for (OverlayEdge edge : edges) {
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelCollapsedEdge(edge, 0);
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelCollapsedEdge(edge, 1);
      }
    }
    labelConnectedLinearEdges();
  }

  private void labelCollapsedEdge(OverlayEdge edge, int geomIndex) {
    //Debug.println("\n------  labelCollapsedEdge - geomIndex= " + geomIndex);
    //Debug.print("BEFORE: " + edge.toStringNode());
    OverlayLabel label = edge.getLabel();
    if (! label.isCollapse(geomIndex)) return;
      /**
       * This must be a collapsed edge which is disconnected
       * from any area edges (e.g. a fully collapsed shell or hole).
       * It can be labelled according to its parent source ring role. 
       */
    label.setLocationCollapse(geomIndex);
    //Debug.print("AFTER: " + edge.toStringNode());
  }

  /**
   * There can be edges which have unknown location
   * but are connected to a Line edge with known location.
   * In this case line location is propagated to the connected edges.
   */
  private void labelConnectedLinearEdges() {
    //TODO: can these be merged to avoid two scans?
    propagateLineLocations(0);
    if (inputGeometry.hasEdges(1)) {
      propagateLineLocations(1);
    }
  }

  private void propagateLineLocations(int geomIndex) {
    // find L edges
    List<OverlayEdge> lineEdges = findLinearEdgesWithLocation(geomIndex);
    Deque<OverlayEdge> edgeStack = new ArrayDeque<OverlayEdge>(lineEdges);
    
    propagateLineLocations(geomIndex, edgeStack);
  }
  
  private void propagateLineLocations(int geomIndex, Deque<OverlayEdge> edgeStack) {
    // traverse line edges, labelling unknown ones that are connected
    while (! edgeStack.isEmpty()) {
      OverlayEdge lineEdge = edgeStack.removeFirst();
      // assert: lineEdge.getLabel().isLine(geomIndex);
      
      // for any edges around origin with unknown location for this geomIndex,
      // mark them as Exterior
      // add those edges to stack to continue traversal
      propagateLineLocation(lineEdge, geomIndex, edgeStack, inputGeometry);
    }
  }
  
  private static void propagateLineLocation(OverlayEdge eStart, int index, 
      Deque<OverlayEdge> edgeStack, InputGeometry inputGeometry) {
    OverlayEdge e = eStart.oNextOE();
    int lineLoc = eStart.getLabel().getLineLocation(index);
    
    /**
     * If the parent geom is an L (dim 1) 
     * then only propagate EXTERIOR locations.
     */
    if (! inputGeometry.isArea(index) 
        && lineLoc != Location.EXTERIOR) return;
    
    do {
      OverlayLabel label = e.getLabel();
      //Debug.println("propagateLineLocationAtNode - checking " + index + ": " + e);
      if ( label.isLineLocationUnknown(index) ) {
        /**
         * If edge is not a boundary edge, 
         * its location is now known for this area
         */
        label.setLocationLine(index, lineLoc);
        //Debug.println("propagateLineLocationAtNode - setting "+ index + ": " + e);

        /**
         * Add sym edge to stack for graph traversal
         * (Don't add e itself, since e origin node has now been scanned)
         */
        edgeStack.addFirst( e.symOE() );
      }
      e = e.oNextOE();
    } while (e != eStart);
  }
  
  /**
   * Finds all OverlayEdges which are labelled as L dimension.
   * 
   * @param geomIndex
   * @return list of L edges
   */
  private List<OverlayEdge> findLinearEdgesWithLocation(int geomIndex) {
    List<OverlayEdge> lineEdges = new ArrayList<OverlayEdge>();
    for (OverlayEdge edge : edges) {
      OverlayLabel lbl = edge.getLabel();
      if (lbl.isLinear(geomIndex)
          && ! lbl.isLineLocationUnknown(geomIndex)) {
        lineEdges.add(edge);
      }
    }
    return lineEdges;
  }

  /**
   * At this point there may still be edges which have unknown location
   * relative to an input geometry.
   * This must be because they are NOT_PART edges for that geometry, 
   * and are disconnected from any edges of that geometry.
   * 
   * If the input geometry is an Area the edge location can
   * be determined via a PIP test.
   * If the input is not an Area the location is EXTERIOR. 
   */
  private void labelDisconnectedEdges() {
    for (OverlayEdge edge : edges) {
      //Debug.println("\n------  checking for Disconnected edge " + edge);
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelDisconnectedEdge(edge, 0);
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelDisconnectedEdge(edge, 1);
      }
    }
  }

  /**
   * Determines the location of an edge relative to a target input geometry.
   * The edge has no location information
   * because it is disconnected from other
   * edges that would provide that information.
   * The location is determined by checking 
   * if the edge lies inside the target geometry area (if any).
   * 
   * @param edge the edge to label
   * @param geomIndex the input geometry to label against
   */
  private void labelDisconnectedEdge(OverlayEdge edge, int geomIndex) { 
     OverlayLabel label = edge.getLabel();
     //Assert.isTrue(label.isNotPart(geomIndex));
    
    /**
     * if target geom is not an area then 
     * edge must be EXTERIOR, since to be 
     * INTERIOR it would have been labelled
     * when it was created.
     */
    if (! inputGeometry.isArea(geomIndex)) {
      label.setLocationAll(geomIndex, Location.EXTERIOR);
      return;
    };
    
    //Debug.println("\n------  labelDisconnectedEdge - geomIndex= " + geomIndex);
    //Debug.print("BEFORE: " + edge.toStringNode());
    /**
     * Locate edge in input area using a Point-In-Poly check.
     * This should be safe even with precision reduction, 
     * because since the edge has remained disconnected
     * its interior-exterior relationship 
     * can be determined relative to the original input geometry.
     */
    //int edgeLoc = locateEdge(geomIndex, edge);
    int edgeLoc = locateEdgeBothEnds(geomIndex, edge);
    label.setLocationAll(geomIndex, edgeLoc);
    //Debug.print("AFTER: " + edge.toStringNode());
  }

  /**
   * Determines the {@link Location} for an edge within an Area geometry
   * via point-in-polygon location.
   * <p>
   * NOTE this is only safe to use for disconnected edges,
   * since the test is carried out against the original input geometry,
   * and precision reduction may cause incorrect results for edges
   * which are close enough to a boundary to become connected. 
   * 
   * @param geomIndex the parent geometry index
   * @param edge the edge to locate
   * @return the location of the edge
   */
  private int locateEdge(int geomIndex, OverlayEdge edge) {
    int loc = inputGeometry.locatePointInArea(geomIndex, edge.orig());
    int edgeLoc = loc != Location.EXTERIOR ? Location.INTERIOR : Location.EXTERIOR;
    return edgeLoc;
  }  
  
  /**
   * Determines the {@link Location} for an edge within an Area geometry
   * via point-in-polygon location,
   * by checking that both endpoints are interior to the target geometry.
   * Checking both endpoints ensures correct results in the presence of topology collapse.
   * <p>
   * NOTE this is only safe to use for disconnected edges,
   * since the test is carried out against the original input geometry,
   * and precision reduction may cause incorrect results for edges
   * which are close enough to a boundary to become connected. 
   * 
   * @param geomIndex the parent geometry index
   * @param edge the edge to locate
   * @return the location of the edge
   */
  private int locateEdgeBothEnds(int geomIndex, OverlayEdge edge) {
    /*
     * To improve the robustness of the point location,
     * check both ends of the edge.
     * Edge is only labelled INTERIOR if both ends are.
     */
    int locOrig = inputGeometry.locatePointInArea(geomIndex, edge.orig());
    int locDest = inputGeometry.locatePointInArea(geomIndex, edge.dest());
    boolean isInt = locOrig != Location.EXTERIOR && locDest != Location.EXTERIOR;
    int edgeLoc = isInt ? Location.INTERIOR : Location.EXTERIOR;
    return edgeLoc;
  } 

  public void markResultAreaEdges(int overlayOpCode) {
    for (OverlayEdge edge : edges) {
      markInResultArea(edge, overlayOpCode);
    }
  }

  /**
   * Marks an edge which forms part of the boundary of the result area.
   * This is determined by the overlay operation being executed,
   * and the location of the edge.
   * The relevant location is either the right side of a boundary edge,
   * or the line location of a non-boundary edge.
   * 
   * @param e the edge to mark
   * @param overlayOpCode the overlay operation
   */
  public void markInResultArea(OverlayEdge e, int overlayOpCode) {
    OverlayLabel label = e.getLabel();
    if ( label.isBoundaryEither()
        && OverlayNG.isResultOfOp(
              overlayOpCode,
              label.getLocationBoundaryOrLine(0, Position.RIGHT, e.isForward()),
              label.getLocationBoundaryOrLine(1, Position.RIGHT, e.isForward()))) {
      e.markInResultArea();  
    }
    //Debug.println("markInResultArea: " + e);
  }
  
  /**
   * Unmarks result area edges where the sym edge 
   * is also marked as in the result.
   * This has the effect of merging edge-adjacent result areas,
   * as required by polygon validity rules.
   */
  public void unmarkDuplicateEdgesFromResultArea() {
    for (OverlayEdge edge : edges ) {
      if ( edge.isInResultAreaBoth() ) {
        edge.unmarkFromResultAreaBoth();     
      }
    }
  }

}