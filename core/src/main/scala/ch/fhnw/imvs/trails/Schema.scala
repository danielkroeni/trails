package ch.fhnw.imvs.trails

trait SchemaElement { elem =>
  private def simpleName(c: Class[_]): String = c.getName.split("\\$").last

  class SchemaProperty[T] {
    lazy val name: String = simpleName(getClass)
  }

  lazy val name: String = simpleName(getClass)
  def properties: Seq[SchemaProperty[_]]
}

abstract class SchemaNode extends SchemaElement

abstract class SchemaEdge extends SchemaElement {
  type From <: SchemaNode
  type To <: SchemaNode
  def properties = Seq()
}


abstract class Schema {
  def nodes: Seq[SchemaNode]
  def edges: Seq[SchemaEdge]

  private lazy val nodeMap = nodes.map(n => (n.name, n)).toMap
  private lazy val edgeMap = edges.map(e => (e.name, e)).toMap

  def node(name: String): Option[SchemaNode] = nodeMap.get(name)
  def edge(name: String): Option[SchemaEdge] = edgeMap.get(name)
}

