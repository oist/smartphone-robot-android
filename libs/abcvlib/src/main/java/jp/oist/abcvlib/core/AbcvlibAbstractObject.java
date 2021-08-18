package jp.oist.abcvlib.core;

import jp.oist.abcvlib.core.inputs.Inputs;

/**
 * An interface to bundle both AbcvlibActivity and AbcvlibService into one class. Mainly used
 * as a means to start an AbcvlibLooper object and pass a reference to either a AbcvlibActivity or
 * AbcvlibService
 */
public interface AbcvlibAbstractObject {
    Inputs getInputs();
}
