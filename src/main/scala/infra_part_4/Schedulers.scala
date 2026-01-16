package infra_part_4

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import utils.ActorSystemEnhancements

import scala.concurrent.duration.DurationInt

/**
 * Schedulers
 * - Send a message at a certain time
 * - Run an arbitrary computation at a time
 * - Things to be careful with
 * ---- If you schedule many arbitrary computations, use a dedicated ExecutionContext. Need to take care to not starve the actor system of threads.
 * ---- Cancel schedules you are not using.
 *
 * Timers
 * - Send message to yourself at a certain time
 * - Manipulate the passage of time in tests
 *
 */

object Schedulers {

  object LoggerActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[${context.self.path}] Received: $message")
      Behaviors.same
    }
  }

  def demoScheduler(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val loggerActor = context.spawn(LoggerActor(), "loggerActor")

      context.log.info("[system] System starting")
      context.scheduleOnce(1.second, loggerActor, "reminder")

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoScheduler")

    import system.executionContext // take care with futures
    system.scheduler.scheduleOnce(2.seconds, () => system.terminate)
  }

  // timeout pattern
  def demoActorWithTimeout(): Unit = {
    val timeoutActor: Behavior[String] = Behaviors.receive { (context, message) =>
      val schedule = context.scheduleOnce(1.second, context.self, "timeout")
      message match {
        case "timeout" =>
          context.log.info("Stopping!")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received $message")
          Behaviors.same
      }
    }

    val system = ActorSystem(timeoutActor, "TimeoutDemo")
    system ! "trigger"
    Thread.sleep(2000)
    system ! "are you there"

  }

  /**
   * Exercise: enhance the timeoutActor to reset its timer with every new message (except the "timeout" message)
   *
   */

  object ResettingTimeoutActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"Received: $message")
      resettingTimeoutActor(context.scheduleOnce(1.second, context.self, "timeout"))
    }

    def resettingTimeoutActor(schedule: Cancellable): Behavior[String] = Behaviors.receive { (context, message) =>
      // start the scheduler
      var schedule = context.scheduleOnce(1.second, context.self, "timeout")

      message match {
        case "timeout" =>
          context.log.info("Stopping!")
          Behaviors.stopped
        case _ =>
          context.log.info(s"Received: $message")
          // reset scheduler
          schedule.cancel()
          // start another scheduler
          resettingTimeoutActor(context.scheduleOnce(1.second, context.self, "timeout"))
          Behaviors.same
      }
    }
  }

  def demoActorResettingTimeout(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val resettingTimeoutActor = context.spawn(ResettingTimeoutActor(), "resetter")

      resettingTimeoutActor ! "start timer"
      Thread.sleep(500)
      resettingTimeoutActor ! "reset"
      Thread.sleep(700)
      resettingTimeoutActor ! "this should still be visible"
      Thread.sleep(1200)
      resettingTimeoutActor ! "This should not be visible"
      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoResettingTimeoutActor").withFiniteLifespan(4.seconds)
    import system.executionContext
    system.scheduler.scheduleOnce(3.seconds, () => system.terminate())

  }

  def main(args: Array[String]): Unit = {
    demoActorResettingTimeout()
  }
}
