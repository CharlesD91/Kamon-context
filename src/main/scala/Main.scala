import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}
import kamon.Kamon
import kamon.context.Context
import net.bytebuddy.agent.ByteBuddyAgent
import kamon.tag.Lookups.plain

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object Main extends App {

  // attach Kanela java agent to JVM
//  Kamon2.init()
  private val kanelaAgentJarName =
    "/home/charles/Downloads/kanela-agent-1.0.4.jar"
  ByteBuddyAgent.attach(new File(kanelaAgentJarName), Utils.pid())

  implicit val ctxShift: ContextShift[IO] = IO.contextShift(global)
  val anotherExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  val context = Context.of("key", "value")
  val contextTagAfterTransformations =
    for {
      scope <- IO {
        Kamon.storeContext(context)
      }
      len <- IO("Hello Kamon!").map(_.length)
      _ <- IO(len.toString)
      _ <- IO.shift(global)
      _ <- IO.shift
      _ <- IO.shift(anotherExecutionContext)
    } yield {
      val tagValue = Kamon.currentContext().getTag(plain("key"))
      scope.close()
      tagValue
    }

  val res = contextTagAfterTransformations.unsafeRunSync()

  println(res)
}

object Utils {

  def pid(): String = {
    val jvm = ManagementFactory.getRuntimeMXBean.getName
    jvm.substring(0, jvm.indexOf('@'))
  }
}
