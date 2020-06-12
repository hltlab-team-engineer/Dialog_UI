package xyz.hiroshifuu.speechapp.messages;

import java.util.List;

import xyz.hiroshifuu.speechapp.commons.Hypothesis;

public interface RecSessionResult {

    /**
     * <p>Returns a flat list of linearizations where
     * the information about which hypothesis produced
     * the linearizations and what is the language of
     * the linearization is not preserved.</p>
     *
     * <p>The implementation MUST return a (possibly empty) list
     * which MUST NOT contain empty <code>String</code>s.
     * <code>null</code> is not allowed as a return value.</p>
     *
     * @return (flat) list of linearizations
     */
    public List<String> getLinearizations();


    /**
     * <p>The implementation MUST return a (possibly empty) list
     * which MUST NOT contain empty <code>String</code>s
     * <code>null</code> is not allowed as a return value.</p>
     *
     * @return list of utterances
     */
    public List<String> getUtterances();

    public List<Hypothesis> getHypotheses();

}