/*
 * Copyright 2008-2012 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.immutant.messaging;

import java.io.Closeable;

import javax.jms.JMSException;
import javax.jms.XAConnection;

import org.immutant.runtime.ClojureRuntime;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.polyglot.messaging.BaseMessageProcessor;
import org.projectodd.polyglot.messaging.BaseMessageProcessorGroup;
import org.projectodd.polyglot.messaging.MessageProcessorService;

public class MessageProcessorGroup extends BaseMessageProcessorGroup implements Closeable, MessageProcessorGroupMBean {

    public MessageProcessorGroup(ServiceRegistry registry, ServiceName baseServiceName,
            String destinationName, XAConnection connection, Object setupHandler ) {
        super( registry, baseServiceName, destinationName, MessageProcessor.class );
        this.setupHandler = setupHandler;
        this.connection = connection;
    }

    @Override
    protected BaseMessageProcessor instantiateProcessor() {
        return new MessageProcessor( this.clojureRuntimeInjector.getValue() );
    }
    
    @Override
    protected void startConnection(StartContext context) {
        try {
            getConnection().start();
        } catch (JMSException e) {
            context.failed( new StartException( e ) );
        }
    }
    
    @Override
    protected MessageProcessorService createMessageProcessorService(BaseMessageProcessor processor) {
        return new ClojureMessageProcessorService( this, processor, this.clojureRuntimeInjector.getValue(),
                this.setupHandler );
    }
    
    //convenience method so it's closeable like a consumer
    @SuppressWarnings("rawtypes")
    @Override
    public void close() {
        ServiceController service = getServiceRegistry().getService( getBaseServiceName() );
        if (service != null) {
            service.setMode( Mode.REMOVE );
        }
    }
    
    public Injector<ClojureRuntime> getClojureRuntimeInjector() {
        return clojureRuntimeInjector;
    }
    
    private final InjectedValue<ClojureRuntime> clojureRuntimeInjector = new InjectedValue<ClojureRuntime>();
    private Object setupHandler;
}