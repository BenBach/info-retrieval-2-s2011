import com.google.common.collect.HashMultimap;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import weka.core.*;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class Retrieval {
    public enum SimilarityMeasure {
        L1,
        L2
    }

    public static class Similarity {
        private double distance;
        private Instance instance;

        public Similarity(double distance, Instance instance) {
            this.distance = distance;
            this.instance = instance;
        }

        public double getDistance() {
            return distance;
        }

        public Instance getInstance() {
            return instance;
        }
    }

    @Option(name = "-i", aliases = {"--index"}, multiValued = true, required = false, usage = "the indices to be used")
    private List<File> indicesUsed;
    @Argument(multiValued = true, required = true, index = 0, usage = "the names of the query documents",
            metaVar = "QUERY")
    private List<String> queryDocuments;
    @Option(name = "-k", required = false, usage = "the number k of to-be-retrieved documents")
    private int k = 5;
    @Option(name = "-m", aliases = {"--measure"}, required = false,
            usage = "the similarity function to be used for similarity retrieval")
    private SimilarityMeasure similarityMeasure = SimilarityMeasure.L1;

    public void run() throws Exception {
        setupIndices();

        DistanceFunction distanceFunction = null;
        switch (similarityMeasure) {
            case L1:
                distanceFunction = new ManhattanDistance();
                break;
            case L2:
                distanceFunction = new EuclideanDistance();
                break;
            default:
                System.out.println("unknown similarity measure");
                System.exit(1);
        }

        printProgramStatus();

        for (File indexFile : indicesUsed) {
            System.out.println("\n\n-------- Index: " + indexFile + " ---------");

            // Prepare Data
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(indexFile.toURI().toURL().toString());
            Instances indexInstances = source.getDataSet();

            Attribute classAttribute = null;
            Attribute documentAttribute = null;

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

            List<Instance> documentVectors = new LinkedList<Instance>();

            Enumeration instances = indexInstances.enumerateInstances();
            while (instances.hasMoreElements()) {
                Instance instance = (Instance) instances.nextElement();

                String document = instance.toString(documentAttribute);

                if (!queryDocuments.contains(document)) continue;

                documentVectors.add(instance);
            }

            com.google.common.collect.Multimap<Instance, Similarity> similarities = HashMultimap.create();
            instances = indexInstances.enumerateInstances();
            while (instances.hasMoreElements()) {
                Instance instance = (Instance) instances.nextElement();

                for (Instance documentVector : documentVectors) {
                    // skip same document
                    if (documentVector.toString(documentAttribute).equals(instance.toString(documentAttribute)))
                        continue;

                    double distance = distanceFunction.distance(documentVector, instance);

                    similarities.put(documentVector, new Similarity(distance, instance));
                }
            }
        }
    }

    public static void findkNearestDocuments(int k, Instances data) {
        for (int c = 0; c < data.numInstances(); c++) {
            Instance instance = data.instance(c);
        }
    }

    public static void experimentRunner(weka.core.DistanceFunction distanceFunction, Instances data) {


    }

    private void setupIndices() {
        if (indicesUsed == null || indicesUsed.size() == 0) {
            String workingDirectory = System.getProperty("user.dir");
            File file = new File(workingDirectory);
            indicesUsed = Arrays.asList(file.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".arff");
                }
            }));
        }

        if (indicesUsed.size() == 0) {
            System.err.println("No .arff files found in current directory, or no .arff files specified");
            System.exit(1);
        }
    }

    private void printProgramStatus() {
        System.out.println(String.format("k                 : %d", k));
        System.out.println(String.format("Similarity Measure: %s", similarityMeasure));
        System.out.println("Used inidices:");
        for (File indexFile : indicesUsed) {
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
