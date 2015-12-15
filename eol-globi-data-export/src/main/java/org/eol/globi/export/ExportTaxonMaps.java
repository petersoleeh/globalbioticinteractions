package org.eol.globi.export;

import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class ExportTaxonMaps implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) {
        String query = "START study = node:studies('*:*')\n" +
                "MATCH study-[:COLLECTED]->specimen-[:ORIGINALLY_DESCRIBED_AS]->origTaxon, specimen-[:CLASSIFIED_AS]->taxon " +
                "WITH distinct(origTaxon.name) as origName, origTaxon.externalId? as origId, taxon " +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon " +
                "RETURN origId as providedTaxonId" +
                ", origName as providedTaxonName" +
                ", linkedTaxon.externalId? as resolvedTaxonId" +
                ", linkedTaxon.name? as resolvedTaxonName";

        HashMap<String, Object> params = new HashMap<String, Object>();

        ExportUtil.writeResults(writer, study.getUnderlyingNode().getGraphDatabase(), query, params, true);
    }
}
