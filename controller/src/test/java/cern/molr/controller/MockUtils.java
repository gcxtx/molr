/**
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING”. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.molr.controller;

import cern.molr.commons.domain.Mole;
import cern.molr.commons.domain.Service;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class that provides utility methods for mocking domain objects
 *
 * @author tiagomr
 */
public class MockUtils {
    public static List<Mole> getMockedContainers(int numberOfAgentContainers, int numberOfServices, int numberOfEntryPoints) {
        List<Mole> mockedMoles = new ArrayList<>();
        for (int index = 0; index < numberOfAgentContainers; ++index) {
            mockedMoles.add(getMockedContainer(index, numberOfServices, numberOfEntryPoints));
        }
        return mockedMoles;
    }

    public static Mole getMockedContainer(int containerNumber, int numberOfServices, int numberOfEntryPoints) {
        Mole mockedMole = mock(Mole.class);
        when(mockedMole.getContainerName()).thenReturn("MockedContainerName" + containerNumber);
        when(mockedMole.getContainerPath()).thenReturn("MockedContainerPath" + containerNumber);
        List<Service> mockedService = getMockedService(numberOfServices, numberOfEntryPoints);
        when(mockedMole.getServices()).thenReturn(mockedService);
        return mockedMole;
    }

    public static List<Service> getMockedService(int numberOfServices, int numberOfEntryPoints) {
        List<Service> mockedServices = new ArrayList<>();
        for (int serviceIndex = 0; serviceIndex < numberOfServices; ++serviceIndex) {
            List<String> entryPoints = new ArrayList<>();
            for (int index = 0; index < numberOfEntryPoints; ++index) {
                entryPoints.add("MockedEntryPoint" + index);
            }
            Service mockedService = mock(Service.class);
            when(mockedService.getAgentName()).thenReturn("MockedAgentName");
            when(mockedService.getClassName()).thenReturn("MockedClassName");
            when(mockedService.getEntryPoints()).thenReturn(entryPoints);
            mockedServices.add(mockedService);
        }
        return mockedServices;
    }

}