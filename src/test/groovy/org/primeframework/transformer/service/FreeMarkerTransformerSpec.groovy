package org.primeframework.transformer.service

import org.primeframework.transformer.domain.TagNode
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Predicate
/**
 * @author Daniel DeGroff
 */
class FreeMarkerTransformerSpec extends Specification {

  /**
   * No lambdas until Groovy v3.
   */
  @Shared
  def doNotTransform = new Predicate<TagNode>() {
    @Override
    public boolean test(TagNode tag) {
      return false;
    }
  }

  def "BBCode to HTML - using a predicate"() {

    when:
      def document = new BBCodeParser().buildDocument("[b]bold[/b]No format.[b]bold[/b]")
      def result = new BBCodeToHTMLTransformer().init().transform(document, doNotTransform, null)

    then: "the transformed result is not transformed"
       result == "[b]bold[/b]No format.[b]bold[/b]"
  }
}
