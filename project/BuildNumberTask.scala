import java.util.Calendar
import java.text.SimpleDateFormat
import sys.process._

object BuildNumberTask {

  def run(): Unit = {

    val format = new SimpleDateFormat("'DATE:' dd-MM-yyyy ' | TIME:' hh:mm")
    val today = Calendar.getInstance()

    println("git commit - m \"BUILD №" + buildNumber + " | " + format.format(today.getTime) + "\"")
  }

  def buildNumber(): Int = {

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
