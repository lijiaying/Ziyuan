package tester;

import java.util.ArrayList;
import java.util.List;

import tzuyu.engine.TzClass;
import tzuyu.engine.TzConfiguration;
import tzuyu.engine.iface.ITzManager;
import tzuyu.engine.model.Query;
import tzuyu.engine.model.RelativeNegativeIndex;
import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.Statement;
import tzuyu.engine.model.StatementKind;
import tzuyu.engine.model.TVAnswer;
import tzuyu.engine.model.TzuYuAction;
import tzuyu.engine.model.VarIndex;
import tzuyu.engine.model.Variable;
import tzuyu.engine.store.MethodParameterStore;
import tzuyu.engine.utils.Pair;
import tzuyu.engine.utils.Permutation;
import tzuyu.engine.utils.Randomness;

/**
 * 
 * The random generation strategy to generate parameters, the random style is
 * adopted from the Randoop approach except that the receiver of the statement
 * is the one after calling the previous statement.
 * 
 * @author Spencer Xiao
 * 
 */
public class RandomTCGStrategy implements ITCGStrategy {

	private IParameterSelector selector;
	private TestCaseStore store;

	private MethodParameterStore parameterStore;
	private TzClass project;
	private TzConfiguration config;

	public RandomTCGStrategy(ITzManager<?> prjFactory) {
		selector = new ParameterSelector(prjFactory);
		store = new TestCaseStore();
		parameterStore = new MethodParameterStore();
	}

	public void setProject(TzClass project) {
		this.project = project;
		this.config = project.getConfiguration();
		selector.setProject(project);
	}

