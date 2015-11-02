import java.io.{FileWriter, File}
import java.nio.file.{Paths, Files}
import java.util.Calendar
import java.text.SimpleDateFormat
import sbt.Keys._
import sbt._
import scala.sys.process.{ProcessLogger, Process}

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

  // Название архива, в который складываются исходники проекта.
  private val zipName: String = "zakupay"

  // Логгер, использующийся для запуска комманд в bash с помощью Scala, который втихую скрывает все сообщения.
  private val silentBashLogger = ProcessLogger((o: String) => {}, (e: String) => {})

//  // Общая часть выполнения сборки разных задач.
//  private val commonBuild = taskKey[Unit]("asdasd")
//  commonBuild := getCommonBuildTaskInitialize().value

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

  // Возвращает реализацию для задачи, которая будет выполнять сборку проекта.
  def getMakeBuildTaskInitialize(): Def.Initialize[Task[Unit]] = {

    Def.task {
      val logger: TaskStreams = streams.value
      val zipFileName = (zipName + "_" + getVersion + ".zip").replace(" ", "_")

      // Удаляем скомпилированные классы.
      clean.value
      logger.log.info("Clean: OK")

      // Удаялем предыдущий архив от сборки.
      val directory: File = new File("..")
      directory.listFiles.foreach(file => {
        if (!file.isDirectory && file.name.contains(zipName) && file.name.contains(".zip")) {
          file.delete
        }
      })
      logger.log.info("Remove previous zip: OK")

      // Создаем новый архив.
      Process(Seq("zip", "-9", "-r", "../" + zipFileName, ".", "-x", "*.git*", "*.idea*", "*zakupay*.zip*")) ! silentBashLogger

      logger.log.info("Make new zip: OK")
      logger.log.info("Version: " + getVersion)
    }
  }

  // Возвращает реализацию для задачи, которая будет выполнять сборку проекта c увеличением номера сборки и созданием
  // якорного коммита.
  def getMakeForwardBuildTaskInitialize(): Def.Initialize[Task[Unit]] = {

    Def.task {

      val logger: TaskStreams = streams.value

      try {
        // Увеличиваем build-номер.
        incBuild
        logger.log.info("Increment build number: OK")

        // Формируем сообщение о выполнении сборки.
        val dateFormatter = new SimpleDateFormat(dateFormat)
        val versionMessage = versionMessageStart + getVersion + dateFormatter.format(Calendar.getInstance().getTime)

        // Записываем сообщение в специальный файл.
        val file = new FileWriter(appVersionFile)
        file.write(versionMessage)
        file.close
        logger.log.info("Write message to version file: OK")


        // Добавляем этот файл в stage.
        Process(Seq("git", "add", appVersionFile)) !

        logger.log.info("Stage version file: OK")


        // Выполняем локальный коммит.
        Process(Seq("git", "commit", "-m", versionMessage, "-q")) !

        logger.log.info("Local commit: OK")


        // Отправляем изменения на сервер.
        Process(Seq("git", "push", "-q")) !

        logger.log.info("Push to server: OK")

        // Удаляем скомпилированные классы.
        clean.value
        logger.log.info("Clean: OK")

        // Удаялем предыдущий архив от сборки.
        val directory: File = new File("..")
        directory.listFiles.foreach(file => {
          if (!file.isDirectory && file.name.contains(zipName) && file.name.contains(".zip")) {
            file.delete
          }
        })
        logger.log.info("Remove previous zip: OK")

        // Создаем новый архив.
        val zipFileName = (zipName + "_" + getVersion + ".zip").replace(" ", "_")
        Process(Seq("zip", "-9", "-r", "../" + zipFileName, ".", "-x", "*.git*", "*.idea*", "*zakupay*.zip*")) ! silentBashLogger

        logger.log.info("Make new zip: OK")
        logger.log.info("Version: " + getVersion)
      }
      catch {
        // В случае ошибки откатываем build-номер.
        case e: Exception => decBuild
      }
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

      // Проверяем существование файла с номером версии.
      val isAppVersionFileExists = Files.exists(Paths.get(appVersionFile))

      if (isAppVersionFileExists) {

        // Извлекаем сообщение о номере версии из файла.
        val versionMessage = ("cat " + appVersionFile !!)

        // Разбиваем сообщение на слова.
        val messageParts = versionMessage.split(" ")

        // Часть x.x.x разбиваем дополнительно.
        val versionNumbers = messageParts(1).split("\\.")

        major = versionNumbers(0).toInt
        minor = versionNumbers(1).toInt
        patch = versionNumbers(2).toInt
        build = messageParts(3).toInt
      }

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
