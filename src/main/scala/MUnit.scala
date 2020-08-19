import cats.effect.IO
import scala.concurrent.{ExecutionContext, Future}

// MUnit demo of how to run both () => Unit and () => F[Unit] tests
class MUnit extends munit.FunSuite {
  override def munitTests(): Seq[Test] = {
    val tests = super.munitTests()

    implicit val ec: ExecutionContext = ExecutionContext.global
    def handleIO(test: Test): Test = {
      val bodyIO: () => TestValue = () => {
        test.body() match {
          case fio: Future[IO[_]] => fio.map(_.unsafeRunSync())
          case f => f
        }
      }
      new Test(test.name, bodyIO, test.tags, test.location)
    }

    tests.map(handleIO)
  }
  test("hello") {
    val obtained = 42
    val expected = 43

    assert(obtained == expected)
  }
  test("hello2") {
    for {
      obtained <- IO(42)
      expected <- IO(43)
    } yield assertEquals(obtained, expected)

  }

}
