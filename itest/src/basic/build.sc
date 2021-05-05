import $exec.plugins
import mill._, scalalib._, jnilib._

object app extends ScalaModule with JavahModule with CLibModule {

  def scalaVersion = "3.0.0-RC3"

  def includes: T[Seq[os.Path]] = T{
    Seq(millSourcePath / "src" / "include") ++
      JavahModule.findIncludes()
  }

  def compileFlags = T{
    Seq("-O3", "-fPIC") ++
      includes().map(p => s"-I$p")
  }

  def forkEnv = T{
    Map(
      "LD_LIBRARY_PATH" -> s"${link().path / os.up}"
    )
  }

}
