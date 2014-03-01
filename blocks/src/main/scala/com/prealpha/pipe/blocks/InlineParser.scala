package com.prealpha.pipe.blocks

import com.prealpha.pipe.math.{ParseException, MathParser}
import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

object InlineParser extends RegexParsers {
  def apply(input: String): Try[String] = {
    if (input == "")
      scala.util.Success("")
    else
      parse(content, input) match {
        case Success(result, next) => scala.util.Success(result)
        case NoSuccess(msg, next) => scala.util.Failure(new ParseException(msg, input, next))
      }
  }

  def apply(block: Block): Try[Block] = {
    try {
      val parsedArgLine = InlineParser(block.argLine).get
      val parsedChildLines =
        if (block.instance == "_text")
          block.childLines.map(apply).map(_.get)
        else
          block.childLines
      val parsedChildBlocks = block.childBlocks.map(apply).map(_.get)
      val parsedBlock = Block(block.instance, parsedArgLine, block.level, parsedChildLines, parsedChildBlocks)
      scala.util.Success(parsedBlock)
    } catch {
      case px: ParseException[_] => scala.util.Failure(px)
    }
  }

  override def skipWhitespace = false

  def content: Parser[String] = phrase(elem.+) ^^ (_.mkString)

  def elem: Parser[String] = mathElem | inlineElem | normalElem

  def mathElem: Parser[String] = new Parser[String] {
    override def apply(in: Input): ParseResult[String] = ("$" ~> ((not("$") ~> ".".r).+ ^^ (_.mkString.trim)) <~ "$").apply(in) match {
      case Error(msg, next) => Error(msg, next)
      case Failure(msg, next) => Failure(msg, next)
      // TODO: the type annotation is only for IntelliJ
      case Success(result: String, next) => MathParser.apply(result) match {
        case scala.util.Success(latex) => Success("$" + latex + "$", next)
        case scala.util.Failure(exception) => Failure(exception.getMessage, next)
      }
    }
  }

  def inlineElem: Parser[String] = new Parser[String] {
    override def apply(in: Input): ParseResult[String] = ("(|" ~> "[a-zA-Z0-9$]+".r).apply(in) match {
      case Error(msg, next) => Error(msg, next)
      case Failure(msg, next) => Failure(msg, next)
      case Success(result, next) => result match {
        case "latex" =>
          (((not("(|") ~> not("|)") ~> ".".r).+ ^^ (_.mkString)) <~ "|)" ^^ (_.trim)).apply(next)
        case _ =>
          Failure(s"unrecognized inline $result", in.drop(2))
      }
    }
  }

  def normalElem: Parser[String] = (not("(|") ~> not("|)") ~> not("$") ~> ".".r).+ ^^ (_.mkString)
}