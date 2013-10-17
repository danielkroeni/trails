package ch.fhnw.imvs.trails



trait TrailsPrimitives {
  import Tr._

  /** Type of a single element in the graph. Typically a common super type of Vertex and Edge. */
  type Elem
  type Edge <: Elem
  type Node <: Elem
  type Id
  type Env

  final case class State[+Head <: SchemaElement](path: List[Elem], cycles: Set[List[Elem]], labels: Map[String,List[Any]])

  object Traverser {
    def run[P <: SchemaElement,A](tr: Tr[Env,State[Nothing],State[P],A], env: Env): Stream[(List[Elem],A)] =
      tr(env)(State[Nothing](Nil,Set(),Map().withDefaultValue(Nil))).map { case (s,a) => (s.path.reverse, a) }
  }

  final implicit class RepetitionSyntax[S<: SchemaElement,A](t1: Tr[Env, State[S], State[S],A]) {
    def * : Tr[Env, State[S],State[S],Stream[A]] = many(t1)
    def + : Tr[Env, State[S],State[S],Stream[A]] = many1(t1)
  }

  final implicit class AsSyntax[I <: SchemaElement,O<: SchemaElement,A](t1: Tr[Env,State[I],State[O],A]) {
    def as(name: String): Tr[Env,State[I],State[O],A] = label[I,O,A](t1,name)
  }

  def V[M <: SchemaElement, N <: SchemaNode](sn: N): Tr[Env,State[M],State[N],Node]

  def outE[E <: SchemaEdge](se: E): Tr[Env,State[E#From],State[E],Edge]

  def inE[E <: SchemaEdge](se: E): Tr[Env,State[E#To],State[E],Edge]

  def outV[E <: SchemaEdge]: Tr[Env,State[E],State[E#From],Node]

  def inV[E <: SchemaEdge]: Tr[Env,State[E],State[E#To],Node]

  def out[E <: SchemaEdge](se: E): Tr[Env,State[E#From],State[E#To],Node] =
    outE(se) ~> inV

  def in[E <: SchemaEdge](se: E): Tr[Env,State[E#To],State[E#From],Node] =
    inE(se) ~> outV

  final def extendPath[I <: SchemaElement, O <: SchemaElement](p: Elem): Tr[Env,State[I],State[O],Unit] =
    updateState(s => s.copy(path = p :: s.path))

  final def streamToTraverser[S<: SchemaElement,A](s: Stream[A]): Tr[Env, State[S],State[S],A] =
    Tr(env => in => s.map((in,_)))

  def get[T, E <: SchemaElement](p: E#SchemaProperty[T]): Tr[Env,State[E],State[E],T]

  def many[E<: SchemaElement,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] =
    newCycleScope(internal_many(tr))

  private final def internal_many[E<: SchemaElement,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]]=
    Tr(env => in => (success(Stream[A]()).choice(internal_many1(tr)))(env)(in).map {
      case (<|(o),<|(a)) => (o,a)
      case (|>(o),|>(a)) => (o,a)
    })

  def many1[E<: SchemaElement,A](tr:Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]]=
    newCycleScope(internal_many1(tr))

  private def internal_many1[E<: SchemaElement,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] =
    for { (sl,a)  <- slice(tr)
          State(p,c,l) <- getState[Env, State[E]] if !c.contains(sl)
          _       <- setState[Env,State[E],State[E]](State(p, c+sl,l))
          as      <- internal_many(tr)
    } yield a #:: as

  def slice[I<: SchemaElement,O<: SchemaElement,A](tr: Tr[Env,State[I],State[O],A]): Tr[Env,State[I],State[O],(List[Elem],A)] =
    for {
      s0 <- getState[Env, State[I]]
      a  <- tr
      s1 <- getState[Env, State[O]]
    } yield (s1.path.take(s1.path.size - s0.path.size), a)

  private def newCycleScope[E <: SchemaElement,A](tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],A] =
    for {
      s0  <- getState[Env,State[E]]
      _   <- setState[Env,State[E],State[E]](State(s0.path, Set(),s0.labels))
      res <- tr
      _   <- updateState[Env,State[E],State[E]](s1 => s1.copy(cycles = s0.cycles))
    } yield res


  def label[I<: SchemaElement,O<: SchemaElement,A](tr: Tr[Env,State[I],State[O],A], name: String): Tr[Env,State[I],State[O],A] = {
    for {
      a <- tr
      _ <- updateState[Env,State[O],State[O]]((s: State[O]) => s.copy(labels = s.labels.updated(name, a :: s.labels(name)) ))
    } yield a
  }

  def label[S<: SchemaElement,A](name: String): Tr[Env, State[S],State[S],List[Any]] =
    getState.map(_.labels(name))
}
