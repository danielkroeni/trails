package ch.fhnw.imvs.trails.blueprint

import ch.fhnw.imvs.trails.{SchemaEdge, SchemaNode}
import com.tinkerpop.blueprints.{Edge, Graph, Vertex}


object BlueprintTestUtil {

  def createNode[N <: SchemaNode](db: Graph, sn: N, idKey: N#SchemaProperty[_], idValue: Any, props: (N#SchemaProperty[_],Any)*): Vertex = {
    val node = db.addVertex(idValue)
    ((BlueprintTrails.BlueprintTypeTag, sn.name) +: (idKey.name, idValue) +: props.map{case (p,v) => (p.name, v)}).foreach { case (k,v) =>
      node.setProperty(k,v)
    }
    node
  }

  def createEdge(se: SchemaEdge, from: Vertex, to: Vertex): Edge =
    from.addEdge(se.name, to)
}
