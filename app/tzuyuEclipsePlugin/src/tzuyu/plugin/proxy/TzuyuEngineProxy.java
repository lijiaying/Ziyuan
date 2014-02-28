/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.proxy;

import tzuyu.engine.TzClass;
import tzuyu.engine.Tzuyu;
import tzuyu.engine.iface.IReferencesAnalyzer;
import tzuyu.engine.iface.TzReportHandler;
import tzuyu.engine.iface.TzuyuEngine;
import tzuyu.plugin.command.gentest.GenTestPreferences;
import tzuyu.plugin.console.TzConsole;
import tzuyu.plugin.core.dto.WorkObject;
import tzuyu.plugin.core.exception.PluginException;
import tzuyu.plugin.reporter.GenTestReporter;
import tzuyu.plugin.reporter.PluginLogger;

/**
 * @author LLT
 * 
 */
public class TzuyuEngineProxy implements TzuyuEngine {
	private Tzuyu tzuyu;

	public TzuyuEngineProxy(TzClass project, TzReportHandler reporter,
			IReferencesAnalyzer refAnalyzer) {
		tzuyu = new Tzuyu(project, reporter, refAnalyzer);
	}

	public void run() {
		tzuyu.run();
	}

	public static void generateTestCases(WorkObject workObject,
			GenTestPreferences config) {
		try {
			TzConsole.showConsole().clearConsole();
			TzClass tzProject = ProjectConverter.from(workObject, config);
			new TzuyuEngineProxy(tzProject, new GenTestReporter(config),
					new PluginReferencesAnalyzer(workObject.getProject())).run();
		} catch (PluginException e) {
			PluginLogger.logEx(e);
		} 
	}

}
