package icsetlv.svm;

import icsetlv.common.utils.Assert;

import java.util.SortedSet;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;
import net.sf.javaml.classification.AbstractClassifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.apache.commons.lang.ObjectUtils;

/**
 * Wrapper for the libSVM library.
 * More clear and easier interface. 
 */
public class LibSVM extends AbstractClassifier {

	public static svm_print_interface svm_print_console = null;
	public static svm_print_interface svm_print_null = new svm_print_interface() {
		public void print(String s) {
		}
	};
	/* By default console output is turned off. */
	static {
		svm.svm_set_print_string_function(svm_print_null);
	}

	private static final long serialVersionUID = -8901871714620581945L;

	private svm_parameter getDefaultParameters() {
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.C_SVC;
		param.C = 1;
		param.kernel_type = svm_parameter.LINEAR;
		param.degree = 1;
		param.gamma = 0; // 1/k
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 1;
		param.nr_weight = 0;/*4;*/
		param.weight_label = /*new int[]{1, 1, 0, 0};*/new int[0];
		param.weight = /*new double[]{1, 1, 0, 0};*/ new double[0];
		return param;
	}

	/**
	 * Returns a reference to the parameter configuration of the SVM
	 * 
	 * @return the current configuration
	 */
	public svm_parameter getParameters() {
		if (svmParam == null) {
			svmParam = getDefaultParameters();
		}
		return svmParam;
	}

	private static svm_problem transformDataset(Dataset data) {
		svm_problem p = new svm_problem();
		p.l = data.size();
		p.y = new double[data.size()];
		p.x = new svm_node[data.size()][];
		int tmpIndex = 0;
		for (int j = 0; j < data.size(); j++) {
			Instance tmp = data.instance(j);
			p.y[tmpIndex] = data.classIndex(tmp.classValue());
			p.x[tmpIndex] = new svm_node[tmp.keySet().size()];
			int i = 0;
			SortedSet<Integer> tmpSet = tmp.keySet();
			for (int index : tmpSet) {
				p.x[tmpIndex][i] = new svm_node();
				p.x[tmpIndex][i].index = index;
				p.x[tmpIndex][i].value = tmp.value(index);
				i++;
			}
			tmpIndex++;
		}
		return p;
	}

	private svm_parameter svmParam;
	private Dataset data;
	private svm_model model;

	public svm_model getModel(){
		Assert.assertTrue(isClassifierBuiltDone);
		return model;
	}

	/**
	 * Set the parameters that will be used for training.
	 * 
	 * @param param
	 *            a set of parameters
	 */
	public void setParameters(svm_parameter param) {
		this.svmParam = param;
	}



	/**
	 * Set the print interface that will be used for training.
	 * print a print interface. If <code>LibSVM.svm_print_console</code> is
	 * provided then output will be printed to standard out. If
	 * <code>LibSVM.svm_print_null</code> is provided then no output will be
	 * printed.
	 * 
	 * By default this the printmode is set to <code>LibSVM.svm_print_null</code>
	 */
	public static void setPrintInterface(svm_print_interface print) {
		svm.svm_set_print_string_function(print);
	}


	private boolean isClassifierBuiltDone = false;

	@Override
	public void buildClassifier(Dataset data) {
		super.buildClassifier(data);
		this.data = data;
		svm_problem p = transformDataset(data);
		for (int i = 0; i < data.size(); i++) {
			System.out.println(data.get(i));
		}
		System.out.println();
		model = svm.svm_train(p, getParameters());

		double[][] coef = model.sv_coef;

		assert model.SV != null;
		assert model.SV.length > 0;

		double[][] prob = new double[model.SV.length][data.noAttributes()];
		for (int i = 0; i < model.SV.length; i++) {
			for (int j = 0; j < data.noAttributes(); j++) {
				prob[i][j] = 0;
			}
		}
		for (int i = 0; i < model.SV.length; i++) {
			for (int j = 0; j < model.SV[i].length; j++) {
				prob[i][model.SV[i][j].index] = model.SV[i][j].value;
			}
		}
		/* Weights are only available for linear SVMs */
		if (getParameters().svm_type == svm_parameter.C_SVC) {
			double w_list[][][] = new double[model.nr_class][model.nr_class - 1][data
			                                                                     .noAttributes()];

			for (int i = 0; i < data.noAttributes(); ++i) {
				for (int j = 0; j < model.nr_class - 1; ++j) {
					int index = 0;
					int end = 0;
					double acc;
					for (int k = 0; k < model.nr_class; ++k) {
						acc = 0.0;
						index += (k == 0) ? 0 : model.nSV[k - 1];
						end = index + model.nSV[k];
						for (int m = index; m < end; ++m) {
							acc += coef[j][m] * prob[m][i];
						}
						w_list[k][j][i] = acc;
					}
				}
			}

			weights = new double[data.noAttributes()];
			for (int i = 0; i < model.nr_class - 1; ++i) {
				for (int j = i + 1, k = i; j < model.nr_class; ++j, ++k) {
					for (int m = 0; m < data.noAttributes(); ++m) {
						weights[m] = (w_list[i][k][m] + w_list[j][i][m]);

					}
				}
			}
		} else {
			weights = null;
		}
		isClassifierBuiltDone = true;
	}

