package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.mysql.database.MysqlDeployDatabaseQueryBuilder
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.sql.SqlStreamingAction

import scala.concurrent.{ExecutionContext, Future}

case class MysqlClientDbQueries(project: Project, clientDatabase: Database)(implicit ec: ExecutionContext) extends ClientDbQueries {

  def existsByModel(modelName: String): Future[Boolean] = {
    val query = MysqlDeployDatabaseQueryBuilder.existsByModel(project.id, modelName)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsByRelation(relationId: String): Future[Boolean] = {
    val query = MysqlDeployDatabaseQueryBuilder.existsByRelation(project.id, relationId)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsDuplicateByRelationAndSide(relationId: String, relationSide: RelationSide): Future[Boolean] = {
    val query = MysqlDeployDatabaseQueryBuilder.existsDuplicateByRelationAndSide(project.id, relationId, relationSide)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field match {
      case f: ScalarField   => MysqlDeployDatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, f.name)
      case f: RelationField => MysqlDeployDatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, f)
    }
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean] = {
    val query = MysqlDeployDatabaseQueryBuilder.existsDuplicateValueByModelAndField(project.id, model.name, field.name)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = MysqlDeployDatabaseQueryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]
}
