antlr4Settings
antlr4PackageName in Antlr4 := Some("edu.umich.verdict.parser")
antlr4GenListener in Antlr4 := true
antlr4GenVisitor in Antlr4 := true

lazy val root = (project in file(".")).
  settings(
    organization := "edu.umich",
    name := "verdict",
    version := "0.1.0",
    scalaVersion := "2.11.7"
  )