	/*
	 * This method generates parameter for a trace in which there is no
	 * constructor call, thus for each trace we add an implicit constructor
	 * call.
	 * 
	 * For each trace we start from the first statement to the last one without
	 * using the success prefix in order to generate more parameters for each
	 * statement.
	 * 
	 * @see tester.ITCGStrategy#generate(core.Query)
	 */
	public List<TestCase> generate(Query trace) {

		// Get the main object

		TestCase current = TestCase.epsilon;
		Variable receiver = null;
		int size = trace.size();
		for (int index = 0; index < size; index++) {
			TzuYuAction stmt = trace.getStatement(index);
//			TODO LLT: the statements in comments are for support static methods			
//			boolean isStatic = stmt.getAction().isStatic();
//			if (current.getReceiver() != null) {
//				receiver = current.getReceiver();
//			}
			receiver = current.getReceiver();
			List<TestCase> goodTraces = new ArrayList<TestCase>();

			int currentSize = index + 1;
			Query currentQuery = trace.getSubQuery(currentSize);
			boolean noArgument = stmt.getAction().hasNoArguments();
			// ensure the statement will be executed once
			boolean hasArg = true;
			// test sub queries.
			for (int count = 0; count < config.getTestsPerQuery() && hasArg; count++) {
				hasArg = !noArgument;
				List<Variable> rawParams = generateParamsForStatement(receiver,
						stmt);

				// ///////////////////////////////////////////////////////////////////
				// Start to process the returned raw parameters by updating the
				// sequences and the statement index
				List<Variable> parameters = new ArrayList<Variable>(
						rawParams.size());
				int stmtCount = 0;
				List<Sequence> sequences = new ArrayList<Sequence>();
				List<VarIndex> indices = new ArrayList<VarIndex>();
				
				/* TODO LLT: for static methods
				int firstIdx = 0;
				if (isStatic && receiver != null) {
					// if the current method is static, it won't have a
					// constructor receiver from the previous stmts, but we
					// still need to collect the previous sequence to
					// concatenate with the new one.
					sequences.add(receiver.owner);
					stmtCount += receiver.owner.size();
				} else if (current.getReceiver() == null
						&& current != TestCase.epsilon
						&& current.getTrace()
								.getStatement(current.getTrace().size() - 1)
								.getAction().isStatic()) {
					// the previous one is static
					Sequence lastSequence = current.getSequence();
					sequences.add(lastSequence);
					if (!isStatic && receiver != null) {
						// the previous one is static, and this is non-static
						// and before static method is also a non-static method.
						firstIdx = 1;
						indices.add(VarIndex.plus(rawParams.get(0).getVarIndex(), stmtCount));
					}
					stmtCount += lastSequence.size();
				}
				

				for (int i = firstIdx; i < rawParams.size(); i++) {
					Variable raw = rawParams.get(i);
					indices.add(VarIndex.plus(raw.getVarIndex(), stmtCount));
					sequences.add(raw.owner);
					stmtCount += raw.owner.size();
				}
				*/
				
				for (Variable raw : rawParams) {
					indices.add(VarIndex.plus(raw.getVarIndex(), stmtCount));
					sequences.add(raw.owner);
					stmtCount += raw.owner.size();
				}

				Sequence newSeq = Sequence.concatenate(sequences);
				int seqSize = newSeq.size();

				for (VarIndex varIdx : indices) {
					parameters.add(new Variable(newSeq, varIdx));
				}
				// End of processing of raw parameters
				// ////////////////////////////////////////////////////////////////////

				// Then execute the parameters to decide whether the parameter
				// sequence
				// is valid. We would prefer the parameter sequences that could
				// pass the
				// guard condition test.
				ExecutionResult result = RuntimeExecutor.executeGuard(stmt,
						parameters);

				List<RelativeNegativeIndex> inputs = new ArrayList<RelativeNegativeIndex>();

				for (Variable var : parameters) {
					RelativeNegativeIndex relIdx = new RelativeNegativeIndex(
							-(seqSize - var.getStmtIdx()), var.getArgIdx());
					inputs.add(relIdx);
				}

				Statement statement = new Statement(stmt, inputs);
				TestCase newCase;
				if (result.isPassing()) {
					List<Object> runtime = result.getRuntime();
					boolean isNormal = RuntimeExecutor.executeStatement(stmt,
							runtime);
					if (isNormal) {
						if (stmt.getAction().isConstructor()
								&& receiver != null) {
							TestCaseNode node = new TestCaseNode(
									TVAnswer.Rejecting, statement, newSeq);
							newCase = current.extend(node);
						} else {
							TestCaseNode node = new TestCaseNode(
									TVAnswer.Accepting, statement, newSeq);
							newCase = current.extend(node);
							goodTraces.add(newCase);
						}
					} else {
						TestCaseNode node = new TestCaseNode(
								TVAnswer.Rejecting, statement, newSeq);
						newCase = current.extend(node);
					}
				} else {
					TestCaseNode node = new TestCaseNode(TVAnswer.Unknown,
							statement, newSeq);
					newCase = current.extend(node);
				}
				store.addTestCase(newCase);
			}

			if (goodTraces.size() > 0) {
				// there is good traces to continue
				current = Randomness.randomMember(goodTraces);
			} else {
				// all traces are bad traces, we return
				List<TestCase> normalCases = store
						.selectNormalTraces(currentQuery);
				List<TestCase> errorCases = store
						.selectErrorTraces(currentQuery);
				List<TestCase> unknownCases = store
						.selectUnkownTraces(currentQuery);
				List<TestCase> result = new ArrayList<TestCase>();
				result.addAll(normalCases);
				result.addAll(errorCases);
				result.addAll(unknownCases);

				return result;
			}
		}

		List<TestCase> normalCases = store.selectNormalTraces(trace);
		List<TestCase> errorCases = store.selectErrorTraces(trace);
		List<TestCase> unknownCases = store.selectUnkownTraces(trace);
		List<TestCase> result = new ArrayList<TestCase>();
		result.addAll(normalCases);
		result.addAll(errorCases);
		result.addAll(unknownCases);

		return result;
	}

