import java.io.File;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.Prediction;
import weka.core.EuclideanDistance;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.NearestNeighbourSearch;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.kohsuke.args4j.Option;


public class Retrieval
{
	private static File[] indicesUsed;
	private File[] queryDocuments;	
	
	@Option(name = "-k", aliases = {"--knn"}, usage = "Defines k value.")
	private int k;
	@Option(name = "-m", aliases = {"--manhattan"}, usage = "Enables Manhattan Distance.")
	private boolean similarityL1;
	@Option(name = "-e", aliases = {"--euclid"}, usage = "Enables Euclidian Distance.")
	private boolean similarityL2;
	
	
	
	public static void main(String[] args) throws Exception
	{		
	    for (File dataPath : indicesUsed) {
            System.out.println("\n\n-------- Dataset: " + dataPath + " ---------");

            // Prepare Data
            DataSource source = new DataSource(dataPath.getAbsolutePath());
            Instances data = source.getDataSet();

            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            // run algorithms
            System.out.println("--- Running Similarity function ---");

            // experimentRunner()          
            
            
        }
    }
	public static void findkNearestDocuments(int k, Instances data)
	{
		for(int c = 0; c < data.numInstances(); c++)
		{
			Instance instance = data.instance(c);
			
			if()
		}			
	}
	
	
	
	
	public static void experimentRunner(weka.core.DistanceFunction distanceFunction,  Instances data) {


	}
}
