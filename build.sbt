import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name :="nbrno"

scalaVersion :="2.10.1"

version :="0.1"

resolvers += "coda" at "http://repo.codahale.com"

resolvers += "sonatype-repo" at "http://oss.sonatype.org"

resolvers += "maven-repo" at "http://search.maven.org"

resolvers += "jboss repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"

classpathTypes ~= (_ + "orbit")

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "net.databinder" %% "unfiltered-filter" % "0.6.8"

libraryDependencies += "net.databinder" %% "unfiltered-json" % "0.6.7"

libraryDependencies += "net.databinder" %% "unfiltered-jetty" % "0.6.8"

libraryDependencies += "com.typesafe.slick" %% "slick" % "1.0.1-RC1"

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)

libraryDependencies += "com.lambdaworks" % "scrypt" % "1.4.0"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.4"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"