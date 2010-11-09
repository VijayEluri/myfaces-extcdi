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
package org.apache.myfaces.extensions.cdi.jsf.impl.scope.conversation;

import org.apache.myfaces.extensions.cdi.jsf.impl.scope.conversation.spi.ConversationFactory;
import org.apache.myfaces.extensions.cdi.jsf.impl.scope.conversation.spi.ConversationKey;
import org.apache.myfaces.extensions.cdi.jsf.impl.scope.conversation.spi.EditableConversation;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowScoped;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationConfig;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.event.StartConversationEvent;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Gerhard Petracek
 */
@ApplicationScoped
public class JsfAwareConversationFactory implements ConversationFactory, BeanManagerAware
{
    private static final long serialVersionUID = 2329113439978807663L;

    private BeanManager beanManager;

    enum ConversationPropertyKeys
    {
        TIMEOUT("timeout");

        private String key;

        ConversationPropertyKeys(String key)
        {
            this.key = key;
        }

        String getKey()
        {
            return key;
        }
    }
    
    public EditableConversation createConversation(ConversationKey conversationKey, ConversationConfig configuration)
    {
        EditableConversation conversation;

        if(WindowScoped.class.isAssignableFrom(conversationKey.getScope()))
        {
            conversation = new DefaultConversation(conversationKey,
                                                   new WindowConversationExpirationEvaluator(),
                                                   configuration,
                                                   this.beanManager);

            return processCreatedConversation(conversation, configuration.isStartConversationEventEnabled());
        }

        if(ViewAccessScoped.class.isAssignableFrom(conversationKey.getScope()))
        {
            conversation = new DefaultConversation(conversationKey,
                                                   new ViewAccessConversationExpirationEvaluator(),
                                                   configuration,
                                                   this.beanManager);

            return processCreatedConversation(conversation, configuration.isStartConversationEventEnabled());
        }

        conversation = new DefaultConversation(conversationKey,
                                               new TimeoutConversationExpirationEvaluator(
                                                       configuration.getConversationTimeoutInMinutes()),
                                               configuration,
                                               this.beanManager);

        return processCreatedConversation(conversation, configuration.isStartConversationEventEnabled());
    }

    private EditableConversation processCreatedConversation(EditableConversation conversation,
                                                            boolean startConversationEventEnable)
    {
        if(startConversationEventEnable)
        {
            this.beanManager.fireEvent(new StartConversationEvent(conversation));
        }
        return conversation;
    }

    public void setBeanManager(BeanManager beanManager)
    {
        this.beanManager = beanManager;
    }
}