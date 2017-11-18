package textractor

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token
import groovy.util.logging.Log4j
import org.apache.log4j.Level
import org.apache.uima.analysis_engine.AnalysisEngine
import org.apache.uima.analysis_engine.AnalysisEngineProcessException
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.factory.AggregateBuilder
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.tcas.Annotation
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import java.util.regex.Matcher

import static AnnotationHelper.*
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline

@Log4j
class AnnotationHelperTest {
    static class NamedEntityMentionMatcher extends JCasAnnotator_ImplBase {
        @Override
        void process(JCas jCas) throws AnalysisEngineProcessException {
            Matcher matcher = jCas.documentText =~ /([A-Z].+\.)/
            matcher.each {
                Sentence sent = new Sentence(jCas)
                sent.begin = matcher.start(1)
                sent.end = matcher.end(1)
                sent.addToIndexes()
            }

            matcher = jCas.documentText =~ /(?i)(pneumonia|fever|cough|sepsis|weakness)/
            matcher.each {
                NamedEntity nem = new NamedEntity(jCas)
                nem.begin = matcher.start(1)
                nem.end = matcher.end(1)
                nem.addToIndexes()
            }
        }
    }

    @BeforeClass
    static void setupClass() {
    }

    AnalysisEngine engine

    @Before
    void setUp() throws Exception {
        log.setLevel(Level.INFO)
        AggregateBuilder builder = new AggregateBuilder()
        builder.with {
            add(createEngineDescription(NamedEntityMentionMatcher))
        }
        this.engine = builder.createAggregate()
    }

    @Test void testJCasCreate() {
        // -------------------------------------------------------------------
        // run pipeline to generate annotations
        // -------------------------------------------------------------------
        def text = """
Patient has fever but no cough and pneumonia is ruled out.
The patient does not have pneumonia or sepsis.
        """
        JCas jcas = engine.newJCas()
        Sentence sent = AnnotationHelper.create(jcas, [type:Sentence, begin:0, end:text.length()])
        JCas jcas2 = sent.getCAS().getJCas()
        assert jcas == jcas2
        Collection<Sentence> sents = JCasUtil.select(jcas, Sentence)
        assert sents.size() == 1
    }

    @Test void testJCasSelect() {
        // -------------------------------------------------------------------
        // run pipeline to generate annotations
        // -------------------------------------------------------------------
        def text = """\
        Patient has fever but no cough and pneumonia is ruled out.
        There is no increase in weakness.
        Patient does not have measles.
        """
        JCas jcas = engine.newJCas()
        jcas.setDocumentText(text)
        runPipeline(jcas, engine)

        // -------------------------------------------------------------------
        // test the results by selecting annotations with
        // miscellaneous filter arguments
        // -------------------------------------------------------------------
        assert select(jcas, [type:NamedEntity]).size() == 4

        assert select(jcas,
            [type:Sentence, filter:not(contains(NamedEntity))]).size() == 1

        assert select(jcas,
            [type:Sentence, filter:and({it.coveredText.startsWith('Patient')},
                        {it.coveredText.endsWith('out.') })]).size() == 1

        def sentsWithMentions = select(jcas,
            [type:Sentence, filter:contains(NamedEntity)])

        assert sentsWithMentions.size() == 2

        assert select(jcas,
            [type:NamedEntity, filter:coveredBy(sentsWithMentions[0])]).size() == 3

        assert select(jcas,
            [type:NamedEntity, filter:not(coveredBy(sentsWithMentions[0]))]).size() == 1

        assert select(jcas,
            [type:NamedEntity, filter:between(0, 60)]).size() == 3

        assert select(jcas,
            [type:NamedEntity, filter:before(60)]).size() == 3

        assert select(jcas,
            [type:NamedEntity, filter:after(60)]).size() == 1
    }


    @Test void testApplyPattern() {
        // -------------------------------------------------------------------
        // run pipeline to generate annotations
        // -------------------------------------------------------------------
        def text = """\
        Patient has fever but no cough and pneumonia is ruled out.
        There is no increase in weakness.
        Patient does not have measles.
        """
        JCas jcas = engine.newJCas()
        jcas.setDocumentText(text)
        runPipeline(jcas, engine)

        Collection<Annotation> sents = select(jcas, [type:Sentence])
        def pattern1 = ~/Patient/
        def pattern2 = ~/There|has|but/
        applyPatterns(
            searchSet:sents,
            patterns:[pattern1, pattern2],
            action: { AnnotationMatchResult m ->
                create(jcas, [type:Token, begin:m.start(0), end:m.end(0)]) }
        )
        Collection<Annotation> tokens = select(jcas, [type:Token])
        assert tokens.size() == 5
        assert tokens[0].coveredText == 'Patient'
        assert tokens[1].coveredText == 'has'
        assert tokens[2].coveredText == 'but'
        assert tokens[3].coveredText == 'There'
        assert tokens[4].coveredText == 'Patient'
    }
}
