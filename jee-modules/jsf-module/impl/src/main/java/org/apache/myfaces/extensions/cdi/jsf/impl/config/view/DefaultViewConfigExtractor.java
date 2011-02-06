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
package org.apache.myfaces.extensions.cdi.jsf.impl.config.view;

import org.apache.myfaces.extensions.cdi.core.api.config.view.ViewConfig;
import org.apache.myfaces.extensions.cdi.core.api.config.view.ViewMetaData;
import org.apache.myfaces.extensions.cdi.core.api.config.view.DefaultErrorView;
import org.apache.myfaces.extensions.cdi.core.api.security.Secured;
import org.apache.myfaces.extensions.cdi.jsf.api.config.view.Page;
import org.apache.myfaces.extensions.cdi.jsf.impl.config.view.spi.ViewConfigEntry;
import org.apache.myfaces.extensions.cdi.jsf.impl.config.view.spi.ViewConfigExtractor;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.myfaces.extensions.cdi.jsf.impl.util.ExceptionUtils.missingInlineViewConfigRootMarkerException;

/**
 * @author Gerhard Petracek
 */
class DefaultViewConfigExtractor implements ViewConfigExtractor
{
    public ViewConfigEntry extractViewConfig(Class<? extends ViewConfig> viewDefinitionClass)
    {
        //use the interface to make clear which information we really need
        ViewConfigEntry viewConfigEntry = new ExtractedViewConfigDefinitionEntry(viewDefinitionClass);

        return extractViewConfigEntry(viewDefinitionClass, viewConfigEntry);
    }

    public boolean isInlineViewConfig(Class<? extends ViewConfig> viewDefinitionClass)
    {
        return isResolvable(viewDefinitionClass, new ArrayList<Class<? extends Annotation>>());
    }

    public ViewConfigEntry extractInlineViewConfig(Class<? extends ViewConfig> viewDefinitionClass)
    {
        Class viewConfigRootMarker = ViewConfigCache.getInlineViewConfigRootMarker();

        if(viewConfigRootMarker == null)
        {
            throw missingInlineViewConfigRootMarkerException(viewDefinitionClass);
        }

        int startIndex = viewConfigRootMarker.getPackage().getName().length() + 1;
        String basePath = viewDefinitionClass.getName()
                .substring(startIndex, viewDefinitionClass.getName().lastIndexOf("."));

        basePath = basePath.replace(".", "/");

        //use the interface to make clear which information we really need
        ViewConfigEntry viewConfigEntry = new ExtractedInlineViewConfigDefinitionEntry(viewDefinitionClass, basePath);

        return extractViewConfigEntry(viewDefinitionClass, viewConfigEntry);
    }

    private ViewConfigEntry extractViewConfigEntry(Class<? extends ViewConfig> viewDefinitionClass,
                                                   ViewConfigEntry viewConfigEntry)
    {
        scanViewConfigClass(viewDefinitionClass, (ExtractedViewConfigDefinitionEntry)viewConfigEntry);

        return new DefaultViewConfigEntry(viewConfigEntry.getViewId(),
                                          viewDefinitionClass,
                                          viewConfigEntry.getNavigationMode(),
                                          viewConfigEntry.getViewParameter(),
                                          viewConfigEntry.getAccessDecisionVoters(),
                                          viewConfigEntry.getErrorView(),
                                          viewConfigEntry.getMetaData());
    }

    private Collection<Annotation> extractViewMetaData(
            Class<?> targetClass, ExtractedViewConfigDefinitionEntry entry)
    {
        List<Annotation> result = new ArrayList<Annotation>();

        for(Annotation annotation : targetClass.getAnnotations())
        {
            if(annotation.annotationType().isAnnotationPresent(ViewMetaData.class))
            {
                ViewMetaData metaData = annotation.annotationType().getAnnotation(ViewMetaData.class);

                if(!entry.getFoundAndBlockedMetaDataTypes().contains(annotation.annotationType()))
                {
                    result.add(annotation);

                    if(metaData.override())
                    {
                        entry.blockMetaData(annotation.annotationType());
                    }
                }
            }
        }

        return result;
    }

