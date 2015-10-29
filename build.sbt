
name := "manual"

lazy val build = taskKey[Unit]("Build number task")
build := BuildNumberTask.run()