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
package com.kubehelper.viewmodels;

import com.kubehelper.common.Global;
import com.kubehelper.domain.models.ConfigsModel;
import com.kubehelper.services.CommonService;
import com.kubehelper.services.ConfigsService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlNativeComponent;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.event.AfterSizeEvent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.DelegatingVariableResolver;
import org.zkoss.zul.Div;

/**
 * Class for displaying Kube Helper dashboard Cluster and nodes metrics.
 * ViewModel initializes ..kubehelper/pages/dashboard.zul
 *
 * @author JDev
 */
@VariableResolver(DelegatingVariableResolver.class)
public class ConfigsVM {

    private int centerLayoutHeight = 700;

    private ConfigsModel configsModel;

    @WireVariable
    private ConfigsService configsService;

    @WireVariable
    private CommonService commonService;

    private boolean autoSyncEnabled;


    @Init
    public void init() {
        configsModel = (ConfigsModel) Global.ACTIVE_MODELS.computeIfAbsent(Global.CONFIGS_MODEL, (k) -> Global.NEW_MODELS.get(Global.CONFIGS_MODEL));
        configsService.checkConfigLocation(configsModel);
//        configsService.showDashboard(dashboardModel);
    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        Selectors.wireComponents(view, this, false);
        Selectors.wireEventListeners(view, this);
        BindUtils.postNotifyChange(null, null, this, ".");
        Clients.evalJavaScript("highlightConfig();");
    }

    @Listen("onAfterSize=#centerLayoutIpsAndPortsID")
    public void onAfterSizeCenter(AfterSizeEvent event) {
        centerLayoutHeight = event.getHeight() - 3;
        BindUtils.postNotifyChange(null, null, this, ".");
    }

    @Command
    public void saveConfig() {
        Div configBlock = (Div) Path.getComponent("//indexPage/templateInclude/configBlockId");
        HtmlNativeComponent nativeConfig = (HtmlNativeComponent) configBlock.getChildren().get(0);
//        TODO
//        configsModel.setConfig(configBlock.toString());
        configsService.updateConfig(configsModel);
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public ConfigsVM setAutoSyncEnabled(boolean autoSyncEnabled) {
        this.autoSyncEnabled = autoSyncEnabled;
        return this;
    }

    public String getConfig() {
        return configsModel.getConfig();
    }

    public void setConfig(String config) {
        configsModel.setConfig(config);
    }
}