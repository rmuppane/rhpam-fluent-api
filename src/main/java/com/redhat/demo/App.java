package com.redhat.demo;

import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.Split;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.conf.AuditMode;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;

/**
 * Hello world! Simple stateless process
 */
public class App {
    public static void main(String[] args) {

        RuleFlowProcess processDef = getProcessDefinition();

        KieBase kbase = getKieBase(processDef);

        KieSession ksession = getKieSession(kbase);

        ksession.startProcess("hello");

        ksession.dispose();
    }

    private static KieSession getKieSession(KieBase kbase) {
        DeploymentDescriptor descriptor = new DeploymentDescriptorManager().getDefaultDescriptor().getBuilder()
                .auditMode(AuditMode.NONE).get();

        RuntimeEnvironmentBuilder runtimeBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultInMemoryBuilder();
        RuntimeEnvironment env = runtimeBuilder.persistence(false)
                                               .addEnvironmentEntry("KieDeploymentDescriptor", descriptor)
                                               .knowledgeBase(kbase)
                                               .get();

        RuntimeManager manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(env, "1.0");

        KieSession ksession = manager.getRuntimeEngine(EmptyContext.get()).getKieSession();
        return ksession;
    }

    private static KieBase getKieBase(RuleFlowProcess processDef) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        // ProcessBuilderImpl pbuilder = (ProcessBuilderImpl) ProcessBuilderFactory.newProcessBuilder(kbuilder);
        // pbuilder.buildProcess(processDef, null);

        byte[] processBytes = XmlBPMNProcessDumper.INSTANCE.dump(processDef).getBytes();
        kbuilder.add(ResourceFactory.newByteArrayResource(processBytes), ResourceType.BPMN2);
        
        return kbuilder.newKieBase();
    }

    private static RuleFlowProcess getProcessDefinition() {
        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("hello");
        factory
               // header
               .name("hello process").packageName("com.redhat.demo")
               // nodes
               .startNode(1).name("Start").done()
               .splitNode(10).name("xor").type(Split.TYPE_XOR)
               .constraint(2, "cos1", "code", "mvel", "true")
               .constraint(4, "cos2", "code", "mvel", "false").done()
               .actionNode(2).name("Action").action("java", "System.out.println(\"Hello World\");").done()
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
