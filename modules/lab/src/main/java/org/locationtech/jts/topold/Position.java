package org.locationtech.jts.topold;

/**
 * A code denoting a position relative to an edge in a {@link Topology}.
 *
 * @version 1.4
 */
public class Position
{
  /** A code for a position that is <i>on</i> an edge */
  public static final int ON      = 0;
  /** A code for a position that is <i>left</i> of an edge */
  public static final int LEFT    = 1;
  /** A code for a position that is <i>right</i> of an edge */
  public static final int RIGHT   = 2;

  /**
   * Returns LEFT if the position is RIGHT, RIGHT if the position is LEFT,
   * or the position otherwise.
   */

  /**
   * Computes the position which is the opposite of the input.
   *
   * @param position a position
   * @return LEFT if the position is RIGHT, RIGHT if the position is LEFT,
   * or the input position otherwise
   */
  public static final int opposite(int position)
  {
    if (position == LEFT) return RIGHT;
    if (position == RIGHT) return LEFT;
    return position;
  }
}