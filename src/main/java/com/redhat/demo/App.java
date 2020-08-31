package com.redhat.demo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.compiler.compiler.ProcessBuilderFactory;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.jbpm.compiler.ProcessBuilderImpl;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.workflow.core.node.Split;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.runtime.conf.AuditMode;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;
import org.kie.test.util.db.PersistenceUtil;

/**
 * Hello world! Simple stateless process
 */
public class App {
    public static void main(String[] args) {

        RuleFlowProcess processDef = getProcessDefinition();

        KieBase kbase = getKieBase(processDef);

        // configure persistence
        KieSession ksession = getKieSession(kbase, false);

        ProcessInstance pi = ksession.startProcess("hello");
        System.out.println("Process Instance Id      ... " + pi.getId());

        ksession.dispose();

        // when persistence is on, it closes the runtime
        System.exit(0);
    }

    private static KieSession getKieSession(KieBase kbase, boolean persistence) {
        DeploymentDescriptor descriptor = new DeploymentDescriptorManager().getDefaultDescriptor().getBuilder()
                .auditMode(AuditMode.NONE).get();

        RuntimeEnvironmentBuilder runtimeBuilder;
        RuntimeEnvironment env;

        RuntimeManager manager = null;

        if (persistence) {
            Properties properties = new Properties();
            properties.put("driverClassName", "org.h2.Driver");
            properties.put("className", "org.h2.jdbcx.JdbcDataSource");
            properties.put("user", "sa");
            properties.put("password", "sa");
            properties.put("url", "jdbc:h2:mem:test");
            properties.put("datasourceName", "jdbc/jbpm-ds");
            PersistenceUtil.setupPoolingDataSource(properties);
            Map<String, String> map = new HashMap<String, String>();
            map.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");                            

            runtimeBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder();
            env = runtimeBuilder.persistence(persistence)
                                .addEnvironmentEntry("KieDeploymentDescriptor", descriptor)
                                .entityManagerFactory(emf)
                                .knowledgeBase(kbase)
                                .get();

            manager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(env, "1.0");

        } else {
            runtimeBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultInMemoryBuilder();
            env = runtimeBuilder.persistence(persistence)
                                .addEnvironmentEntry("KieDeploymentDescriptor", descriptor)
                                .knowledgeBase(kbase)
                                .get();

            manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(env, "1.0");
        }

        return manager.getRuntimeEngine(EmptyContext.get()).getKieSession();
    }

    private static KieBase getKieBase(RuleFlowProcess processDef) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        ProcessBuilderImpl pbuilder = (ProcessBuilderImpl) ProcessBuilderFactory.newProcessBuilder(kbuilder);
        pbuilder.buildProcess(processDef, null);

        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        Collection<KiePackage> knowledgePackages = kbuilder.getKnowledgePackages();
        
        kbase.addPackages(knowledgePackages);
        
        return kbase;
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
