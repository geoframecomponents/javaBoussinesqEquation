package org.boussinesq.boussinesq.dirichletBoundaryConditions;

import org.boussinesq.boussinesq.computationalDomain.AbstractDomain;

public class ComputeTDirichlet {

	/**
	 * Compute T for Dirichlet cells.
	 * 
	 * @desc according the head-based Boundary Conditions (Dirichlet) at the
	 *       equation (30) of [Cordano & Rigon, 2012], the matrix T in row
	 *       compressed form for a Dirichlet cell is computed only if the cell
	 *       that I'm observing or the adjacency cell are a Dirichlet cell.
	 *       Other T is imposed equal to zero.
	 * 
	 * @param mesh
	 *            the object mesh is passed so every field of the mesh class is
	 *            available
	 * @param T
	 *            the array of T in Row Compressed Form
	 * 
	 * @return the array of T in RC-F for Dirichlet cells
	 */
	public double[] computeTDirichlet(double[] T, IsNoValue verifyDirichlet) {

		/*
		 * the matrix T is an array because this code uses the Row Compressed
		 * Form to stored sparse matrix
		 */
		double[] arrayT = new double[T.length];

		/* for-loop to analyze the mesh cell by cell */
		for (int i = 0; i < AbstractDomain.Np; i++) {

			if (!verifyDirichlet.isNoValue(AbstractDomain.etaDirichlet[i],
					AbstractDomain.NOVALUE)) {

				// Dirichlet cells
				for (int j = AbstractDomain.Mp[i]; j < AbstractDomain.Mp[i + 1]; j++) {
					arrayT[j] = T[j];
				}
			} else {

				// non Dirichlet cells
				/*
				 * nested for-loop to analyze shared edges between the i-th cell
				 * and the Mi[j]-th cell
				 */
				for (int j = AbstractDomain.Mp[i]; j < AbstractDomain.Mp[i + 1]; j++) {

					if (!verifyDirichlet.isNoValue(
							AbstractDomain.etaDirichlet[AbstractDomain.Mi[j]],
							AbstractDomain.NOVALUE)) {

						// adjacent Dirichlet cell
						arrayT[j] = T[j];
					} else {

						// adjacent non Dirichlet cell
						arrayT[j] = 0.0;
					}
				}

			}

		}

		return arrayT;
	}

}
