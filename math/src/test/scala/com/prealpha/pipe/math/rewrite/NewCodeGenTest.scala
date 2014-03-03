package com.prealpha.pipe.math.rewrite

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import com.prealpha.pipe.math.ParseException

class NewCodeGenTest extends FlatSpec with ShouldMatchers{
  def compile(expr: MathExpr*): String = CodeGen.genEntire(expr)

  "symbols" should "be translated literally to their LaTeX forms" in {
    // :foo
    compile(Symbol("foo")) should be ("\\foo")
  }

  it should "not interfere with nearby forms" in {
    // :foo :bar
    compile(Symbol("foo"), Symbol("bar")) should be("\\foo \\bar")
    // :foo bar
    compile(Symbol("foo"), Chunk("bar")) should be("\\foo bar")
    // :foo + bar
    compile(Symbol("foo"), Chunk("+"), Symbol("bar")) should be("\\foo + \\bar")
  }

  "parenthesis" should "open and close correctly" in {
    // (akbar)
    compile(Paren(Seq(Chunk("akbar")))) should be ("\\left( akbar \\right)")
  }

  it should "not interfere with nearby forms" in {
    // foo (aba jss) baz
    compile(Symbol("foo"), Paren(Seq(Chunk("aba"), Chunk("jss"))), Chunk("baz")) should be {
      "\\foo \\left( aba jss \\right) baz"
    }
  }

  it should "nest properly" in {
    // ((:Delta) :delta)
    compile(Paren(Seq(Paren(Seq(Symbol("Delta"))), Symbol("delta")))) should be {
      "\\left( \\left( \\Delta \\right) \\delta \\right)"
    }
    // ((:Delta) (:delta))
    compile(Paren(Seq(Paren(Seq(Symbol("Delta"))), Paren(Seq(Symbol("delta")))))) should be {
      "\\left( \\left( \\Delta \\right) \\left( \\delta \\right) \\right)"
    }
  }

  "Over division" should "translate cleanly to latex" in {
    // a / b
    compile(OverDiv(Symbol("a"), Chunk("b"))) should be ("\\dfrac{\\a}{b}")
  }

  it should  "remove unnecissary parenthesis" in {
    // (:alpha :beta) / (foo bar)
    compile(OverDiv(Paren(Seq(Symbol("alpha"), Symbol("beta"))), Paren(Seq(Chunk("foo"), Chunk("bar"))))) should be {
      "\\dfrac{\\alpha \\beta}{foo bar}"
    }

    // (:alpha :beta) / foo
    compile(OverDiv(Paren(Seq(Symbol("alpha"), Symbol("beta"))), Chunk("foo"))) should be {
      "\\dfrac{\\alpha \\beta}{foo}"
    }

    // :alpha / (foo bar)
    compile(OverDiv(Symbol("alpha"), Paren(Seq(Chunk("foo"), Chunk("bar"))))) should be {
      "\\dfrac{\\alpha}{foo bar}"
    }
  }

  it should "not remove extra parenthesis" in {
    // ((:alpha :beta)) / (foo bar)
    compile(OverDiv(Paren(Seq(Paren(Seq(Symbol("alpha"), Symbol("beta"))))), Paren(Seq(Chunk("foo"), Chunk("bar"))))) should be {
      "\\dfrac{\\left( \\alpha \\beta \\right)}{foo bar}"
    }
  }

  it should "nest properly" in {
    // :alpha / :beta / :gamma
    compile(OverDiv(OverDiv(Symbol("alpha"), Symbol("beta")), Symbol("gamma"))) should be {
      "\\dfrac{\\dfrac{\\alpha}{\\beta}}{\\gamma}"
    }
    // (:alpha / :beta) / :gamma
    compile(OverDiv(Paren(Seq(OverDiv(Symbol("alpha"), Symbol("beta")))), Symbol("gamma"))) should be {
      "\\dfrac{\\dfrac{\\alpha}{\\beta}}{\\gamma}"
    }
  }

