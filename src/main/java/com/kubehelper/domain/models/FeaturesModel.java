/*
Kube Helper
Copyright (C) 2021 JDev

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.kubehelper.domain.models;

import com.kubehelper.common.Global;
import com.kubehelper.common.KubeHelperException;
import com.kubehelper.domain.filters.FeaturesFilter;
import com.kubehelper.domain.results.FeatureResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JDev
 */
public class FeaturesModel implements PageModel {

    private String templateUrl = "~./zul/pages/features.zul";
    public static String NAME = Global.FEATURES_MODEL;
    private List<String> namespaces = new ArrayList<>();
    private List<String> pods = new ArrayList<>();
    private List<FeatureResult> featuresResults = new ArrayList<>();
    private FeaturesFilter filter = new FeaturesFilter();
    private List<KubeHelperException> buildExceptions = new ArrayList<>();
    private String selectedNamespace = "all";

    public FeaturesModel() {
    }

    public void addFeatureResult(FeatureResult featureResult) {
        featuresResults.add(featureResult);
//        filter.addResourceTypesFilter(searchResult.getResourceType());
//        filter.addNamespacesFilter(searchResult.getNamespace());
    }

    public FeaturesModel addGroupFilter(String resourceName) {
//        if (StringUtils.isNotBlank(resourceName)) {
//            filter.addResourceNamesFilter(resourceName);
//        }
        return this;
    }

    public void addParseException(Exception exception) {
        this.buildExceptions.add(new KubeHelperException(exception));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTemplateUrl() {
        return templateUrl;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public FeaturesModel setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
        return this;
    }

    public List<String> getPods() {
        return pods;
    }

    public FeaturesModel setPods(List<String> pods) {
        this.pods = pods;
        return this;
    }

    public List<FeatureResult> getFeaturesResults() {
        return featuresResults;
    }

    public FeaturesModel setFeaturesResults(List<FeatureResult> featuresResults) {
        this.featuresResults = featuresResults;
        return this;
    }

    public List<KubeHelperException> getBuildExceptions() {
        return buildExceptions;
    }

    public FeaturesModel setBuildExceptions(List<KubeHelperException> buildExceptions) {
        this.buildExceptions = buildExceptions;
        return this;
    }

    public boolean hasBuildErrors() {
        return !buildExceptions.isEmpty();
    }

    public FeaturesFilter getFilter() {
        return filter;
    }

    public FeaturesModel setFilter(FeaturesFilter filter) {
        this.filter = filter;
        return this;
    }

    public String getSelectedNamespace() {
        return selectedNamespace;
    }

    public FeaturesModel setSelectedNamespace(String selectedNamespace) {
        this.selectedNamespace = selectedNamespace;
        return this;
    }
}
