package ch.fhnw.imvs.trails

import scala.language.implicitConversions

final case class ~[+A,+B](a: A, b: B) { override def toString: String = s"$a ~ $b" }
sealed trait |[+A,+B] // Sum
final case class <|[A](a: A) extends |[A,Nothing]
final case class |>[B](a: B) extends |[Nothing,B]

/** trails provides purely functional graph traverser combinators. */

final class Tr[E,I,O,+A](tr: E => I => Stream[(O,A)]) extends (E => I => Stream[(O,A)]) {
  import Tr._

  def apply(e: E): I => Stream[(O,A)] = tr(e)

  def seq[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,A~B] =
    for(a <- this; b <- t2) yield new ~(a,b)

  def choice[B,O2](t2: => Tr[E,I,O2,B]): Tr[E,I,O|O2,A|B] =
    Tr(e => i =>
      this(e)(i).map{ case (o,a) => ( <|[O](o): O|O2, <|[A](a): A|B )} #:::
        t2(e)(i).map{ case (o,a) => (|>[O2](o): O|O2,|>[B](a): A|B)}
    )

  def opt: Tr[E,I,I|O,Option[A]] =
    Tr(e => i => simplifyResult(success(None).choice(map(Some(_))))(e)(i))

  def flatMap[P,B](f: A => Tr[E,O,P,B]): Tr[E,I,P,B] =
    Tr(e => i => apply(e)(i).flatMap { case (m,a) => f(a)(e)(m) })

  def map[B](f: A => B): Tr[E,I,O,B] =
    flatMap(a => success(f(a)))

  def filter(p: A => Boolean): Tr[E,I,O,A] =
    flatMap[O,A]((a: A) => if(p(a)) success(a) else fail)

  def ~[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,A~B] = seq(t2)
  def |[B,O2](t2: => Tr[E,I,O2,B])(implicit ev: O =:!= O2): Tr[E,I,O|O2,A|B] = choice(t2)
  def |[B](t2: => Tr[E,I,O,B]):  Tr[E,I,O,A|B] = simplifyState[E,I,O,A|B](choice(t2))
  def ~>[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,B] = seq(t2).map{ case a ~ b => b }
  def <~[P,B](t2: Tr[E,O,P,B]): Tr[E,I,P,A] = seq(t2).map{ case a ~ b => a }
  def ? : Tr[E,I,I|O,Option[A]] = opt
  def ^^[B](f: A => B): Tr[E,I,O,B] = map(f)

  def withFilter(p: A => Boolean): TrWithFilter = new TrWithFilter(this, p)
  final class TrWithFilter(tr: Tr[E,I,O,A], p: A => Boolean) {
    def map[B](f: A => B): Tr[E,I,O,B] = tr.filter(p).map(f)
    def flatMap[P,B](f: A => Tr[E,O,P,B]): Tr[E,I,P,B] = tr.filter(p).flatMap(f)
    def withFilter(q: A => Boolean): TrWithFilter = new TrWithFilter(tr, x => p(x) && q(x))
  }
}


object Tr {
  def apply[E,I,O,A](t1: E => I => Stream[(O,A)]): Tr[E,I,O,A] = new Tr(t1)

  def success[E,S,A](a: A): Tr[E,S,S,A] =
    Tr(_ => s => Stream((s,a)))

  def fail[E,S]: Tr[E,S,S,Nothing] =
    Tr(_ => _ => Stream())

  def getEnv[E,S]: Tr[E,S,S,E] =
    Tr(e => s => Stream((s, e)))

  def getState[E,S]: Tr[E,S,S,S] =
    Tr(_ => s => Stream((s, s)))

  def setState[E,I,O](o: O): Tr[E,I,O,Unit] =
    Tr(_ => _ => Stream((o, ())))

  def updateState[E,I,O](f: I => O): Tr[E,I,O,Unit] =
    getState[E,I]flatMap(i => setState(f(i)))

  def sub[E,I,O,A](tr: Tr[E,I,O,A]): Tr[E,I,I,Stream[A]] =
    Tr(e => i => Stream((i, tr(e)(i).map(_._2))))

   implicit def simplifyState[E,I,O,A](tr: Tr[E,I,O|O,A]): Tr[E,I,O,A] =
    Tr(e => i => tr(e)(i).map {
      case (<|(o), a) => (o,a)
      case (|>(o), a) => (o,a)
    })

  def simplifyResult[E,I,O,A](tr: Tr[E,I,O,A|A]): Tr[E,I,O,A] =
    Tr(e => i => tr(e)(i).map {
      case (o, <|(a)) => (o,a)
      case (o, |>(a)) => (o,a)
    })

  // See https://github.com/milessabin/shapeless
  trait =:!=[A, B]
  implicit def neq[A, B] : A =:!= B = null
  implicit def neqAmbig1[A] : A =:!= A = ???
  implicit def neqAmbig2[A] : A =:!= A = ???
}




