package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.Specimen;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.doi.DOI;

import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = recordNumber -> true;
    private AtomicLong currentLine = null;
    private String currentResource = null;

    private Dataset dataset;

    private GeoNamesService geoNamesService = new GeoNamesServiceImpl();

    private ImportLogger importLogger = new ImportLogger() {
        @Override
        public void warn(LogContext study, String message) {

        }

        @Override
        public void info(LogContext study, String message) {

        }

        @Override
        public void severe(LogContext study, String message) {

        }
    };
    private String sourceCitationLastAccessed;

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(nodeFactory);
        this.parserFactory = parserFactory;
    }

    @Override
    public void setFilter(ImportFilter importFilter) {
        this.importFilter = importFilter;
    }

    protected ImportFilter getFilter() {
        return this.importFilter;
    }

    @Override
    public void setLogger(ImportLogger importLogger) {
        this.importLogger = importLogger;
    }

    public ImportLogger getLogger() {
        return this.importLogger;
    }

    public void setGeoNamesService(GeoNamesService geoNamesService) {
        this.geoNamesService = geoNamesService;
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }

    public String getSourceCitation() {
        return getDataset() == null ? null : getDataset().getCitation();
    }

    public DOI getSourceDOI() {
        return dataset == null ? null : dataset.getDOI();
    }

    protected void setBasisOfRecordAsLiterature(Specimen specimen) throws NodeFactoryException {
        specimen.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord("http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/BasisOfRecord.html#LITERATURE", "Literature"));
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return dataset;
    }

    String getSourceCitationLastAccessed() {
        if (StringUtils.isBlank(sourceCitationLastAccessed)) {
            sourceCitationLastAccessed = CitationUtil.sourceCitationLastAccessed(getDataset());
        }
        return sourceCitationLastAccessed;
    }

    protected void setCurrentResource(String currentResource) {
        this.currentResource = currentResource;
    }

    protected String getCurrentResource() {
        return this.currentResource;
    }

    protected void setCurrentLine(long currentLine) {
        if (this.currentLine == null) {
            this.currentLine = new AtomicLong();
        }
        this.currentLine.set(currentLine);
    }

    protected long getCurrentLine() {
        return this.currentLine.get();
    }

    protected String createMsg(String message) {
        StringBuilder builder = new StringBuilder();
        if (currentLine != null) {
            builder.append("[");
            if (StringUtils.isNotBlank(getCurrentResource())) {
                builder.append(getCurrentResource());
            }
            builder.append(":");
            builder.append(getCurrentLine());
            builder.append("] ");
        }
        return builder.append(message).toString();
    }

}
