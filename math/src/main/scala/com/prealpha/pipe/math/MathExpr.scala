package com.prealpha.pipe.math

import scala.util.parsing.input.Positional

private[math] sealed trait MathExpr extends Positional

private[math] abstract class Wrapper(val c: Seq[MathExpr], val left: String, val right: String) extends MathExpr
private[math] object Wrapper {
  def unapply(wrapper: Wrapper): Option[(Seq[MathExpr], String, String)] = {
    Some((wrapper.c, wrapper.left, wrapper.right))
  }
}

private[math] case class Chunk(contents: String) extends MathExpr
private[math] case class Symbol(name: String) extends MathExpr
private[math] case class Paren(override val c: Seq[MathExpr]) extends Wrapper(c, "(", ")")
private[math] case class Brace(override val c: Seq[MathExpr]) extends Wrapper(c, "\\{", "\\}")
private[math] case class Bracket(override val c: Seq[MathExpr]) extends Wrapper(c, "[", "]")
private[math] case class Macro(name: String, c: Seq[Seq[MathExpr]]) extends MathExpr
private[math] case class SuperMacro(name: String, c: Seq[Seq[Seq[MathExpr]]]) extends MathExpr
private[math] case class SuperScript(normal: MathExpr, over: MathExpr) extends MathExpr
private[math] case class SubScript(normal: MathExpr, under: MathExpr) extends MathExpr
private[math] case class OverDiv(numer: MathExpr, denom: MathExpr) extends MathExpr
private[math] case class SideDiv(numer: MathExpr, denom: MathExpr) extends MathExpr
private[math] case class Align(nextTo: MathExpr) extends MathExpr
private[math] case class Comment(text: String) extends MathExpr
