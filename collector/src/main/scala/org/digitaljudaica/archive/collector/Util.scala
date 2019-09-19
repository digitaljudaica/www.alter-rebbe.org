package org.digitaljudaica.archive.collector

import java.io.{BufferedWriter, File, FileWriter}

import scala.io.Source

object Util {

  def filesWithExtensions(directory: File, extension: String): Seq[String] = {
    (if (!directory.exists) Seq.empty else directory.listFiles.toSeq)
      .map(_.getName)
      .filter(_.endsWith(extension)).map(_.dropRight(extension.length))
  }

  def writeTeiYaml(
    directory: File,
    fileName: String,
    layout: String,
    tei: String,
    title: String,
    target: String
  ): Unit = writeYaml(directory, fileName, layout, Seq(
    "tei" -> tei,
    "title" -> title,
    "target" -> target
  ))

  def writeYaml(
    directory: File,
    fileName: String,
    layout: String,
    yaml: Seq[(String, String)]
  ): Unit = writeYaml(directory, fileName, ("layout", layout) +: yaml)

  private def writeYaml(
    directory: File,
    fileName: String,
    yaml: Seq[(String, String)]
  ): Unit = {
    val result: Seq[String] =
      Seq(
        "---"
      ) ++
        (for ((name, value) <- yaml) yield name + ": " + value) ++
      Seq("---", "")

    write(directory, fileName + ".html", result.mkString("\n"))
  }

  def write(directory: File, fileName: String, content: String): Unit =
    write(new File(directory, fileName), content)

  def read(file: File): Seq[String] = {
    val source = Source.fromFile(file)
    val result = source.getLines.toSeq
    source.close
    result
  }

  def write(file: File, content: String): Unit = {
    file.getParentFile.mkdirs()
    val writer: BufferedWriter = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }

  def splice(file: File, start: String, end: String, what: Seq[String]): Unit = {
    println(s"Splicing ${file.getName}.")
    write(file, splice(read(file), start, end, what).mkString("\n"))
  }

  private def splice(lines: Seq[String], start: String, end: String, what: Seq[String]): Seq[String] = {
    val (prefix, tail) = lines.span(_ != start)
    if (tail.isEmpty) lines else {
      val (_, suffix) = tail.tail.span(_ != end)
      if (suffix.isEmpty) lines else {
        prefix ++ Seq(start) ++ what ++ suffix
      }
    }
  }

  def deleteFiles(directory: File): Unit = {
    directory.mkdirs()
    for (file <- directory.listFiles()) file.delete()
  }

  def removeConsecutiveDuplicates[T](seq: Seq[T]): Seq[T] = seq match {
    case Nil => Nil
    case x :: y :: xs if x == y => removeConsecutiveDuplicates(y :: xs)
    case x :: xs => x +: removeConsecutiveDuplicates(xs)
  }
}
