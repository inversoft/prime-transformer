package org.primeframework.transformer.service

import org.primeframework.transformer.domain.TagNode
import spock.lang.Specification

import java.util.function.Predicate
/**
 * @author Daniel DeGroff
 */
class FreeMarkerTransformerSpec extends Specification {

  def "BBCode to HTML - using a predicate"() {

    when:
      def document = new BBCodeParser().buildDocument("[b]bold[/b]No format.[b]bold[/b]")
      def transformedResult = new BBCodeToHTMLTransformer().init().transform(document, new Predicate<TagNode>() {
        @Override
        public boolean test(TagNode tag) {
          return false;
        }
      }, null)

    then: "the transformed result is not transformed"
       transformedResult.result == "[b]bold[/b]No format.[b]bold[/b]"
  }
}
