/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import sav.commons.testdata.opensource.TestPackage;
import faultLocalization.FaultLocalizationReport;
import faultLocalization.LineCoverageInfo;

/**
 * @author LLT
 *
 */
public class FaultLocalizationPackageTest extends AbstractTzPackageTest {
	private TzuyuCore tzCore;
	
	@Before
	public void setup() {
		super.setup();
		tzCore = new TzuyuCore(context, appData);
	}
	
	public void runTest(TestPackage testPkg) throws Exception {
		prepare(testPkg);
		FaultLocalizationReport report;
		if (isUseSlicer()) {
			report = tzCore.faultLocalization2(testingClassNames,
					testingPackages, junitClassNames, true);
		} else {
			report = tzCore.faultLocalization(testingClassNames,
					junitClassNames, false);
		}
		List<LineCoverageInfo> lineCoverageInfos = report.getLineCoverageInfos();
		Collections.reverse(lineCoverageInfos);
		printList(lineCoverageInfos);
	}
	
	@Test
	public void testjavaparser46() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "46");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjavaparser57() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "57");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjavaparser63() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "63");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjodatime194() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "194");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjodatime201() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "201");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjodatime187() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "187");
		setUseSlicer(true);
		runTest(testPkg);
	}
	
	@Test
	public void testjavadiffutils18() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "18");
		setUseSlicer(true);
		runTest(testPkg);
	}
	//	End generated part
}
