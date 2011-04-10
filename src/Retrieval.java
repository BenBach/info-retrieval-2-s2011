import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import weka.core.*;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;


public class Retrieval {
    public enum SimilarityMeasure {
        L1(new ManhattanDistance()),
        L2(new EuclideanDistance());

        private DistanceFunction distanceFunction;

        SimilarityMeasure(DistanceFunction distanceFunction) {
            this.distanceFunction = distanceFunction;
        }

        public DistanceFunction getDistanceFunction() {
            return distanceFunction;
        }
    }

    public static class DocumentSimilarity implements Comparable<DocumentSimilarity> {
        private double distance;
        private String sourceDocument;
        private String targetDocument;
        private String index;

        public DocumentSimilarity(double distance, String sourceDocument, String targetDocument, String index) {
            this.distance = distance;
            this.sourceDocument = sourceDocument;
            this.targetDocument = targetDocument;
            this.index = index;
        }

        public double getDistance() {
            return distance;
        }

        public String getSourceDocument() {
            return sourceDocument;
        }

        public String getTargetDocument() {
            return targetDocument;
        }

        public String getIndex() {
            return index;
        }

        @Override
        public int compareTo(DocumentSimilarity o) {
            return Double.compare(distance, o.distance);
        }
    }

    @Option(name = "-i", aliases = {"--index"}, multiValued = true, required = false, usage = "the indices to be used")
    private List<String> indicesNames;
    private List<File> indices;
    @Argument(multiValued = true, required = true, index = 0, usage = "the names of the query documents",
            metaVar = "QUERY")
    private List<String> queryDocuments;
    @Option(name = "-k", required = false, usage = "the number k of to-be-retrieved documents")
    private int k = 5;
    @Option(name = "-m", aliases = {"--measure"}, required = false,
            usage = "the similarity function to be used for similarity retrieval")
    private SimilarityMeasure similarityMeasure = SimilarityMeasure.L1;
    private Attribute classAttribute = null;
    private Attribute documentAttribute = null;

    public void run() throws Exception {
        setupIndices();
        printProgramStatus();

        // index -> document -> similarity
        Multimap<String, DocumentSimilarity> similarities = HashMultimap.create();

        for (File indexFile : indices) {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(indexFile.getAbsolutePath());
            Instances indexInstances = source.getDataSet();

            Enumeration attributes = indexInstances.enumerateAttributes();
            while (attributes.hasMoreElements()) {
                Attribute attribute = (Attribute) attributes.nextElement();

                if (classAttribute == null && attribute.name().matches(".*[Cc]lass.*"))
                    classAttribute = attribute;

                if (documentAttribute == null && attribute.name().matches(".*[Dd]ocument.*"))
                    documentAttribute = attribute;

                if (documentAttribute != null && classAttribute != null) break;
            }

            if (classAttribute == null) {
                System.err.println("No class attribute found for index " + indexFile);
                System.err.println("Aborting");
                System.exit(1);
            }

            if (documentAttribute == null) {
                System.err.println("No document attribute found for index " + indexFile);
                System.err.println("Aborting");
                System.exit(1);
            }

            List<Instance> documentVectors = Lists.newLinkedList();

            Enumeration instances = indexInstances.enumerateInstances();
            while (instances.hasMoreElements()) {
                Instance instance = (Instance) instances.nextElement();

                String document = getInstanceName(instance);

                if (!queryDocuments.contains(document)) continue;

                documentVectors.add(instance);
            }

            similarityMeasure.getDistanceFunction().setInstances(indexInstances);

            // document -> similarity
            Multimap<String, DocumentSimilarity> similaritiesForIndex = HashMultimap.create();

            // calculate distance to all other documents in the index file
            instances = indexInstances.enumerateInstances();
            while (instances.hasMoreElements()) {
                Instance instance = (Instance) instances.nextElement();
                String instanceName = getInstanceName(instance);

                for (Instance queryInstance : documentVectors) {
                    String queryInstanceName = getInstanceName(queryInstance);
                    // skip same document
                    if (instanceName.equals(queryInstanceName))
                        continue;

                    double distance = similarityMeasure.getDistanceFunction().distance(queryInstance, instance);

                    similaritiesForIndex.put(queryInstanceName, new DocumentSimilarity(distance, queryInstanceName,
                            instanceName, indexFile.getName()));
                }

                trimMatchesToSizeK(similaritiesForIndex);
            }

            trimMatchesToSizeK(similaritiesForIndex);

            similarities.putAll(similaritiesForIndex);
        }

        for (String queryDocument : similarities.keySet()) {
            Collection<DocumentSimilarity> similarityCollection = similarities.get(queryDocument);
            List<DocumentSimilarity> similarityList = Lists.newArrayList(similarityCollection);
            Collections.sort(similarityList);

            System.out.println("\nMatches for " + queryDocument);

            for (DocumentSimilarity documentSimilarity : similarityList) {
                System.out.println(String.format("%-40.40s % 15.5f %-33.33s", documentSimilarity.getTargetDocument(),
                        documentSimilarity.getDistance(), documentSimilarity.getIndex()));
            }
        }
    }