	/**
	 * This method is used to generate test cases for a trace which treats
	 * constructors as normal methods.
	 * 
	 * @param trace
	 * @return
	 */
	@Deprecated
	public List<TestCase> generate2(Query trace) {

		TestCase prefix = findAGoodMaximumPrefix(trace);
		if (!prefix.isEpsilon() && !prefix.isNormal()) {
			List<TestCase> result = new ArrayList<TestCase>();
			result.add(prefix);
			return result;
		}

		Query remainningQuery = trace.getRemainingQuery(prefix.getTrace());

		int size = remainningQuery.size();
		TestCase current = prefix;
		int prefixSize = prefix.getTrace().size();
		for (int index = 0; index < size; index++) {

			List<TestCase> goodTraces = new ArrayList<TestCase>();

			TzuYuAction stmt = remainningQuery.getStatement(index);
			Variable receiver = current.getReceiver();
			int currentSize = prefixSize + index + 1;
			Query currentQuery = trace.getSubQuery(currentSize);
			boolean noArgument = stmt.getAction().hasNoArguments();
			// ensure the statement will be executed once
			boolean hasArg = true;
			for (int count = 0; count < config.getTestsPerQuery() && hasArg; count++) {
				hasArg = !noArgument;
				List<Variable> rawParams = generateParamsForStatement2(
						receiver, stmt);

				// ///////////////////////////////////////////////////////////////////
				// Start to process the returned raw parameters by updating the
				// sequences and the statement index
				List<Variable> parameters = new ArrayList<Variable>(
						rawParams.size());
				int stmtCount = 0;
				List<Sequence> sequences = new ArrayList<Sequence>();
				List<VarIndex> indices = new ArrayList<VarIndex>();

				for (Variable raw : rawParams) {
					indices.add(new VarIndex(raw.getStmtIdx() + stmtCount,
							raw.getArgIdx()));
					sequences.add(raw.owner);
					stmtCount += raw.owner.size();
				}

				Sequence newSeq = Sequence.concatenate(sequences);
				int seqSize = newSeq.size();

				for (VarIndex varIdx : indices) {
					parameters.add(new Variable(newSeq, varIdx.getStmtIdx(),
							varIdx.getArgIdx()));
				}
				// End of processing of raw parameters
				// ////////////////////////////////////////////////////////////////////

				// Then execute the parameters to decide whether the parameter
				// sequence
				// is valid. We would prefer the parameter sequences that could
				// pass the
				// guard condition test.
				ExecutionResult result = RuntimeExecutor.executeGuard(stmt,
						parameters);

				List<RelativeNegativeIndex> inputs = new ArrayList<RelativeNegativeIndex>();

				for (Variable var : parameters) {
					RelativeNegativeIndex relIdx = new RelativeNegativeIndex(
							-(seqSize - var.getStmtIdx()), var.getArgIdx());
					inputs.add(relIdx);
				}

				Statement statement = new Statement(stmt, inputs);

				if (result.isPassing()) {
					List<Object> runtime = result.getRuntime();
					boolean isNormal = RuntimeExecutor.executeStatement(stmt,
							runtime);
					if (isNormal) {
						if (stmt.getAction().isConstructor()
								&& receiver != null) {
							TestCaseNode node = new TestCaseNode(
									TVAnswer.Rejecting, statement, newSeq);
							TestCase newCase = current.extend(node);
							store.addTestCase(newCase);
						} else {
							TestCaseNode node = new TestCaseNode(
									TVAnswer.Accepting, statement, newSeq);
							TestCase newCase = current.extend(node);
							goodTraces.add(newCase);
							store.addTestCase(newCase);
						}
					} else {
						TestCaseNode node = new TestCaseNode(
								TVAnswer.Rejecting, statement, newSeq);
						TestCase newCase = current.extend(node);
						store.addTestCase(newCase);
					}
				} else {
					TestCaseNode node = new TestCaseNode(TVAnswer.Unknown,
							statement, newSeq);
					TestCase newCase = current.extend(node);
					store.addTestCase(newCase);
				}
			}

			if (goodTraces.size() > 0) {
				// there is good traces to continue
				current = Randomness.randomMember(goodTraces);
			} else {
				// all traces are bad traces, we return
				List<TestCase> normalCases = store
						.selectNormalTraces(currentQuery);
				List<TestCase> errorCases = store
						.selectErrorTraces(currentQuery);
				List<TestCase> unknownCases = store
						.selectUnkownTraces(currentQuery);
				List<TestCase> result = new ArrayList<TestCase>();
				result.addAll(normalCases);
				result.addAll(errorCases);
				result.addAll(unknownCases);

				return result;
			}
		}

		List<TestCase> normalCases = store.selectNormalTraces(trace);
		List<TestCase> errorCases = store.selectErrorTraces(trace);
		List<TestCase> unknownCases = store.selectUnkownTraces(trace);
		List<TestCase> result = new ArrayList<TestCase>();
		result.addAll(normalCases);
		result.addAll(errorCases);
		result.addAll(unknownCases);

		return result;
	}

