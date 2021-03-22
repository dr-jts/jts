package org.locationtech.jts.topold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

/**
 * A region is a subset of {@link TopologyFace}s
 * from a {@link Topology}.
 * The geometry for a Region is a {@link Polygon}
 * if the region is <b>edge-connected</b>.
 * Otherwise, it will be a {@link MultiPolygon).
 *
 * @author Martin Davis
 * @version 1.0
 */
public class Region
{
  /**
   * An object used to label region faces in the region topology
   */
  private static final String faceLabel = "REGION";

  private Collection inputRegionFaces;

  public Region(Collection faces) {
    this.inputRegionFaces = faces;
  }

  public Geometry getGeometry(GeometryFactory geomFactory)
  {
    // use another topology to build the polygonal geometry for the region
    Topology topo = buildTopology();
    labelFacesInRegion(topo);
    Collection polys = extractRegionPolygons(topo, geomFactory);
    return geomFactory.buildGeometry(polys);
  }

  private Topology buildTopology()
  {
    Collection uniqueDE = findUniqueEdges(inputRegionFaces);
    TopologyBuilder topoBuilder = new TopologyBuilder();
    for (Iterator i = uniqueDE.iterator(); i.hasNext(); ) {
      TopologyDirectedEdge de = (TopologyDirectedEdge) i.next();
      TopologyEdge e = (TopologyEdge) de.getEdge();
      TopologyEdge regionEdge = topoBuilder.addEdge(e.getCoordinates());
      regionEdge.setData(new Boolean(de.getEdgeDirection()));
    }
    return topoBuilder.getTopology();
  }

  private void labelFacesInRegion(Topology regionTopo)
  {
    for (Iterator edgeIt = regionTopo.getGraph().edgeIterator(); edgeIt.hasNext(); ) {
      TopologyEdge edge = (TopologyEdge) edgeIt.next();
      Boolean isOnRight = (Boolean) edge.getData();
      int regionFaceIndex = isOnRight.booleanValue() ? 0 : 1;
      edge.getFaces()[regionFaceIndex].setData(faceLabel);
    }
  }

  private Collection extractRegionPolygons(Topology regionTopo, GeometryFactory geomFactory)
  {
    List polys = new ArrayList();
    for (Iterator faceIt = regionTopo.getFaces().iterator(); faceIt.hasNext(); ) {
      TopologyFace face = (TopologyFace) faceIt.next();
      if (face.getData() == faceLabel)
        polys.add(face.getPolygon(geomFactory));
    }
    return polys;
  }

  /**
   * Finds the {@link TopologyDirectedEdge}s for the unique edges in the region.
   * These are the edges of the boundary of the region.
   * Depends on the fact that edges will occur at most twice in the face set.
   *
   * @return a Collection<TopologyDirectedEdge>
   */
  private Collection findUniqueEdges(Collection faces)
  {
    Map uniqueEdges = new HashMap();

    for (Iterator faceIt = faces.iterator(); faceIt.hasNext(); ) {
      TopologyFace face = (TopologyFace) faceIt.next();

      for (Iterator deIt = face.iterator(); deIt.hasNext(); ) {
        TopologyDirectedEdge de = (TopologyDirectedEdge) deIt.next();

        TopologyEdge e = (TopologyEdge) de.getEdge();
        if (uniqueEdges.containsKey(e)) {
          uniqueEdges.remove(e);
        }
        else {
          uniqueEdges.put(e, de);
        }
      }
    }
    return uniqueEdges.values();
  }



}