package net.fwbrasil.activate.entity

import net.fwbrasil.activate.util.uuid.UUIDUtil
import scala.collection.mutable.{ Map => MutableMap }
import net.fwbrasil.activate.util.Reflection.toRichClass
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.HashMap

object EntityHelper {

	private[this] val entitiesMetadatas =
		MutableMap[String, EntityMetadata]()

	private[this] val concreteEntityClasses =
		MutableMap[Class[_ <: Entity], List[Class[Entity]]]()

	def clearMetadatas = {
		entitiesMetadatas.clear
		concreteEntityClasses.clear
	}

	def metadatas =
		entitiesMetadatas.values.toList.sortBy(_.name)

	def allConcreteEntityClasses =
		concreteEntityClasses.values.flatten.toSet

	def concreteClasses[E <: Entity](clazz: Class[E]) =
		concreteEntityClasses.getOrElseUpdate(clazz, {
			for (
				(hash, metadata) <- entitiesMetadatas;
				if ((clazz == metadata.entityClass || clazz.isAssignableFrom(metadata.entityClass)) && metadata.entityClass.isConcreteClass)
			) yield metadata.entityClass
		}.toList.asInstanceOf[List[Class[Entity]]])

	def initialize(referenceClass: Class[_]): Unit =
		synchronized {
			UUIDUtil.generateUUID
			for (entityClass <- EntityEnhancer.enhancedEntityClasses(referenceClass))
				if (!entityClass.isInterface()) {
					val entityClassHashId = getEntityClassHashId(entityClass)
					val entityName = getEntityName(entityClass)
					entitiesMetadatas += (entityClassHashId -> new EntityMetadata(entityName, entityClass))
				}
		}

	private def getEntityMetadataOption(clazz: Class[_]) =
		entitiesMetadatas.get(getEntityClassHashId(clazz))

	def getEntityMetadata(clazz: Class[_]) =
		getEntityMetadataOption(clazz).get

	// Example ID (45 chars)
	// e1a59a08-7c5b-11e1-91c3-73362e1b7d0d-9a70c810
	// |---------------UUID---------------| |-hash-|
	// 0                                 35 37    44

	private val entityIdClassHashPattern =
		"[0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12}-([0-9a-z]{8})".r

	private def classHashIdsCache = new HashMap[Class[_], String]() with SynchronizedMap[Class[_], String]

	def getEntityClassHashId(entityClass: Class[_]): String =
		classHashIdsCache.getOrElseUpdate(
			entityClass, getEntityClassHashId(getEntityName(entityClass)))

	def getEntityClassFromId(entityId: String) =
		getEntityClassFromIdOption(entityId).get

	def getEntityClassFromIdOption(entityId: String) =
		entityId match {
			case entityIdClassHashPattern(hash) =>
				entitiesMetadatas.get(hash).map(_.entityClass)
			case other =>
				None
		}

	private def getEntityClassHashId(entityName: String): String =
		normalizeHex(Integer.toHexString(entityName.hashCode))

	private def normalizeHex(hex: String) = {
		val length = hex.length
		if (length == 8)
			hex
		else if (length < 8)
			hex + (for (i <- 0 until (8 - length)) yield "0").mkString("")
		else
			hex.substring(0, 8)
	}

	def getEntityName(entityClass: Class[_]) = {
		val alias = entityClass.getAnnotation(classOf[Alias])
		if (alias != null)
			alias.value
		else {
			entityClass.getSimpleName
		}
	}

}
