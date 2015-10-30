import java.io.FileWriter
import java.util.Calendar
import java.text.SimpleDateFormat
import sbt.Keys.TaskStreams

import scala.sys.process._
object AppVersioningTasks {

  //region Структуры и поля данных

  // Структура для хранения информации по версии продукта.
  class Version(var major: Int = 0, var minor: Int = 0, var patch: Int = 1, var build: Int = 1) {

    override def toString = major + "." + minor + "." + patch + " build " + build
  }

  // Внутри объекта храним информацию о версии продукта.
  private var innerVersion: Version = null

  // Префикс, с которого начинается сообщение-коммит, говорящее о выполненной сборке.
  private val versionMessageStart: String = "VERSION "
  // Формат для представления текущего момента.
  private val dateFormat: String = "' | DATE:' dd-MM-yyyy ' | TIME:' HH:mm"
  // Файл, в котором записана текущая версия продукта.
  private val appVersionFile: String = "project/app.version"

  //endregion

  //region Реализация задач для SBT

  // Возвращает строковое представление версии продукта.
  def getVersion(): String = getVersionStruct.toString

  // Печатает строковое представление версии продукта.
  def printVersion(): Unit = println(getVersionStruct.toString)

  // Увеличивает номер major-версии.
  def incMajor(): Unit = {
    val version = getVersionStruct
    version.major = version.major + 1
  }

  // Увеличивает номер minor-версии.
  def incMinor(): Unit = {
    val version = getVersionStruct
    version.minor = version.minor + 1
  }

  // Увеличивает номер patch-версии.
  def incPatch(): Unit = {
    val version = getVersionStruct
    version.patch = version.patch + 1
  }

  // Выполняет сборку.
  def makeBuild(s: TaskStreams): Unit = {

    try {

      // Увеличиваем build-номер.
      incBuild
      s.log.info("Increment build number: OK")

      // Формируем сообщение о выполнении сборки.
      val dateFormatter = new SimpleDateFormat(dateFormat)
      val versionMessage = versionMessageStart + getVersion + dateFormatter.format(Calendar.getInstance().getTime)

      // Записываем сообщение в специальный файл.
      val file = new FileWriter(appVersionFile)
      file.write(versionMessage)
      file.close
      s.log.info("Write message to file: OK")

      // Добавляем этот файл в stage.
      Process(Seq("git", "add", appVersionFile)) !

      s.log.info("Stage file: OK")


      // Выполняем локальный коммит.
      Process(Seq("git", "commit", "-m", versionMessage, "-q")) !

      s.log.info("Local commit: OK")

      // Отправляем изменения на сервер.
      Process(Seq("git", "push", "-q")) !

      s.log.info("Push to server: OK")
      s.log.info("Version: " + getVersion)
    }
    catch {
      // В случае ошибки откатываем build-номер.
      case e: Exception => decBuild
    }
  }

  //endregion

  //region Внутренние методы

  // Возвращает информацию по версии продукта в виде экземпляра типа Version.
  private def getVersionStruct: Version = {

    // Если внутреннее значение не установлено, то определяем его.
    if (innerVersion == null) {

      // Начальные значения версий.
      var major = 0
      var minor = 0
      var patch = 1
      var build = 1

      // Извлекаем сообщения коммитов.
      val gitLogOutput = "git log --pretty=format:%s" !!
      val commitMessages = gitLogOutput.split("\n").filter(_ != "")
      var found = false

      // Перебираем все сообщения.
      commitMessages.foreach(message => {

        // Ищем первое сообщение, которое начинается с "VERSION" - это последняя выполненная сборка.
        if (!found && message.startsWith(versionMessageStart)) {

          // Разбиваем на слова.
          val messageParts = message.split(" ")

          // Часть x.x.x разбиваем дополнительно.
          val versionNumbers = messageParts(1).split("\\.")

          major = versionNumbers(0).toInt
          minor = versionNumbers(1).toInt
          patch = versionNumbers(2).toInt
          build = messageParts(3).toInt

          found = true
        }
      })

      innerVersion = new Version(major, minor, patch, build)
    }

    innerVersion
  }

  // Увеличивает build-номер.
  private def incBuild(): Unit = {
    val version = getVersionStruct
    version.build = version.build + 1
  }

  // Уменьшает build-номер.
  private def decBuild(): Unit = {
    val version = getVersionStruct
    version.build = version.build - 1
  }

  //endregion
}
