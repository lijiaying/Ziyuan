/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.strategies.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sav.common.core.utils.CollectionBuilder;
import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 *
 */
public class AgentVmRunner extends VMRunner {
	protected String agentJarPath;
	protected Map<String, String> agentParams;
	private List<String> programArgs;

	public AgentVmRunner(String agentJarPath) {
		this.agentJarPath = agentJarPath;
		agentParams = new HashMap<String, String>();
		programArgs = new ArrayList<String>();
	}
	
	@Override
	protected void buildVmOption(CollectionBuilder<String, ?> builder,
			VMConfiguration config) {
		StringBuilder sb = new StringBuilder("-javaagent:").append(agentJarPath);
		List<String> agentParams = getAgentParams();
		if (agentParams != null) {
			sb.append("=")
				.append(StringUtils.join(agentParams, ","));
		}
		builder.add(sb.toString());
		super.buildVmOption(builder, config);
	}
	
	@Override
	protected void buildProgramArgs(VMConfiguration config,
			CollectionBuilder<String, Collection<String>> builder) {
		super.buildProgramArgs(config, builder);
		for (String arg : programArgs) {
			builder.add(arg);
		}
	}
	
	public List<String> getProgramArgs() {
		return programArgs;
	}
	
	public void setProgramArgs(List<String> arguments) {
		this.programArgs = arguments;
	}

	public void addProgramArg(String opt, String... values) {
		addProgramArg(opt, Arrays.asList(values));
	}
	
	public void addProgramArg(String opt, List<String> values) {
		programArgs.add("-" + opt);
		for (String value : values) {
			programArgs.add(value);
		}
	}

	private List<String> getAgentParams() {
		ArrayList<String> params = new ArrayList<String>();
		if (!agentParams.isEmpty()) {
			for (Entry<String, String> entry : agentParams.entrySet()) {
				params.add(newAgentOption(entry.getKey(), entry.getValue()));
			}
		}
		appendAgentParams(params);
		return params;
	}

	protected void appendAgentParams(ArrayList<String> params) {
		// override if needed.
	}

	protected String newAgentOption(String opt, String value) {
		return StringUtils.join(getAgentOptionSeparator(), opt, value);
	}

	protected String getAgentOptionSeparator() {
		return "=";
	}
}
