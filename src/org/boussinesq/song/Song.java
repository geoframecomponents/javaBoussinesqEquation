package org.boussinesq.song;

import java.io.File;
import java.io.IOException;

import org.wordpress.growworkinghard.usefulClasses.FileWrite;
import org.wordpress.growworkinghard.usefulClasses.GUIpathFileRead;

public class Song {

	double[] x;
	// double[] porosity;
	double hydraulicConductivity;
	int t;
	int alpha;
	static int h1;
	int nmax;

	public Song(int time, int Np, double ks) {

		x = new double[Np];
		t = time;
		alpha = 0;
		h1 = 1;
		nmax = 10;
		hydraulicConductivity = ks;

		for (int i = 0; i < x.length; i++) {

			x[i] = i;

		}

	}

	public double[] computeA(double[] ax, double xi0) {

		double[] a = new double[ax.length];

		for (int i = 0; i < a.length; i++) {

			a[i] = ax[i] * Math.pow(xi0, 2);

		}

		double sum = 0;

		for (int i = 0; i < a.length; i++) {

			sum += a[i];

		}

		System.out.println("Somma a: " + sum);

		return a;

	}

	public double[] computeXI(double[] porosity) {

		double[] xi = new double[x.length];

		for (int i = 0; i < xi.length; i++) {

			xi[i] = x[i]
					* Math.pow(
							2
									* porosity[i]
									* (alpha + 1)
									/ (h1 * hydraulicConductivity * Math.pow(t,
											alpha + 1)), 0.5);

		}

		// System.out.println(Arrays.toString(xi));
		return xi;
	}
	
	public File defineSolutionPrintLocation(){
		
		GUIpathFileRead guiDir = new GUIpathFileRead();
		File path = guiDir.saveDialog("Input path of Song solution");
				
		return path;
	}

	public void beqSong(double[] porosity) throws IOException {

		File outputPathSong = defineSolutionPrintLocation();
		
		String song = "songks";
		song = song.concat(Double.toString(hydraulicConductivity));
		song = song.concat("days").concat(Integer.toString(t/(3600*24)));
		
		FileWrite.openTxtFile(song, outputPathSong, true);
		
		double[] ax = new double[nmax];
		double[] solutionDimensionless = new double[x.length];
		double[] solution = new double[x.length];

		ax = SongCoefficient.CoefficientSongSolution(nmax,
				(alpha / (alpha + 1)));

		// System.out.println("Ax" + Arrays.toString(ax));

		double xi0 = 0;
		double sum = 0;

		for (int i = 1; i < ax.length; i++) {

			sum += ax[i];

		}

		xi0 = Math.pow(sum, -0.5);

		// System.out.println(xi0);

		solutionDimensionless = SongDimensionless.beqSongDimensionless(
				computeXI(porosity), xi0, computeA(ax, xi0));

		for (int i = 0; i < solutionDimensionless.length; i++) {

			// System.out.println(solutionDimensionless[i]);

			solution[i] = h1 * Math.pow(t, alpha) * solutionDimensionless[i];

		}

		FileWrite.writeOneDoubleColumn(solution);
		FileWrite.closeTxtFile();
		
		
		
	}

	public static void main(String[] args) throws IOException {

		int time = 3600 * 24 * 5;
		int dim = 1000;

		double[] porosity = new double[dim];

		for (int i = 0; i < dim; i++) {

			porosity[i] = 0.4;

		}

		Song s = new Song(time, dim, 0.1);
		s.beqSong(porosity);

		System.exit(1);

	}

}
