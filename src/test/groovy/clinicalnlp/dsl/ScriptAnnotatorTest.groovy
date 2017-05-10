package clinicalnlp.dsl

import clinicalnlp.dsl.ScriptAnnotator
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence
import org.apache.uima.analysis_engine.AnalysisEngine
import org.apache.uima.fit.factory.AggregateBuilder
import org.apache.uima.fit.factory.AnalysisEngineFactory
import org.apache.uima.fit.pipeline.SimplePipeline
import org.apache.uima.jcas.JCas
import org.junit.Test

class ScriptAnnotatorTest {

    @Test
    void smokeTest() {
        def text = """\
        Patient has fever but no cough and pneumonia is ruled out.
        There is no increase in weakness.
        Patient does not have measles.
        """

        AggregateBuilder builder = new AggregateBuilder()
        builder.with {
            add(AnalysisEngineFactory.createEngineDescription(ScriptAnnotator,
                    ScriptAnnotator.PARAM_SCRIPT_FILE,
                    'groovy/TestSentenceDetector.groovy'))
        }
        AnalysisEngine engine = builder.createAggregate()
        JCas jcas = engine.newJCas()
        jcas.setDocumentText(text)
        SimplePipeline.runPipeline(jcas, engine)
        assert jcas.select(type:Sentence).size() == 3

        builder = new AggregateBuilder()
        builder.with {
            add(AnalysisEngineFactory.createEngineDescription(ScriptAnnotator,
                    ScriptAnnotator.PARAM_BINDING_SCRIPT_FILE,
                    'groovy/TestBindingScript.groovy',
                    ScriptAnnotator.PARAM_SCRIPT_FILE,
                    'groovy/TestConceptDetector.groovy'))
        }
        engine = builder.createAggregate()
        jcas = engine.newJCas()
        jcas.setDocumentText(text)
        SimplePipeline.runPipeline(jcas, engine)
        assert jcas.select(type:NamedEntity).size() == 5
    }
}
