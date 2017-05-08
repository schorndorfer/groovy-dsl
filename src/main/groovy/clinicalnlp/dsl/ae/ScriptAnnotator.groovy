package clinicalnlp.dsl.ae

import clinicalnlp.dsl.DSL
import com.google.common.base.Charsets
import com.google.common.io.Resources
import groovy.util.logging.Log4j
import org.apache.uima.UimaContext
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.jcas.JCas
import org.apache.uima.resource.ResourceInitializationException
import org.codehaus.groovy.control.CompilerConfiguration

@Log4j
class ScriptAnnotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    public static final String PARAM_SCRIPT_FILE = 'scriptFileName'
    public static final String PARAM_BINDING_SCRIPT_FILE = 'bindingScriptFileName'

    private Script script

    @ConfigurationParameter(name = 'scriptFileName', mandatory = true,
        description = 'File holding Groovy script')
    private String scriptFileName

    @ConfigurationParameter(name = 'bindingScriptFileName', mandatory = false,
        description = 'File holding Groovy script for bindings')
    private String bindingScriptFileName

    @Override
    void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext)
        Class.forName('clinicalnlp.dsl.DSL')
        CompilerConfiguration config = new CompilerConfiguration()
        config.setScriptBaseClass(DSL.canonicalName)
        GroovyShell shell = new GroovyShell(config)

        try {
            log.info "Loading groovy config file: ${scriptFileName}"
            URL url = Resources.getResource(scriptFileName)
            String scriptContents = Resources.toString(url, Charsets.UTF_8)
            this.script = shell.parse(scriptContents)
            if (bindingScriptFileName) {
                log.info "Loading groovy config binding file: ${bindingScriptFileName}"
                Script bindingsScript = shell.parse(Resources.toString(
                    Resources.getResource(bindingScriptFileName), Charsets.UTF_8))
                bindingsScript.setProperty('context', context)
                this.script.setBinding(new Binding(bindingsScript.run()))
            }

        } catch (IOException e) {
            throw new ResourceInitializationException()
        }
    }

    @Override
    void process(JCas aJCas) {
        this.script.setProperty('jcas', aJCas)
        this.script.run()
    }
}
