import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence
import org.apache.uima.jcas.JCas

import java.util.regex.Matcher

import static textractor.AnnotationHelper.*

JCas jcas = (JCas) getProperty('jcas')

Matcher m = (jcas.documentText =~ /([A-Z].+\.)/)
m.each {
	println m.group(1)
	create(jcas, [type:Sentence, begin:m.start(1), end:m.end(1)])
}
