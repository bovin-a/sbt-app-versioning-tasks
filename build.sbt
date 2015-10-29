
name := "manual"

lazy val hello = taskKey[Unit]("Prints 'Hello World'")
hello := Task.taskRoutine()