package org.digitaljudaica.archive.collector.reference

import org.digitaljudaica.archive.collector.Layout
import org.digitaljudaica.reference.{Entity, Name}
import org.digitaljudaica.util.Collections
import org.digitaljudaica.xml.Ops._
import scala.xml.{Elem, Node, Text}

final class Named(
  named: org.digitaljudaica.reference.Named,
  container: Names,
  layout: Layout
) extends ReferenceSource(container) {

  def entity: Entity = named.entity
  def id: String = named.id
  def role: Option[String] = named.role
  def names: Seq[Name] = named.names
  def content: Seq[Elem] = named.content

  override val references: Seq[Reference] = bindReferences(content.flatMap(element =>
    org.digitaljudaica.reference.Reference.all(element)))

  override def isNames: Boolean = true

  override def viewer: String = "namesViewer"

  override def name: String = names.head.name

  override def url: String = layout.namedUrl(id)

  def toListXml: Elem =
    <l><ref target={layout.namedUrl(id)} role="namesViewer">{names.head.toXml}</ref></l>

  def toXml(references: Seq[Reference]): Elem = {
    <named xml:id={id} role={role.orNull}>
      {for (name <- names) yield name.toXml}
      {content.filterNot(Named.isMentions) :+ Named.mentions(
        references.filter(_.ref.get == id),
        <ref target={layout.namedInTheListUrl(id)} role="namesViewer">[...]</ref>
      )}
    </named>
      .copy(label = entity.element)
  }
}

object Named {

  private def isMentions(element: Elem): Boolean =
    (element.label == "p") && element.attributeOption("rendition").contains("mentions")

  private def mentions(references: Seq[Reference], toList: Elem): Elem = {
    val fromNames: Seq[Reference] = references.filter(_.source.isNames)
    val bySource: Seq[(String, Seq[Reference])] =
      (if (fromNames.isEmpty) Seq.empty else Seq((fromNames.head.source.collection.reference, fromNames))) ++
        references.filterNot(_.source.isNames).groupBy(_.source.collection.reference).toSeq.sortBy(_._1)

    <p rendition="mentions">
      {toList}
      {for ((source, references) <- bySource) yield <l>{
        Seq[Node](Text(source + ": ")) ++
          (for (ref <- Collections.removeConsecutiveDuplicates(references.map(_.source)))
            yield <ref target={ref.url} role={ref.viewer}>{ref.name}</ref>)
        }</l>
      }</p>
  }
}
