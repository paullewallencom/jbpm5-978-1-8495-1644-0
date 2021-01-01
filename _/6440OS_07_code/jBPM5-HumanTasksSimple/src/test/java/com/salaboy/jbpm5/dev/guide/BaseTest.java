/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.salaboy.jbpm5.dev.guide;

import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


import org.drools.SystemEventListenerFactory;
import org.jbpm.task.Group;
import org.jbpm.task.User;
import org.jbpm.task.identity.UserGroupCallbackManager;

import org.jbpm.task.service.SendIcal;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.TaskServiceSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTest {

    protected Logger logger;
    
    protected EntityManagerFactory emf;

    protected Map<String, User> users = new HashMap<String, User>();
    protected Map<String, Group> groups = new HashMap<String, Group>();

    protected TaskService taskService;
    protected TaskServiceSession taskSession;

    protected EntityManagerFactory createEntityManagerFactory() { 
        return Persistence.createEntityManagerFactory("org.jbpm.task");
    }
    
    protected void setUp() throws Exception {
        Properties conf = new Properties();
        conf.setProperty("mail.smtp.host", "localhost");
        conf.setProperty("mail.smtp.port", "2345");
        conf.setProperty("from", "from@domain.com");
        conf.setProperty("replyTo", "replyTo@domain.com");
        conf.setProperty("defaultLanguage", "en-UK");
        SendIcal.initInstance(conf);

        // Use persistence.xml configuration
        emf = createEntityManagerFactory();

        taskService = new TaskService(emf, SystemEventListenerFactory.getSystemEventListener());
        taskSession = taskService.createSession();
        MockUserInfo userInfo = new MockUserInfo();
        taskService.setUserinfo(userInfo);
        addUsersAndGroups(taskSession);
        disableUserGroupCallback();
        
        logger = LoggerFactory.getLogger(getClass());
    }

    protected void tearDown() throws Exception {
        taskSession.dispose();
        emf.close();
    }
    
    public void disableUserGroupCallback() {
        UserGroupCallbackManager.getInstance().setCallback(null);
    }
    
     private void addUsersAndGroups(TaskServiceSession taskSession) {
        User user = new User("salaboy");
        User watman = new User("watman");
        taskSession.addUser(user);
        taskSession.addUser(watman);
        User administrator = new User("Administrator");
        taskSession.addUser(administrator);
        users.put("salaboy", user);
        users.put("watman", watman);
        users.put("administrator", administrator);
        Group myGroup = new Group("group1");
        taskSession.addGroup(myGroup);
        groups.put("group1", myGroup);

    }

    public static Object eval(Reader reader, Map vars) {
        vars.put("now", new Date());
        return TaskService.eval(reader, vars);
    }
    
    public Object eval(String str, Map vars) {
        vars.put("now", new Date());
        return TaskService.eval(str, vars);
    }
    
    protected Map<String, Object> fillVariables() { 
        Map <String, Object> vars = new HashMap<String, Object>();
        vars.put( "users", users );
        vars.put( "groups", groups );        
        vars.put( "now", new Date() );
        return vars;
    }
    
   
}
