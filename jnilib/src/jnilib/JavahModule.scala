package mill.jnilib

import mill.PathRef
import mill.T

trait JavahModule extends mill.scalalib.JavaModule {
  def javahOutputDirectory: os.Path = millSourcePath / "src"

  def javah() = T.command{
    val out = javahOutputDirectory
    os.makeDir.all(out)
    os.walk(compile().classes.path).filter(_.ext == "class").foreach { path =>
      gjavah.javah(
        path.toNIO,
        out.toNIO
      )
    }
    PathRef(out)
  }
}

object JavahModule extends mill.define.ExternalModule {
  import mill._

  /** Try to find directories containing JNI header files, based on common
    * system installation paths.
    *
    * The search logic is as follows:
    *
    * 1. If the user has set JAVA_HOME, check if $JAVA_HOME/include and
    *    $JAVA_HOME/include/<kernel> exist and use those.
    * 2. If no JAVA_HOME has been set, try to find include directories of the
    *    JVM started by the system's 'java' command.
    */
  def findIncludes: T[Seq[os.Path]] = T { findJniIncludes() }

  def findJniIncludes()(implicit ctx: mill.api.Ctx): Seq[os.Path] = {
    def reportFailure(message: String) = {
      T.log.error(message)
      T.log.error("No JNI includes could be found.")
      Seq.empty
    }

    val kernel: String = sys.props("os.name").toLowerCase match {
      case "linux" => "linux"
      case mac if mac.startsWith("mac") => "darwin"
      case other =>
        return reportFailure(s"Detected OS: $other. Automatic JNI header search is not " +
          "supported for this operating system.")
    }

    def getIncludes(dirString: String) = {
      val dir = os.Path(dirString)
      if (os.exists(dir / "include") && os.exists(dir / "include" / kernel))
        Some(Seq(dir / "include", dir / "include" / kernel))
      else None
    }

    if (sys.env.contains("JAVA_HOME")) {
      val home = sys.env("JAVA_HOME")
      getIncludes(home) match {
        case Some(dirs) => dirs
        case None =>
          reportFailure(
            s"JAVA_HOME has been set to '$home', but no JNI headers could be found. " +
            "You can unset JAVA_HOME to use system discovery, or set it to a " +
            s"path which contains the JNI 'include/' and 'include/$kernel/' directories"
          )
      }
    } else if (sys.props("os.name").toLowerCase.startsWith("mac")) {
      val base = os.root / "Library" / "Java" / "JavaVirtualMachines"

      if (!os.exists(base) || !os.isDir(base))
        reportFailure(s"No java virtual machine could be found in '$base'")

      val candidates = os.list(base).map(p => p / "Contents" / "Home")

      // TODO: there could be multiple java versions installed. We pick the
      // first one found, but this may not always be the desired behavior.
      candidates.foreach { path =>
        getIncludes(path.toString) match {
          case Some(dirs) => return dirs
          case _ =>
        }
      }
      return reportFailure(
        s"Searched ${candidates.mkString(", ")} directories, but could not find any " +
        "containing JNI headers."
      )
    } else {
      val whichRes = os.proc("which", "java").call(check = false)
      if (whichRes.exitCode != 0) {
        return reportFailure("Could not find the 'java' command")
      }

      val javaPath = {
        val rawPath = os.Path(whichRes.out.text().trim())
        os.followLink(rawPath) match {
          case Some(path) => path
          case None =>
            return reportFailure(s"The 'java' command points to '$rawPath', which is invalid")
        }
      }

      val javaHome = javaPath / os.up / os.up
      getIncludes(javaHome.toString) match {
        case Some(dirs) => dirs
        case None =>
          return reportFailure(s"The 'java' command points to '$javaPath', but no JNI " +
            s"headers could be found under '$javaHome/include' and '$javaHome/include/$kernel'")
      }
    }
  }

  lazy val millDiscover: mill.define.Discover[this.type] = mill.define.Discover[this.type]
}
