package ch.fhnw.imvs.trails

trait TrailsPrimitives { self: Trails =>
  /** Type of a single element in the graph. Typically a common super type of Vertex and Edge. */
  type Elem
  type Edge <: Elem
  type Node <: Elem
  type Id
  type Env

  final case class State[+Head <: Elem](path: List[Elem])

  object Traverser {
    def run[P <: Elem,A](tr: Tr[Env,State[Nothing],State[P],A], env: Env): Stream[(List[Elem],A)] =
      tr(env)(State[Nothing](Nil)).map { case (s,a) => (s.path.reverse, a) }
  }

  def V: Tr[Env,State[Nothing],State[Node],Node]

  def V(id: Id): Tr[Env,State[Nothing],State[Node],Node]

  def V(p: Node => Boolean): Tr[Env,State[Nothing],State[Node],Node] =
    for { v <- V if p(v) } yield v

  def E: Tr[Env,State[Nothing],State[Edge],Edge]

  def E(id: Id): Tr[Env,State[Nothing],State[Edge],Edge]

  def E(p: Edge => Boolean): Tr[Env,State[Nothing],State[Edge],Edge] =
    for { e <- E if p(e) } yield e

  def outE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge]

  def inE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge]

  def outV: Tr[Env,State[Edge],State[Node],Node]

  def inV: Tr[Env,State[Edge],State[Node],Node]

  def out(edgeName: String): Tr[Env,State[Node],State[Node],Node] =
    outE(edgeName) ~> inV

  def in(edgeName: String): Tr[Env,State[Node],State[Node],Node] =
    inE(edgeName) ~> outV

  final def extendPath[I <: Elem, O <: Elem](p: O): Tr[Env,State[I],State[O],Unit] =
    updateState(s => s.copy(path = p :: s.path))

  final def streamToTraverser[S<: Elem,A](s: Stream[A]): Tr[Env, State[S],State[S],A] = {
    // Requires custom lazyFoldRight because Stream#foldRight is not lazy
    def rec(xs: Stream[Tr[Env, State[S],State[S],A]]): Tr[Env, State[S],State[S],A] =
      if (xs.isEmpty) fail
      else choice(xs.head, rec(xs.tail))

    rec(s.map(success[Env,State[S],A]))
  }

  def get[A](name: String)(e: Elem): A
}
