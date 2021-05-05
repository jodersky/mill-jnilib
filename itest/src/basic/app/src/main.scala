@native def answer(): Int

@main def run() = {
  System.loadLibrary("app")
  assert(answer() == 42)
}
