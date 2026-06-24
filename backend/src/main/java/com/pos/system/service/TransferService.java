package com.pos.system.service;

import com.pos.system.dto.request.CreateTransferRequest;
import com.pos.system.dto.request.ReceiveTransferRequest;
import com.pos.system.dto.response.StockTransferResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferItemRepository stockTransferItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final BranchRepository branchRepository;

    @Transactional
    public StockTransferResponse crear(CreateTransferRequest request, Long userId) {
        Branch origen = branchRepository.findById(request.getSucursalOrigenId())
                .orElseThrow(() -> new BadRequestException("Sucursal origen no encontrada"));
        Branch destino = branchRepository.findById(request.getSucursalDestinoId())
                .orElseThrow(() -> new BadRequestException("Sucursal destino no encontrada"));

        if (!origen.getActiva() || !destino.getActiva()) {
            throw new BadRequestException("Ambas sucursales deben estar activas");
        }
        if (origen.getId().equals(destino.getId())) {
            throw new BadRequestException("La sucursal origen y destino deben ser diferentes");
        }

        StockTransfer transfer = stockTransferRepository.save(StockTransfer.builder()
                .sucursalOrigenId(request.getSucursalOrigenId())
                .sucursalDestinoId(request.getSucursalDestinoId())
                .motivo(request.getMotivo())
                .solicitadoPor(userId)
                .estado(StockTransfer.Estado.SOLICITADA)
                .build());

        List<StockTransferItem> items = new ArrayList<>();
        for (var itemReq : request.getItems()) {
            StockTransferItem item = stockTransferItemRepository.save(StockTransferItem.builder()
                    .transferId(transfer.getId())
                    .productId(itemReq.getProductId())
                    .cantidadSolicitada(itemReq.getCantidad())
                    .build());
            items.add(item);
        }

        return toResponse(transfer, items);
    }

    @Transactional
    public StockTransferResponse despachar(Long transferId, Long userId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BadRequestException("Transferencia no encontrada"));

        if (transfer.getEstado() != StockTransfer.Estado.SOLICITADA) {
            throw new BadRequestException("Solo se pueden despachar transferencias en estado SOLICITADA");
        }

        List<StockTransferItem> items = stockTransferItemRepository.findByTransferId(transferId);
        User mockUser = User.builder().id(userId).build();

        for (var item : items) {
            List<Warehouse> originWarehouses = warehouseRepository.findByBranchId(transfer.getSucursalOrigenId());
            if (originWarehouses.isEmpty()) {
                throw new BadRequestException("La sucursal origen no tiene bodegas configuradas");
            }
            Warehouse originWarehouse = originWarehouses.get(0);

            item.setCantidadDespachada(item.getCantidadSolicitada());
            stockTransferItemRepository.save(item);

            stockMovementService.transferStock(
                    item.getProductId(),
                    originWarehouse.getId(),
                    findDestinationWarehouse(transfer.getSucursalDestinoId()).getId(),
                    BigDecimal.valueOf(item.getCantidadSolicitada()),
                    "Transferencia #" + transferId + ": " + (transfer.getMotivo() != null ? transfer.getMotivo() : ""),
                    mockUser
            );
        }

        transfer.setEstado(StockTransfer.Estado.EN_TRANSITO);
        transfer.setDespachadoPor(userId);
        transfer.setFechaDespacho(LocalDateTime.now());
        stockTransferRepository.save(transfer);

        return toResponse(transfer, items);
    }

    @Transactional
    public StockTransferResponse recibir(Long transferId, ReceiveTransferRequest request, Long userId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BadRequestException("Transferencia no encontrada"));

        if (transfer.getEstado() != StockTransfer.Estado.EN_TRANSITO) {
            throw new BadRequestException("Solo se pueden recibir transferencias en estado EN_TRANSITO");
        }

        List<StockTransferItem> items = stockTransferItemRepository.findByTransferId(transferId);
        Map<Long, Integer> recibidos = request.getItems().stream()
                .collect(Collectors.toMap(ReceiveTransferRequest.ReceivedItem::getProductId,
                        ReceiveTransferRequest.ReceivedItem::getCantidadRecibida));

        User mockUser = User.builder().id(userId).build();
        Warehouse destWarehouse = findDestinationWarehouse(transfer.getSucursalDestinoId());

        for (var item : items) {
            int recibido = recibidos.getOrDefault(item.getProductId(), 0);
            item.setCantidadRecibida(recibido);

            if (recibido > 0) {
                int despachado = item.getCantidadDespachada() != null ? item.getCantidadDespachada() : 0;
                int diferencia = despachado - recibido;

                stockMovementService.createMovement(
                        item.getProductId(),
                        destWarehouse.getId(),
                        StockMovement.MovementType.ENTRADA_TRANSFERENCIA,
                        BigDecimal.valueOf(recibido),
                        null,
                        "Recepción transferencia #" + transferId + (diferencia > 0 ? " (merma en tránsito: " + diferencia + ")" : ""),
                        null,
                        mockUser
                );
            }
            stockTransferItemRepository.save(item);
        }

        transfer.setEstado(StockTransfer.Estado.RECIBIDA);
        transfer.setRecibidoPor(userId);
        transfer.setFechaRecepcion(LocalDateTime.now());
        stockTransferRepository.save(transfer);

        return toResponse(transfer, items);
    }

    @Transactional
    public StockTransferResponse cancelar(Long transferId, Long userId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BadRequestException("Transferencia no encontrada"));

        if (transfer.getEstado() != StockTransfer.Estado.SOLICITADA) {
            throw new BadRequestException("Solo se pueden cancelar transferencias en estado SOLICITADA");
        }

        transfer.setEstado(StockTransfer.Estado.CANCELADA);
        stockTransferRepository.save(transfer);

        List<StockTransferItem> items = stockTransferItemRepository.findByTransferId(transferId);
        return toResponse(transfer, items);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse obtener(Long transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BadRequestException("Transferencia no encontrada"));
        List<StockTransferItem> items = stockTransferItemRepository.findByTransferId(transferId);
        return toResponse(transfer, items);
    }

    @Transactional(readOnly = true)
    public List<StockTransferResponse> listar(Long sucursalOrigenId, Long sucursalDestinoId,
                                               StockTransfer.Estado estado,
                                               LocalDateTime desde, LocalDateTime hasta) {
        List<StockTransfer> transfers;

        if (sucursalOrigenId != null && sucursalDestinoId != null) {
            transfers = stockTransferRepository
                    .findBySucursalOrigenIdAndSucursalDestinoIdOrderByFechaSolicitudDesc(sucursalOrigenId, sucursalDestinoId);
        } else if (sucursalOrigenId != null) {
            transfers = stockTransferRepository.findBySucursalOrigenIdOrderByFechaSolicitudDesc(sucursalOrigenId);
        } else if (sucursalDestinoId != null) {
            transfers = stockTransferRepository.findBySucursalDestinoIdOrderByFechaSolicitudDesc(sucursalDestinoId);
        } else if (estado != null) {
            transfers = stockTransferRepository.findByEstadoOrderByFechaSolicitudDesc(estado);
        } else if (desde != null && hasta != null) {
            transfers = stockTransferRepository.findByFechaSolicitudBetweenOrderByFechaSolicitudDesc(desde, hasta);
        } else {
            transfers = stockTransferRepository.findAll();
        }

        return transfers.stream()
                .map(t -> toResponse(t, stockTransferItemRepository.findByTransferId(t.getId())))
                .toList();
    }

    private Warehouse findDestinationWarehouse(Long branchId) {
        List<Warehouse> warehouses = warehouseRepository.findByBranchId(branchId);
        if (warehouses.isEmpty()) {
            throw new BadRequestException("La sucursal destino no tiene bodegas configuradas");
        }
        return warehouses.get(0);
    }

    private StockTransferResponse toResponse(StockTransfer transfer, List<StockTransferItem> items) {
        List<StockTransferResponse.TransferItemResponse> itemResponses = items.stream()
                .map(item -> {
                    String productName = productRepository.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("Producto #" + item.getProductId());
                    return StockTransferResponse.TransferItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productName(productName)
                            .cantidadSolicitada(item.getCantidadSolicitada())
                            .cantidadDespachada(item.getCantidadDespachada())
                            .cantidadRecibida(item.getCantidadRecibida())
                            .build();
                })
                .toList();

        return StockTransferResponse.builder()
                .id(transfer.getId())
                .sucursalOrigenId(transfer.getSucursalOrigenId())
                .sucursalDestinoId(transfer.getSucursalDestinoId())
                .estado(transfer.getEstado().name())
                .motivo(transfer.getMotivo())
                .solicitadoPor(transfer.getSolicitadoPor())
                .despachadoPor(transfer.getDespachadoPor())
                .recibidoPor(transfer.getRecibidoPor())
                .fechaSolicitud(transfer.getFechaSolicitud())
                .fechaDespacho(transfer.getFechaDespacho())
                .fechaRecepcion(transfer.getFechaRecepcion())
                .items(itemResponses)
                .build();
    }
}
