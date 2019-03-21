package org.eol.globi.data;

import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForBarnesTest extends GraphDBTestCase {

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService();
    }

    @Test
    public void importHeadAndTail() throws IOException, NodeFactoryException, StudyImporterException {
        final String firstFourLines = "Record number\tIn-ref ID\tIndividual ID\tPredator\tPredator common name\tPredator  taxon\tPredator lifestage\tType of feeding interaction\tPredator length\tPredator length unit\tPredator dimension measured\tPredator standard length\tPredator fork length\tPredator total length\tPredator TL/FL/SL conversion reference\tStandardised predator length\tPredator measurement type\tPredator length-mass conversion method\tPredator length-mass conversion reference\tPredator quality of length-mass conversion\tPredator mass\tPredator mass unit\tPredator mass check\tPredator mass check diff\tPredator ratio mass/mass\tSI predator mass\tDiet coverage\tPrey\tPrey common name\tPrey taxon\tPrey length\tPrey length unit\tPrey conversion to length method\tPrey quality of conversion to length\tPrey conversion to length reference\tSI prey length\tPrey dimension measured\tPrey width\tPrey width unit\tPrey measurement type\tPrey mass\tPrey mass unit\tPrey mass check\tPrey mass check diff\tPrey ratio mass/mass\tSI prey mass\tPrey conversion to mass method\tPrey conversion to mass reference\tPrey quality of conversion to mass\tGeographic location\tLatitude\tLongitude\tDepth\tMean annual temp\tSD annual temp\tMean PP\tSD PP\tReference\tSpecific habitat\tNotes / assumptions\n" +
                "1\tATSH063\t1\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t7.8000E+02\tmm\tfork length\t7.5433E+02\t7.8000E+02\t9.3990E+02\tFishbase (species)\t9.3990E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t1.5399E+03\tg\t4.3453E+04\t4.1913E+04\t2.8218E+01\t1.5399E+03\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t1.1259E+02\tmm\tn/a\t0\tn/a\t1.1259E+01\tlength\tn/a\tn/a\tindividual\t1.4274E+01\tg\t7.4699E+01\t6.0425E+01\t5.2333E+00\t1.4274E+01\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "2\tATSH080\t2\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t7.9000E+02\tmm\tfork length\t7.6400E+02\t7.9000E+02\t9.5195E+02\tFishbase (species)\t9.5195E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t1.5978E+03\tg\t4.5146E+04\t4.3548E+04\t2.8256E+01\t1.5978E+03\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t8.4443E+01\tmm\tn/a\t0\tn/a\t8.4443E+00\tlength\tn/a\tn/a\tindividual\t6.0213E+00\tg\t3.1511E+01\t2.5490E+01\t5.2333E+00\t6.0213E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "3\tATSH089\t3\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t8.3000E+02\tmm\tfork length\t8.0269E+02\t8.3000E+02\t1.0002E+03\tFishbase (species)\t1.0002E+02\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t1.8436E+03\tg\t5.2357E+04\t5.0513E+04\t2.8400E+01\t1.8436E+03\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t1.0595E+02\tmm\tn/a\t0\tn/a\t1.0595E+01\tlength\tn/a\tn/a\tindividual\t1.1893E+01\tg\t6.2242E+01\t5.0349E+01\t5.2333E+00\t1.1893E+01\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "4\tATSH143\t4\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.9000E+02\tmm\tfork length\t2.8046E+02\t2.9000E+02\t3.4945E+02\tFishbase (species)\t3.4945E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t8.7631E+01\tg\t2.2332E+03\t2.1456E+03\t2.5484E+01\t8.7631E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t9.3301E+01\tmm\tn/a\t0\tn/a\t9.3301E+00\tlength\tn/a\tn/a\tindividual\t8.1220E+00\tg\t4.2505E+01\t3.4383E+01\t5.2333E+00\t8.1220E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "5\tATSH161\t5\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.6000E+02\tmm\tfork length\t2.5144E+02\t2.6000E+02\t3.1330E+02\tFishbase (species)\t3.1330E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t6.3866E+01\tg\t1.6094E+03\t1.5455E+03\t2.5199E+01\t6.3866E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t8.6900E+01\tmm\tn/a\t0\tn/a\t8.6900E+00\tlength\tn/a\tn/a\tindividual\t6.5623E+00\tg\t3.4343E+01\t2.7780E+01\t5.2333E+00\t6.5623E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "6\tATSH166\t6\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.8000E+02\tmm\tfork length\t2.7079E+02\t2.8000E+02\t3.3740E+02\tFishbase (species)\t3.3740E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t7.9161E+01\tg\t2.0101E+03\t1.9309E+03\t2.5392E+01\t7.9161E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t8.1465E+01\tmm\tn/a\t0\tn/a\t8.1465E+00\tlength\tn/a\tn/a\tindividual\t5.4065E+00\tg\t2.8294E+01\t2.2888E+01\t5.2333E+00\t5.4065E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "7\tATSH172\t7\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.7000E+02\tmm\tfork length\t2.6112E+02\t2.7000E+02\t3.2535E+02\tFishbase (species)\t3.2535E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t7.1245E+01\tg\t1.8023E+03\t1.7311E+03\t2.5298E+01\t7.1245E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t7.6371E+01\tmm\tn/a\t0\tn/a\t7.6371E+00\tlength\tn/a\tn/a\tindividual\t4.4543E+00\tg\t2.3311E+01\t1.8857E+01\t5.2333E+00\t4.4543E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "8\tATSH192\t8\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.9500E+02\tmm\tfork length\t2.8529E+02\t2.9500E+02\t3.5548E+02\tFishbase (species)\t3.5548E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t9.2080E+01\tg\t2.3507E+03\t2.2587E+03\t2.5529E+01\t9.2080E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t8.4262E+01\tmm\tn/a\t0\tn/a\t8.4262E+00\tlength\tn/a\tn/a\tindividual\t5.9828E+00\tg\t3.1310E+01\t2.5327E+01\t5.2333E+00\t5.9828E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "9\tATSH205\t9\tRhizoprionodon terraenovae\tAtlantic sharpnose shark\tectotherm vertebrate\tadult\tpredacious/piscivorous\t2.8000E+02\tmm\tfork length\t2.7079E+02\t2.8000E+02\t3.3740E+02\tFishbase (species)\t3.3740E+01\tindividual\tM=0.0056SL^2.897 \tBonfil et al. (1990)\t1\t7.9161E+01\tg\t2.0101E+03\t1.9309E+03\t2.5392E+01\t7.9161E+01\tall\tteleosts/molluscs/crustaceans\tteleosts/molluscs/crustaceans\tmixed\t8.8580E+01\tmm\tn/a\t0\tn/a\t8.8580E+00\tlength\tn/a\tn/a\tindividual\t6.9504E+00\tg\t3.6374E+01\t2.9423E+01\t5.2333E+00\t6.9504E+00\tM=0.01L^3\tGeneralised\t5\t\"Apalachicola Bay, Florida\"\t29�40'N\t85�10'W\t30\t24.1\t4.2\t866\t214\tBethea et al (2004)\tCoastal Bay\tNone\n" +
                "34922\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t4.2580E+02\tmm\ttotal length\t3.4916E+02\t0.0000E+00\t4.2580E+02\tFishbase (species)\t4.2580E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t7.1462E+02\tg\t4.0401E+03\t3.3255E+03\t5.6535E+00\t7.1462E+02\tall\tDeltentosteus quadrimaculatus\tFour-spotted goby\tectotherm vertebrate\t6.9190E+00\tcm\tL = 10^ ((logM-log(0.0074))/3.05)\t1\tMerella et al. (1997)\t6.9190E+00\tmass\tn/a\tn/a\tindividual\t2.7000E+00\tg\t1.7334E+01\t1.4634E+01\t6.4201E+00\t2.7000E+00\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34923\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t1.1920E+02\tmm\ttotal length\t9.7744E+01\t0.0000E+00\t1.1920E+02\tFishbase (species)\t1.1920E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t1.7550E+01\tg\t8.8635E+01\t7.1085E+01\t5.0505E+00\t1.7550E+01\tall\tLepidotrigla cavillone\tLarge-scaled gurnard\tectotherm vertebrate\t4.4221E+00\tcm\tL = 10^ ((logM-log(0.0078))/3.194)\t1\tPapaconstantinou (1982)\t4.4221E+00\tmass\tn/a\tn/a\tindividual\t9.0000E-01\tg\t4.5256E+00\t3.6256E+00\t5.0284E+00\t9.0000E-01\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34924\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t1.5410E+02\tmm\ttotal length\t1.2636E+02\t0.0000E+00\t1.5410E+02\tFishbase (species)\t1.5410E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t3.7066E+01\tg\t1.9151E+02\t1.5444E+02\t5.1667E+00\t3.7066E+01\tall\tLepidotrigla cavillone\tLarge-scaled gurnard\tectotherm vertebrate\t3.6788E+00\tcm\tL = 10^ ((logM-log(0.0078))/3.194)\t1\tPapaconstantinou (1982)\t3.6788E+00\tmass\tn/a\tn/a\tindividual\t5.0000E-01\tg\t2.6056E+00\t2.1056E+00\t5.2112E+00\t5.0000E-01\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34925\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t3.5470E+02\tmm\ttotal length\t2.9085E+02\t0.0000E+00\t3.5470E+02\tFishbase (species)\t3.5470E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t4.1983E+02\tg\t2.3354E+03\t1.9156E+03\t5.5628E+00\t4.1983E+02\tall\tTrachurus sp.\tfish\tectotherm vertebrate\t2.4112E+01\tcm\tL = 10^ ((logM-log(0.0072))/3.0327)\t2\tDorel (1986)\t2.4112E+01\tmass\tn/a\tn/a\tindividual\t1.1200E+02\tg\t7.3361E+02\t6.2161E+02\t6.5501E+00\t1.1200E+02\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34926\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t2.6470E+02\tmm\ttotal length\t2.1705E+02\t0.0000E+00\t2.6470E+02\tFishbase (species)\t2.6470E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t1.7907E+02\tg\t9.7060E+02\t7.9153E+02\t5.4204E+00\t1.7907E+02\tall\tGaidropsarus sp.\trockling\tectotherm vertebrate\t1.0897E+01\tcm\tL = 10^ ((logM-log(0.0053))/3.0262)\t2\tFroese & Pauly (2007)\t1.0897E+01\tmass\tn/a\tn/a\tindividual\t7.3000E+00\tg\t6.7709E+01\t6.0409E+01\t9.2752E+00\t7.3000E+00\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34927\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t3.7980E+02\tmm\ttotal length\t3.1144E+02\t0.0000E+00\t3.7980E+02\tFishbase (species)\t3.7980E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t5.1230E+02\tg\t2.8671E+03\t2.3548E+03\t5.5966E+00\t5.1230E+02\tall\tGaidropsarus sp.\trockling\tectotherm vertebrate\t6.5987E+00\tcm\tL = 10^ ((logM-log(0.0053))/3.0262)\t2\tFroese & Pauly (2007)\t6.5987E+00\tmass\tn/a\tn/a\tindividual\t1.6000E+00\tg\t1.5037E+01\t1.3437E+01\t9.3979E+00\t1.6000E+00\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34928\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t3.7980E+02\tmm\ttotal length\t3.1144E+02\t0.0000E+00\t3.7980E+02\tFishbase (species)\t3.7980E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t5.1230E+02\tg\t2.8671E+03\t2.3548E+03\t5.5966E+00\t5.1230E+02\tall\tGadiculus argenteus\tGadiculus\tectotherm vertebrate\t7.7720E+00\tcm\tL = 10^ ((logM-log(0.0058))/3.1083)\t2\tFroese & Pauly (2007)\t7.7720E+00\tmass\tn/a\tn/a\tindividual\t3.4000E+00\tg\t2.4569E+01\t2.1169E+01\t7.2261E+00\t3.4000E+00\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34929\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t8.8300E+01\tmm\ttotal length\t7.2406E+01\t0.0000E+00\t8.8300E+01\tFishbase (species)\t8.8300E+00\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t7.3261E+00\tg\t3.6030E+01\t2.8704E+01\t4.9180E+00\t7.3261E+00\tall\tGonostomatidae sp.\tBristlemouth\tectotherm vertebrate\t5.4092E+00\tcm\tL = 10^ ((logM-log(0.0047))/2.964)\t3\tFroese & Pauly (2007)\t5.4092E+00\tmass\tn/a\tn/a\tindividual\t7.0000E-01\tg\t8.2827E+00\t7.5827E+00\t1.1832E+01\t7.0000E-01\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34930\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t8.8300E+01\tmm\ttotal length\t7.2406E+01\t0.0000E+00\t8.8300E+01\tFishbase (species)\t8.8300E+00\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t7.3261E+00\tg\t3.6030E+01\t2.8704E+01\t4.9180E+00\t7.3261E+00\tall\tGonostomatidae sp.\tBristlemouth\tectotherm vertebrate\t5.4092E+00\tcm\tL = 10^ ((logM-log(0.0047))/2.964)\t3\tFroese & Pauly (2007)\t5.4092E+00\tmass\tn/a\tn/a\tindividual\t7.0000E-01\tg\t8.2827E+00\t7.5827E+00\t1.1832E+01\t7.0000E-01\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n" +
                "34931\tn/a\tn/a\tZeus faber\tJohn dory\tectotherm vertebrate\tadult\tpiscivorous\t3.5470E+02\tmm\ttotal length\t2.9085E+02\t0.0000E+00\t3.5470E+02\tFishbase (species)\t3.5470E+01\tmid-class\tM=0.0230SL^2.9114\tDorel (1986)\t1\t4.1983E+02\tg\t2.3354E+03\t1.9156E+03\t5.5628E+00\t4.1983E+02\tall\tMicromesistius potassou\tBlue-whiting\tectotherm vertebrate\t2.1704E+01\tcm\tL = 10^ ((logM-log(0.0038))/3.082)\t1\tDorel (1986)\t2.1704E+01\tmass\tn/a\tn/a\tindividual\t5.0000E+01\tg\t5.3502E+02\t4.8502E+02\t1.0700E+01\t5.0000E+01\tWeighed\tn/a\t0\tEastern Mediterranean\t38�00'N\t23�00'E\t75\t19.3\t4.6\t435\t17\tStergiou & Fourtouni 1991\tEuboikos and Pagassitikos Gulfs\tPredator length mid-class\n";


        TestParserFactory parserFactory = new TestParserFactory(new HashMap<String, String>() {
            {
                put(StudyImporterForBarnes.RESOURCE_PATH, firstFourLines);
                put(StudyImporterForBarnes.REFERENCE_PATH, "short,full\nBethea et al (2004),something long\nStergiou & Fourtouni 1991,something longer");
            }
        });
        StudyImporterForBarnes importer = new StudyImporterForBarnes(parserFactory, nodeFactory);
        importStudy(importer);

        Taxon taxon = taxonIndex.findTaxonByName("Zeus faber");
        Transaction tx = getGraphDb().beginTx();
        try {
            Iterable<Relationship> relationships = ((NodeBacked) taxon).getUnderlyingNode().getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
            for (Relationship relationship : relationships) {
                Node predatorSpecimenNode = relationship.getStartNode();
                assertThat((String) predatorSpecimenNode.getProperty(SpecimenConstant.LIFE_STAGE_LABEL), is("post-juvenile adult stage"));
                assertThat((String) predatorSpecimenNode.getProperty(SpecimenConstant.LIFE_STAGE_ID), is("UBERON:0000113"));

            }
            assertThat(taxon, is(notNullValue()));
            assertThat(taxonIndex.findTaxonByName("Rhizoprionodon terraenovae"), is(notNullValue()));
            assertThat("missing location", nodeFactory.findLocation(new LocationImpl(38.0, 23.0, -75.0, null)), is(notNullValue()));
            tx.success();
        } finally {
            tx.finish();
        }

    }

    @Test(expected = NumberFormatException.class)
    public void throwMalformedLocation() {
        LocationUtil.parseDegrees("85º'10'W");

    }
}
