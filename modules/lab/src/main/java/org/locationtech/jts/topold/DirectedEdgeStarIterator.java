package org.locationtech.jts.topold;

import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.DirectedEdgeStar;

/**
 */
public class DirectedEdgeStarIterator implements Iterator {

  public static final int CLOCKWISE = -1;
  public static final int COUNTERCLOCKWISE = 1;

  /**
   * The increment for traversing the array of edges
   */
  private int directionInc;
  private boolean isStarted = false;
  private DirectedEdgeStar deStar;
  private List outEdges;
  private int startIndex;
  private int index;

  public DirectedEdgeStarIterator(DirectedEdgeStar deStar,
                                  DirectedEdge startDE,
                                  int direction) {
    this.deStar = deStar;
    directionInc = direction >= 0 ? COUNTERCLOCKWISE : CLOCKWISE;
    startIndex = deStar.getIndex(startDE);
    index = startIndex;
    outEdges = deStar.getEdges();
  }

  public boolean hasNext() {
    return index != startIndex;
  }

  public Object next() {
    // guard against running past end of iterator
    if (isStarted && index == startIndex)
      return null;

    isStarted = true;
    DirectedEdge nextDE = (DirectedEdge) outEdges.get(index);
    index = deStar.getIndex(index + directionInc);
    return nextDE;
  }
  public void remove() {
  throw new java.lang.UnsupportedOperationException("Method remove() not supported.");
}

}