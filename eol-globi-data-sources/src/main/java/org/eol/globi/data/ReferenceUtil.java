package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceUtil {
    private static final Log LOG = LogFactory.getLog(ReferenceUtil.class);

    public static Map<String, String> buildRefMap(ParserFactory parserFactory, String referencePath) throws StudyImporterException {
        return buildRefMap(parserFactory, referencePath, "short", "full", ',');
    }

    protected static Map<String, String> buildRefMap(ParserFactory parserFactory, String referencePath, String keyColumnName, String valueColumnName, char delimiter) throws StudyImporterException {
        Map<String, String> refMap = new TreeMap<String, String>();
        try {
            LabeledCSVParser referenceParser = parserFactory.createParser(referencePath, CharsetConstant.UTF8);
            referenceParser.changeDelimiter(delimiter);
            while (referenceParser.getLine() != null) {
                String shortReference = referenceParser.getValueByLabel(keyColumnName);
                if (StringUtils.isBlank(shortReference)) {
                    LOG.warn("missing short reference on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                } else {
                    String fullReference = referenceParser.getValueByLabel(valueColumnName);
                    if (StringUtils.isBlank(fullReference)) {
                        LOG.warn("missing full reference for [" + shortReference + "] on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                        fullReference = shortReference;
                    }
                    if (StringUtils.isBlank(refMap.get(StringUtils.trim(shortReference)))) {
                        refMap.put(StringUtils.trim(shortReference), StringUtils.trim(fullReference));
                    } else {
                        LOG.warn("skipping [" + shortReference + "] on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]: key already defined.");
                    }

                }

            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + referencePath + "]", e);
        }
        return refMap;
    }

    public static String createLastAccessedString(String reference) {
        return "Accessed at <" + StringUtils.trim(reference) + "> on " + new DateTime().toString("dd MMM YYYY") + ".";
    }

    public static String sourceCitationLastAccessed(Dataset dataset, String sourceCitation) {
        String resourceURI = dataset.getOrDefault("url", dataset.getArchiveURI().toString());
        return StringUtils.trim(sourceCitation) + separatorFor(sourceCitation) + createLastAccessedString(resourceURI);
    }

    public static String sourceCitationLastAccessed(Dataset dataset) {
        return sourceCitationLastAccessed(dataset, dataset.getCitation());
    }

    private static String separatorFor(String citationPart) {
        String separator = " ";
        if (!StringUtils.endsWith(StringUtils.trim(citationPart), ".")) {
            separator = " . ";
        }
        return separator;
    }
}
