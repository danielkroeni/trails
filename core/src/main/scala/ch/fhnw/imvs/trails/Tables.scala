package ch.fhnw.imvs.trails

import scala.language.existentials
import reflect.ClassTag
import java.sql.{DriverManager, Connection, ResultSet}

trait Tables { self: TrailsPrimitives with Trails =>

//  case class Named(name: String, tag: ClassTag[_])
//
//  /** Maps names to generated subpaths */
//  type NameMap = Map[Named, List[Any]]
//
//
//  def namedSubPath(keyName: String, tr: Tr[Env,State[Any],State[Any],Any]): Tr[Env,State[Any],State[Any],Any] =
//    name[List[Elem]](keyName, tr)((p: List[Elem]) => p)
//
//  /** Returns a traverser which stores the generated sub-paths in a map with the given name as the key.
//    * @param name the name under which the sub-path is stored in the map
//    * @param tr the traverser
//    * @return a traverser which stores the generated sub-paths in a map
//    */
//  def name[A: ClassTag](name: String, tr: Traverser)(select: List[Elem] => A = (x: List[Elem]) => x.asInstanceOf[A]): Traverser =
//    env => ts => {
//      val (trace, state) = ts
//      val size = trace.path.size
//      tr(env)(ts).map { case (State(path, visitedPaths, _), namedSubPaths) =>
//        val currentEvaluation = path.take(path.size - size)
//        val key = Named(name, implicitly[ClassTag[A]])
//        val updated = namedSubPaths.updated(key, select(currentEvaluation) :: namedSubPaths.getOrElse(key, Nil))
//        (Trace(path, visitedPaths), updated)
//      }
//    }

  def pathTable[I<:Elem,O<:Elem,A](tableName: String, tr: Tr[Env,State[I],State[O],A]): Env => Connection => Connection = env => connection => {
    val travRes = Traverser.runAll(tr,env).map(_._1)
    val tab = ScalaTable(travRes)
    //println("headers: " + tab.headers.map(h => h.name + " " + h.tag.runtimeClass.getSimpleName))

    def tagToSqlType(tt: ClassTag[_]): String = tt match {
      case tt if tt == ClassTag[String](classOf[String]) => "varchar(100)"
      case tt if tt == ClassTag.Int => "integer"
      case tt => "varchar(100)" // throw new IllegalArgumentException("Unknown type: " + tt)
    }

    def prepareTable(table: ScalaTable): Connection = {
      val meta = table.headers
      val data = table.rows
      val schemaStmt = connection.createStatement()

      //val schemaSql = s"create memory table $tableName(${meta.map(m => m.name + " " + tagToSqlType(m.tag)).mkString(", ")})"
      val schemaSql = s"create memory table $tableName(${meta.map(m => m + " varchar(100)").mkString(", ")})"
      println(schemaSql)
      schemaStmt.executeUpdate(schemaSql)

      //val sql = s"insert into $tableName (${meta.map(_.name).mkString(", ")}) values (${meta.map(_ => "?").mkString(", ")})"
      val sql = s"insert into $tableName (${meta.mkString(", ")}) values (${meta.map(_ => "?").mkString(", ")})"
      println(sql)
      val ps = connection.prepareStatement(sql)

      //val tags = meta.map(_.tag)

//      for (d <- data) {
//        for (((t,v), i) <- tags.zip(d).zipWithIndex) {
//          t match {
//            case tt if tt == ClassTag[String](classOf[String]) => ps.setString(i + 1, v.headOption.map(_.toString).getOrElse(null) )
//            case tt if tt == ClassTag.Int => ps.setObject(i + 1, v.headOption.map(_.asInstanceOf[Int]).getOrElse(null))
//            case tt=> ps.setString(i + 1, v.headOption.map(_.toString.take(100)).getOrElse(null) ) // throw new IllegalArgumentException("Unknown type: " + t)
//          }
//        }
//        ps.addBatch()
//      }

      for(row <- data) {
        for{(field,i) <- row.zipWithIndex} {
          ps.setString(i+1, field.headOption.map(_.toString().take(100)).getOrElse(null))
        }
        ps.addBatch()
      }

      ps.executeBatch()
      ps.close()

      connection
    }
    prepareTable(tab)
  }

  def from(first: Env => Connection => Connection, inMemDb: (Env => Connection => Connection)*): Env => Connection = env => {
    Class.forName("org.hsqldb.jdbc.JDBCDriver")
    val connection = DriverManager.getConnection("jdbc:hsqldb:mem:dsl;shutdown=true","SA","")
    inMemDb.foldLeft(first(env)(connection))((fs, f) => f(env)(fs))
  }

  def processTable[T](dbF: Env => Connection, query: String, resultF: ResultSet => T): Env => T = env => {
    val conn = dbF(env)
    val stmt =  conn.createStatement()
    val rs = stmt.executeQuery(query)
    val res = resultF(rs)

    rs.close()
    stmt.close()
    conn.close()

    res
  }

  implicit class ConnectionSyntax(dbf: Env => Connection) {
    def extract[T](query: String)(resultF: ResultSet => T ): Env => T = processTable[T](dbf, query, resultF)
  }

  implicit class TablesSyntax[I<:Elem,O<:Elem,A](t1: Tr[Env,State[I],State[O],A]) {
    // Much better idea: Use "AST construction style" like scala parser combinators ^^ t1 ~ t2 => t1.prop("a")
//    def as(n: String): Traverser = namedSubPath(n, t1)

    def asTable(name: String): Env => Connection => Connection = pathTable(name, t1)
//    def run(e: Env) = t1(e)((State(Nil,Set(),Map()),Map()))
  }

  def printResultSet(result: ResultSet) {
    val meta = result.getMetaData
    val colCount = meta.getColumnCount

    val columnNames = for(i <- (1 to colCount).toList) yield meta.getColumnLabel(i) //TODO alias names
    println(columnNames.mkString(" | "))

    val builder = List.newBuilder[IndexedSeq[Any]]
    while(result.next()) {
      builder += (for (i <- 1 to colCount) yield (result.getString(i)))
    }

    for (d <- builder.result()) {
      println(d.mkString(" | "))
    }
  }

  /**
   * A table representation of the named parts of a Stream of Traces.
   * @param traces the traces
   */
  case class ScalaTable(private val traces: Stream[State[Elem]]) {

    val headers: Vector[String] = traces.foldLeft(Set[String]())((acc, t) => acc ++ t.labels.keys).toVector.sorted
    def rows: Vector[Vector[List[List[Elem]]]] = traces.toVector.map(t => headers.map(h => t.labels(h)))

    def pretty: String = {
      val colls = for((name, index) <- headers.zipWithIndex ) yield {
        val col = rows.map(row => row(index).toString())
        val maxSize = col.map(_.size).max
        (name, maxSize, col)
      }

      val headerLine = colls.map { case (named, maxSize, _) => named.padTo(maxSize, " ").mkString }
      val data = for(i <- 0 until rows.size) yield {
        colls.map {case (name, maxSize, col) => col(i).padTo(maxSize, " ").mkString }
      }
      val separatorLine = colls.map { case (name, maxSize, _) => "-" * maxSize }
      (separatorLine +: headerLine +: separatorLine +: data :+ separatorLine).map(_.mkString("|","|","|")).mkString("\n")
    }
  }
}