  "superscript" should "work as expected in simple cases" in {
    // :alpha^2
    compile(SuperScript(Symbol("alpha"), Chunk("2"))) should be ("\\alpha^{2}")
    // alpha^2
    compile(SuperScript(Chunk("alpha"), Chunk("2"))) should be ("alpha^{2}")
    // 5^x
    compile(SuperScript(Chunk("5"), Chunk("x"))) should be ("5^{x}")
  }

  it should "capture parenthated expressions" in {
    // x^(a + b)
    compile(SuperScript(Chunk("x"), Paren(List(Chunk("a"), Chunk("+"), Chunk("b"))))) should be {
      "x^{a + b}"
    }
  }

  "subscript" should "do the same as superscript" in {
    // :alpha_2
    compile(SubScript(Symbol("alpha"), Chunk("2"))) should be ("\\alpha_{2}")
    // alpha_2
    compile(SubScript(Chunk("alpha"), Chunk("2"))) should be ("alpha_{2}")
    // 5_x
    compile(SubScript(Chunk("5"), Chunk("x"))) should be ("5_{x}")
    // x_(a + b)
    compile(SubScript(Chunk("x"), Paren(List(Chunk("a"), Chunk("+"), Chunk("b"))))) should be {
      "x_{a + b}"
    }
  }

  "basic macros" should "compile correctly" in {
    compile(Macro("sqrt", Seq(Seq(Chunk("x"))))) should be ("\\sqrt{x}")
    compile(Macro("sqrt", Seq(Seq(Chunk("x"), Chunk("+"), Symbol("alpha"))))) should be {
      "\\sqrt{x + \\alpha}"
    }
    intercept[ParseException[_]] {
      compile(Macro("sqrt", Seq()))
    }

    compile(Macro("sum", Seq())) should be ("\\sum")
    compile(Macro("sum", Seq(Seq(Chunk("a"))))) should be ("\\sum_{a}")
    compile(Macro("sum", Seq(Seq(Chunk("i = 0"))))) should be ("\\sum_{i = 0}")
    compile(Macro("sum", Seq(Seq(Chunk("i"), Chunk("="), Chunk("0"))))) should be ("\\sum_{i = 0}")
    compile(Macro("sum", Seq(Seq(Chunk("i = 0"))))) should be ("\\sum_{i = 0}")
    intercept[ParseException[_]] {
      compile(Macro("sum",Seq(Seq(), Seq(), Seq())))
    }

    compile(Macro("limit", Seq())) should be ("\\lim")
    compile(Macro("limit", Seq(Seq(Chunk("i to 0"))))) should be ("\\lim_{i to 0}")
    compile(Macro("limit", Seq(Seq(Chunk("i")), Seq(Symbol("inf"))))) should be ("\\lim_{i \\to \\inf}")
    intercept[ParseException[_]] {
      compile(Macro("limit", Seq(Seq(), Seq(), Seq())))
    }
  }

  "the cases supermacro" should "compile correctly" in {
    compile(SuperMacro("cases", Seq(
      Seq(Seq(Chunk("x"), Chunk("<"), Chunk("5")), Seq(Chunk("foo"))),
      Seq(Seq(Chunk("x"), Chunk(">="), Chunk("5")), Seq(Chunk("bar")))
    ))) should be {
      "\\begin{cases}\n" +
      "x < 5 & foo \\\n" +
      "x >= 5 & bar\n" +
      "\\end{cases}"
    }
    // TODO(TyOverby): More tests
  }

  "the matrix supermacro" should "compile correctly" in {
    compile(SuperMacro("matrix", Seq(
      Seq(Seq(Chunk("a")), Seq(Chunk("b")), Seq(Chunk("c"))),
      Seq(Seq(Chunk("d")), Seq(Chunk("e")), Seq(Chunk("f"))),
      Seq(Seq(Chunk("g")), Seq(Chunk("h")), Seq(Chunk("i")))
    ))) should be {
      "\\left( \\begin{array}{ccc}\n" +
      "a & b & c \\\\\n" +
      "d & e & f \\\\\n" +
      "g & h & i\n" +
      "\\end{array} \\right)"
    }

    // TODO(TyOverby): More Tests
  }
}
