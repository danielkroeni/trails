package ch.fhnw.imvs.trails

import org.scalatest.FunSuite


class TrailsTest extends FunSuite {
  object T extends Trails
  import T._

  test("seq") {
    val t0 = seq(success[Null,Null,String]("fst"), success[Null,Null,String]("snd"))
    val res0 = t0(null)(null)

    assert(res0.size === 1)
    val (_, fst ~ snd) = res0.head
    assert(fst === "fst")
    assert(snd === "snd")
  }

  test("seq with fail -> fail") {
    val t0 = seq(success[Null,Null,String]("fst"), T.fail[Null,Null])
    val res0 = t0(null)(null)
    assert(res0.size === 0)

    val t1 = seq(T.fail[Null,Null], success[Null,Null,String]("snd"))
    val res1 = t1(null)(null)
    assert(res1.size === 0)
  }

  test("choice") {
    val t0: Tr[Null,Null,Null,String] = choice(success("left"), success("right"))
    val res0 = t0(null)(null)
    assert(res0.size === 2)
    val values = res0.map(_._2)
    assert(values contains "left")
    assert(values contains "right")
  }

  test("choice has fail as neutral element") {
    val t0: Tr[Null,Null,Null,String] = choice(success("left"), T.fail)
    val res0 = t0(null)(null)
    assert(res0.size === 1)
    val (_, v0) = res0.head
    assert(v0 === "left")

    val t1: Tr[Null,Null,Null,String] = choice(T.fail, success("right"))
    val res1 = t1(null)(null)
    assert(res1.size === 1)
    val (_, v1) = res1.head
    assert(v1 === "right")
  }

  test("choice is lazy") {
    def manyS: Tr[Null,Null,Null,String] = choice(success("S"), manyS)
    val res0 = manyS(null)(null)

    val ten = res0.take(10)
    assert(ten.size === 10)
    ten.foreach{
      case (s0,v0) => assert(v0 === "S")
    }
  }

  test("mmany (choice is lazy II)") {
    def mmany[E,S,A](tr: Tr[E, S, S, A]): Tr[E, S, S, Stream[A]] = T.success[E,S,Stream[A]](Stream()) | tr ~ mmany(tr) ^^ { case a ~ as => a #:: as}

    val t0 = mmany(success[Null,Null,String]("S"))
    val ten = t0(null)(null).take(10)

    assert(ten.size === 10)

    ten.zipWithIndex.foreach {
      case ((s0,v0),i) =>
        assert(v0.size === i)
        assert(v0.forall(_ == "S"))
    }
  }

  test("seq should not allow meaningless recursion") {
    intercept[StackOverflowError] {
      lazy val manyS: Tr[Null,Null,Null,Nothing] = seq(manyS, T.fail[Null,Null]).map{case a ~ b => b}
      manyS
    }
  }
}
