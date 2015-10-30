
name := "manual"

lazy val getVersion = taskKey[String]("Return current version of application")
getVersion := AppVersioningTasks.getVersion()

lazy val printVersion = taskKey[Unit]("Print current version of application")
printVersion := AppVersioningTasks.printVersion()

lazy val incMajor = taskKey[Unit]("Increment major number of application version")
incMajor := AppVersioningTasks.incMajor()

lazy val incMinor = taskKey[Unit]("Increment minor number of application version")
incMinor := AppVersioningTasks.incMinor()

lazy val incPatch = taskKey[Unit]("Increment patch number of application version")
incPatch := AppVersioningTasks.incPatch()

lazy val makeBuild = taskKey[Unit]("Make project build")
makeBuild := AppVersioningTasks.makeBuild(streams.value)