	private TestCase findAGoodMaximumPrefix(Query query) {

		Query currentQuery = query;
		Query prefix = currentQuery;
		while (prefix.size() > 0) {
			TestCase prefixTestCase = store.selectANormalTrace(prefix);
			if (prefixTestCase != null) {
				return prefixTestCase;
			}
			// Try to find the bad sequence for the prefix
			prefixTestCase = store.selectAnErroneousTrace(prefix);
			if (prefixTestCase != null) {
				// the prefix corresponds to a bad sequence,
				// a null value representing there is no good sequence
				// for the longest prefix
				return prefixTestCase;
			}
			currentQuery = prefix;
			prefix = currentQuery.getImmediatePrefix();
		}

		return TestCase.epsilon;
	}

	private List<Variable> generateParamsForStatement(Variable receiver,
			TzuYuAction stmt) {

		// for normal methods we need to distinguish
		// the static method from instance method
		boolean isStatic = stmt.getAction().isStatic();

		List<Class<?>> paramTypes = stmt.getInputTypes();
		List<Variable> parameters = new ArrayList<Variable>(paramTypes.size());
		boolean selectVarFail = false;
		for (int index = 0; index < paramTypes.size() && !selectVarFail; index++) {
			Class<?> type = paramTypes.get(index);
			// The first argument of an instance method is the receiver object
			if (index == 0 && !isStatic) {
				// initialize the receiver for a non-static method.
				if (receiver == null) {
					receiver = selector.selectDefaultReceiver(project
							.getTarget());
				} 
				parameters.add(receiver);
			} else {
				Variable arg = null;
				// try 3 times.
				for (int i = 0; i < 3; i++) {
					arg = selector.selectVariable(receiver, type);
					// Variable arg = selector.selectNewParameter(type);
					SequenceRuntime result = RuntimeExecutor
							.executeSequence(arg.owner);
					if (!result.isSuccessful()) {
						selectVarFail = true;
						continue;
					} else {
						parameters.add(arg);
						parameterStore.add(stmt.getAction(), index, arg);
						selectVarFail = false;
						break;
					}
				}
				
			}
		}
		// Test whether the parameter generation successes
		if (parameters.size() != paramTypes.size()) {
			parameters = getInputsAfterFailed(receiver, stmt);
		}
		return parameters;
	}

	@Deprecated
	private List<Variable> generateParamsForStatement2(Variable receiver,
			TzuYuAction stmt) {

		// for normal methods we need to distinguish
		// the static method from instance method
		boolean isStatic = stmt.getAction().isStatic();

		List<Class<?>> paramTypes = stmt.getInputTypes();
		List<Variable> parameters = new ArrayList<Variable>(paramTypes.size());
		for (int index = 0; index < paramTypes.size(); index++) {
			Class<?> type = paramTypes.get(index);
			// The first argument of an instance method is the receiver object
			if (index == 0 && !isStatic) {
				if (receiver == null) {
					// The method may be inherited from superclass, but we need
					// to test
					// the target class, thus do not use the variable type to
					// generate
					// the receiver
					Variable mainVariable = selector
							.selectNullReceiver(project.getTarget());
					parameters.add(mainVariable);
				} else {
					// Variable receiver = receiver.getReceiver();
					parameters.add(receiver);
				}
			} else {
				for (int i = 0; i < 3; i++) {
					Variable arg = selector.selectNewVariable(type);
					SequenceRuntime result = RuntimeExecutor
							.executeSequence(arg.owner);
					if (!result.isSuccessful()) {
						continue;
					} else {
						parameters.add(arg);
						parameterStore.add(stmt.getAction(), index, arg);
						break;
					}
				}
			}
		}
		// Test whether the parameter generation successes
		if (parameters.size() != paramTypes.size()) {
			parameters = getInputsAfterFailed(receiver, stmt);
		}
		return parameters;
	}

