package edu.upenn.cis.tomato.core;

import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

public class StaticAnalyzer {
	private SourceBundle sourceBundle;
	// need to customize JS extractor to read from sourceBundle
	private SourceModule sourceModule;
	private CallGraph cg;
	private PointerAnalysis pa;

	// consider adding more options?
	public StaticAnalyzer(SourceBundle sourceBundle) {
		//TODO: Add language/environment versioning
	}
	public SourceBundle getSourceBundle() {
		return sourceBundle;
	}
	public SuspectList getAllSuspects() {
		//TODO
		return null;
	}
}
