package org.locationtech.jts.topold;

import java.util.*;




/**
 * Traverses a region of adjacent faces in breadth-first order
 * according to the supplied boundary condition.
 * If {@link isBoundary} is implemented to always return false,
 * will traverse all faces in the Topology.
 *
 * @author Martin Davis
 * @version 1.0
 */
public abstract class BoundedRegionFaceVisitor
{
  private LinkedList faceQueue = new LinkedList();

  public BoundedRegionFaceVisitor(TopologyFace startFace)
  {
    faceQueue.addLast(startFace);
  }

  public void visitAll()
  {
    while (! faceQueue.isEmpty()) {
      TopologyFace currFace = (TopologyFace) faceQueue.getFirst();
      faceQueue.removeFirst();
      if (isVisited(currFace)) continue;

      visit(currFace);
      addAdjacentRegionFaces(currFace);
//System.out.println(faceQueue.size());
    }
  }

  private void addAdjacentRegionFaces(TopologyFace face)
  {
    Set adjFaces = new HashSet();
    for (Iterator deIt = face.iterator(); deIt.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) deIt.next();
      if (! isBoundary(de)) {
        TopologyFace adjFace = ((TopologyDirectedEdge) de.getSym()).getFace();
        // face may be null if an outside or hole edge is not a boundary
        if (adjFace != null && ! isVisited(adjFace))
          adjFaces.add(adjFace);
      }
    }
    // add unique adjacent interior faces to queue
    for (Iterator adjFaceIt = adjFaces.iterator(); adjFaceIt.hasNext(); ) {
      TopologyFace adjFace = (TopologyFace) adjFaceIt.next();
      faceQueue.addLast(adjFace);
    }
  }

  protected abstract boolean isBoundary(TopologyDirectedEdge de);

  protected abstract boolean isVisited(TopologyFace face);

  protected abstract void visit(TopologyFace face);
}