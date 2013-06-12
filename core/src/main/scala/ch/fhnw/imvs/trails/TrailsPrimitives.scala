package ch.fhnw.imvs.trails

trait TrailsPrimitives { self: Trails =>
  /** Type of a single element in the graph. Typically a common super type of Vertex and Edge. */
  type Elem
  type Edge <: Elem
  type Node <: Elem
  type Id
  type Env

  final case class State[+Head <: Elem](path: List[Elem], cycles: Set[List[Elem]], labels: Map[String,List[Any]])

  final implicit class RepetitionSyntax[S<: Elem,A](t1: Tr[Env, State[S],State[S],A]) {
       def * : Tr[Env, State[S],State[S],Stream[A]] = self.many(t1)
       def + : Tr[Env, State[S],State[S],Stream[A]] = self.many1(t1)
  }

  final implicit class AsSyntax[I<: Elem,O<: Elem,A](t1: Tr[Env,State[I],State[O],A]) {
    def as(name: String): Tr[Env,State[I],State[O],A] = self.label[I,O,A](t1,name)
  }

  object Traverser {
    def run[P <: Elem,A](tr: Tr[Env,State[Elem],State[P],A], env: Env): Stream[(List[Elem],A)] =
      tr(env)(State[Elem](Nil,Set(), Map().withDefaultValue(Nil))).map { case (s,a) => (s.path.reverse, a) }
  }

  def V: Tr[Env,State[Elem],State[Node],Node]

  def V(id: Id): Tr[Env,State[Elem],State[Node],Node]

  def V(p: Node => Boolean): Tr[Env,State[Elem],State[Node],Node] =
    for { v <- V if p(v) } yield v

  def E: Tr[Env,State[Elem],State[Edge],Edge]

  def E(id: Id): Tr[Env,State[Elem],State[Edge],Edge]

  def E(p: Edge => Boolean): Tr[Env,State[Elem],State[Edge],Edge] =
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

  def many[E<: Elem,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] =
    newCycleScope(internal_many(tr))

  private final def internal_many[E<: Elem,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]]=
    choice(success(Stream()),internal_many1(tr))

  def many1[E<: Elem,A](tr:Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]]=
    newCycleScope(internal_many1(tr))

  private def internal_many1[E<: Elem,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] =
    for { (sl,a)  <- slice(tr)
          State(p,c,l) <- getState[Env, State[E]] if !c.contains(sl)
          _       <- setState[Env,State[E],State[E]](State(p, c+sl,l))
          as      <- internal_many(tr)
    } yield a #:: as

  def slice[I<: Elem,O<: Elem,A](tr: Tr[Env,State[I],State[O],A]): Tr[Env,State[I],State[O],(List[Elem],A)] =
    for {
      s0 <- getState[Env, State[I]]
      a  <- tr
      s1 <- getState[Env, State[O]]
    } yield (s1.path.take(s1.path.size - s0.path.size), a)

  private def newCycleScope[E <: Elem,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],A] =
    for {
      s0  <- getState[Env,State[E]]
      _   <- setState[Env,State[E],State[E]](State(s0.path, Set(),s0.labels))
      res <- tr
      _   <- updateState[Env,State[E],State[E]](s1 => s1.copy(cycles = s0.cycles))
    } yield res


  def label[I<: Elem,O<: Elem,A](tr: Tr[Env,State[I],State[O],A], name: String): Tr[Env,State[I],State[O],A] = {
    for {
      a <- tr
      _ <- updateState((s: State[O]) => s.copy(labels = s.labels.updated(name, a :: s.labels(name)) ))
    } yield a
  }

  def label[S<: Elem,A](name: String): Tr[Env, State[S],State[S],List[Any]] =
   getState.map(_.labels(name))

//  def sub[I<:Elem,O<:Elem,A](tr: Tr[Env,State[I],State[O],A]): Tr[Env,State[I],State[I],A] =
//    for {
//      s <- getState[Env, State[I]]
//      a <- tr
//      _ <- setState[Env,State[O],State[I]](s)
//    } yield a

  def sub[I<:Elem,O<:Elem,A](tr: Tr[Env,State[I],State[O],A]): Tr[Env,State[I],State[I],Stream[A]] =
    e => s => Stream((s, tr(e)(s).map(_._2)))
}
