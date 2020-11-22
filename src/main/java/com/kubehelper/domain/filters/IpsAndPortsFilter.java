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
package com.kubehelper.domain.filters;

import org.apache.commons.lang3.StringUtils;

/**
 * @author JDev
 */
public class IpsAndPortsFilter {
    private String namespace = "", resourceType = "", resourceName = "", creationTime = "", ip = "", hostInfo = "", ports = "", additionalInfo = "";

    public String getNamespace() {
        return namespace;
    }

    public IpsAndPortsFilter setNamespace(String namespace) {
        this.namespace = namespace == null ? "" : namespace.trim();
        return this;
    }

    public String getResourceType() {
        return resourceType;
    }

    public IpsAndPortsFilter setResourceType(String resourceType) {
        this.resourceType = resourceType == null ? "" : resourceType.trim();
        return this;
    }

    public String getResourceName() {
        return resourceName;
    }

    public IpsAndPortsFilter setResourceName(String resourceName) {
        this.resourceName = resourceName == null ? "" : resourceName.trim();
        return this;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public IpsAndPortsFilter setCreationTime(String creationTime) {
        this.creationTime = creationTime == null ? "" : creationTime.trim();
        return this;
    }

    public String getIp() {
        return ip;
    }

    public IpsAndPortsFilter setIp(String ip) {
        this.ip = ip == null ? "" : ip.trim();
        return this;
    }

    public String getHostInfo() {
        return hostInfo;
    }

    public IpsAndPortsFilter setHostInfo(String hostInfo) {
        this.hostInfo = hostInfo;
        return this;
    }

    public String getPorts() {
        return ports;
    }

    public IpsAndPortsFilter setPorts(String ports) {
        this.ports = ports == null ? "" : ports.trim();
        return this;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public IpsAndPortsFilter setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }

    public boolean isFilterActive() {
        return StringUtils.isNoneBlank(namespace, resourceType, resourceName, creationTime, ip, hostInfo, ports, additionalInfo);
    }
}