	private double[] weights;
	private double[] prob_estimates;

	/**
	 * Provides access to the weights the support vectors obtained during
	 * training.
	 * 
	 * @return weight vector
	 */
	public double[] getWeights() {
		Assert.assertTrue(isClassifierBuiltDone);
		return weights;
	}

	public double[] rawDecisionValues(Instance i) {
		return svm_predict_raw(model, convert(i));
	}

	/* Method to get raw decision values */
	private double[] svm_predict_raw(svm_model model, svm_node[] x) {
		if (model.param.svm_type == svm_parameter.ONE_CLASS
				|| model.param.svm_type == svm_parameter.EPSILON_SVR
				|| model.param.svm_type == svm_parameter.NU_SVR) {
			double[] res = new double[1];
			svm.svm_predict_values(model, x, res);
			return res;
		} else {
			int nr_class = model.nr_class;
			double[] dec_values = new double[nr_class * (nr_class - 1) / 2];
			svm.svm_predict_values(model, x, dec_values);
			return dec_values;
		}
	}

	private svm_node[] convert(Instance instance) {
		svm_node[] x = new svm_node[instance.noAttributes()];
		for (int i = 0; i < instance.noAttributes(); i++) {
			x[i] = new svm_node();
			x[i].index = i;
			x[i].value = instance.value(i);
		}
		return x;
	}

	@Override
	public Object classify(Instance instance) {
		svm_node[] x = convert(instance);
		double d = svm.svm_predict(model, x);
		Object out = data.classValue((int) d);
		return out;
	}

	public int[] getLabels() {
		int res[] = new int[model.nr_class];
		svm.svm_get_labels(model, res);
		return res;
	}


	public divider getExplicitDivider(){
		Assert.assertTrue(isClassifierBuiltDone);
		int pSize = data.noAttributes();
		double bias = 0;
		double[] wVec = new double[pSize];

		if(model.nr_class!=2){
			System.out.println("Can not be binary divided...");
		}

		int[] sIndex = new int[model.nr_class];	
		for (int i = 1; i < model.nr_class; i++) {
			sIndex[i] = sIndex[i - 1] + model.nSV[i - 1];
		}
		int p = 0;
		for (int i = 0; i < model.nr_class; i++) {

			for (int j = i + 1; j < model.nr_class; j++) {
				int si = sIndex[i];
				int sj = sIndex[j];
				double[] coef1 = model.sv_coef[j - 1];
				double[] coef2 = model.sv_coef[i];

				for (int k = 0; k < model.nSV[i]; k++) {
					for (int m = 0; m < pSize; m++) {
						wVec[m] += coef1[si + k] * model.SV[si + k][m].value;
					}
				}

				for (int k = 0; k < model.nSV[j]; k++) {
					for (int m = 0; m < pSize; m++) {
						wVec[m] += coef2[sj + k] * model.SV[sj + k][m].value;
					}
				}

				bias = model.rho[p];
				p++;
			}
		}
		divider dv = new divider(wVec, bias);
		return dv;
	}

	/**
	 * Provides access to the accuracy of the trained model
	 * 
	 * @return modelAccuracy
	 */
	public double modelAccuracy() {
		int rightClassification = 0, wrongClassification = 0;
		for (Instance ins : data) {
			if (ObjectUtils.equals(classify(ins), ins.classValue())) {
				rightClassification++;
			} else {
				wrongClassification++;
			}
		}
		Assert.assertTrue(rightClassification + wrongClassification == data.size());
		System.out.println("# Training point: " + data.size());
		System.out.println("# Right classification: " + rightClassification);
		return (double) rightClassification / (double) data.size();
	}
}
