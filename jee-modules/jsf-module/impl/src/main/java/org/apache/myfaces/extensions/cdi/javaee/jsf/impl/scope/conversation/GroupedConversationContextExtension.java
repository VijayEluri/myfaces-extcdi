/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationScoped;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowScoped;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;
import org.apache.myfaces.extensions.cdi.core.impl.scope.conversation.AbstractGroupedConversationContext;
import org.apache.myfaces.extensions.cdi.core.impl.scope.conversation.ConversationContextAdapter;

import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.event.Observes;

/**
 * extension for registering the adapter for grouped conversations
 *
 * @author Gerhard Petracek
 */
public class GroupedConversationContextExtension implements Extension
{
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager)
    {
        AbstractGroupedConversationContext codiConversationContext = new GroupedConversationContext(manager);
        event.addContext(new ConversationContextAdapter(WindowScoped.class, codiConversationContext));
        event.addContext(new ConversationContextAdapter(ConversationScoped.class, codiConversationContext));
        event.addContext(new ConversationContextAdapter(ViewAccessScoped.class, codiConversationContext));
    }
}
