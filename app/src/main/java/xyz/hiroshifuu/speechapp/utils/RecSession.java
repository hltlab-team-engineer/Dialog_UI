package xyz.hiroshifuu.speechapp.utils;

import java.io.IOException;

import xyz.hiroshifuu.speechapp.commons.NotAvailableException;
import xyz.hiroshifuu.speechapp.messages.RecSessionResult;

public interface RecSession {

    void create() throws IOException, NotAvailableException;

    void sendChunk(byte[] bytes, boolean isLast) throws IOException;

    String getCurrentResult() throws IOException;

    RecSessionResult getResult() throws IOException;

    boolean isFinished();

    void cancel();
}