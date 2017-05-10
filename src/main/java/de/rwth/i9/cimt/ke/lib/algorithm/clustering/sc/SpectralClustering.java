package de.rwth.i9.cimt.ke.lib.algorithm.clustering.sc;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smile.clustering.KMeans;
import smile.math.Math;
import smile.math.matrix.ColumnMajorMatrix;
import smile.math.matrix.DenseMatrix;
import smile.math.matrix.EigenValueDecomposition;
import smile.math.matrix.Lanczos;
import smile.math.matrix.SingularValueDecomposition;

public class SpectralClustering implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SpectralClustering.class);

	/**
	 * The number of clusters.
	 */
	private int k;
	/**
	 * The cluster labels of data.
	 */
	private int[] y;
	/**
	 * The number of samples in each cluster.
	 */
	private int[] size;
	/**
	 * The width of Gaussian kernel.
	 */
	private double sigma;
	/**
	 * The distortion in feature space.
	 */
	private double distortion;

	/**
	 * Constructor. Spectral graph clustering.
	 * 
	 * @param W
	 *            the adjacency matrix of graph.
	 * @param k
	 *            the number of clusters.
	 */
	public SpectralClustering(double[][] W, int k) {
		if (k < 2) {
			throw new IllegalArgumentException("Invalid number of clusters: " + k);
		}

		this.k = k;
		int n = W.length;

		for (int i = 0; i < n; i++) {
			if (W[i].length != n) {
				throw new IllegalArgumentException("The adjacency matrix is not square.");
			}

			if (W[i][i] != 0.0) {
				throw new IllegalArgumentException(String.format("Vertex %d has self loop: ", i));
			}

			for (int j = 0; j < i; j++) {
				if (W[i][j] != W[j][i]) {
					throw new IllegalArgumentException("The adjacency matrix is not symmetric.");
				}

				if (W[i][j] < 0.0) {
					throw new IllegalArgumentException("Negative entry of adjacency matrix: " + W[i][j]);
				}
			}
		}

		double[] D = new double[n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				D[i] += W[i][j];
			}

			if (D[i] == 0.0) {
				throw new IllegalArgumentException("Isolated vertex: " + i);
			}

			D[i] = 1.0 / Math.sqrt(D[i]);
		}

		DenseMatrix L = new ColumnMajorMatrix(n, n);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				double l = D[i] * W[i][j] * D[j];
				L.set(i, j, l);
				L.set(j, i, l);
			}
		}

		EigenValueDecomposition eigen = Lanczos.eigen(L, k);
		double[][] Y = eigen.getEigenVectors().array();
		for (int i = 0; i < n; i++) {
			Math.unitize2(Y[i]);
		}

		KMeans kmeans = new KMeans(Y, k);
		distortion = kmeans.distortion();
		y = kmeans.getClusterLabel();
		size = kmeans.getClusterSize();
	}

	/**
	 * Constructor. Spectral clustering the data.
	 * 
	 * @param data
	 *            the dataset for clustering.
	 * @param k
	 *            the number of clusters.
	 * @param sigma
	 *            the smooth/width parameter of Gaussian kernel, which is a
	 *            somewhat sensitive parameter. To search for the best setting,
	 *            one may pick the value that gives the tightest clusters
	 *            (smallest distortion, see {@link #distortion()}) in feature
	 *            space.
	 */
	public SpectralClustering(double[][] data, int k, double sigma) {
		if (k < 2) {
			throw new IllegalArgumentException("Invalid number of clusters: " + k);
		}

		if (sigma <= 0.0) {
			throw new IllegalArgumentException("Invalid standard deviation of Gaussian kernel: " + sigma);
		}

		this.k = k;
		this.sigma = sigma;

		int n = data.length;
		double gamma = -0.5 / (sigma * sigma);

		DenseMatrix W = new ColumnMajorMatrix(n, n);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				double w = Math.exp(gamma * Math.squaredDistance(data[i], data[j]));
				W.set(i, j, w);
				W.set(j, i, w);
			}
		}

		double[] D = new double[n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				D[i] += W.get(i, j);
			}

			if (D[i] < 1E-5) {
				logger.error(String.format("Small D[%d] = %f. The data may contain outliers.", i, D[i]));
			}

			D[i] = 1.0 / Math.sqrt(D[i]);
		}

		DenseMatrix L = W;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				double l = D[i] * W.get(i, j) * D[j];
				L.set(i, j, l);
				L.set(j, i, l);
			}
		}

		EigenValueDecomposition eigen = Lanczos.eigen(L, k);
		double[][] Y = eigen.getEigenVectors().array();
		for (int i = 0; i < n; i++) {
			Math.unitize2(Y[i]);
		}

		KMeans kmeans = new KMeans(Y, k);
		distortion = kmeans.distortion();
		y = kmeans.getClusterLabel();
		size = kmeans.getClusterSize();
	}

	/**
	 * Constructor. Spectral clustering with Nystrom approximation.
	 * 
	 * @param data
	 *            the dataset for clustering.
	 * @param l
	 *            the number of random samples for Nystrom approximation.
	 * @param k
	 *            the number of clusters.
	 * @param sigma
	 *            the smooth/width parameter of Gaussian kernel, which is a
	 *            somewhat sensitive parameter. To search for the best setting,
	 *            one may pick the value that gives the tightest clusters
	 *            (smallest distortion, see {@link #distortion()}) in feature
	 *            space.
	 */
	public SpectralClustering(double[][] data, int k, int l, double sigma) {
		if (l < k || l >= data.length) {
			throw new IllegalArgumentException("Invalid number of random samples: " + l);
		}

		if (k < 2) {
			throw new IllegalArgumentException("Invalid number of clusters: " + k);
		}

		if (sigma <= 0.0) {
			throw new IllegalArgumentException("Invalid standard deviation of Gaussian kernel: " + sigma);
		}

		this.k = k;
		this.sigma = sigma;

		int n = data.length;
		double gamma = -0.5 / (sigma * sigma);

		int[] index = Math.permutate(n);
		double[][] x = new double[n][];
		for (int i = 0; i < n; i++) {
			x[i] = data[index[i]];
		}
		data = x;

		DenseMatrix C = new ColumnMajorMatrix(n, l);
		double[] D = new double[n];
		for (int i = 0; i < n; i++) {
			double sum = 0.0;
			for (int j = 0; j < n; j++) {
				if (i != j) {
					double w = Math.exp(gamma * Math.squaredDistance(data[i], data[j]));
					sum += w;
					if (j < l) {
						C.set(i, j, w);
					}
				}
			}

			if (sum < 1E-5) {
				logger.error(String.format("Small D[%d] = %f. The data may contain outliers.", i, sum));
			}

			D[i] = 1.0 / Math.sqrt(sum);
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < l; j++) {
				C.set(i, j, D[i] * C.get(i, j) * D[j]);
			}
		}

		DenseMatrix W = new ColumnMajorMatrix(l, l);
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < l; j++) {
				W.set(i, j, C.get(i, j));
			}
		}
		SingularValueDecomposition svd = Lanczos.svd(W, k);
		double[] e = svd.getSingularValues();
		double scale = Math.sqrt((double) l / n);
		for (int i = 0; i < k; i++) {
			if (e[i] <= 0.0) {
				throw new IllegalStateException("Non-positive eigen value: " + e[i]);
			}

			e[i] = scale / e[i];
		}

		DenseMatrix U = svd.getU();
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < k; j++) {
				U.mul(i, j, e[j]);
			}
		}

		double[][] Y = C.abmm(U).array();
		for (int i = 0; i < n; i++) {
			Math.unitize2(Y[i]);
		}

		KMeans kmeans = new KMeans(Y, k);
		distortion = kmeans.distortion();
		size = kmeans.getClusterSize();

		int[] label = kmeans.getClusterLabel();
		y = new int[n];
		for (int i = 0; i < n; i++) {
			y[index[i]] = label[i];
		}
	}

	/**
	 * Returns the number of clusters.
	 */
	public int getNumClusters() {
		return k;
	}

	/**
	 * Returns the cluster labels of data.
	 */
	public int[] getClusterLabel() {
		return y;
	}

	/**
	 * Returns the size of clusters.
	 */
	public int[] getClusterSize() {
		return size;
	}

	/**
	 * Returns the width of Gaussian kernel.
	 */
	public double getGaussianKernelWidth() {
		return sigma;
	}

	/**
	 * Returns the distortion in feature space.
	 */
	public double distortion() {
		return distortion;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("Spectral Clustering distortion in feature space: %.5f%n", distortion));
		sb.append(String.format("Clusters of %d data points:%n", y.length));
		for (int i = 0; i < k; i++) {
			int r = (int) Math.round(1000.0 * size[i] / y.length);
			sb.append(String.format("%3d\t%5d (%2d.%1d%%)%n", i, size[i], r / 10, r % 10));
		}

		return sb.toString();
	}

}
