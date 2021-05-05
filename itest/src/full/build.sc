import $exec.plugins
import mill._, scalalib._, jnilib._

object lib1 extends CLibModule
object app extends ScalaModule with JavahModule with CLibModule {

  def scalaVersion = "3.0.0-RC3"

  def compileFlags = T{
    super.compileFlags() ++
      Seq(s"-I${lib1.millSourcePath / "src" / "include"}") ++
      JavahModule.findIncludes().map(path => s"-I${path}")
  }

  def linkFlags = T {
    super.linkFlags() ++ Seq(s"-L${lib1.link().path / os.up}", "-llib1")
  }

  def forkEnv = T{
    Map(
      "LD_LIBRARY_PATH" -> s"${link().path / os.up}:${lib1.link().path / os.up}"
    )
  }

}
