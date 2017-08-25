import java.io.IOException;
import java.math.BigInteger;

import dk.alexandra.fresco.framework.sce.SCEFactory;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.SequentialEvaluator;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.sce.resources.ResourcePoolImpl;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.builder.NumericIOBuilder;
import dk.alexandra.fresco.lib.lp.Matrix;
import dk.alexandra.fresco.suite.ProtocolSuite;
import dk.alexandra.fresco.suite.dummy.bool.DummyProtocolSuite;
import dk.alexandra.fresco.demo.cli.CmdLineUtil;
import dk.alexandra.fresco.demo.helpers.DemoNumericApplication;
import dk.alexandra.fresco.demo.helpers.ProtocolBuilderHelper;
import dk.alexandra.fresco.demo.helpers.ResourcePoolHelper;
import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.BuilderFactory;
import dk.alexandra.fresco.framework.Computation;
import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.builder.ComputationBuilder;
import dk.alexandra.fresco.framework.builder.NumericBuilder;
import dk.alexandra.fresco.framework.builder.ProtocolBuilder;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderNumeric.SequentialNumericBuilder;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import fresco.LogisticRegression;
import fresco.dsl.Add;
import fresco.dsl.Cached;
import fresco.dsl.ClosedInt;
import fresco.dsl.Divide;
import fresco.dsl.DummyApplication;
import fresco.dsl.DummyResourcePool;
import fresco.dsl.Expression;
import fresco.dsl.FixedPoint;
import fresco.dsl.Generator;
import fresco.dsl.IntExpression;
import fresco.dsl.KnownInt;
import fresco.dsl.Multiply;
import fresco.dsl.SquareRoot;
import fresco.dsl.Subtract;
import fresco.dsl.Truncate;
import plain.MatrixKt;
import plain.MatrixType;
import plain.Vector;
import fresco.dsl.GentestKt;

class TestSqrtApp implements Application<BigInteger, ProtocolBuilderNumeric.SequentialNumericBuilder> {

	@Override
	public Computation<BigInteger> prepareApplication(SequentialNumericBuilder producer) {
		
		//producer.advancedNumeric().sqrt(in, 32);
		return null;
	}
	
}



class MyDummyApplication implements Application<BigInteger, ProtocolBuilderNumeric.SequentialNumericBuilder> {
	Expression expression;
	
	MyDummyApplication(Expression expression) {
		this.expression = expression;
	}

	public Computation<BigInteger> prepareApplication(SequentialNumericBuilder builder) {
        Computation<SInt> computation = expression.build(builder);
        Computation<BigInteger> open = builder.numeric().open(computation);        
        return open;
    }
}

class EvalOpenMatrixApp implements Application<plain.MatrixType, SequentialNumericBuilder> {
	fresco.dsl.matrices.MatrixType mat;
	
	EvalOpenMatrixApp(fresco.dsl.matrices.MatrixType mat) {
		this.mat = mat;
	}
	
	private BigInteger toSigned(BigInteger val, BigInteger mod) {
	    if (val.subtract(mod.shiftRight(1)).signum() == 1)
	        val = val.subtract(mod);
	    return val;		
	}
	
