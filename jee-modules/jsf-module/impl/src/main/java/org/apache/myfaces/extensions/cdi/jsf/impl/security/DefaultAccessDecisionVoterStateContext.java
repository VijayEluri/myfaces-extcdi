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
package org.apache.myfaces.extensions.cdi.jsf.impl.security;

import org.apache.myfaces.extensions.cdi.core.api.security.AccessDecisionState;
import org.apache.myfaces.extensions.cdi.core.api.security.SecurityViolation;
import org.apache.myfaces.extensions.cdi.core.impl.security.spi.EditableAccessDecisionVoterContext;

import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
@RequestScoped
public class DefaultAccessDecisionVoterStateContext implements EditableAccessDecisionVoterContext
{
    private static final long serialVersionUID = -6354543616450765352L;

    private AccessDecisionState state = AccessDecisionState.INITIAL;

    private List<SecurityViolation> securityViolations;

    private Map<String, Object> metaData = new HashMap<String, Object>();

    /**
     * {@inheritDoc}
     */
    public AccessDecisionState getState()
    {
        return this.state;
    }

    /**
     * {@inheritDoc}
     */
    public List<SecurityViolation> getViolations()
    {
        if(this.securityViolations == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.securityViolations);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getMetaData()
    {
        return Collections.unmodifiableMap(this.metaData);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getMetaDataFor(String key, Class<T> targetType)
    {
        return (T)this.metaData.get(key);
    }

    public void addMetaData(String key, Object metaData)
    {
        //TODO specify nested security calls
        this.metaData.put(key, metaData);
    }

    /**
     * {@inheritDoc}
     */
    public void setState(AccessDecisionState accessDecisionVoterState)
    {
        if(AccessDecisionState.VOTE_IN_PROGRESS.equals(accessDecisionVoterState))
        {
            this.securityViolations = new ArrayList<SecurityViolation>(); //lazy init
        }

        this.state = accessDecisionVoterState;

        if(AccessDecisionState.INITIAL.equals(accessDecisionVoterState) ||
                AccessDecisionState.VOTE_IN_PROGRESS.equals(accessDecisionVoterState))
        {
            return;
        }

        //meta-data is only needed until the end of a voting process
        this.metaData.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void addViolation(SecurityViolation securityViolation)
    {
        if(this.securityViolations == null)
        {
            throw new IllegalStateException(
                    AccessDecisionState.VOTE_IN_PROGRESS.name() + " is required for adding security-violations");
        }
        this.securityViolations.add(securityViolation);
    }
}
