package org.eol.globi.data;

import java.util.Map;

public interface InteractionListener {
    void newLink(Map<String, String> link) throws StudyImporterException;
}
