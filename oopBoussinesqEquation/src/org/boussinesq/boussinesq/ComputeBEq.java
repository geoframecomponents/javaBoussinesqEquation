package org.boussinesq.boussinesq;

import java.io.IOException;
import java.text.DecimalFormat;

import org.boussinesq.RowCompressedForm.RCConjugateGradient;
import org.boussinesq.RowCompressedForm.RCIndexDiagonalElement;
import org.boussinesq.boussinesq.NOdirichletBoundaryConditions.Solver;
import org.boussinesq.boussinesq.computationalDomain.ComputationalDomain;
import org.boussinesq.machineEpsilon.MachineEpsilon;
import org.francescoS.usefulClasses.FileWrite;
import org.francescoS.usefulClasses.TextIO;

import cern.colt.matrix.tdouble.algo.solver.IterativeSolverDoubleNotConvergedException;

public class ComputeBEq implements TimeSimulation {

	double[] aquiferThickness;
	double[] volumeSource;
	int[] indexDiag;
	double tolerance;

	double[] volume;
	
	double volumeOld = 0;
	double volumeNew = 0;
	
	String fileName = null;
	
	public static long timeCompute;
	public static long timeSolver;
	
	
	Solver newton;
	RCConjugateGradient cg;
	DecimalFormat myformatter;
	RCIndexDiagonalElement rcIndexDiagonalElement;
	MachineEpsilon cMEd;
	
	public ComputeBEq() {
		

		cg = new RCConjugateGradient(ComputationalDomain.Np);
		newton = new Solver();
		
		aquiferThickness = new double[ComputationalDomain.Np];
		volumeSource = new double[ComputationalDomain.Np];
		
		volume = new double[ComputationalDomain.Np];
		
		myformatter = computePattern();
		
		rcIndexDiagonalElement = new RCIndexDiagonalElement();
		cMEd = new MachineEpsilon();
		
	}
	
	public DecimalFormat computePattern(){
		
		int[] c = new int[String.valueOf(SIMULATIONTIME).length()];
		
		StringBuilder builder = new StringBuilder(c.length);
		
		for (int i:c){
			
			builder.append(c[i]);
			
		}
		
		String pattern = builder.toString();
		
		return new DecimalFormat(pattern);
		
	}
	
	public void writeSolution(int time, double[] eta) throws IOException{
		
		FileWrite.writeStringIntString("Iteration number", (int) time/TIMESTEP + 1, "[ ]");
		FileWrite.writeStringDoubleString("Timestep", TIMESTEP, "[s]");
		FileWrite.writeStringDoubleString("Time of simulation", SIMULATIONTIME, "[s]");
		FileWrite.writeStringDoubleString("Time of simulation", SIMULATIONTIME/60, "[min]");
		FileWrite.writeStringDoubleString("Initial volume", volumeOld, "[ m^3]");
		FileWrite.writeStringDoubleString("Total volume", volumeNew, "[m^3]");
		FileWrite.writeStringDoubleString("Time convergence of CG", timeSolver/1000000000.0, "[s]");
		FileWrite.writeFourStringColumn("PiezHead","AquifThick","WaterVolume","Source");
		FileWrite.writeFourStringColumn("[m]", "[m]", "[m^3]", "[m^3/s]");
		FileWrite.writeFourDoubleColumn(eta,aquiferThickness,volume,volumeSource);
		FileWrite.closeTxtFile();
		
	}
	
	public double computeVolume(int index,double eta){
		
		double volume;
		
		volume = PolygonGeometricalWetProperties.computeWaterVolume(eta, ComputationalDomain.bedRockElevation[index], ComputationalDomain.porosity[index], ComputationalDomain.planArea[index]);
				
		volume = volume	- TIMESTEP * ComputationalDomain.planArea[index] * ComputationalDomain.source[index]
						+ TIMESTEP * ComputationalDomain.planArea[index] * ComputationalDomain.c[index] * Math.pow(volume/ComputationalDomain.planArea[index], ComputationalDomain.m[index]);
		
		return volume;
		
	}
		
	public void openTxtFile(int time) throws IOException{
		
		fileName = myformatter.format(time);
		FileWrite.openTxtFile(fileName.concat(".txt"), BoussinesqEquation.solutionDir, false);
		
	}
	
	public void computeOutputFeatures(double[] eta){
		
		for (int j = 0; j < eta.length; j++) {
			
			volumeSource[j] = ComputationalDomain.source[j] * ComputationalDomain.planArea[j];
			aquiferThickness[j] = Math.max(0, eta[j]-ComputationalDomain.bedRockElevation[j]);
			volume[j] = computeVolume(j,eta[j]);
			volumeNew = volumeNew + volume[j];

		}
		
	}
	
	public void computeInitialVolume(){
		
		for (int i = 0; i < ComputationalDomain.Np; i++){
			
			volume[i] = PolygonGeometricalWetProperties.computeWaterVolume(ComputationalDomain.eta[i], ComputationalDomain.bedRockElevation[i], ComputationalDomain.porosity[i], ComputationalDomain.planArea[i]);
			volumeOld = volumeOld + volume[i];
	
		}

		TextIO.putln("Initial volume: " + volumeOld);
		
	}
	
	public void computeVolumeConservation(){
		
		if (Math.abs(volumeNew-volumeOld) > Math.pow(10, -6)){
			
			TextIO.putln("WARNING!!! The system is losing mass");
			TextIO.putln("The difference between initial volume and compute volume is: " + Math.abs(volumeNew-volumeOld));
	
		}


		volumeNew = 0;
		
	}
	
	public double[] solutionMethod(double[] etaOld, double[] matT, double[] arrb) throws IterativeSolverDoubleNotConvergedException{
		
		double[] eta = new double[etaOld.length];
		
		eta = newton.newtonIteration(arrb, matT, indexDiag, etaOld, cg,
				tolerance);
		
		return eta;
		
	}
	
	public void firstThings(){
		
		indexDiag = rcIndexDiagonalElement.computeIndexDiag(ComputationalDomain.Np,
				ComputationalDomain.Mp, ComputationalDomain.Mi);

		tolerance = cMEd.computeMachineEpsilonDouble();
		
	}
	
	public static void main(String[] args) {

	}

}