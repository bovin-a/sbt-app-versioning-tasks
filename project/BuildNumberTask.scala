import java.util.Calendar
import java.text.SimpleDateFormat
import sys.process._
import sbt.Keys._

object BuildNumberTask {

  def run(): Unit = {

    val formatter = new SimpleDateFormat("' | DATE:' dd-MM-yyyy ' | TIME:' hh:mm")
    val buildNumber = getBuildNumber
    val buildMessage = "BUILD #" + buildNumber + formatter.format(Calendar.getInstance().getTime)
    val gitCommand = "git commit -F project/build.number"

    "git add project/build.number" !!

    gitCommand !!

    "git push" !!
  }

  def getBuildNumber(): Int = {

    var buildNumber = 1
    val gitLogOutput = "git log --pretty=format:\"%s\"" !!
    val commitMessages = gitLogOutput.split("\n").filter(_ != "")

    commitMessages.foreach(m => {
      if (buildNumber == 1 && m.startsWith("BUILD #")) {
        buildNumber = m.replaceAllLiterally("BUILD #", "").split(" ")(0).toInt
      }
    })

    buildNumber
  }

}