	@Override
	public Computation<plain.MatrixType> prepareApplication(SequentialNumericBuilder bld) {
		
		NumericBuilder nb = bld.numeric();
		BigInteger mod = bld.factory.getBasicNumericFactory().getModulus();
		
		System.out.println("mod=" + mod);
		System.out.println("bl=" + bld.factory.getBasicNumericFactory().getMaxBitLength());
		
		@SuppressWarnings("rawtypes")
		Computation[][] outs = new Computation[mat.getNumberOfRows()][mat.getNumberOfColumns()];
		
	    for (int i = 0; i < mat.getNumberOfRows(); i++) {
	    	for (int j = 0; j < mat.getNumberOfColumns(); j++) {
	    		Computation<SInt> cij = mat.get(i, j).build(bld);
	    		outs[i][j] = nb.open(cij);
	    	}
	    }
	    
	    //System.out.println("Returning");
	    
		return new Computation<plain.MatrixType>() {
			@Override
			public plain.MatrixType out() {
				//System.out.println("Doing out");
				Double[][] ret = new Double[mat.getNumberOfRows()][mat.getNumberOfColumns()];
				
			    for (int i = 0; i < mat.getNumberOfRows(); i++) {
			    	for (int j = 0; j < mat.getNumberOfColumns(); j++) {
			    		BigInteger outij = (BigInteger) outs[i][j].out();
			    		outij = toSigned(outij, mod);
			    		ret[i][j] = fresco.dsl.FixedPointExpressionKt.asFixedPoint(outij); //outij.doubleValue();
			    		//System.out.println("val " + outij + " double val " + ret[i][j]);
			    		//
			    		//ret[i][j] = ((BigInteger) outs[i][j].out()).doubleValue();///Math.pow(2,16);
			    	}
			    }
			    
			    return new plain.Matrix(ret);
			}
		};
	}
}
		
class EvalOpenVectorApp implements Application<plain.Vector, SequentialNumericBuilder> {
	fresco.dsl.matrices.Vector vec;
	
	EvalOpenVectorApp(fresco.dsl.matrices.Vector vec) {
		this.vec = vec;
	}
	
	private BigInteger toSigned(BigInteger val, BigInteger mod) {
	    if (val.subtract(mod.shiftRight(1)).signum() == 1)
	        val = val.subtract(mod);
	    return val;		
	}
	
	private String exToString(Expression ex) {
		
		if (ex instanceof FixedPoint) {
			return "f:" + exToString(((FixedPoint) ex).getUnderlyingInt());
		}
		
		String str = "";
		Cached cex = (Cached) ex;
		if (cex.getLatestVal() != null)
			return "*" + ex;
		
		if (ex instanceof Add) {
			Add addex = (Add) ex;
			return "(" + exToString(addex.getLeft()) + ")+(" + exToString(addex.getRight()) + ")";
		} else if (ex instanceof Subtract) {
			Subtract addex = (Subtract) ex;
			return "(" + exToString(addex.getLeft()) + ")-(" + exToString(addex.getRight()) + ")";
		} else if (ex instanceof Multiply) {
			Multiply addex = (Multiply) ex;
			return "(" + exToString(addex.getLeft()) + ")*(" + exToString(addex.getRight()) + ")";
		} else if (ex instanceof Divide) {
			Divide addex = (Divide) ex;
			return "(" + exToString(addex.getLeft()) + ")/(" + exToString(addex.getRight()) + ")";
		} else if (ex instanceof SquareRoot) {
			SquareRoot addex = (SquareRoot) ex;
			return "sqrt(" + exToString(addex.getExpr()) + ")";
		} else if (ex instanceof ClosedInt) {
			ClosedInt addex = (ClosedInt) ex;
			return "[" + addex.getInputParty() + ":" + addex.getValue() + "]";
		} else if (ex instanceof KnownInt) {
			KnownInt addex = (KnownInt) ex;
			return "<" + addex.getValue() + ">";			
		} else if (ex instanceof Truncate) {
			Truncate addex = (Truncate) ex;
			return "{" + exToString(addex.getExpr()) + "/" + addex.getBits() + "}";
		} else {
			return "?" + ex;
		}
	}
	
