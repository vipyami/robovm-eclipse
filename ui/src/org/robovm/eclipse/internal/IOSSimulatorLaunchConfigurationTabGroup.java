/*
 * Copyright (C) 2012 Trillian Mobile AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.eclipse.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaSourceLookupTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.eclipse.RoboVMPlugin;

/**
 * @author niklas
 *
 */
public class IOSSimulatorLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        setTabs(new ILaunchConfigurationTab[] {
            new SimulatorTab(),
            new IOSArgumentsTab(),
            new JavaClasspathTab(),
            new JavaSourceLookupTab(),
            new IOSEnvironmentTab(),
            new CommonTab()
        });
    }

    public static class SimulatorTab extends RoboVMTab {
        private final boolean showProject;

        private Combo deviceTypeCombo;
        private Combo archCombo;

        public SimulatorTab() {
            this(true);
        }
        
        public SimulatorTab(boolean showProject) {
            this.showProject = showProject;
        }

        @Override
        public void createControl(Composite parent) {
            Composite root = createRoot(parent);
            if (showProject) {
                createProjectEditor(root);
            }
            createSimulatorEditor(root);
            setControl(root);
        }

        protected void createSimulatorEditor(Composite parent) {
            Group group = new Group(parent, SWT.NONE);
            group.setText("iOS Simulator:");
            group.setFont(group.getFont());
            group.setLayout(new GridLayout(2, false));
            group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            ((GridData) group.getLayoutData()).horizontalSpan = 1;
            ((GridLayout) group.getLayout()).verticalSpacing = 0;

            Label typeLabel = new Label(group, SWT.NONE);
            typeLabel.setFont(group.getFont());
            typeLabel.setText("Device type:");
            typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

            List<DeviceType> types = Collections.<DeviceType> emptyList();
            try {
                types = DeviceType.listDeviceTypes(RoboVMPlugin.getRoboVMHome());
            } catch (IOException e) {
                RoboVMPlugin.log(e);
            }
            String[] deviceDisplayNames = new String[types.size()];
            for (int i = 0; i < deviceDisplayNames.length; i++) {
                deviceDisplayNames[i] = types.get(i).getSimpleDeviceTypeId();
            }
            deviceTypeCombo = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            deviceTypeCombo.setItems(deviceDisplayNames);
            deviceTypeCombo.select(0);
            deviceTypeCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            deviceTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    updateLaunchConfigurationDialog();
                }
            });

            Label archLabel = new Label(group, SWT.NONE);
            archLabel.setFont(group.getFont());
            archLabel.setText("Arch:");
            archLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

            String[] archs = new String[] {Arch.x86.toString(), Arch.x86_64.toString()};
            archCombo = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            archCombo.setItems(archs);
            archCombo.select(Arrays.asList(archs).indexOf(IOSSimulatorLaunchConfigurationDelegate.DEFAULT_ARCH.toString()));
            archCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            archCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    updateLaunchConfigurationDialog();
                }
            });
            
            setControl(group);
        }

        @Override
        public void initializeFrom(ILaunchConfiguration config) {
            super.initializeFrom(config);
            try {
                String v = config.getAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_DEVICE_TYPE,
                        (String) null);
                DeviceType type = DeviceType.getDeviceType(RoboVMPlugin.getRoboVMHome(), v);
                if (type == null) {
                    type = DeviceType.getBestDeviceType(RoboVMPlugin.getRoboVMHome());
                }

                String[] items = deviceTypeCombo.getItems();
                for (int i = 0; i < items.length; i++) {
                    if (items[i].equals(type.getSimpleDeviceTypeId())) {
                        deviceTypeCombo.select(i);
                        break;
                    }
                }
            } catch (Exception e) {
                RoboVMPlugin.log(e);
            }
            
            try {
                String v = config.getAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_ARCH,
                        IOSSimulatorLaunchConfigurationDelegate.DEFAULT_ARCH.toString());
                int idx = Arrays.asList(archCombo.getItems()).indexOf(v);
                if (idx != -1) {
                    archCombo.select(idx);
                }
            } catch (Exception e) {
                RoboVMPlugin.log(e);
            }
        }

        @Override
        public void performApply(ILaunchConfigurationWorkingCopy wc) {
            super.performApply(wc);
            String selection = deviceTypeCombo.getItem(deviceTypeCombo.getSelectionIndex());
            wc.setAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_DEVICE_TYPE, selection);
            String arch = archCombo.getItem(archCombo.getSelectionIndex());
            wc.setAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_ARCH, arch);
        }

        @Override
        public void setDefaults(ILaunchConfigurationWorkingCopy wc) {
            super.setDefaults(wc);
            try {
                wc.setAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_DEVICE_TYPE,
                        DeviceType.getBestDeviceType(RoboVMPlugin.getRoboVMHome()).getSimpleDeviceTypeId());
            } catch (IOException e) {
                RoboVMPlugin.log(e);
            }
            wc.setAttribute(IOSSimulatorLaunchConfigurationDelegate.ATTR_IOS_SIM_ARCH, 
                    IOSSimulatorLaunchConfigurationDelegate.DEFAULT_ARCH.toString());
        }

    }

}
