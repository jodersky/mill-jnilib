import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest_mill0.9:0.4.0`
import de.tobiasroeser.mill.integrationtest.MillIntegrationTestModule
import mill._, scalalib._, publish._

object jnilib extends ScalaModule with PublishModule {
  def scalaVersion = "2.13.5"
  def scalacOptions = Seq(
    "-deprecation",
    "-release", "8"
  )

  val millVersion = "0.9.6"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )

  def publishVersion = "0.1.0"
  def pomSettings = PomSettings(
    description = "jnilib",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/mill-jnilib",
    licenses = Seq(License.`BSD-3-Clause`),
    versionControl = VersionControl.github("jodersky", "mill-jnilib"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://github.com/jodersky")
    )
  )

  def artifactName = "mill-jnilib"
}

object itest extends MillIntegrationTestModule {
  import de.tobiasroeser.mill.integrationtest._

  def millTestVersion  = jnilib.millVersion
  def pluginsUnderTest = Seq(jnilib)
  override def testInvocations =
    Seq(
      PathRef(sources().head.path / "clib") -> Seq(
        TestInvocation.Targets(Seq("__.link"))
      ),
      PathRef(sources().head.path / "basic") -> Seq(
        TestInvocation.Targets(Seq("__.run"))
      ),
      PathRef(sources().head.path / "full") -> Seq(
        TestInvocation.Targets(Seq("__.run"))
      )
    )
}