	@Override
	public Computation<plain.Vector> prepareApplication(SequentialNumericBuilder bld) {
		
		NumericBuilder nb = bld.numeric();
		BigInteger mod = bld.factory.getBasicNumericFactory().getModulus();
		
		System.out.println("mod=" + mod);
		System.out.println("bl=" + bld.factory.getBasicNumericFactory().getMaxBitLength());
		
		@SuppressWarnings("rawtypes")
		Computation[] out = new Computation[vec.getSize()];
		
	    for (int i = 0; i < vec.getSize(); i++) {
	    	Expression ex = vec.get(i);
	    	//System.out.println("ex: " + exToString(ex));
	    	Computation<SInt> cij = ex.build(bld);
	    	//System.out.println("ex: " + exToString(ex));
	    	out[i] = nb.open(cij);
	    }
	    
		return new Computation<plain.Vector>() {
			@Override
			public plain.Vector out() {
				System.out.println("Calling Vector::out");
				double[] ret = new double[vec.getSize()];
				
			    for (int i = 0; i < vec.getSize(); i++) {
		    		BigInteger outi = (BigInteger) out[i].out();
			    	outi = toSigned(outi, mod);
			    	ret[i] = fresco.dsl.FixedPointExpressionKt.asFixedPoint(outi);
			    }
			    
			    return new plain.Vector(ret);
			}
		};
	}
}

class CoroutineRunner<T> implements ComputationBuilder<T> {
	Generator<Object,Object> gen;
	
	CoroutineRunner(Generator<Object,Object> gen) {
		this.gen = gen;
	}
	
	@SuppressWarnings("rawtypes")
	Application app = null;
	
	T output;

	@SuppressWarnings("unchecked")
	@Override
	public Computation<T> build(SequentialNumericBuilder builder) {
		return builder.seq(
				bld -> {
					System.out.println("In build");
					Object out = gen.next(new Object());
					app = new EvalOpenVectorApp((fresco.dsl.matrices.Vector) out);
					System.out.println("Created app");
					return app.prepareApplication(bld);					
				}
				).whileLoop(
				a -> a!=null,
				(a,bld) -> {
					System.out.println("Here I am again" + a + "/" + bld);
					System.out.println("In build");
					Object out = gen.next(a);
					System.out.println("Got" + out);
					if (out == null) {
						output = (T) a;
						return new Computation() {
							@Override
							public Object out() {
								// TODO Auto-generated method stub
								return null;
							}
						};
					}
					app = new EvalOpenVectorApp((fresco.dsl.matrices.Vector) out);
					System.out.println("Created app");
					return app.prepareApplication((SequentialNumericBuilder) bld);					
				}
				).seq((whileState, seq) -> new Computation() {
					@Override
					public Object out() {
						return output;
					}
				});
		
	}
//		return producer.seq(seq -> {
//			System.out.println("*** Initialization");
//			return () -> new Object();
//		}).whileLoop(
//			state -> state != null,
//			(state, bld) -> {
//				System.out.println("Calling with state=" + state);
//				Object out = gen.next(state);
//				System.out.println("Evaluating " + out);
//				if (out instanceof fresco.dsl.matrices.Vector) {
//					app = new EvalOpenVectorApp((fresco.dsl.matrices.Vector) out);
//				} else if (out instanceof fresco.dsl.matrices.MatrixType) {
//					app = new EvalOpenMatrixApp((fresco.dsl.matrices.MatrixType) out);
//				} else {
//					System.out.println("Unrecognized object: " + out);
//				}
//				System.out.println("Built app " + app);
//				return app.prepareApplication(bld);
//				//return bld.seq(bldi -> app.prepareApplication(bldi)).seq((a,b) -> new Computation<Object>() {
//				//	@Override
//				//	public Object out() {
//				//		System.out.println("Calling out()");
//				//		return b;
//				//	}
//				//	
//				//});
//				
////				Computation comp = app.prepareApplication(bld);
////				System.out.println("Returning a new computation");
////				return new Computation<Object>() {
////					@Override
////					public Object out() {
////						System.out.println("Calling out(), returing " + comp.out());
////						return comp.out();
////					}					
////				}; 
//			}
//		).seq((whileState, seq) -> () -> (T) whileState); // cast to final output
//	}
//	
}

