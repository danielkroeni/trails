package ch.fhnw.imvs.trails

object TestSchema extends Schema {
  object Node extends SchemaNode {
    object Name extends SchemaProperty[String]
    object Nr extends SchemaProperty[Int]

    def properties = Seq(Name,Nr)
  }

  object E extends SchemaEdge {
    type From = Node.type; type To = Node.type

  }

  object F extends SchemaEdge {
    type From = Node.type; type To = Node.type
  }

  object G extends SchemaEdge {
    type From = Node.type; type To = Node.type
  }

  def nodes = Seq(Node)
  def edges = Seq(E,F,G)
}
