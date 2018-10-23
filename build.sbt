
scalaVersion := "2.12.7"

scalacOptions in ThisBuild ++= Seq(
  "-language:_",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)

name := "fpmortals"
organization := "hu.andrasszatmari"
version := "0.1"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.github.mpilquist"  %% "simulacrum"     % "0.13.0",
  "com.chuusai"           %% "shapeless"      % "2.3.3",
  "org.scalaz"            %% "scalaz-core"    % "7.2.26",
  "com.propensive"        %% "contextual"     % "1.1.0",
  "eu.timepit"            %% "refined-scalaz" % "0.9.2",
  "org.scalatest"         %% "scalatest"      % "3.0.5" % "test"
)

val derivingVersion = "1.0.0"
libraryDependencies ++= Seq(
  "org.scalaz" %% "deriving-macro" % derivingVersion % "provided",
  compilerPlugin("org.scalaz" %% "deriving-plugin" % derivingVersion),
  "org.scalaz" %% "scalaz-deriving"            % derivingVersion,
  "org.scalaz" %% "scalaz-deriving-magnolia"   % derivingVersion,
  "org.scalaz" %% "scalaz-deriving-scalacheck" % derivingVersion,
  "org.scalaz" %% "scalaz-deriving-jsonformat" % derivingVersion
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)