import java.util.Calendar
import java.text.SimpleDateFormat
import sbt.Compile
import sys.process._
import sbt.Keys._

object BuildNumberTask {

  def run(): Unit = {

    val formatter = new SimpleDateFormat("'DATE:' dd-MM-yyyy ' | TIME:' hh:mm")
    val buildNumber = getBuildNumber
    val gitCommand = "git commit -m \"BUILD №" + buildNumber + " | " + formatter.format(Calendar.getInstance().getTime) + "\""

    "echo " + buildNumber + " > project/build.number" !!

    "git add *" !!

    gitCommand !!

    "git push" !!
  }

  def getBuildNumber(): Int = {

    var buildNumber = 1
    val gitLogOutput = "git log --pretty=format:\"%s\"" !!
    val commitMessages = gitLogOutput.split("\n").filter(_ != "")

    commitMessages.foreach(m => {
      if (buildNumber == 1 && m.startsWith("BUILD №")) {
        buildNumber = m.replaceAllLiterally("BUILD №", "").split(" ")(0).toInt
      }
    })

    buildNumber
  }

}
