package jp.oist.abcvlib.core;

public interface IOReadyListener {
    /**
     * Called by abcvlibActivity after both Inputs and Output objects have been created. Implement
     * this method in your MainActivity and put any code that uses the outputs or inputs there so as
     * to ensure no null pointers.
     */
    void onIOReady();
}
