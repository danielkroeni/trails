package ch.fhnw.imvs.trails

import scala.language.implicitConversions

/** trails provides purely functional graph traverser combinators. */
trait Trails { self =>

  type Tr[-E,-I,+O,+A] = E => I => Stream[(O,A)]

  /** Provides some nice infix syntax. */
  final implicit class Syntax[E,I,O,A](t1: Tr[E,I,O,A]) {
    def ~[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,A~B] = self.seq(t1, t2)
    def ~>[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,B] = self.map(seq(t1, t2)){ case a ~ b => b }
    def <~[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,A] = self.map(seq(t1, t2)){ case a ~ b => a }
    def |(t2: => Tr[E,I,O,A]): Tr[E,I,O,A] = self.choice(t1, t2)

    def ^^[B](f: A => B): Tr[E,I,O,B] = self.map[E,I,O,A,B](t1)(f)

    def flatMap[P,B](f: A => Tr[E,O,P,B]): Tr[E,I,P,B] = self.flatMap(t1)(f)
    def map[B](f: A => B): Tr[E,I,O,B] = self.map(t1)(f)
    def filter(p: A => Boolean): Tr[E,I,O,A] = self.filter(t1)(p)
    def withFilter(p: A => Boolean): TrWithFilter = new TrWithFilter(t1, p)
    final class TrWithFilter(tr: Tr[E,I,O,A], p: A => Boolean) {
      def map[B](f: A => B): Tr[E,I,O,B] = self.map(self.filter(tr)(p))(f)
      def flatMap[P,B](f: A => Tr[E,O,P,B]): Tr[E,I,P,B] = self.flatMap(self.filter(tr)(p))(f)
      def withFilter(q: A => Boolean): TrWithFilter = new TrWithFilter(tr, x => p(x) && q(x))
    }
  }

  final implicit class SameStateSyntax[E,S,A](t1: Tr[E,S,S,A]) {
    def ? : Tr[E,S,S,Option[A]] = self.opt(t1)
  }

  final case class ~[+A,+B](a: A, b: B) {
    override def toString: String = s"$a ~ $b"
  }

  /* Monadic API. */
  final def flatMap[E,I,M,O,A,B](tr: Tr[E,I,M,A])(f: A => Tr[E,M,O,B]): Tr[E,I,O,B] =
    e => i => tr(e)(i).flatMap { case (m,a) => f(a)(e)(m) }

  /** Returns a traverser which returns its input as the output.
    * @return a traverser which returns its input
    */
  final def success[E,S,A](a: A): Tr[E,S,S,A] =
    _ => s => Stream((s,a))

  /* MonadPlus addons. */

  /** Returns the 'parallel' composition of the two given traversers which follows both alternatives.
    * @param either one of the traverser to follow
    * @param or one of the traverser to follow
    * @return the parallel composition of the two given traversers
    */
  final def choice[E,I,O,A](either: Tr[E,I,O,A], or: => Tr[E,I,O,A]): Tr[E,I,O,A] =
    e => i => either(e)(i) #::: or(e)(i)

  /** A traverser that always fails. */
  final def fail[E,S]: Tr[E,S,S,Nothing] =
    _ => _ => Stream()

  /* Environment access and State read and write */
  final def getEnv[E,S]: Tr[E,S,S,E] =
    e => s => Stream((s, e))

  final def getState[E,S]: Tr[E,S,S,S] =
    _ => s => Stream((s, s))

  final def setState[E,I,O](o: O): Tr[E,I,O,Unit] =
    _ => _ => Stream((o, ()))

  final def updateState[E,I,O](f: I => O): Tr[E,I,O,Unit] =
    flatMap(getState[E,I])(i => setState(f(i)))

  // Every monad is a functor
  final def map[E,I,O,A,B](tr: Tr[E,I,O,A])(f: A => B): Tr[E,I,O,B] =
    flatMap(tr)(a => success(f(a)))

  // filter can be implemented for any monad plus instance
  final def filter[E,I,O,A](tr: Tr[E,I,O,A])(f: A => Boolean): Tr[E,I,O,A] =
    flatMap(tr)(a => if(f(a)) success(a) else fail)

  /** Returns the sequential composition of fst and snd which first follows the fst traverser
    * and then feeds the results into the snd traverser.
    * @param fst the first traverser to apply
    * @param snd the subsequent traverser to apply
    * @return the sequential composition of fst and snd
    */
  final def seq[E,I,M,O,A,B](fst: Tr[E,I,M,A], snd: Tr[E,M,O,B]): Tr[E,I,O,A~B] =
    for(a <- fst; b <- snd) yield new ~(a,b)

  /** Returns a traverser which optionally follows the given traverser.
    * @param tr the traverser to follow optionally
    * @return a traverser which optionally follows the given traverser
    */
  final def opt[E,S,A](tr: Tr[E,S,S,A]): Tr[E,S,S,Option[A]] =
    choice(success[E,S,Option[A]](None), map(tr)(Some(_)))
}