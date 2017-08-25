import java.math.BigInteger;

import fresco.LogisticRegression;
import fresco.dsl.DummyEvaluationKt;
import plain.MatrixKt;
import plain.MatrixType;
import plain.Vector;

public class RunIt {

	public static void main(String[] args) {
        Vector hp1 = new plain.Vector(
                110.0, 110.0, 93.0, 110.0, 175.0, 105.0, 245.0, 62.0,
                95.0, 123.0, 123.0, 180.0, 180.0, 180.0, 205.0, 215.0
        );
        
        Vector hp2 = new plain.Vector(
                230.0, 66.0, 52.0, 65.0, 97.0, 150.0, 150.0, 245.0,
                175.0, 66.0, 91.0, 113.0, 264.0, 175.0, 335.0, 109.0
        );

        Vector wt1 = new plain.Vector(
                2.62, 2.875, 2.32, 3.215, 3.44, 3.46, 3.57, 3.19,
                3.15, 3.44, 3.44, 4.07, 3.73, 3.78, 5.25, 5.424
        );
        
        Vector wt2 = new plain.Vector(
                5.345, 2.2, 1.615, 1.835, 2.465, 3.52, 3.435, 3.84,
                3.845, 1.935, 2.14, 1.513, 3.17, 2.77, 3.57, 2.78
        );
        
        Vector am1 = new plain.Vector(
                1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        );
        
        Vector am2 = new plain.Vector(
                0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
        );
        
        Vector ones1 = new plain.Vector(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1);

        Vector ones2 = new plain.Vector(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1);
        
        MatrixType X1 = MatrixKt.matrixFromVectors(hp1, wt1, ones1).transpose();
        
        MatrixType X2 = MatrixKt.matrixFromVectors(hp2, wt2, ones2).transpose();
        
        double lambda = 1.0;
        int numberOfIterations = 4;
        
        MatrixType[] Xs = new MatrixType[] { X1, X2 };
        Vector[] Ys = new Vector[] { am1, am2 };        
        
        LogisticRegression logistic = new LogisticRegression();
        
        fresco.dsl.matrices.Vector beta = logistic.fitLogisticModel(
                Xs, Ys, lambda = 1.0, numberOfIterations = 4
        );

        Vector result = DummyEvaluationKt.evaluate(beta);
        System.out.println("Result: " + result);
        
        //val intercept = 1.65707
        //val beta_hp = 0.00968555
        //val beta_wt = -1.17481        
	}
}
