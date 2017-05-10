package clinicalnlp.dsl

import org.apache.uima.cas.text.AnnotationFS
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.cas.TOP
import org.apache.uima.jcas.tcas.Annotation

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.apache.uima.fit.util.JCasUtil.selectCovered

class DSL extends Script {

    @Override
    Object run() {
        super.run()
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Static initialization
    // -----------------------------------------------------------------------------------------------------------------

    static {
        // -------------------------------------------------------------------------------------------------------------
        // Extend JCas class with create function
        // -------------------------------------------------------------------------------------------------------------
        JCas.metaClass.create = { Map attrs ->
            TOP a = attrs.type.newInstance(getDelegate())
            attrs.each { k, v ->
                if (a.metaClass.hasProperty(a, k)) {
                    if (k != 'type') {
                        a."${k}" = v
                    }
                }
            }
            a.addToIndexes()
            return a
        }

        // -------------------------------------------------------------------------------------------------------------
        // Extend JCas class with select function
        // -------------------------------------------------------------------------------------------------------------
        JCas.metaClass.select = { Map args ->
            Class type = args.type
            Closure filter = args.filter
            Collection<AnnotationFS> annotations = (type != null ? JCasUtil.select(getDelegate(), type) :
                    JCasUtil.selectAll(getDelegate()))
            if (filter) {
                Collection<Annotation> filtered = []
                annotations.each {
                    if (filter.call(it) == true) { filtered << it }
                }
                annotations = filtered
            }
            return annotations
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Create an annotation with specified attributes and add to indexes
     */
    static create = { JCas jcas, Map args ->
        TOP a = args.type.newInstance(jcas)
        args.each { k, v ->
            if (a.metaClass.hasProperty(a, k)) {
                if (k != 'type') {
                    a."${k}" = v
                }
            }
        }
        a.addToIndexes()
        return a
    }

    /**
     * Select an annotation with specified attributes
     */
    static select = { JCas jcas, Map args ->
        Class type = args.type
        Closure filter = args.filter
        Collection<AnnotationFS> annotations = (type != null ? JCasUtil.select(jcas, type) :
            JCasUtil.selectAll(jcas))
        if (filter) {
            Collection<Annotation> filtered = []
            annotations.each {
                if (filter.call(it) == true) { filtered << it }
            }
            annotations = filtered
        }
        return annotations
    }

    /**
     * Removed covered annotations
     */
    static removeCovered = { JCas jcas, Map args ->
        Collection<Annotation> anns = args.anns
        Collection<Class<? extends Annotation>> removeTypes = args.types
        Comparator<Annotation> comparator = args.comparator

        // first remove duplicate annotations, picking one (at random) to keep
        if (comparator == null) { comparator = new AnnotationComparator() }
        TreeSet<Annotation> uniques = new TreeSet<Annotation>(comparator)
        anns.each { Annotation ann ->
            uniques.add(ann)
        }

        // next, remove annotations that are embedded inside other annotations
        Collection<Annotation> embedded = []
        uniques.each { Annotation ann ->
            removeTypes.each { Class type ->
                embedded.addAll(JCasUtil.selectCovered(jcas, type, ann))
            }
        }
        embedded.each {
            it.removeFromIndexes()
        }
    }

    /**
     * Apply a set of regex patterns to a collection of annotations. For each match, apply
     * the specified closure action.
     */
    static applyPatterns = { Map args ->
        Collection<Annotation> searchSet = args.searchSet
        Collection<Pattern> patterns = args.patterns
        Closure action = args.action
        searchSet.each { ann ->
            patterns.each { p ->
                AnnotationMatchResult m = new AnnotationMatchResult(p, ann)
                m.each { action.call(m) }
            }
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Filter predicates
    // -----------------------------------------------------------------------------------------------------------------

    static not = { Closure pred ->
        { TOP ann ->
            !pred.call(ann)
        }
    }

    static and = { Closure... preds ->
        { TOP ann ->
            for (Closure pred : preds) {
                if (pred.call(ann) == false) { return false }
            }
            true
        }
    }

    static or = { Closure... preds ->
        { TOP ann ->
            for (Closure pred : preds) {
                if (pred.call(ann) == true) { return true }
            }
            false
        }
    }

    static contains = { Class<? extends Annotation> type ->
        { Annotation ann ->
            selectCovered(ann.CAS.getJCas(), type, ann).size() > 0;
        }
    }

    static coveredBy = { Annotation coveringAnn ->
        { Annotation ann ->
            (ann != coveringAnn &&
                    coveringAnn.begin <= ann.begin &&
                    coveringAnn.end >= ann.end)
        }
    }

    static between = { Integer begin, Integer end ->
        { Annotation ann ->
            (begin <= end && begin <= ann.begin && end >= ann.end)
        }
    }

    static before = { Integer index ->
        { Annotation ann ->
            ann.end < index
        }
    }

    static after = { Integer index ->
        { Annotation ann ->
            ann.begin > index
        }
    }
}