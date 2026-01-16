package infra_part_4

import akka.actor.TypedActor
import akka.actor.TypedActor.context
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

/**
 * Akka configuration - see reference https://doc.akka.io/libraries/akka-core/current/general/configuration-reference.html
 *
 * Akka uses 3 file formats for configuration
 * - HOCON: superset of JSON, easier to read
 * - JSON
 * - Properties
 *
 * Passing configuration to an ActorSystem
 * - a plain String (all formats supported)
 * - application.conf
 * - a dedicated file
 *
 */
object AkkaConfiguration {

  object SimpleLoggingActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(message)
      Behaviors.same
    }
  }

  // 1 - inline configuration
  def demoInlineConfig(): Unit = {
    // HOCON, Human Optimised Configuration Notation -- super set of JSON and managed by Lightbend
    /** replace akka with pekko*/
    val configString: String =
      """
        |akka {
        |  loglevel = "DEBUG"
        |}
        |""".stripMargin
    val config = ConfigFactory.parseString(configString)
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemo", ConfigFactory.load(config))

    system ! "A message to remember"

    Thread.sleep(1000)
    system.terminate()
  }

  // 2 - config file - application.conf file name is very important
  def demoConfigFile(): Unit = {
    val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemo", specialConfig)

    system ! "A message to remember"

    Thread.sleep(1000)
    system.terminate()
  }

  // 3 - a different config in another file
  def demoSeparateConfigFile(): Unit = {
    val separateConfig = ConfigFactory.load("secretDir/secretConfiguration.conf")
    println(separateConfig.getString("akka.loglevel"))
  }

  // 4 - different file formats (JSON, properties)
  def demoOtherFileFormats(): Unit = {
    val jsonConfig = ConfigFactory.load("json/jsonConfiguration.json")
    println(s"json config with customer property: ${jsonConfig.getString("aJsonProperty")}")
    println(s"json config with Akka property: ${jsonConfig.getString("akka.loglevel")}")

    // properties format
    val propsConfig = ConfigFactory.load("properties/propsConfiguration.properties")
    println(s"properties config with customer property: ${propsConfig.getString("mySimpleProperty")}")
    println(s"properties config with Akka property: ${propsConfig.getString("akka.loglevel")}")

  }

  def main(args: Array[String]): Unit = {
    demoInlineConfig()
  }
}
