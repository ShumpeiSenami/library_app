package infra.rdb

import models._
import java.time.LocalDate
import org.scalatest.TryValues
import org.scalatest.fixture.FlatSpec
import scalikejdbc.{SQL, _}
import scalikejdbc.scalatest.AutoRollback
import scala.util.{Success, Try}

class BookRepositoryOnJDBCSpec extends FlatSpec with AutoRollback with TryValues {

  val repository = new BookRepositoryOnJDBC
  config.DBsWithEnv("test").setupAll

  override def fixture(implicit session: FixtureParam): Unit = {
    SQL(
      "INSERT INTO `books` (`id`, `name`, `author`, `published_date`, `description`) VALUES ('test1_id', 'Test Book1', 'Test Author1', '2016-02-04', 'Test Description1')"
    ).update.apply()
    SQL(
      "INSERT INTO `books` (`id`, `name`, `author`, `published_date`, `description`) VALUES ('test2_id', 'Test Book2', 'Test Author2', '2016-02-05', 'Test Description2')"
    ).update.apply()
    SQL(
      "INSERT INTO `books` (`id`, `name`, `author`, `published_date`, `description`) VALUES ('test3_id', 'Test Book3', 'Test Author3', '2016-02-06', 'Test Description3')"
    ).update.apply()
  }

  val expectedBook1 = new Book(
    id = "test1_id",
    name = Name("Test Book1"),
    author = Some(Author("Test Author1")),
    publishedDate = Some(PublishedDate(LocalDate.of(2016, 2, 4))),
    description = Some(Description("Test Description1"))
  )

  val expectedBook2 = new Book(
    id = "test2_id",
    name = Name("Test Book2"),
    author = Some(Author("Test Author2")),
    publishedDate = Some(PublishedDate(LocalDate.of(2016, 2, 5))),
    description = Some(Description("Test Description2"))
  )

  val expectedBook3 = new Book(
    id = "test3_id",
    name = Name("Test Book3"),
    author = Some(Author("Test Author3")),
    publishedDate = Some(PublishedDate(LocalDate.of(2016, 2, 6))),
    description = Some(Description("Test Description3"))
  )

  "findByName()" should "本の名前に該当するレコードをBook型のインスタンスとして取得できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val result = repository.findByName("Test Book1")
    assert(result.success.value.head === expectedBook1)
  }

  "findById()" should "引数のIDに該当するレコードをBook型のインスタンスとして取得できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val result = repository.findById("test1_id")
    assert(result.success.value.head === expectedBook1)
  }

  "searchName()" should "本の名前に部分一致するレコード(3件)を取得できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val result = repository.searchName("Test Book")
    assert(result.success.value.toSet === Set(expectedBook1, expectedBook2, expectedBook3))
  }

  "add()" should "Book型のインスタンスをDBに格納できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val newBook = new Book(
      id = "test4_id",
      name = Name("Test Book4"),
      author = Some(Author("Test Author4")),
      publishedDate = Some(PublishedDate(LocalDate.of(2018, 4, 1))),
      description = Some(Description("Test Description4"))
    )

    val result: Try[Unit] = repository.add(newBook)
    val findRecord        = repository.findById("test4_id")

    assert(result.success === Success(()))
    assert(findRecord.success.value.get === newBook)
  }

  "update()" should "bookレコードを更新できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val updatingBook = new Book(
      id = "test1_id",
      name = Name("Test Book5"),
      author = Some(Author("Test Author5")),
      publishedDate = Some(PublishedDate(LocalDate.of(2017, 4, 1))),
      description = Some(Description("Test Description5"))
    )
    val result: Try[Unit] = repository.update(updatingBook)
    val findRecord        = repository.findById("test1_id")

    assert(result.success === Success(()))
    assert(findRecord.success.value.get === updatingBook)
  }

  "findAll()" should "bookテーブルのレコードを全て取得できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val result: Try[Seq[Book]] = repository.findAll()
    val expectedRecordNumber   = 20
    assert(result.success.value.size === expectedRecordNumber)
  }

  "delete()" should "引数のIDに該当するレコード削除できる" in { implicit session =>
    implicit val ctx = ContextOnJDBC(session)

    val result     = repository.delete("test1_id")
    val findRecord = repository.findByName("test1_id")

    assert(result.success === Success(()))
    assert(findRecord.success.value === Nil)
  }
}
