package com.prealpha.pipe.document.generators.latex

import com.prealpha.pipe.document.Block
import com.prealpha.pipe.document.generators._
import com.prealpha.pipe.math._

object EquationBlock extends BlockGenerator {
  def processAlign(maths: Seq[MathExpr], alignTo: MathExpr): Seq[MathExpr] =
    maths.map(a => processAlignOne(a, alignTo))


  def processAlignOne(math: MathExpr, alignTo: MathExpr): MathExpr = math match {
    case x if x == alignTo => Align(x)
    case Paren(xs) => Paren(processAlign(xs, alignTo))
    case Macro(n, o: Seq[_]) => Macro(n, o.map(a=>processAlign(a, alignTo)))
    case SuperMacro(n, o: Seq[_]) => SuperMacro(n, o.map(_.map(a => processAlign(a, alignTo))))
    case a => a
  }

  override def captures(block: Block)(implicit ctx: CompileContext): Boolean =
    block.instance == "equation"

  override def produce(block: Block)(implicit ctx: CompileContext): (String, ResultContext) = {
    val args = block.argLine.split("\\s+").toList.filter(!_.isEmpty)
    val sb = new StringBuilder

    val numbered = args.contains("numbered")

    sb ++= (if (numbered) "\\begin{align}" else "\\begin{align*}") ++= "\n"

    val alignOn: MathExpr = if (args.isEmpty) Never else MathParser.tryParse(args(0)).getOrElse(Seq(Never))(0)

    val alignedLines =
      for {(rawLine, mathLineNum) <- block.childLines.zipWithIndex if rawLine.exists(!_.isWhitespace)
          compLine = MathParser.tryParse(rawLine).get } yield {
        (processAlign(compLine, alignOn), mathLineNum)
      }
    val compiledLines =
      for ((line, mathLineNum)  <- alignedLines)
      yield {
        try {
          CodeGen.genEntire(line)
        } catch {
          case ex: ParseException[_] => throw ex.copy(msg = ex.msg + s"on line ${block.lineNum + mathLineNum}")
        }
      }

    sb.append(compiledLines.mkString(" \\\\\n"))

    sb ++= "\n"
    sb ++= (if (numbered) "\\end{align}" else "\\end{align*}")

    (sb.toString(), ResultContext(Set("amsmath")))
  }
}

object MathBlock extends BlockGenerator {
  override def captures(block: Block)(implicit ctx: CompileContext): Boolean =
    block.instance == "math"

  override def produce(block: Block)(implicit ctx: CompileContext): (String, ResultContext) = {
    def parseInline(s: String): (String, ResultContext) = {
      val isb = new StringBuilder
      isb.append("$")
        .append(CodeGen.genEntire(MathParser.tryParse(s).get))
        .append("$")
      (isb.toString(), ResultContext(Set("amsmath")))
    }

    val argline = if (block.argLine.trim.length != 0)
      List(parseInline(block.argLine))
    else Nil

    val children = block.childBlocks.map({
      case b@Block("_text", _, _, _, _, _) => {
        merge(b.childLines.map(parseInline))
      }
      case b@Block("equation", _, _, _, _, _) => compile(b)
      case x => throw new BlockException(x, "The |equation is allowd inside of |math")
    })

    merge(argline ++ children)
  }
}