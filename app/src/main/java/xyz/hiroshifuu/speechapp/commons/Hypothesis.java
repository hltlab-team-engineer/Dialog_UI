package xyz.hiroshifuu.speechapp.commons;

import java.util.List;

import xyz.hiroshifuu.speechapp.messages.Linearization;

public class Hypothesis {
    private final String mUtterance;
    private final List<Linearization> mLinearizations;

    public Hypothesis(String utterance, List<Linearization> linearizations) {
        mUtterance = utterance;
        mLinearizations = linearizations;
    }

    public String getUtterance() {
        return mUtterance;
    }

    public List<Linearization> getLinearizations() {
        return mLinearizations;
    }
}
