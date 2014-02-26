package org.boussinesq.boussinesq;

/**
 * Mass-Conservative groundwater equation integration
 * 
 * @desc	In this code is implemented a conservative finite-volume
 * 			numerical solution for the two-dimensional groundwater flow
 * 			(Boussinesq) equation, which can be used for investigation
 * 			of hillslope subsurface flow processes and simulations of
 * 			catchment hydrology.
 * 
 * 			The idea is taken from:
 * 
 * 			"A mass-conservative method for the integration of the
 * 			 two dimensional groundwater (Boussinesq) equation"
 * 			E.Cordano, R.Rigon 2012 - Water Resources Research
 * 
 * @author	E. Cordano, G. Formetta, R. Rigon, F. Serafin, 2014
 * Copyright GPL v. 3 (http://www.gnu.org/licenses/gpl.html)
 * */

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.boussinesq.song.Song;

import cern.colt.matrix.tdouble.algo.solver.IterativeSolverDoubleNotConvergedException;
//import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix1D;
//import cern.colt.matrix.tdouble.impl.SparseRCDoubleMatrix2D;

/**
 * The Class BoussinesqEquation.
 */
public class BoussinesqEquation {

	/** The deltat. */
	static int deltat = 3600;

	/** legth of the simulation */
	static int simTime = 3600 * 24;

	static double tolerance = 0;

	BoussinesqEquation() {

		tolerance = computeMachineEpsilonDouble();

	}

	/**
	 * Calculate machine epsilon double.
	 * 
	 * @desc this method compute the tolerance of the machine. For more info go
	 *       to
	 *       https://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
	 *       . In c/c++ section there's write that:
	 * 
	 *       In such languages as C or C++ when you do something like while( 1.0
	 *       + eps > 1.0 ) the expression is calculated not with 64 bits
	 *       (double) but with the processor precision (80 bits or more depends
	 *       on the processor and compile options). Below program calculates
	 *       exactly on 32 bits (float) and 64 bits (double)
	 * 
	 * 
	 * @return the tolerance of the machine
	 */
	private static double computeMachineEpsilonDouble() {

		// machine tolerance
		double machEps = 1.0d;

		do
			machEps /= 2.0d;
		while ((double) (1.0 + (machEps / 2.0)) != 1.0);

		return machEps;
	}

	/**
	 * Compute index of diagonal.
	 * 
	 * @desc this method computes the indices of the diagonal of the adjacency
	 *       matrix; this matrix and all the sparse matrices are stored in Row
	 *       Compressed Form. More information at the web site
	 *       https://en.wikipedia.org/wiki/Sparse_matrix
	 * 
	 * @param mesh
	 *            the object mesh is passed so every field of the mesh class is
	 *            available
	 * 
	 * @return the array that holds the indices of the diagonal entries of the
	 *         sparse adjacency matrix in Row Compressed Form
	 */
	public int[] computeIndexDiag() {

		// declaration of the array that holds the indices of diagonal entries
		int[] indexDiag = new int[Mesh.Np];

		/* for-loop to analyze the matrix cell by cell */
		for (int i = 0; i < Mesh.Np; i++) {
			/*
			 * nested for-loop to analyze diagonal entries, which are identified
			 * by a negative number
			 */
			for (int j = Mesh.Mp[i]; j < Mesh.Mp[i + 1]; j++) {

				if (Mesh.Mi[j] == i) {
					indexDiag[i] = j;
				}

			}
		}

		return indexDiag;
	}

