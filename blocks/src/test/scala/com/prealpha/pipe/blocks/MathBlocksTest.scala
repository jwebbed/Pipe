
package com.prealpha.pipe.blocks

import org.scalatest._
import com.prealpha.pipe.blocks.generators.latex.RootBlock

class MathBlocksTest extends FlatSpec with Matchers {
  def compile(b: Block): String = {
    val rb = RootBlock
    rb.force(b)
  }

  def parse(s: String): Block = {
    BlocksParser.parse(s)
  }

  "a single equation inside of math mode" should "compile" in {
    val input = "|equation\n  (a+b) / c"
    val parsed = parse(input)
    val output = compile(parsed)

    output should be("\\begin{align*}\n\\dfrac{a + b}{c}\n\\end{align*}")
  }

  "two equations with a line break" should "produce the valid line break" in {
    val input = "|equation\n  (a + b) / c\n  (a) / c + (b) / c"
    val parsed = parse(input)
    val output = compile(parsed)

    output should be("\\begin{align*}\n\\dfrac{a + b}{c} \\\\\n\\dfrac{a}{c} + \\dfrac{b}{c}\n\\end{align*}")
  }

  "equation groups with text explanation" should "produce \\text commands and align correctly" in {
    val input = "|equation =\n  (a + b) / c = (a) / c + (b) / c # distribution (sort of)"
    compile(parse(input)) should
      be("\\begin{align*}\n\\dfrac{a + b}{c} &= \\dfrac{a}{c} + \\dfrac{b}{c} && \\text{distribution (sort of)}\n\\end{align*}")
  }

  "an equation block with a newline afterwards" should "not produce an extra newline in the output latex" in {
    val input = "|equation\n  a+b=c\n  \n"
    compile(parse(input)) should be("\\begin{align*}\na + b = c\n\\end{align*}")
  }
  "an equation with multiple forms with a newline afterwards" should "not produce an extra newline in the output latex" in {
    val input = "|equation\n  a+b=c\n  c=a+b\n  \n|equation\n  a+b+c"
    compile(parse(input)) should be("\\begin{align*}\na + b = c \\\\\nc = a + b\n\\end{align*}\n\\begin{align*}\na + b + c\n\\end{align*}")
  }

}
