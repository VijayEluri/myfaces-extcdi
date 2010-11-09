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
package org.apache.myfaces.extensions.cdi.jsf2.impl.navigation;

import org.apache.myfaces.extensions.cdi.jsf.impl.navigation.ViewConfigAwareNavigationHandler;
import org.apache.myfaces.extensions.cdi.core.api.Deactivatable;
import org.apache.myfaces.extensions.cdi.core.impl.util.ClassDeactivation;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.context.FacesContext;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * We have to ensure the invocation order for the type-safe navigation feature/s.
 * 
 * @author Gerhard Petracek
 */
public class CodiNavigationHandler extends ConfigurableNavigationHandler implements Deactivatable
{
    private final NavigationHandler wrapped;
    private final boolean deactivated;
    private Map<String, Set<NavigationCase>> navigationCases;

    public CodiNavigationHandler(NavigationHandler navigationHandler)
    {
        this.wrapped = navigationHandler;
        this.deactivated = !isActivated();
    }

    //TODO
    public void handleNavigation(FacesContext context, String fromAction, String outcome)
    {
        if(this.deactivated)
        {
            this.wrapped.handleNavigation(context, fromAction, outcome);
        }
        else
        {
            getWrappedNavigationHandler().handleNavigation(context, fromAction, outcome);
        }
    }

    private NavigationHandler getWrappedNavigationHandler()
    {
        ViewConfigAwareNavigationHandler viewConfigAwareNavigationHandler =
                new ViewConfigAwareNavigationHandler(this.wrapped);

        return new AccessScopeAwareNavigationHandler(viewConfigAwareNavigationHandler);
    }

    public NavigationCase getNavigationCase(FacesContext context, String action, String outcome)
    {
        if (this.wrapped instanceof ConfigurableNavigationHandler)
        {
            return ((ConfigurableNavigationHandler) this.wrapped).getNavigationCase(context, action, outcome);
        }
        //TODO add support for implicit navigation in combination with view-config based typesafe navigation
        return null;
    }

    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        if (this.wrapped instanceof ConfigurableNavigationHandler)
        {
            return ((ConfigurableNavigationHandler) this.wrapped).getNavigationCases();
        }

        //workaround for mojarra
        if(this.navigationCases == null)
        {
            this.navigationCases = new HashMap<String, Set<NavigationCase>>();
        }
        return this.navigationCases;
    }

    public boolean isActivated()
    {
        return ClassDeactivation.isClassActivated(getClass());
    }
}