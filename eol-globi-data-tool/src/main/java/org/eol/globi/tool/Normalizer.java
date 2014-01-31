package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.Study;
import org.eol.globi.export.DarwinCoreExporter;
import org.eol.globi.export.ExporterAssociationAggregates;
import org.eol.globi.export.ExporterAssociations;
import org.eol.globi.export.ExporterMeasurementOrFact;
import org.eol.globi.export.ExporterOccurrenceAggregates;
import org.eol.globi.export.ExporterOccurrences;
import org.eol.globi.export.ExporterReferences;
import org.eol.globi.export.ExporterTaxa;
import org.eol.globi.export.GlobiOWLExporter;
import org.eol.globi.export.StudyExportUnmatchedSourceTaxaForStudies;
import org.eol.globi.export.StudyExportUnmatchedTargetTaxaForStudies;
import org.eol.globi.export.StudyExporter;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderFactoryImpl;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.EcoRegionFinderProxy;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);

    private EcoRegionFinder ecoRegionFinder = null;

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new Normalizer().normalize();
    }

    public void normalize() throws StudyImporterException {
        normalize("./");
    }

    private EcoRegionFinder getEcoRegionFinder() {
        if (null == ecoRegionFinder) {
            ecoRegionFinder = new EcoRegionFinderProxy(new EcoRegionFinderFactoryImpl());
        }
        return ecoRegionFinder;
    }

    public void setEcoRegionFinder(EcoRegionFinder finder) {
        this.ecoRegionFinder = finder;
    }

    public void normalize(String baseDir) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService(baseDir);
        importData(graphService);
        exportData(graphService, baseDir);
        graphService.shutdown();
        ecoRegionFinder.shutdown();
    }


    protected void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        List<Study> studies = NodeFactory.findAllStudies(graphService);
        exportUnmatchedTaxa(studies, baseDir);
        exportDarwinCoreAggregatedByStudy(baseDir, studies);
        exportDarwinCoreAll(baseDir, studies);
        exportDataOntology(studies, baseDir);
    }

    private void exportUnmatchedTaxa(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
            export(studies, baseDir + "unmatchedSourceTaxa.csv", new StudyExportUnmatchedSourceTaxaForStudies());
            export(studies, baseDir + "unmatchedTargetTaxa.csv", new StudyExportUnmatchedTargetTaxaForStudies());
        } catch (IOException e) {
            throw new StudyImporterException("failed to export unmatched source taxa", e);
        }
    }

    private void exportDarwinCoreAggregatedByStudy(String baseDir, List<Study> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies,
                baseDir + "aggregatedByStudy/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.csv", new ExporterAssociationAggregates());
                put("occurrence.csv", new ExporterOccurrenceAggregates());
                put("references.csv", new ExporterReferences());
                put("taxa.csv", new ExporterTaxa());
            }
        });
    }

    private void exportDarwinCoreAll(String baseDir, List<Study> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies, baseDir + "all/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.csv", new ExporterAssociations());
                put("occurrence.csv", new ExporterOccurrences());
                put("references.csv", new ExporterReferences());
                put("taxa.csv", new ExporterTaxa());
                put("measurementOrFact.csv", new ExporterMeasurementOrFact());
            }
        });
    }

    private void exportDataOntology(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            export(studies, baseDir + "globi.ttl.gz", new GlobiOWLExporter());
        } catch (OWLOntologyCreationException e) {
            throw new StudyImporterException("failed to export as owl", e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(List<Study> studies, String pathPrefix, Map<String, DarwinCoreExporter> exporters) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(pathPrefix));
            FileWriter darwinCoreMeta = writeMetaHeader(pathPrefix);
            for (Map.Entry<String, DarwinCoreExporter> exporter : exporters.entrySet()) {
                export(studies, pathPrefix, exporter.getKey(), exporter.getValue(), darwinCoreMeta);
            }
            writeMetaFooter(darwinCoreMeta);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private void importData(GraphDatabaseService graphService) {
        NodeFactory factory = new NodeFactory(graphService, TaxonPropertyEnricherFactory.createTaxonEnricher());
        for (Class importer : StudyImporterFactory.getAvailableImporters()) {
            try {
                importData(importer, factory);
            } catch (StudyImporterException e) {
                LOG.error("problem encountered while importing [" + importer.getName() + "]", e);
            }
        }
    }

    protected void importData(Class importer, NodeFactory factory) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private void writeMetaFooter(FileWriter darwinCoreMeta) throws IOException {
        darwinCoreMeta.write("</archive>");
        darwinCoreMeta.flush();
        darwinCoreMeta.close();
    }

    private FileWriter writeMetaHeader(String pathPrefix) throws IOException {
        FileWriter darwinCoreMeta = new FileWriter(pathPrefix + "meta.xml", false);
        darwinCoreMeta.write("<?xml version=\"1.0\"?>\n" +
                "<archive xmlns=\"http://rs.tdwg.org/dwc/text/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://rs.tdwg.org/dwc/text/  http://services.eol.org/schema/dwca/tdwg_dwc_text.xsd\">\n");
        return darwinCoreMeta;
    }

    private void export(List<Study> importedStudies, String exportPath, String filename, DarwinCoreExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        export(importedStudies, exportPath + filename, studyExporter);
        LOG.info("darwin core meta file writing... ");
        studyExporter.exportDarwinCoreMetaTable(darwinCoreMeta, filename);
        LOG.info("darwin core meta file written. ");
    }

    private void export(List<Study> importedStudies, String exportPath, StudyExporter studyExporter) throws IOException {
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(exportPath));
        if (exportPath.endsWith(".gz")) {
            fos = new GZIPOutputStream(fos);
        }
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
        }
        writer.flush();
        writer.close();
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }


    private StudyImporter createStudyImporter(Class<StudyImporter> studyImporter, NodeFactory factory) throws StudyImporterException {
        factory.setEcoRegionFinder(getEcoRegionFinder());
        factory.setDoiResolver(new DOIResolverImpl());
        ParserFactory parserFactory = new ParserFactoryImpl();
        StudyImporter importer = new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
        importer.setLogger(new StudyImportLogger(factory));
        return importer;
    }

}