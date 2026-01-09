package actors_part_2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object ActorState {

  /* Exercise - use setup method to create a word counter which
    - splits each message into words
    - keeps track of the total number of words received so far
    - log the current number of words + the total number of words
   */


  object WordCounter {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      var total = 0

      Behaviors.receiveMessage { message =>
        val newCount = message.split(" ").length
        total += newCount
        context.log.info(s"Message word count: $newCount - total count: $total")
        Behaviors.same
      }
    }
  }

  trait SimpleThing

  case object EatChocolate extends SimpleThing

  case object CleanUpTheFloor extends SimpleThing

  case object LearnAkka extends SimpleThing

  /**
   * Message types must be immutable and serializable <--- neither of these can be checked by the compiler
   * --- To simply achieve / enforce this
   * - Use case classes/objects
   * - Use a flat type hierarchy
   *
   */

  object SimpleHuman {
    def apply(): Behavior[SimpleThing] = Behaviors.setup { context =>
      var happiness = 0

      Behaviors.receiveMessage {
        case EatChocolate =>
          context.log.info(s"[$happiness] eating chocolate")
          happiness += 1
          Behaviors.same
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Clear up the floor")
          happiness -= 2
          Behaviors.same
        case LearnAkka =>
          context.log.info(s"[$happiness] Learning Akka, YAY!")
          happiness += 99
          Behaviors.same
      }
    }
  }

  def demoSimpleHuman(): Unit = {
    val human = ActorSystem(SimpleHuman(), "DemoSimpleHuman")

    human ! LearnAkka
    human ! EatChocolate
    (1 to 30).foreach(_ => human ! CleanUpTheFloor)

    Thread.sleep(1000)
    human.terminate()
  }

  object SimpleHuman_V2 {
    def apply(): Behavior[SimpleThing] = statelessHuman(0)

    def statelessHuman(happiness: Int): Behavior[SimpleThing] = Behaviors.receive { (context, message) =>
      message match {
        case EatChocolate =>
          context.log.info(s"[$happiness] Eating chocolate")
          statelessHuman(happiness + 1)
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Wiping the floor, ugh.....")
          statelessHuman(happiness - 2)
        case LearnAkka =>
          context.log.info(s"[$happiness] Learning Akka, YAY!")
          statelessHuman(happiness + 99)
      }

    }
  }

  /** Tips for turning stateful actor into stateless actor
   * - Each var/mutable field becomes an immutable method argument
   * - Each state change becomes a new behaviour obtained by calling the method with a different argument
   */

  /**
   * Exercise - refactor stateful word counter into stateless
   *
   */

  object WordCounter_V2 {
    def apply(): Behavior[String] = active(0)

    def active(total: Int): Behavior[String] = Behaviors.receive { (context, message) =>
      val newCount = message.split(" ").length
      context.log.info(s"Message word count: $newCount - total count: ${total + newCount}")
      active(total + newCount)
    }
  }

  def demoWordCounter(): Unit = {
    val wordCounter = ActorSystem(WordCounter(), "WordCountDemo")

    wordCounter ! "I am learning Akka"
    wordCounter ! "I hope you will be stateless one day"
    wordCounter ! "Let's see the next one"

    Thread.sleep(1000)
    wordCounter.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoSimpleHuman()
  }

  /** Notes
   * Can use the "setup" method to add state
   * - Easy to write at first
   * - Can be harder to test, read and understand later
   *
   * Change to "stateless"
   * - Move mutable state to method argument
   * - Change state by changing behaviour
   * - Easy to modify, read, understand and modularise
   *
   * */

}
