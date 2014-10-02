/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package gentest;

import gentest.data.Sequence;

import java.util.List;

import junit.FileCompilationUnitPrinter;
import junit.TestsPrinter;

import org.junit.Before;
import org.junit.Test;

import sav.common.core.Pair;
import sav.common.core.SavException;
import testdata.SampleProgram;
import builder.FixTraceGentestBuidler;

/**
 * @author LLT
 *
 */
public class FixTraceGentestBuilderTest extends AbstractGTTest {
	private String srcPath;
	
	@Before
	public void beforeMethod()  {
		srcPath = config.getTestScrPath("gentest");
	}
	
	@Test
	public void testGenerate() throws SavException {
		FixTraceGentestBuidler builder = new FixTraceGentestBuidler(100);
		Pair<List<Sequence>, List<Sequence>> tcs = builder.forClass(SampleProgram.class)
			.method("Max", "m")
			.evaluationMethod(SampleProgram.class, "checkMax")
			.param("m.a", "m.b", "m.c", "m.return")
			.generate();
		TestsPrinter printer = new TestsPrinter(srcPath, 
				"testdata.gentest.pass", "testdata.gentest.fail", 
				"test", SampleProgram.class.getSimpleName());
		printer.setCuPrinter(new FileCompilationUnitPrinter());
		printer.printTests(tcs);
	}
}