package ch.fhnw.imvs.trails

import scala.reflect.ClassTag

trait SchemaElement { elem =>
  private def simpleName(c: Class[_]): String = c.getName.split("\\$").last

  case class SchemaProperty[T: ClassTag](desc: String = "") extends Identity {
    val parent: SchemaElement = elem
    private lazy val className = simpleName(getClass)
    def name: String = className
  }

  lazy val name: String = simpleName(getClass)
  def dbName: String = name

  def properties: Seq[SchemaProperty[_]]
}

abstract case class SchemaNode(val desc: String = "") extends SchemaElement {
  override def toString(): String = s"(Node $name)"

  def idProperties: Seq[Identity]
}

abstract case class SchemaEdge(val isId: Boolean = false, val desc: String = "") extends SchemaElement with Identity {
  type From <: SchemaNode
  type To <: SchemaNode
  def from: From
  def to: To

  override def toString(): String =  s"(Edge $name[${from.name}->${to.name}])"
}

trait Identity

abstract case class Schema(desc: String = "") {
  def nodes: Seq[SchemaNode]
  def edges: Seq[SchemaEdge]

  private lazy val nodeMap = nodes.map(n => (n.name, n)).toMap
  private lazy val edgeMap = edges.map(e => (e.name, e)).toMap

  def node(name: String): Option[SchemaNode] = nodeMap.get(name)
  def edge(name: String): Option[SchemaEdge] = edgeMap.get(name)
}

