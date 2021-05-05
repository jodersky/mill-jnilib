package mill.jnilib

import mill.PathRef
import mill.T

/** A library written in C or C++ */
trait CLibModule extends mill.Module {

  /**
    * The folders where the source files for this module live
    */
  def cSources = T.sources{ millSourcePath / "src" }

  /**
    * Folders containing source files that are generated rather than
    * hand-written; these files can be generated in this target itself,
    * or can refer to files generated from other targets
    */
  def generatedCSources = T{ Seq.empty[PathRef] }

  /**
    * The folders containing all source files fed into the compiler
    */
  def allCSources = T{ cSources() ++ generatedCSources() }

  def allHeaderFiles: T[Seq[PathRef]] = T {
    for {
      root <- allCSources()
      if os.exists(root.path)
      path <- if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path)
      if os.isFile(path) && (
        path.ext == "h" || path.ext == "hpp"
      )
    } yield PathRef(path)
  }

  implicit val subPathRw: upickle.default.ReadWriter[os.SubPath] =
    implicitly[upickle.default.ReadWriter[String]].bimap(
      path => path.toString,
      str => os.SubPath(str)
    )

  def allCCppFiles: T[Seq[(PathRef, os.SubPath)]] = T {
    for {
      root <- allCSources()
      if os.exists(root.path)
      path <- if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path)
      if os.isFile(path) && (
        path.ext == "c" || path.ext == "cpp" || path.ext == "cc"
      )
    } yield PathRef(path) -> (path.subRelativeTo(root.path) / os.up)
  }

  def compiler: T[String] = T{ "gcc" }

  def compileFlags: T[Seq[String]] = T{ Seq("-fPIC", "-O3") }

  def objects = T {
    // NOTE: currently we recompile all source files if headers or any c sources
    // change. It would be much smarter to establish a dependency graph of C
    // source files and headers (with -M) and only recompile what is needed.
    allHeaderFiles(): @annotation.nowarn("msg=pure expression does nothing")

    for (
      (src, relDir) <- allCCppFiles()
    ) yield {
      val outDir = T.dest / relDir
      os.makeDir.all(outDir)
      val out = outDir / s"${src.path.baseName}.o"
      val command = Seq[os.Shellable](
        compiler(),
        compileFlags(),
        "-c",
        "-o", out.relativeTo(os.pwd),
        src.path.relativeTo(os.pwd) // use a relative path as it's easier to look at in logs
      )
      T.log.info(command.value.mkString(" "))
      os.proc(command: _*).call()
      PathRef(out)
    }
  }

  def linkFlags: T[Seq[String]] = T { Seq.empty[String] }

  /** The library name, WITHOUT any os-dependent naming conventions.
    *
    * E.g. set this to 'foo' for the library 'libfoo.so' on Linux and
    * `libfoo.dylib` on Mac */
  def libName: T[String] = millSourcePath.last

  def link = T {
    val libname = if (sys.props("os.name").toLowerCase().startsWith("mac")) {
      s"lib${libName()}.dylib"
    } else {
      s"lib${libName()}.so"
    }
    val out = T.dest / libname

    val command: Seq[os.Shellable] = Seq(
      compiler(), "-shared",
      objects().map(_.path.relativeTo(os.pwd)),
      linkFlags(),
      "-o", out.relativeTo(os.pwd)
    )
    T.log.info(command.value.mkString(" "))
    os.proc(command: _*).call()
    PathRef(out)
  }

}