    private void scanViewConfigClass(Class<?> viewDefinitionClass, ExtractedViewConfigDefinitionEntry scannedViewConfig)
    {
        String defaultExtension = ExtractedViewConfigDefinitionEntry.DEFAULT_EXTENSION;
        String defaultPageName = ExtractedViewConfigDefinitionEntry.DEFAULT_PAGE_NAME;
        String rootPath = ExtractedViewConfigDefinitionEntry.ROOT_PATH;

        String currentBasePath;
        Page pageAnnotation;
        Secured securedAnnotation;

        Class<?> currentClass = viewDefinitionClass;
        while (currentClass != null && !Object.class.getName().equals(currentClass.getName()))
        {
            //security
            if (currentClass.isAnnotationPresent(Secured.class))
            {
                securedAnnotation = currentClass.getAnnotation(Secured.class);
                scannedViewConfig.addAccessDecisionVoters(securedAnnotation.value());

                if (scannedViewConfig.getErrorView() == null &&
                        !DefaultErrorView.class.getName().equals(securedAnnotation.errorView().getName()))
                {
                    scannedViewConfig.setErrorView(securedAnnotation.errorView());
                }
            }

            //meta-data
            scannedViewConfig.addMetaData(
                    extractViewMetaData(currentClass, scannedViewConfig));

            //page definition
            if (currentClass.isAnnotationPresent(Page.class))
            {
                pageAnnotation = currentClass.getAnnotation(Page.class);

                if (!pageAnnotation.extension().equals(defaultExtension))
                {
                    scannedViewConfig.setExtension(pageAnnotation.extension());
                }

                if (!pageAnnotation.basePath().equals(rootPath))
                {
                    currentBasePath = pageAnnotation.basePath();

                    if (!".".equals(currentBasePath))
                    {
                        scannedViewConfig
                                .setSimpleClassNameToPathMapping(currentClass.getSimpleName(), currentBasePath);
                    }

                    if (rootPath.equals(scannedViewConfig.getBasePath()))
                    {
                        scannedViewConfig.setBasePath(currentBasePath);
                    }
                }

                if (!scannedViewConfig.isKnownNavigationMode() &&
                        !pageAnnotation.navigation().equals(Page.NavigationMode.DEFAULT))
                {
                    scannedViewConfig.setNavigationMode(pageAnnotation.navigation());
                }

                if (!scannedViewConfig.isKnownViewParameter() &&
                        !pageAnnotation.viewParams().equals(Page.ViewParameter.DEFAULT))
                {
                    scannedViewConfig.setViewParameter(pageAnnotation.viewParams());
                }

                if (!pageAnnotation.name().equals(defaultPageName))
                {
                    scannedViewConfig.setPageName(pageAnnotation.name());
                }
            }

            //scan interfaces
            for(Class interfaceClass : currentClass.getInterfaces())
            {
                scanViewConfigClass(interfaceClass, scannedViewConfig);
            }

            //scan super class
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * @param annotated current class to scan
     * @param scannedAnnotations simple cycle prevention
     * @return true to signal that the class is resolvable via EL
     */
    private boolean isResolvable(Class annotated, List<Class<? extends Annotation>> scannedAnnotations)
    {
        if(annotated.isAnnotation())
        {
            scannedAnnotations.add(annotated);
        }

        Class<? extends Annotation> annotationClass;
        for(Annotation annotation : annotated.getAnnotations())
        {
            annotationClass = annotation.annotationType();

            if(scannedAnnotations.contains(annotationClass))
            {
                continue;
            }

            if(Named.class.equals(annotationClass) || "javax.faces.bean.ManagedBean".equals(annotationClass.getName()))
            {
                return true;
            }

            //to support stereotypes
            if(isResolvable(annotationClass, scannedAnnotations))
            {
                return true;
            }
        }
        return false;
    }
}
