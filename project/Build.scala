import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "deepdive",
    base = file("."),
    settings = Defaults.defaultSettings 
      ++ packSettings // This settings add pack and pack-archive commands to sbt
      ++ Seq(
        // [Optional] Specify mappings from program name -> Main class (full package path)
        packMain := Map("deepdive" -> "org.deepdive.Main"),
        // Add custom settings here
        // [Optional] JVM options of scripts (program name -> Seq(JVM option, ...))
        // packJvmOpts := Map("deepdive" -> Seq("-Xmx2g")),
        // [Optional] Extra class paths to look when launching a program
        // packExtraClasspath := Map("deepdive" -> Seq("${PROG_HOME}/etc")), 
        // [Optional] (Generate .bat files for Windows. The default value is true)
        packGenerateWindowsBatFile := true,
        // [Optional] jar file name format in pack/lib folder (Since 0.5.0)
        //   "default"   (project name)-(version).jar 
        //   "full"      (organization name).(project name)-(version).jar
        //   "no-version" (organization name).(project name).jar
        //   "original"  (Preserve original jar file names)
        packJarNameConvention := "default",
        // [Optional] List full class paths in the launch scripts (default is false) (since 0.5.1)
        packExpandedClasspath := false
      ) 
    // To publish tar.gz archive to the repository, add the following line (since 0.3.6)
    ++ publishPackArchive  
    // Before 0.3.6, use below:
    // ++ addArtifact(Artifact("myprog", "arch", "tar.gz"), packArchive).settings
  )
}