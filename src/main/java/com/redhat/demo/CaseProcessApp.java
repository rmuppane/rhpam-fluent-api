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
import org.jbpm.process.core.datatype.impl.type.StringDataType;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
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
import org.kie.internal.runtime.manager.context.CaseContext;
import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;
import org.kie.test.util.db.PersistenceUtil;

/**
 * Hello world! Simple stateless process
 */
public class CaseProcessApp {
    public static void main(String[] args) {

        RuleFlowProcess processDef = getProcessDefinition();

        KieBase kbase = getKieBase(processDef);

        // configure persistence
        KieSession ksession = getKieSession(kbase, true);

        ProcessInstance pi = ksession.startProcess("datavalidation");
        System.out.println("Process Instance Id      ... " + pi.getId());
        
        
        // Resource res = ResourceFactory.newByteArrayResource(XmlBPMNProcessDumper.INSTANCE.dump(process).getBytes());
        // res.setSourcePath("process.bpmn2"); // source path or target path must be set to be added into kbase

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

            // manager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(env, "1.0");
            manager = RuntimeManagerFactory.Factory.get().newPerCaseRuntimeManager(env, "1.0");

        } else {
            runtimeBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultInMemoryBuilder();
            env = runtimeBuilder.persistence(persistence)
                                .addEnvironmentEntry("KieDeploymentDescriptor", descriptor)
                                .knowledgeBase(kbase)
                                .get();

            // manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(env, "1.0");
            manager = RuntimeManagerFactory.Factory.get().newPerCaseRuntimeManager(env, "1.0");
        }

        return manager.getRuntimeEngine(CaseContext.get("1.0")).getKieSession();
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

    // This will cover the two stages
    private static RuleFlowProcess getProcessDefinition() {
        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("datavalidation");
        factory
               // header
               .name("data validation process").packageName("com.redhat.demo")
               .variable("caseId", new StringDataType(), "caseIdPrefix", "DV-")
               .variable("firstName", new StringDataType())
               .variable("lastName", new StringDataType())
               .variable("dataentry", new StringDataType())
               // nodes
               .startNode(1).name("Start").done()
               .milestoneNode(2).name("DataEntry").constraint("org.kie.api.runtime.process.CaseData(data.get(\"dataentry\") != null && data.get(\"dataentry\").equals(\"completed\"))").done()
               .compositeNode(3)
	               .name("Data entry")
	               .startNode(1).name("User Data entry Start").done()
	               .actionNode(2).name("Script task").action("java",
                                                               "System.out.println(\"Stage 1\");").done()
	               .endNode(3).name("Data entry stage End").terminate(true).done()
	               .connection(1, 2)
	               .connection(2, 3)
	               .done()
	           .compositeNode(4)
	               .name("Data Approval")
	               .startNode(1).name("User Data approval").done()
	               .actionNode(2).name("Script task").action("java",
                                                               "System.out.println(\"Stage 2\");").done()
	               .endNode(3).name("Data Approval Stage End").terminate(true).done()
	               .connection(1, 2)
	               .connection(2, 3)
	               .done()
               .endNode(5).name("End").done()
               // connections
               .connection(1, 2)
               .connection(2, 3)
               .connection(3, 4)
               .connection(4, 5);
        
        System.out.println("Validation " + factory.validate()) ;
        return factory.getProcess();
    }
}
