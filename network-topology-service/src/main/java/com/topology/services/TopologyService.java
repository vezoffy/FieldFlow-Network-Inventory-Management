package com.topology.services;

import com.topology.clients.CustomerClient;
import com.topology.clients.InventoryClient;
import com.topology.dto.*;
import com.topology.enums.AssetType;
import com.topology.exceptions.CustomerInactiveException;
import com.topology.exceptions.DeviceNotAssignedException;
import com.topology.exceptions.TopologyServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TopologyService implements TopologyServiceInterface {

    // --- Sonar: Constants for repeated string literals ---
    private static final String NODE_TYPE_CUSTOMER = "CUSTOMER";
    private static final String NODE_TYPE_SPLITTER = "SPLITTER";
    private static final String NODE_TYPE_FDH = "FDH";
    private static final String NODE_TYPE_CORE_SWITCH = "CORE_SWITCH";
    private static final String NODE_TYPE_HEADEND = "HEADEND";

    // --- Sonar: Constants for exception message formats ---
    private static final String CUSTOMER_NOT_ACTIVE_MSG = "Customer with ID %d is not active and has no assigned network path.";
    private static final String DEVICE_NOT_FOUND_MSG = "Device with serial number '%s' not found.";
    private static final String DEVICE_NOT_ASSIGNED_MSG = "Device with serial number '%s' is not assigned to any customer.";
    private static final String UNSUPPORTED_INFRA_TYPE_MSG = "Unsupported infrastructure asset type: %s";


    private final CustomerClient customerClient;
    private final InventoryClient inventoryClient;

    @Autowired
    public TopologyService(CustomerClient customerClient, InventoryClient inventoryClient) {
        this.customerClient = customerClient;
        this.inventoryClient = inventoryClient;
    }

    /**
     * --- Sonar: Refactored to reduce Cognitive Complexity ---
     * Flattened the nested .flatMap() calls using Mono.zip.
     */
    public Mono<CustomerPathResponse> traceCustomerPath(Long customerId) {
        return customerClient.getCustomerAssignment(customerId)
                .flatMap(customer -> {
                    if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                        return Mono.error(new CustomerInactiveException(String.format(CUSTOMER_NOT_ACTIVE_MSG, customerId)));
                    }

                    // Chain dependent Monos
                    Mono<SplitterDto> splitterMono = inventoryClient.getSplitterDetails(customer.getSplitterId());
                    Mono<FdhDto> fdhMono = splitterMono.flatMap(splitter -> inventoryClient.getFdhDetails(splitter.getFdhId()));
                    Mono<CoreSwitchDto> csMono = fdhMono.flatMap(fdh -> inventoryClient.getCoreSwitchDetails(fdh.getCoreSwitchId()));
                    Mono<HeadendDto> headendMono = csMono.flatMap(cs -> inventoryClient.getHeadendDetails(cs.getHeadendId()));

                    // Zip them to get all results at once
                    return Mono.zip(splitterMono, fdhMono, csMono, headendMono)
                            .map(tuple -> buildHierarchicalPath(customer, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
                });
    }

    private CustomerPathResponse buildHierarchicalPath(CustomerAssignmentDto customer, SplitterDto splitter, FdhDto fdh, CoreSwitchDto coreSwitch, HeadendDto headend) {
        HierarchicalNetworkNode customerNode = new HierarchicalNetworkNode();
        customerNode.setType(NODE_TYPE_CUSTOMER);
        customerNode.setIdentifier(customer.getName());
        customerNode.setDetail("Port: " + customer.getAssignedPort());
        customerNode.setAssets(customer.getAssignedAssets());

        String splitterDetail = splitter.getPortCapacity() + " Ports";
        if (splitter.getNeighborhood() != null && !splitter.getNeighborhood().isEmpty()) {
            splitterDetail += ", " + splitter.getNeighborhood();
        }
        HierarchicalNetworkNode splitterNode = new HierarchicalNetworkNode(NODE_TYPE_SPLITTER, "Splitter-" + splitter.getId(), splitterDetail, splitter.getSerialNumber(), splitter.getModel());
        splitterNode.setChild(customerNode);

        HierarchicalNetworkNode fdhNode = new HierarchicalNetworkNode(NODE_TYPE_FDH, fdh.getName(), fdh.getRegion(), fdh.getSerialNumber(), fdh.getModel());
        fdhNode.setChild(splitterNode);

        HierarchicalNetworkNode coreSwitchNode = new HierarchicalNetworkNode(NODE_TYPE_CORE_SWITCH, coreSwitch.getName(), coreSwitch.getLocation(), coreSwitch.getSerialNumber(), coreSwitch.getModel());
        coreSwitchNode.setChild(fdhNode);

        HierarchicalNetworkNode headendNode = new HierarchicalNetworkNode(NODE_TYPE_HEADEND, headend.getName(), headend.getLocation(), headend.getSerialNumber(), headend.getModel());
        headendNode.setChild(coreSwitchNode);

        return new CustomerPathResponse(customer.getCustomerId(), customer.getName(), headendNode);
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

    /**
     * --- Sonar: Refactored to reduce Cognitive Complexity ---
     * Extracted nested flatMap logic into private helper methods.
     */
    public Mono<HeadendTopologyDto> getHeadendTopology(Long headendId) {
        return inventoryClient.getHeadendTopology(headendId)
                .flatMap(headendTopology -> Flux.fromIterable(headendTopology.getCoreSwitches())
                        .flatMap(this::enrichCoreSwitch) // Helper method
                        .collectList()
                        .map(enrichedCoreSwitches -> {
                            headendTopology.setCoreSwitches(enrichedCoreSwitches);
                            return headendTopology;
                        }));
    }

    private Mono<CoreSwitchTopologyDto> enrichCoreSwitch(CoreSwitchTopologyDto coreSwitch) {
        return Flux.fromIterable(coreSwitch.getFdhs())
                .flatMap(this::enrichFdh) // Helper method
                .collectList()
                .map(enrichedFdhs -> {
                    coreSwitch.setFdhs(enrichedFdhs);
                    return coreSwitch;
                });
    }

    private Mono<FdhTopologyDto> enrichFdh(FdhTopologyDto fdh) {
        return Flux.fromIterable(fdh.getSplitters())
                .flatMap(this::enrichSplitter) // Helper method
                .collectList()
                .map(enrichedSplitters -> {
                    fdh.setSplitters(enrichedSplitters);
                    return fdh;
                });
    }

    private Mono<SplitterDto> enrichSplitter(SplitterDto splitter) {
        return customerClient.getCustomersBySplitter(splitter.getId())
                .map(customers -> {
                    splitter.setCustomers(customers);
                    return splitter;
                });
    }


    public Mono<Object> traceDevicePath(String serialNumber) {
        return inventoryClient.getAssetAssignmentDetails(serialNumber)
                .flatMap(assignment -> {
                    if (assignment == null) {
                        return Mono.error(new DeviceNotAssignedException(String.format(DEVICE_NOT_FOUND_MSG, serialNumber)));
                    }

                    if (assignment.getCustomerId() != null) {
                        return traceCustomerPath(assignment.getCustomerId()).cast(Object.class);
                    }

                    if (assignment.getAssetType() != AssetType.ONT && assignment.getAssetType() != AssetType.ROUTER) {
                        return traceInfrastructurePath(serialNumber).cast(Object.class);
                    }

                    return Mono.error(new DeviceNotAssignedException(String.format(DEVICE_NOT_ASSIGNED_MSG, serialNumber)));
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
                            path.add(new NetworkNode(NODE_TYPE_SPLITTER, "Splitter-" + splitter.getId(), detail, splitter.getSerialNumber(), splitter.getModel(), null));
                            return buildInfrastructurePath(AssetType.FDH, splitter.getFdhId(), path);
                        });
            case FDH:
                return inventoryClient.getFdhDetails(currentDeviceId)
                        .flatMap(fdh -> {
                            path.add(new NetworkNode(NODE_TYPE_FDH, fdh.getName(), fdh.getRegion(), fdh.getSerialNumber(), fdh.getModel(), null));
                            return buildInfrastructurePath(AssetType.CORE_SWITCH, fdh.getCoreSwitchId(), path);
                        });
            case CORE_SWITCH:
                return inventoryClient.getCoreSwitchDetails(currentDeviceId)
                        .flatMap(coreSwitch -> {
                            path.add(new NetworkNode(NODE_TYPE_CORE_SWITCH, coreSwitch.getName(), coreSwitch.getLocation(), coreSwitch.getSerialNumber(), coreSwitch.getModel(), null));
                            return buildInfrastructurePath(AssetType.HEADEND, coreSwitch.getHeadendId(), path);
                        });
            case HEADEND:
                return inventoryClient.getHeadendDetails(currentDeviceId)
                        .map(headend -> {
                            path.add(new NetworkNode(NODE_TYPE_HEADEND, headend.getName(), headend.getLocation(), headend.getSerialNumber(), headend.getModel(), null));
                            Collections.reverse(path);
                            return path;
                        });
            // --- Sonar: Group unhandled/default cases ---
            case ONT, ROUTER:
            default:
                return Mono.error(new TopologyServiceException(String.format(UNSUPPORTED_INFRA_TYPE_MSG, currentType)));
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