	/**
	 * Get a set of least used parameters for the statement which may not cause
	 * all the statement execution fail.
	 * 
	 * @param receiver
	 * @param statement
	 * @param argIdx
	 *            the list of argument index to choose.
	 * @return
	 */
	private List<Variable> getInputsAfterFailed(Variable receiver,
			TzuYuAction statement) {
		List<Variable> variables = new ArrayList<Variable>();
		StatementKind stmt = statement.getAction();

		List<Class<?>> inputTypes = statement.getInputTypes();

		List<Integer> inputSizes = getInputSize(stmt);
		Permutation permutation = new Permutation(inputSizes, true);
		while (permutation.hasNext()) {

			List<Integer> argIdx = permutation.next();

			boolean isStatic = stmt.isStatic();

			for (int index = 0; index < inputTypes.size(); index++) {

				boolean isReceiver = (index == 0 && !isStatic);

				if (isReceiver) {
					variables.add(receiver);
				} else {
					Variable var = parameterStore.getLRU(stmt, index,
							argIdx.get(index));
					variables.add(var);
				}
			}
		}

		return variables;

	}

	/**
	 * Get a list of integers, each corresponds to the number of different
	 * arguments that have been generated for the parameter of the statement. If
	 * the statement is an instance method, the first element is always 1; If
	 * the statement is a class method (a.k.a. static method) the first element
	 * is the size of the first non-receiver parameter values.
	 * 
	 * @param stmt
	 * @return
	 */
	private List<Integer> getInputSize(StatementKind stmt) {
		List<Class<?>> inputTypes = stmt.getInputTypes();
		List<Integer> inputSizes = new ArrayList<Integer>();

		boolean isStatic = stmt.isStatic();

		for (int index = 0; index < inputTypes.size(); index++) {
			boolean isReceiver = (!isStatic && index == 0);
			if (isReceiver) {
				inputSizes.add(1);
			} else {
				int argSize = parameterStore.getParameterSize(stmt, index);
				inputSizes.add(argSize);
			}
		}
		return inputSizes;
	}

	public List<TestCase> findFailedEvidenceForUnknownStatement(TzuYuAction stmt) {
		return store.findFailedEvidence(stmt);
	}

	public List<TestCase> getAllGeneratedTestCases() {
		Pair<List<TestCase>, List<TestCase>> representatives = store
				.getRepresentativeForAllTestCases();

		List<TestCase> resultCases = new ArrayList<TestCase>();
		resultCases.addAll(representatives.first());
		resultCases.addAll(representatives.second());
		return resultCases;
	}

	public Pair<List<TestCase>, List<TestCase>> getAllTestcases(boolean pass, boolean fail) {
		return store.getTestcases(pass, fail);
	}
	
	@Override
	public Pair<List<Sequence>, List<Sequence>> getAllTestSequences(boolean pass, boolean fail) {
		// Only retrieve good test cases
		Pair<List<TestCase>, List<TestCase>> cases = getAllTestcases(project.getConfiguration()
				.isPrintPassTests(), project.getConfiguration()
				.isPrintFailTests());
		return Pair.of(extractSequences(cases.a), extractSequences(cases.b));
	}
	
	private List<Sequence> extractSequences(List<TestCase> tcs) {
		List<Sequence> seqs = new ArrayList<Sequence>(tcs.size());
		for (TestCase tc : tcs) {
			seqs.add(tc.getSequence());
		}
		return seqs;
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see tester.ITCGStrategy#countTcs(java.lang.Boolean)
	 */
	public int countTcs(Boolean option) {
		return store.countTcs(option);
	}
}
