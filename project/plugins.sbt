//javaOptions := "-Xmx512M -XX:MaxPermSize=256m -XX:ReservedCodeCacheSize=128m -Dsbt.log.format=true -Dfile.encoding=UTF8 -Duser.home=C:/CustomUserHome"

scalaVersion := "2.10.2"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.0-SNAPSHOT")