	/**
	 * Compute the Boussinesq Equation.
	 * 
	 * @desc in this method the temporal loop is implemented. Before start the
	 *       loop, the eta array is initialized with eta of Dirichlet if the
	 *       cell is a Dirichlet cells, otherwise it's inizialized with first
	 *       attempt value.
	 * 
	 * @param mesh
	 *            the object mesh is passed so every field of the mesh class is
	 *            available
	 * @throws IterativeSolverDoubleNotConvergedException
	 *             the iterative solver double not converged exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void computeBEq(String boundaryConditions)
			throws IterativeSolverDoubleNotConvergedException, IOException {

		// allocate the memory for eta array
		double[] eta = new double[Mesh.Np];
		// new conjugate gradient object
		RCConjugateGradient cg = new RCConjugateGradient(Mesh.Np);

		int[] indexDiag = computeIndexDiag();

		if (boundaryConditions.equals("Dirichlet")) {

			SetDirichletBC dirichletBC = new SetDirichletBC();

			FileWriter Rstatfile = new FileWriter(Mesh.outputPathBeqDirichlet);
			PrintWriter errestat = new PrintWriter(Rstatfile);

			System.arraycopy(Mesh.eta, 0, eta, 0, Mesh.eta.length);
			
			for (int t = 0; t < simTime; t += deltat) {

				// initialize eta array
				for (int i = 0; i < eta.length; i++) {
					if (dirichletBC.isNoValue(Mesh.etaDrichelet[i],
							Mesh.NOVALUE)) {

						// not Dirichlet cells
						eta[i] = eta[i];
					} else {

						// Dirichlet cells
						eta[i] = Mesh.etaDrichelet[i];
					}
				}

				double[] matT = dirichletBC.computeT(eta);
				double[] matTDrichelet = dirichletBC.computeTDirichlet(matT);
				double[] matTNoDrichelet = dirichletBC
						.computeTNoDirichlet(matT);

				double[] arrb = dirichletBC.computeB(eta, matTDrichelet,
						Mesh.etaDrichelet);

				eta = dirichletBC.newtonIteration(arrb, matTNoDrichelet,
						indexDiag, eta, cg);

				System.out.println("Simulation time: " + t / 3600);

			}
			for (int j = 0; j < eta.length; j++) {

				errestat.println(eta[j]);

			}

			errestat.println();
			System.out.println();
			Rstatfile.close();

			System.out.println("Exit code");

		} else {

			FileWriter Rstatfile = new FileWriter(Mesh.outputPathBeqNoDirichlet);
			PrintWriter errestat = new PrintWriter(Rstatfile);

			SetNoDirichletBC noDirichletBC = new SetNoDirichletBC();

			// initialize eta array
			System.arraycopy(Mesh.eta, 0, eta, 0, Mesh.eta.length);

			for (int t = 0; t < simTime; t += deltat) {

				double[] matT = noDirichletBC.computeT(eta);

				double[] arrb = noDirichletBC.computeB(eta);

				eta = noDirichletBC.newtonIteration(arrb, matT, indexDiag, eta,
						cg);

				System.out.println("Simulation time: " + t / 3600);

			}
			for (int j = 0; j < eta.length; j++) {

				errestat.println(eta[j]);

			}

			errestat.println();
			System.out.println();
			Rstatfile.close();

			System.out.println("Exit code");

		}

	}

	public static void main(String[] args)
			throws IterativeSolverDoubleNotConvergedException, IOException {
		String simulationType = "Song";
		String boundaryConditions = "Dirichlet";
		// long start=System.nanoTime();
		@SuppressWarnings("unused")
		Mesh mesh = new Mesh(simulationType);
		BoussinesqEquation beq = new BoussinesqEquation();
		beq.computeBEq(boundaryConditions);

		if (simulationType == "Song") {
			double[] songSol = new double[org.boussinesq.boussinesq.Mesh.Np];
			Song s = new Song(simTime, Mesh.Np, Mesh.hydrConductivity[0]);

			songSol = s.beqSong(Mesh.porosity);

			FileWriter Rstatfile = new FileWriter(
					org.boussinesq.boussinesq.Mesh.outputPathSong);
			PrintWriter errestat = new PrintWriter(Rstatfile);

			for (int j = 0; j < songSol.length; j++) {

				errestat.println(songSol[j]);

			}

			errestat.println();
			System.out.println();
			Rstatfile.close();

		}
		// long end=System.nanoTime();
		// System.out.println("End time: " + (end-start));

		System.exit(1);

	}

}
