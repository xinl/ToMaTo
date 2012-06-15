package edu.upenn.cis.tomato.experiment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.FileMapping;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.vis.JsViewer;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;

import edu.upenn.cis.tomato.application.AliasAnalysis;
import edu.upenn.cis.tomato.policy.example.PolicyExample;

public class AliasFinder {

    /**
     * @param args
     * @throws CancelException 
     * @throws IOException 
     * @throws IllegalArgumentException 
     */
    public static void main(String[] args) throws IllegalArgumentException, IOException, CancelException {
        edu.upenn.cis.tomato.util.Util.initializeSystemBuiltinVariables();
        
        PolicyExample.Example_AliasAnalysis_Function();
        
        URL url = new URL("file:///" + new File("").getAbsolutePath() + "/dat/test/alias/Alias_Function.html");
        System.out.println(url);
        String[] path = url.getFile().split("/");
        String mashupPageName = "L" + path[path.length - 1];
        System.out.println("File:" + mashupPageName);
        
        JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
        Set<MappedSourceModule> scripts = WebUtil.extractScriptFromHTML(url);
        MappedSourceModule script = (MappedSourceModule) (scripts.toArray(new SourceModule[scripts.size()])[0]);
        FileMapping mapping = script.getMapping(); // to map line numbers in temporary js file to original web page
        // building call graph
        JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(new WebPageLoaderFactory(
                                                                            new CAstRhinoTranslatorFactory(), 
                                                                            null), 
                                                                    scripts.toArray(new SourceModule[scripts.size()]),
                                                                    CGBuilderType.ONE_CFA_PRECISE_LEXICAL, 
                                                                    AstIRFactory.makeDefaultFactory());
        builder.setBaseURL(url);
        CallGraph cg = builder.makeCallGraph(builder.getOptions());
        PointerAnalysis pa = builder.getPointerAnalysis();
        
        //new JsViewer(cg, pa);
        
        AliasAnalysis.findVariableAlias(mashupPageName, cg, pa);
    }
}
