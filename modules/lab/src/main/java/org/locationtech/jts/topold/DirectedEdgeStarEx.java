package org.locationtech.jts.topold;

import java.util.List;

import org.locationtech.jts.planargraph.DirectedEdge;
import org.locationtech.jts.planargraph.DirectedEdgeStar;

/**
 * Further methods for {@link DirectedEdgeStar}s
 */
public class DirectedEdgeStarEx
{
  public static final int CLOCKWISE = -1;
  public static final int COUNTERCLOCKWISE = 1;

  /**
   * A condition which can be used to select or filter a {@link DirectedEdge}
   */
  public interface Condition {
    /**
     * Tests whether the argument meets the condition.
     *
     * @return <code>true</code> if the edge meets the condition
     */
    boolean isTrue(DirectedEdge de);
  }

  /**
   * Returns the next {@link DirectedEdge} in the {@link DirectedEdgeStar}
   * starting at the specified edge,
   * in the specified direction.
   * If there is only one edge in the star the input edge is returned.
   *
   * @param deStar the edges to scan
   * @param startDE the edge to start at
   * @param direction the scan direction
   * @return the next edge in the star
   */
  public static DirectedEdge nextEdge(DirectedEdgeStar deStar,
                                      DirectedEdge startDE,
                                      int direction)
  {
    return nextEdge(deStar, startDE, direction, null);
  }

  /**
   * Returns the next {@link DirectedEdge} in the {@link DirectedEdgeStar}
   * starting at the specified edge,
   * in the specified direction,
   * for which the specified {@link Condition} is <code>true</code>.
   * If there is only one edge in the star the input edge is returned.
   *
   * @param deStar the edges to scan
   * @param startDE the edge to start at
   * @param direction the scan direction
   * @param condition a condition to select the desired edges
   * @return the next edge in the star
   */
  public static DirectedEdge nextEdge(DirectedEdgeStar deStar,
                                      DirectedEdge startDE,
                                      int direction,
                                      Condition condition)
  {
    int startIndex = deStar.getIndex(startDE);
    int index = startIndex;
    List outEdges = deStar.getEdges();
    int directionInc = direction >= 0 ? COUNTERCLOCKWISE : CLOCKWISE;
    DirectedEdge nextEdge;
    do {
      index = deStar.getIndex(index + directionInc);
      nextEdge = (DirectedEdge) outEdges.get(index);
      if (nextEdge == startDE) break;
      if (condition == null || condition.isTrue(nextEdge)) break;
    } while (true);
    return nextEdge;
  }

}