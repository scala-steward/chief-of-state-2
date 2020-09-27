package com.namely.chiefofstate

import com.google.protobuf.any.Any
import com.namely.protobuf.chiefofstate.v1.common.{MetaData => CosMetaData}
import io.superflat.lagompb.protobuf.v1.core.MetaData

/**
 * Utility methods
 */
object Util {

  /**
   * Extracts the proto message package name
   *
   * @param proto the protocol buffer message
   * @return string
   */
  def getProtoFullyQualifiedName(proto: Any): String = {
    proto.typeUrl.split('/').lastOption.getOrElse("")
  }

  /**
   * Converts the lagom-pb MetaData class to the chief-of-state MetaData
   *
   * @param metaData
   * @return
   */
  def toCosMetaData(metaData: MetaData): CosMetaData = {
    CosMetaData(
      entityId = metaData.entityId,
      // TODO: remove .toInt
      revisionNumber = metaData.revisionNumber.toInt,
      revisionDate = metaData.revisionDate,
      data = metaData.data
    )
  }
}