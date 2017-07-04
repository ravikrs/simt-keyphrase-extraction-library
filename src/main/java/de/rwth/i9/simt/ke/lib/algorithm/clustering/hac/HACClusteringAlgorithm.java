package de.rwth.i9.simt.ke.lib.algorithm.clustering.hac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusterPair;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.LinkageStrategy;

public class HACClusteringAlgorithm extends DefaultClusteringAlgorithm {

	public List<Cluster> performFlatClustering(double[][] distances, String[] clusterNames,
			LinkageStrategy linkageStrategy, Double threshold) {

		checkArguments(distances, clusterNames, linkageStrategy);
		/* Setup model */
		List<Cluster> clusters = createClusters(clusterNames);
		DistanceMap linkages = createLinkages(distances, clusters);

		/* Process */
		HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);
		return builder.flatAgg(linkageStrategy, threshold);
	}

	private void checkArguments(double[][] distances, String[] clusterNames, LinkageStrategy linkageStrategy) {
		if (distances == null || distances.length == 0 || distances[0].length != distances.length) {
			throw new IllegalArgumentException("Invalid distance matrix");
		}
		if (distances.length != clusterNames.length) {
			throw new IllegalArgumentException("Invalid cluster name array");
		}
		if (linkageStrategy == null) {
			throw new IllegalArgumentException("Undefined linkage strategy");
		}
		int uniqueCount = new HashSet<String>(Arrays.asList(clusterNames)).size();
		if (uniqueCount != clusterNames.length) {
			throw new IllegalArgumentException("Duplicate names");
		}
	}

	private DistanceMap createLinkages(double[][] distances, List<Cluster> clusters) {
		DistanceMap linkages = new DistanceMap();
		for (int col = 0; col < clusters.size(); col++) {
			for (int row = col + 1; row < clusters.size(); row++) {
				ClusterPair link = new ClusterPair();
				Cluster lCluster = clusters.get(col);
				Cluster rCluster = clusters.get(row);
				link.setLinkageDistance(distances[col][row]);
				link.setlCluster(lCluster);
				link.setrCluster(rCluster);
				linkages.add(link);
			}
		}
		return linkages;
	}

	private List<Cluster> createClusters(String[] clusterNames) {
		List<Cluster> clusters = new ArrayList<Cluster>();
		for (String clusterName : clusterNames) {
			Cluster cluster = new Cluster(clusterName);
			clusters.add(cluster);
		}
		return clusters;
	}

}
