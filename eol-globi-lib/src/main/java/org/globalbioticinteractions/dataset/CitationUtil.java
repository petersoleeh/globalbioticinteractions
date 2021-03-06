package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class CitationUtil {
    static final String ZENODO_URL_PREFIX = "https://zenodo.org/record/";
    private static final Log LOG = LogFactory.getLog(CitationUtil.class);

    public static String createLastAccessedString(URI reference) {
        return createLastAccessedString(reference.toString());
    }

    public static String createLastAccessedString(String reference) {
        return "Accessed at <" + StringUtils.trim(reference) + "> on " + new DateTime().toString("dd MMM YYYY") + ".";
    }

    public static String sourceCitationLastAccessed(Dataset dataset, String sourceCitation) {
        String archiveURIString = dataset.getArchiveURI().toString();
        String resourceURIString = dataset.getOrDefault("url", archiveURIString);
        URI resourceURI = null;
        try {
            resourceURI = new URI(resourceURIString);
        } catch (URISyntaxException e) {
            //
        }
        if (resourceURI == null || !resourceURI.isAbsolute()) {
            resourceURI = dataset.getArchiveURI();
        }

        return StringUtils.trim(sourceCitation) + separatorFor(sourceCitation) + createLastAccessedString(resourceURI.toString());
    }

    public static String sourceCitationLastAccessed(Dataset dataset) {
        return sourceCitationLastAccessed(dataset, dataset.getCitation());
    }

    static String separatorFor(String citationPart) {
        String separator = " ";
        if (StringUtils.isNotBlank(citationPart) && !StringUtils.endsWith(StringUtils.trim(citationPart), ".")) {
            separator = ". ";
        }
        return separator;
    }

    public static String citationFor(Dataset dataset) {
        String fallbackCitation = dataset.getArchiveURI() == null
                ? dataset.getNamespace()
                : dataset.getArchiveURI().toString();

        String defaultCitation = "<" + fallbackCitation + ">";
        String citation = citationOrDefaultFor(dataset, defaultCitation);

        if (!StringUtils.contains(citation, "doi.org") && !StringUtils.contains(citation, "doi:")) {
            String citationTrimmed = StringUtils.trim(defaultString(citation));
            if (dataset.getDOI() == null) {
                citation = citationTrimmed;
            } else {
                citation = citationTrimmed + separatorFor(citationTrimmed) + dataset.getDOI().toPrintableDOI();
            }
        }
        return StringUtils.trim(citation);
    }

    public static String citationOrDefaultFor(Dataset dataset, String defaultCitation) {
        Set<String> secondaryCitations = new TreeSet<>();
        JsonNode config = dataset.getConfig();
        if (config != null && config.has("tables")) {
            JsonNode tables = config.get("tables");
            for (JsonNode table : tables) {
                if (table.has(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION)) {
                    String secondaryCitation = table
                            .get(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION)
                            .getTextValue();
                    if (StringUtils.isNotBlank(secondaryCitation)) {
                        secondaryCitations.add(secondaryCitation);
                    }
                }
            }
        }
        String secondaryCitationsJoin = StringUtils.join(secondaryCitations, "; ");
        String fallbackCitation = StringUtils.isBlank(secondaryCitationsJoin)
                ? defaultCitation
                : secondaryCitationsJoin;

        String primaryCitation = dataset
                .getOrDefault(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION,
                        dataset.getOrDefault("citation", fallbackCitation));

        return StringUtils.trim(StringUtils.isBlank(primaryCitation)
                ? secondaryCitationsJoin
                : primaryCitation);
    }

    public static DOI getDOI(Dataset dataset) {
        String doi = dataset.getOrDefault("doi", "");
        DOI doiObj = null;
        URI archiveURI = dataset.getArchiveURI();
        if (StringUtils.isBlank(doi)) {
            String recordZenodo = StringUtils.replace(archiveURI.toString(), ZENODO_URL_PREFIX, "");
            String[] split = recordZenodo.split("/");
            if (split.length > 0) {
                doiObj = new DOI("5281", "zenodo." + split[0]);
            }
        }
        try {
            doiObj = doiObj == null ? DOI.create(doi) : doiObj;
        } catch (MalformedDOIException e) {
            LOG.warn("found malformed doi [" + doi + "]", e);

        }
        return doiObj;
    }
}
