package com.prealpha.pipe.document.generators

import com.prealpha.pipe.document.Block

private[generators] case class CompileContext(generators: List[BlockGenerator],
                                              settings: Map[Class[_], Map[String, String]] = Map())


private[generators] case class ResultContext(imports: Set[String] = Set(),
                                             settings: Set[String] = Set(),insideDoc: Set[String] = Set()) {
  def ++(other: ResultContext): ResultContext = {
    ResultContext(
      this.imports ++ other.imports,
      this.settings ++ other.settings,
      this.insideDoc ++ other.insideDoc)
  }
}

private[generators] object EmptyResultContext extends ResultContext(Set(), Set(), Set())

private[generators] trait BlockGenerator {
  def captures(block: Block)(implicit ctx: CompileContext): Boolean

  def produce(block: Block)(implicit ctx: CompileContext): (String, ResultContext)

  final def compile(block: Block)(implicit ctx: CompileContext): (String, ResultContext) = {
    val matching = ctx.generators.filter(_.captures(block)(ctx))
    if (matching.isEmpty) {
      throw new BlockException(block, s"Trying to find a block Generator that matches ${block.toString} failed.  " +
        s"\nAvailable generators are: ${ctx.generators.map(_.getClass.getSimpleName).mkString("[",", ","]")}")
    }

    if (matching.size > 1) {
      System.err.println(s"Multiple block Generators (${matching.toString()}) match ${block.toString}. " +
        s"Going with ${matching.head.toString}")
    }

    matching.head.produce(block)(ctx)
  }

  final def merge(results: Seq[(String, ResultContext)]): (String, ResultContext) = {
    val strMerge = results.map(_._1).mkString("\n")


    val contexts: Seq[Set[String]] = results.map(_._2.imports)
    val combinedImports: Set[String] = contexts.fold(Set())((a, b) => a ++ b)
    //val resultMerge = ResultContext(combinedImports)

    val resultMerge = results.map(_._2).foldLeft(EmptyResultContext: ResultContext)(_ ++ _)

    (strMerge, resultMerge)
  }
}
