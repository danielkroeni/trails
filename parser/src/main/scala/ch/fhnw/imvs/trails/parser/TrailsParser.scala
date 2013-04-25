package ch.fhnw.imvs.trails.parser

import ch.fhnw.imvs.trails.Trails

object TrailsParser extends Trails {

  type Parser[A] = Tr[Unit,List[Char],List[Char],A]

  def item: Parser[Char] =
    env => s => s match {
      case c :: cs => Stream((cs,c))
      case Nil => Stream()
    }

  def sat(p: Char => Boolean): Parser[Char] = filter(item)(p)
  def char(c: Char): Parser[Char] = sat(_ == c)
  def digit: Parser[Char] = sat(_.isDigit)
  def letter: Parser[Char] = sat(_.isLetter)
  def alphanum: Parser[Char] = digit | letter
}
