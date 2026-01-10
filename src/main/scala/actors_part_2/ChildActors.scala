package actors_part_2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

/**
 * Best practices
 * - Keep the ActorSystem's behaviour empty
 * - Set up all your important actors and interactions at the setup of the ActorSystem
 */

object ChildActors {

  /**
   * - actors can create other actors (child): parent --> child --> grandChild --> .....etc
   * - actor hierarchy = three like structure
   * - root of the hierarchy = "guardian" actor (created with the ActorSystem)
   * - actors can be identified via a path
   * ActorSystem creates
   * - the top-level (root) guardian,
   * - the system guardian (for Akka internal messages)
   * - use guardian (for our custom actors)
   *
   * All our actors are child actors of the user guardian
   *
   * --------------- Watching actors -----------------
   * - get a notification (signal) when the actor dies `context.watch(ref)`
   * - Deregistering
   * ---> `context.unwatch(ref)` // works with any actor
   *
   * Properties
   * - the Terminated signal gets sent even if they watched actor is already dead at registration time
   * - registering multiple times may/may not generate multiple Terminated signals
   * - unwatching will not process Terminated signals even if they have already been enqueued
   *
   */

  object Parent {
    trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(message: String) extends Command

    case object StopChild extends Command

    case object WatchChild extends Command

    def apply(): Behavior[Command] = idle()

    def idle(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] Creating child with name $name")
          // create a child actor *reference* and this used to send messages to the child
          val childRef: ActorRef[String] = context.spawn(Child(), name)
          active(childRef)
      }
    }

    def active(childRef: ActorRef[String]): Behavior[Command] = Behaviors.receive[Command] { (context, message) =>
      message match {
        case TellChild(message) =>
          context.log.info(s"[parent] Sending message $message to child")
          childRef ! message // <- send a message to another actor
          Behaviors.same
        case StopChild =>
          context.log.info("[parent] stopping child")
          context.stop(childRef) // only works with CHILD actors
          idle()
        case WatchChild =>
          context.log.info("[parent] watching child")
          context.watch(childRef) // can use any ActorRef
          Behaviors.same
        case _ =>
          context.log.info("[parent] command not supported")
          Behaviors.same
      }
    }.receiveSignal {
      case (context, Terminated(childRefWhichDied)) =>
        context.log.info(s"[parent] Child ${childRefWhichDied.path} was killed by something...")
        idle()
    }
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[${context.self.path.name}] Received $message")
      Behaviors.same
    }
  }

  def demoParentChild(): Unit = {
    import Parent._
    val userGuardianBehaviour: Behavior[Unit] = Behaviors.setup { context =>
      // set up all the important actors in your application
      val parent = context.spawn(Parent(), "parent")
      // set up the initial interaction between the actors
      parent ! CreateChild("child")
      parent ! TellChild("hey kid, you there?")
      parent ! WatchChild
      parent ! StopChild
      parent ! CreateChild("child2")
      parent ! TellChild("yo new kid, how are you?")

      // user guardian usually has no behaviour of its own
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehaviour, "DemoParentChild")
    Thread.sleep(1000)
    system.terminate()
  }

  /**
   * Exercise: Write Parent_V2 that can manage multiple child actors
   */

  object Parent_V2 {
    trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(name: String, message: String) extends Command

    case class StopChild(name: String) extends Command

    case class WatchChild(name: String) extends Command

    def apply(): Behavior[Command] = active(Map())

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] = Behaviors.receive[Command] { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] Creating child: '$name''")
          val childRef = context.spawn(Child(), name)
          active(children + (name -> childRef))
        case TellChild(name, message) =>
          val childOption = children.get(name)
          childOption.fold(context.log.info(s"[parent] Child '$name' could not be found'"))(child => child ! message)
          Behaviors.same
        case WatchChild(name) =>
          context.log.info(s"[parent] watching child with the name $name")
          val childOption = children.get(name)
          childOption.fold(context.log.info(s"[parent] Cannot watch $name: name doesn't exist"))(context.watch)
          Behaviors.same
        case StopChild(name) =>
          context.log.info(s"[Parent] attempting to stop child with name $name")
          val childOption = children.get(name)
          childOption.fold(context.log.info(s"[parent] Child $name could not be stopped: name doesn't exist"))(context.stop)
          active(children - name)
      }
    }.receiveSignal {
      case (context, Terminated(childRefWhichDied)) =>
        context.log.info(s"[parent] Child ${childRefWhichDied.path} was killed by something...")
        val childName = childRefWhichDied.path.name
        active(children - childName)
    }

    def demoParentChild_v2(): Unit = {
      import Parent_V2._
      val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
        val parent = context.spawn(Parent_V2(), "parent")
        parent ! CreateChild("alice")
        parent ! CreateChild("bob")
        parent ! WatchChild("alice")
        parent ! TellChild("alice", "living next door to you")
        parent ! TellChild("daniel", "I hope your Akka skills are good")
        parent ! StopChild("alice")
        parent ! TellChild("alice", "hey Alice, you still there?")

        Behaviors.empty
      }

      val system = ActorSystem(userGuardianBehavior, "DemoParentChildV2")
      Thread.sleep(1000)
      system.terminate()
    }

    def main(args: Array[String]): Unit = {
      demoParentChild()
    }
  }
