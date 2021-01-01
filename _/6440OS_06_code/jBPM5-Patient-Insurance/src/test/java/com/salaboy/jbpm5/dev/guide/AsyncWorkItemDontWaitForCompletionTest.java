/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.salaboy.jbpm5.dev.guide;

import com.salaboy.jbpm5.dev.guide.commands.CheckInCommand;
import com.salaboy.jbpm5.dev.guide.workitems.AsyncGenericWorkItemHandler;
import java.util.HashMap;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderErrors;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.rule.DefaultAgendaEventListener;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jbpm.executor.ExecutorModule;
import org.jbpm.executor.ExecutorServiceEntryPoint;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author salaboy
 */
public class AsyncWorkItemDontWaitForCompletionTest {

    protected ExecutorServiceEntryPoint executor;
    protected StatefulKnowledgeSession session;
    

    public AsyncWorkItemDontWaitForCompletionTest() {
    }

    @Before
    public void setUp() throws Exception {
      
        initializeExecutionEnvironment();
        initializeSession();
    }

    @After
    public void tearDown() {
        executor.clearAllRequests();
        executor.clearAllErrors();
        executor.destroy();
       
    }

    protected void initializeExecutionEnvironment() throws Exception {
        CheckInCommand.reset();
        executor = ExecutorModule.getInstance().getExecutorServiceEntryPoint();
        executor.setThreadPoolSize(1);
        executor.setInterval(3);
        executor.init();
    }

    

    private void initializeSession() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add(new ClassPathResource("async-work-item-nowait.bpmn"), ResourceType.BPMN2);
        if (kbuilder.hasErrors()) {
            KnowledgeBuilderErrors errors = kbuilder.getErrors();

            for (KnowledgeBuilderError error : errors) {
                System.out.println(">>> Error:" + error.getMessage());

            }
            throw new IllegalStateException(">>> Knowledge couldn't be parsed! ");
        }

        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();

        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

        session = kbase.newStatefulKnowledgeSession();
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(session);

        session.addEventListener(new DefaultAgendaEventListener(){

            @Override
            public void afterRuleFlowGroupActivated(org.drools.event.rule.RuleFlowGroupActivatedEvent event) {
                session.fireAllRules();
            }
            
        });
        
    }

    @Test
    public void executorCheckInTestFinishes() throws InterruptedException {
        HashMap<String, Object> input = new HashMap<String, Object>();

        String patientName = "John Doe";
        input.put("bedrequest_patientname", patientName);

        AsyncGenericWorkItemHandler asyncHandler = new AsyncGenericWorkItemHandler(executor, session.getId());
        session.getWorkItemManager().registerWorkItemHandler("Async Work", asyncHandler);

        WorkflowProcessInstance pI = (WorkflowProcessInstance) session.startProcess("PatientCheckIn", input);

        assertEquals(ProcessInstance.STATE_COMPLETED, pI.getState());

        assertEquals(0, CheckInCommand.getCheckInCount());

        Thread.sleep(executor.getInterval()*1000 + 1000);

        assertEquals(1, CheckInCommand.getCheckInCount());
    }

    @Test
    public void executorCheckInTestStoppedBefore() throws InterruptedException {
        HashMap<String, Object> input = new HashMap<String, Object>();

        String patientName = "John Doe";
        input.put("bedrequest_patientname", patientName);

        AsyncGenericWorkItemHandler asyncHandler = new AsyncGenericWorkItemHandler(executor, session.getId());
        session.getWorkItemManager().registerWorkItemHandler("Async Work", asyncHandler);

        WorkflowProcessInstance pI = (WorkflowProcessInstance) session.startProcess("PatientCheckIn", input);

        assertEquals(ProcessInstance.STATE_COMPLETED, pI.getState());

        assertEquals(0, CheckInCommand.getCheckInCount());

        Thread.sleep(1000);

        assertEquals(0, CheckInCommand.getCheckInCount());

        Thread.sleep(1500);

        assertEquals(1, CheckInCommand.getCheckInCount());
    }
}
