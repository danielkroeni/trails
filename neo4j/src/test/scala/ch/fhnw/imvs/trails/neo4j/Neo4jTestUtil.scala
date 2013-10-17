package ch.fhnw.imvs.trails.neo4j

import ch.fhnw.imvs.trails.{SchemaEdge, SchemaNode}
import org.neo4j.graphdb.{DynamicRelationshipType, Relationship, Node, GraphDatabaseService}

object Neo4jTestUtil {
  def createNode[N <: SchemaNode](db: GraphDatabaseService, sn: N, idKey: N#SchemaProperty[_], idValue: Any, props: (N#SchemaProperty[_],Any)*): Node = {
    val node = db.createNode()
    ((Neo4jTrails.Neo4jTypeTag, sn.name) +: (idKey.name, idValue) +: props.map{case (p,v) => (p.name, v)}).foreach { case (k,v) =>
      node.setProperty(k,v)
    }
    node
  }

  def createEdge(se: SchemaEdge, from: Node, to: Node): Relationship =
    from.createRelationshipTo(to, DynamicRelationshipType.withName(se.name))
}
