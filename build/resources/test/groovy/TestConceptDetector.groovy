import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
import org.apache.uima.jcas.JCas

import java.util.regex.Matcher

JCas jcas = (JCas) getProperty('jcas')

Matcher matcher = jcas.documentText =~ getProperty('diseasePattern')
matcher.each {
    NamedEntity nem = new NamedEntity(jcas)
    nem.begin = matcher.start(1)
    nem.end = matcher.end(1)
    nem.addToIndexes()
}



