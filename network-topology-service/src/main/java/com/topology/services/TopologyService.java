package com.topology.services;

import com.topology.clients.CustomerClient;
import com.topology.clients.InventoryClient;
import com.topology.dto.*;
import com.topology.enums.AssetType;
import com.topology.exceptions.CustomerInactiveException;
import com.topology.exceptions.DeviceNotAssignedException;
import com.topology.exceptions.InfrastructureDeviceException;
import com.topology.exceptions.TopologyServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TopologyService {

    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private InventoryClient inventoryClient;

    public Mono<CustomerPathResponse> traceCustomerPath(Long customerId) {
        return customerClient.getCustomerAssignment(customerId)
                .flatMap(customer -> {
                    if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                        return Mono.error(new CustomerInactiveException("Customer with ID " + customerId + " is not active and has no assigned network path."));
                    }

                    return inventoryClient.getSplitterDetails(customer.getSplitterId())
                            .flatMap(splitter -> inventoryClient.getFdhDetails(splitter.getFdhId())
                                    .flatMap(fdh -> inventoryClient.getCoreSwitchDetails(fdh.getCoreSwitchId())
                                            .flatMap(coreSwitch -> inventoryClient.getHeadendDetails(coreSwitch.getHeadendId())
                                                    .map(headend -> {
                                                        HierarchicalNetworkNode customerNode = new HierarchicalNetworkNode();
                                                        customerNode.setType("CUSTOMER");
                                                        customerNode.setIdentifier(customer.getName());
                                                        customerNode.setDetail("Port: " + customer.getAssignedPort());
                                                        customerNode.setAssets(customer.getAssignedAssets());

                                                        String splitterDetail = splitter.getPortCapacity() + " Ports";
                                                        if (splitter.getNeighborhood() != null && !splitter.getNeighborhood().isEmpty()) {
                                                            splitterDetail += ", " + splitter.getNeighborhood();
                                                        }
                                                        HierarchicalNetworkNode splitterNode = new HierarchicalNetworkNode("SPLITTER", "Splitter-" + splitter.getId(), splitterDetail, splitter.getSerialNumber(), splitter.getModel());
                                                        splitterNode.setChild(customerNode);

                                                        HierarchicalNetworkNode fdhNode = new HierarchicalNetworkNode("FDH", fdh.getName(), fdh.getRegion(), fdh.getSerialNumber(), fdh.getModel());
                                                        fdhNode.setChild(splitterNode);

                                                        HierarchicalNetworkNode coreSwitchNode = new HierarchicalNetworkNode("CORE_SWITCH", coreSwitch.getName(), coreSwitch.getLocation(), coreSwitch.getSerialNumber(), coreSwitch.getModel());
                                                        coreSwitchNode.setChild(fdhNode);

                                                        HierarchicalNetworkNode headendNode = new HierarchicalNetworkNode("HEADEND", headend.getName(), headend.getLocation(), headend.getSerialNumber(), headend.getModel());
                                                        headendNode.setChild(coreSwitchNode);

                                                        return new CustomerPathResponse(customerId, customer.getName(), headendNode);
                                                    })
                                            )
                                    )
                            );
                });
    }

    public Mono<FdhTopologyResponse> getFdhTopology(Long fdhId) {
        Mono<FdhDto> fdhMono = inventoryClient.getFdhDetails(fdhId);
        Mono<List<SplitterDto>> splittersMono = inventoryClient.getSplittersByFdh(fdhId);

        return Mono.zip(fdhMono, splittersMono)
                .flatMap(tuple -> {
                    FdhDto fdh = tuple.getT1();
                    List<SplitterDto> splitters = tuple.getT2();

                    return Flux.fromIterable(splitters)
                            .flatMap(splitter -> customerClient.getCustomersBySplitter(splitter.getId())
                                    .map(customers -> new SplitterView(splitter.getId(), splitter.getPortCapacity(), splitter.getUsedPorts(), customers)))
                            .collectList()
                            .map(splitterViews -> new FdhTopologyResponse(fdh.getId(), fdh.getName(), fdh.getRegion(), splitterViews));
                });
    }

    public Mono<HeadendTopologyDto> getHeadendTopology(Long headendId) {
        return inventoryClient.getHeadendTopology(headendId)
                .flatMap(headendTopology -> {
                    return Flux.fromIterable(headendTopology.getCoreSwitches())
                            .flatMap(coreSwitchTopologyDto -> {
                                return Flux.fromIterable(coreSwitchTopologyDto.getFdhs())
                                        .flatMap(fdhTopologyDto -> {
                                            return Flux.fromIterable(fdhTopologyDto.getSplitters())
                                                    .flatMap(splitterDto -> customerClient.getCustomersBySplitter(splitterDto.getId())
                                                            .map(customers -> {
                                                                splitterDto.setCustomers(customers);
                                                                return splitterDto;
                                                            }))
                                                    .collectList()
                                                    .map(enrichedSplitters -> {
                                                        fdhTopologyDto.setSplitters(enrichedSplitters);
                                                        return fdhTopologyDto;
                                                    });
                                        })
                                        .collectList()
                                        .map(updatedFdhs -> {
                                            coreSwitchTopologyDto.setFdhs(updatedFdhs);
                                            return coreSwitchTopologyDto;
                                        });
                            })
                            .collectList()
                            .map(updatedCoreSwitches -> {
                                headendTopology.setCoreSwitches(updatedCoreSwitches);
                                return headendTopology;
                            });
                });
    }

    public Mono<Object> traceDevicePath(String serialNumber) {
        return inventoryClient.getAssetAssignmentDetails(serialNumber)
                .flatMap(assignment -> {
                    if (assignment == null) {
                        return Mono.error(new DeviceNotAssignedException("Device with serial number '" + serialNumber + "' not found."));
                    }

                    // If it's a customer-facing device, trace the customer path
                    if (assignment.getCustomerId() != null) {
                        return traceCustomerPath(assignment.getCustomerId()).cast(Object.class);
                    }

                    // If it's an infrastructure device, trace the infrastructure path
                    if (assignment.getAssetType() != AssetType.ONT && assignment.getAssetType() != AssetType.ROUTER) {
                        return traceInfrastructurePath(serialNumber).cast(Object.class);
                    }

                    // If it's an unassigned ONT or Router
                    return Mono.error(new DeviceNotAssignedException("Device with serial number '" + serialNumber + "' is not assigned to any customer."));
                });
    }

    public Mono<InfrastructurePathResponse> traceInfrastructurePath(String serialNumber) {
        return inventoryClient.getAssetAssignmentDetails(serialNumber)
                .flatMap(assignment -> {
                    Mono<List<NetworkNode>> pathMono = buildInfrastructurePath(assignment.getAssetType(), assignment.getAssetId(), new ArrayList<>());
                    return Mono.zip(Mono.just(assignment), pathMono);
                })
                .map(tuple -> {
                    AssetAssignmentDetailsDto assignment = tuple.getT1();
                    List<NetworkNode> path = tuple.getT2();
                    HierarchicalNetworkNode hierarchicalPath = toHierarchicalPath(path);
                    return new InfrastructurePathResponse(serialNumber, assignment.getAssetType(), hierarchicalPath);
                });
    }

    private Mono<List<NetworkNode>> buildInfrastructurePath(AssetType currentType, Long currentDeviceId, List<NetworkNode> path) {
        if (currentDeviceId == null) {
            return Mono.just(path);
        }

        switch (currentType) {
            case SPLITTER:
                return inventoryClient.getSplitterDetails(currentDeviceId)
                        .flatMap(splitter -> {
                            String detail = splitter.getPortCapacity() + " Ports, " + splitter.getNeighborhood();
                            path.add(new NetworkNode("SPLITTER", "Splitter-" + splitter.getId(), detail, splitter.getSerialNumber(), splitter.getModel(), null));
                            return buildInfrastructurePath(AssetType.FDH, splitter.getFdhId(), path);
                        });
            case FDH:
                return inventoryClient.getFdhDetails(currentDeviceId)
                        .flatMap(fdh -> {
                            path.add(new NetworkNode("FDH", fdh.getName(), fdh.getRegion(), fdh.getSerialNumber(), fdh.getModel(), null));
                            return buildInfrastructurePath(AssetType.CORE_SWITCH, fdh.getCoreSwitchId(), path);
                        });
            case CORE_SWITCH:
                return inventoryClient.getCoreSwitchDetails(currentDeviceId)
                        .flatMap(coreSwitch -> {
                            path.add(new NetworkNode("CORE_SWITCH", coreSwitch.getName(), coreSwitch.getLocation(), coreSwitch.getSerialNumber(), coreSwitch.getModel(), null));
                            return buildInfrastructurePath(AssetType.HEADEND, coreSwitch.getHeadendId(), path);
                        });
            case HEADEND:
                return inventoryClient.getHeadendDetails(currentDeviceId)
                        .map(headend -> {
                            path.add(new NetworkNode("HEADEND", headend.getName(), headend.getLocation(), headend.getSerialNumber(), headend.getModel(), null));
                            Collections.reverse(path);
                            return path;
                        });
            default:
                return Mono.error(new TopologyServiceException("Unsupported infrastructure asset type: " + currentType));
        }
    }

    private HierarchicalNetworkNode toHierarchicalPath(List<NetworkNode> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        HierarchicalNetworkNode root = null;
        HierarchicalNetworkNode current = null;
        for (NetworkNode node : path) {
            HierarchicalNetworkNode hierarchicalNode = new HierarchicalNetworkNode(node.getType(), node.getIdentifier(), node.getDetail(), node.getSerialNumber(), node.getModel());
            if (root == null) {
                root = hierarchicalNode;
                current = root;
            } else {
                current.setChild(hierarchicalNode);
                current = hierarchicalNode;
            }
        }
        return root;
    }
}
