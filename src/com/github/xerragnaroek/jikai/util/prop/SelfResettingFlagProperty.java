package com.github.xerragnaroek.jikai.util.prop;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A BooleanProperty that resets back to false after running all consumer.
 *
 * @author XerRagnaroek
 */
public class SelfResettingFlagProperty extends BooleanProperty {

    public SelfResettingFlagProperty(boolean val) {
        super(val);
    }

    public SelfResettingFlagProperty() {
        super();
    }

    @Override
    protected void runConsumer(Boolean oldV, Boolean newV) {
        super.runConsumer(oldV, newV);
        if (newV) {
            value = false;
        }
    }

    public void onSet(Runnable run) {
        onChangeToTrue(run);
    }

    @JsonCreator
    public static SelfResettingFlagProperty of(boolean val) {
        return new SelfResettingFlagProperty(val);
    }

    public void setFlag() {
        set(true);
    }
}
