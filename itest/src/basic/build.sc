import $exec.plugins
import mill._, scalalib._, jnilib._

object app extends ScalaModule with JavahModule with CLibModule {

  def scalaVersion = "3.0.0-RC3"

  def includes = T.sources(
    Seq(PathRef(millSourcePath / "src" / "include")) ++
    JavahModule.findIncludes()
  )

  def forkEnv = T{
    Map(
      "LD_LIBRARY_PATH" -> s"${link().path / os.up}"
    )
  }

}
