package com.prealpha.pipe.math

object MathCompiler {
  final case class Failure(msg: String, cause: Option[Any], offset: (Int, Int))

  private[math] type Result[+T] = Either[Failure, T]

  /** Attempts to compile `source` from math markup into TeX, producing either a `String` of generated TeX or a `Seq`
    * of one or more compilation [[com.prealpha.pipe.math.MathCompiler.Failure Failure]]s.
    *
    * `source` may consist of multiple lines; in this case, before any further compilation occurs, the markup will be
    * translated into a sequence of logical lines, each of which corresponds to one or more physical lines. If a
    * physical line ends in a backslash character, that line will be joined to the next one to produce a single logical
    * line. Logical lines are separated in the output by TeX line breaks (double backslashes).
    *
    * `alignToken` is supported for the benefit of the document compiler's `|equation` blocks. If some token is
    * provided, the compiler will first attempt to compile that token by itself, which must produce a single "atomic"
    * value; it must be a single expression, not several. Then, after `source` is compiled, the compiler will run an
    * additional stage to verify that each logical line contains exactly one instance of the alignment expression (at
    * the top level) and to insert a TeX aligner (`&`) where the expressions occur. This behavior allows clients to use
    * the output within a TeX equation environment or similar environment requiring aligners.
    *
    * The compiler will attempt to identify and report as many compilation errors as it can at once. Generally, an
    * error occurring during the preprocessor or parser stages will terminate processing of that logical line, while
    * an error during an AST stage will terminate processing of the offending AST node and its ancestors. However,
    * these behaviors are not guaranteed. In `Failure`s, line numbers are reported using the first physical line of
    * `source` as line 1.
    *
    * @param source the math markup to compile
    * @param alignToken optionally, a token which must be present once on every line
    * @return either a compiled Tex string corresponding to `source`, or a `Seq` of one or more compilation `Failure`s
    */
  def compile(source: String, alignToken: Option[String] = None): Either[Seq[Failure], String] = {
    val alignResult = alignToken map parseToken match {
      case Some(Left(failure)) => Left(Seq(failure))
      case Some(Right(alignExpr)) => Right(Some(alignExpr))
      case None => Right(None)
    }
    alignResult.right flatMap { alignExpr =>
      val preprocessed = MathPreprocessor.preprocess(source)
      val results = preprocessed map (_.left map (Seq(_))) map (_.right flatMap { ll =>
        val parsed = MathParser.parseLine(ll)
        val aligned = parsed.right flatMap AlignmentValidator.validateAlignment(alignExpr)
        val generated = aligned.right map (_ map MathCodeGenerator.generate)
        (generated.left map (Seq(_))).right flatMap { exprResults =>
          if (exprResults forall (_.isRight)) {
            Right(exprResults map (_.right.get) mkString " ")
          } else {
            Left(exprResults filter (_.isLeft) map (_.left.get) map { failure =>
              val line = failure.offset._1 + ll.offset(0)._1 - 1
              Failure(failure.msg, failure.cause, (line, failure.offset._2))
            })
          }
        }
      })
      if (results forall (_.isRight))
        Right(results map (_.right.get) mkString " \\\\\n")
      else
        Left((results filter (_.isLeft) map (_.left.get)).flatten)
    }
  }

  private def parseToken(token: String): Result[MathExpr] = {
    val preprocessed = MathPreprocessor.preprocess(token)
    if (preprocessed.length == 1) {
      preprocessed.head.right flatMap { line =>
        MathParser.parseLine(line).right flatMap { result =>
          if (result.length == 1)
            Right(result.head)
          else
            Left(Failure("alignToken is not a single token", None, line.offset(0)))
        }
      }
    } else {
      preprocessed.head.right flatMap { line =>
        Left(Failure("alignToken is not a single logical line", None, line.offset(line.toString.length - 1)))
      }
    }
  }
}
