import Dependencies._
import BuildSettings._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

lazy val commonDeps = Seq(logback, scalaTest, scalaCheck)

lazy val sparkDeps =
  Seq(sparkCore,
      sparkSql,
      sparkStreaming,
      sparkKafka,
      postgres,
      "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.1")

lazy val authDeps = Seq(
  keycloakAdapter,
  keycloak,
  jbossLogging,
  httpClient
)

lazy val akkaDeps = Seq(
  akkaSlf4J,
  akkaCluster,
  akkaTyped,
  akkaClusterTyped,
  akkaStream,
  akkaStreamTyped,
  akkaManagement,
  akkaManagementClusterBootstrap,
  akkaServiceDiscoveryDNS,
  akkaServiceDiscoveryKubernetes,
  akkaClusterHttpManagement,
  akkaClusterHttpManagement,
  akkaTestkitTyped,
  akkaCors,
  akkaKafkaStreams,
  embeddedKafka,
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0.0",
  akkaQuartzScheduler,
  phantomDSL,
  phantomJDK8
)

lazy val akkaPersistenceDeps =
  Seq(akkaPersistence,
      akkaClusterSharding,
      akkaPersistenceTyped,
      akkaClusterShardingTyped,
      akkaPersistenceCassandra,
      cassandraLauncher)

lazy val akkaHttpDeps = Seq(akkaHttp, akkaHttp2, akkaHttpTestkit, akkaHttpCirce)
lazy val circeDeps = Seq(circe, circeGeneric, circeParser)

lazy val slickDeps = Seq(slick, slickHikaryCP, postgres, h2)

lazy val scalafmtSettings = Seq(
  scalafmtOnCompile in ThisBuild := true,
  scalafmtTestOnCompile in ThisBuild := true
)

lazy val dockerSettings = Seq(
  Docker / maintainer := "Hmda-Ops",
  dockerBaseImage := "openjdk:jre-alpine",
  dockerRepository := Some("hmda")
)

lazy val packageSettings = Seq(
  // removes all jar mappings in universal and appends the fat jar
  mappings in Universal := {
    // universalMappings: Seq[(File,String)]
    println("here")
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    // removing means filtering
    val filtered = universalMappings filter {
      case (_, fileName) => !fileName.endsWith(".jar")
    }
    // add the fat jar
    filtered :+ (fatJar -> ("lib/" + fatJar.getName))
  },
  // the bash scripts classpath only needs the fat jar
  scriptClasspath := Seq((assemblyJarName in assembly).value)
)

lazy val `hmda-root` = (project in file("."))
  .settings(hmdaBuildSettings: _*)
  .aggregate(
    common,
    `hmda-platform`,
    `check-digit`,
    `institutions-api`,
    `modified-lar`,
    `hmda-analytics`,
    `census-api`,
    `hmda-regulator`,
    `hmda-reporting`,
    `hmda-spark-reporting`
  )

lazy val common = (project in file("common"))
  .settings(hmdaBuildSettings: _*)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    Seq(
      libraryDependencies ++= commonDeps ++ authDeps ++ akkaDeps ++ akkaPersistenceDeps ++ akkaHttpDeps ++ circeDeps ++ slickDeps
    )
  )

lazy val `hmda-spark-reporting` = (project in file("hmda-spark-reporting"))
  .enablePlugins(sbtdocker.DockerPlugin, AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in assembly := Some("com.hmda.reports.DisclosureReports"),
      assemblyJarName in assembly := "hmda-reports.jar",
      assemblyMergeStrategy in assembly := {
        case PathList("javax", "servlet", xs @ _*)        => MergeStrategy.last
        case PathList("javax", "activation", xs @ _*)     => MergeStrategy.last
        case PathList("org", "apache", xs @ _*)           => MergeStrategy.last
        case PathList("com", "google", xs @ _*)           => MergeStrategy.last
        case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
        case PathList("com", "codahale", xs @ _*)         => MergeStrategy.last
        case PathList("com", "yammer", xs @ _*)           => MergeStrategy.last
        case "META-INF/io.netty.versions.properties"      => MergeStrategy.concat
        case "META-INF/ECLIPSEF.RSA"                      => MergeStrategy.last
        case "META-INF/mailcap"                           => MergeStrategy.last
        case "META-INF/mimetypes.default"                 => MergeStrategy.last
        case "plugin.properties"                          => MergeStrategy.last
        case "log4j.properties"                           => MergeStrategy.last
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    ),
    Seq(
      libraryDependencies ++= sparkDeps ++ circeDeps ++ akkaDeps
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val `hmda-platform` = (project in file("hmda"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.HmdaPlatform"),
      assemblyJarName in assembly := "hmda2.jar",
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case "logback.xml"                           => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol` % "compile->compile;test->test")
  .dependsOn(`check-digit` % "compile->compile;test->test")
  .dependsOn(`census-api` % "compile->compile;test->test")

lazy val `check-digit` = (project in file("check-digit"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin,
                 AkkaGrpcPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.uli.HmdaUli"),
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      },
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol` % "compile->compile;test->test")

lazy val `institutions-api` = (project in file("institutions-api"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.institution.HmdaInstitutionApi"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val `hmda-regulator` = (project in file("hmda-regulator"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin,
                 AkkaGrpcPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.regulator.HmdaRegulatorApp"),
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      },
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol` % "compile->compile;test->test")

lazy val `census-api` = (project in file("census-api"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin,
                 AkkaGrpcPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.census.HmdaCensus"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol`)

lazy val `modified-lar` = (project in file("modified-lar"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.publication.lar.ModifiedLarApp"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol`)
  .dependsOn(common)

lazy val `irs-publisher` = (project in file("irs-publisher"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.publication.lar.IrsPublisherApp"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol`)
  .dependsOn(common)

lazy val `hmda-reporting` = (project in file("hmda-reporting"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.reporting.HmdaReporting"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(`hmda-protocol`)
  .dependsOn(common)

lazy val `hmda-protocol` = (project in file("protocol"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin,
                 AkkaGrpcPlugin)
  .settings(hmdaBuildSettings: _*)

lazy val `hmda-analytics` = (project in file("hmda-analytics"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.analytics.HmdaAnalyticsApp"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