public class Test {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void runApplication(SecureComputationEngine sce,
		      ResourcePool resourcePool) throws IOException {
        
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
        
//        val Xs = arrayOf(X1, X2);
//        val Ys = arrayOf(am1, am2);
        
        LogisticRegression logistic = new LogisticRegression();
        
        double lambda = 1.0;
        int numberOfIterations = 4;
        
        MatrixType[] Xs = new MatrixType[] { X1, X2 };
        Vector[] Ys = new Vector[] { am1, am2 };
        
        Generator<Object,Object> obj = logistic.fitLogisticModelCoroutine(Xs, Ys, lambda, numberOfIterations);
        
        CoroutineRunner<plain.Vector> crun = new CoroutineRunner<plain.Vector>(obj);
        
        Application<plain.Vector, SequentialNumericBuilder> app = new Application() {
			public Computation prepareApplication(ProtocolBuilder producer) {				
				SequentialNumericBuilder nb = (SequentialNumericBuilder) producer;
				return crun.build(nb);
			}
        };
         
        plain.Vector beta = (Vector) sce.runApplication(app, resourcePool);
        
        System.out.println("Beta=" + beta);
        
//        //// compute hessian
//        //fresco.dsl.matrices.MatrixType shess = (fresco.dsl.matrices.MatrixType) obj.next(new Object());
//        //plain.MatrixType hess = (plain.MatrixType) sce.runApplication(new EvalOpenMatrixApp(shess), resourcePool);
//        Object hess = new Object();
//        
//        System.out.println("Hessian = " + hess);
//        
//        Object toSend = hess;
//        plain.Vector vec = null;
//        
//        for (int i = 0; i < 8; i++) {
//        	// compute next beta
//        	fresco.dsl.matrices.Vector svec = (fresco.dsl.matrices.Vector) obj.next(toSend);
//        	if (svec == null) break;
//        	vec = (plain.Vector) sce.runApplication(new EvalOpenVectorApp(svec), resourcePool);
//        	System.out.println("Beta = " + vec);
//        	toSend = vec;
//        }
//        
//        System.out.println("Final beta = " + vec);
        
        
        //BigInteger a = (BigInteger) sce.runApplication(new MyDummyApplication((Expression) mat0), resourcePool);
        //a = fresco.dsl.DummyEvaluationKt.toSigned(a);
        //System.out.println("Dummy works: " + a + "/" + fresco.dsl.FixedPointExpressionKt.asFixedPoint(a));
        
        //fresco.dsl.matrices.MatrixType mat = (fresco.dsl.matrices.MatrixType) mat0;
        //System.out.println("First call: " + mat);
        //plain.MatrixType eval = sce.runApplication(new EvalOpenMatrixApp(mat), resourcePool);
        //System.out.println("First res: " + eval);
        
        //fresco.dsl.matrices.Vector beta = logistic.fitLogisticModel(Xs, Ys, lambda, numberOfIterations);
		
		
		//ClosedInt ci = new ClosedInt(15, 1);
		//KnownInt ki = new KnownInt(23);
		//IntExpression prod = ci.times(ki);
		    //KnownInt ki = new KnownInt(5);
		//BigInteger res = (BigInteger) sce.runApplication(new DummyApplication(prod), resourcePool);
		
		//Generator<Object,Object> obj = GentestKt.yielder();
		//IntExpression ex = (IntExpression) obj.next(new Object());
		//BigInteger res = (BigInteger) sce.runApplication(new DummyApplication(ex), resourcePool);
		//IntExpression ex2 = (IntExpression) obj.next(res);
		//BigInteger res2 = (BigInteger) sce.runApplication(new DummyApplication(ex2), resourcePool);
		    
	    //System.out.println("Result was: " + res + "/" + res2);
    }	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws IOException {
		
	    CmdLineUtil util = new CmdLineUtil();
	    NetworkConfiguration networkConfiguration;

	    util.parse(args);
	    networkConfiguration = util.getNetworkConfiguration();

	    ProtocolSuite psConf = util.getProtocolSuite();
	    SecureComputationEngine sce = new SecureComputationEngineImpl(psConf, util.getEvaluator());

	    ResourcePool resourcePool = ResourcePoolHelper.createResourcePool(
	        psConf, util.getNetworkStrategy(), networkConfiguration);
	    
	    runApplication(sce, resourcePool);
  }		

}
