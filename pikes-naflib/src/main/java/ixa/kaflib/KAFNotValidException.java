package ixa.kaflib;

import java.io.IOException;

public class KAFNotValidException extends IOException {

    private static final String commonMsg = "Input KAF document is not valid.";

    public KAFNotValidException(String msg) {
	super(commonMsg + " " + msg);
    }

}
