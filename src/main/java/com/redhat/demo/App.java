package com.redhat.demo;

import org.drools.compiler.compiler.ProcessBuilderFactory;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.jbpm.compiler.ProcessBuilderImpl;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.Split;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;

/**
 * Hello world! Simple stateless process
 */
public class App {
    public static void main(String[] args) {

        RuleFlowProcess processDef = getProcessDefinition();
        
        KnowledgeBaseImpl kbase = getKnowledgeBase(processDef);

        KieSession ksession = kbase.newKieSession();
        ksession.startProcess("hello");
    }

    private static KnowledgeBaseImpl getKnowledgeBase(RuleFlowProcess processDef) {
        KnowledgeBaseImpl kbase = (KnowledgeBaseImpl) KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addProcess(processDef);

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        ProcessBuilderImpl pbuilder = (ProcessBuilderImpl) ProcessBuilderFactory.newProcessBuilder(kbuilder);
        pbuilder.buildProcess(processDef, null);
        return kbase;
    }

    private static RuleFlowProcess getProcessDefinition() {
        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("hello");
        factory
                // header
                .name("hello process").packageName("org.drools")
                // nodes
                .startNode(1).name("Start").done()
                .splitNode(10).name("xor").type(Split.TYPE_XOR)
                .constraint(2, "cos1", "code", "mvel", "true")
                .constraint(4, "cos2", "code", "mvel", "false").done()
                .actionNode(2).name("Action").action("java", "System.out.println(\"Hello World\")").done()
                .endNode(3).name("End").done()
                .endNode(4).name("End 2").done()
                // connections
                .connection(1, 10)
                .connection(10, 4)
                .connection(10, 2)
                .connection(2, 3);

        return factory.getProcess();
    }
}