    private void trimMatchesToSizeK(Multimap<String, DocumentSimilarity> similarities) {
        for (String queryDocument : queryDocuments) {
            Collection<DocumentSimilarity> similarityCollection = similarities.get(queryDocument);

            if(similarityCollection.size() <= k) return;

            List<DocumentSimilarity> similarityList = Lists.newArrayList(similarityCollection);

            Collections.sort(similarityList, new Comparator<DocumentSimilarity>() {
                public int compare(DocumentSimilarity s1, DocumentSimilarity s2) {
                    return Double.compare(s1.getDistance(), s2.getDistance());
                }
            });

            similarities.replaceValues(queryDocument, similarityList.subList(0, Math.min(k, similarityList.size())));
        }
    }

    private String getInstanceName(Instance instance) {
        return instance.toString(classAttribute) + "/" + instance.toString(documentAttribute);
    }

    private void setupIndices() {
        String workingDirectory = System.getProperty("user.dir");
        File file = new File(workingDirectory);

        if (indicesNames == null || indicesNames.size() == 0) {
            indices = Arrays.asList(file.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".arff") || name.endsWith(".arff.gz");
                }
            }));
        } else {
            indices = Lists.newLinkedList();

            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(file);
            directoryScanner.setCaseSensitive(true);

            for (String indexName : indicesNames) {
                Iterable<String> subIndicesNames =
                        Splitter.on(CharMatcher.is(',')).omitEmptyStrings().trimResults().split(
                                indexName);

                List<String> subIndices = Lists.newArrayList(subIndicesNames);

                for (String subIndice : subIndices) {
                    System.out.println("subindex " + subIndice);
                }

                directoryScanner.setIncludes(subIndices.toArray(new String[subIndices.size()]));
                directoryScanner.scan();
                String[] fileNames = directoryScanner.getIncludedFiles();

                for (String fileName : fileNames) {
                    if (!fileName.endsWith(".arff") && !fileName.endsWith(".arrf.gz")) continue;

                    indices.add(new File(fileName));
                }
            }
        }

        if (indices.size() == 0) {
            System.err.println("No .arff files found in current directory, or no .arff files specified");
            System.exit(1);
        }
    }

    private void printProgramStatus() {
        System.out.println(String.format("k                 : %d", k));
        System.out.println(String.format("Similarity Measure: %s", similarityMeasure));
        System.out.println("Used inidices:");
        for (File indexFile : indices) {
            System.out.println("\t" + indexFile.getName());
        }
        System.out.println("Document query:");
        for (String queryDocument : queryDocuments) {
            System.out.println("\t" + queryDocument);
        }
    }

    public static void main(String[] args) throws Exception {
        Retrieval retrieval = new Retrieval();
        CmdLineParser parser = new CmdLineParser(retrieval);
        parser.setUsageWidth(80); // width of the error display area

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java Retrieval [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
        }

        retrieval.run();
    }

}
