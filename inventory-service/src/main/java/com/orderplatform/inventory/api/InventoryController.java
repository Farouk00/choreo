package com.orderplatform.inventory.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderplatform.inventory.domain.InventoryService;
import com.orderplatform.inventory.domain.Product;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    public record ProductResponse(String sku, String name, int available) {

        static ProductResponse from(Product p) {
            return new ProductResponse(p.getSku(), p.getName(), p.getAvailable());
        }
    }

    public record RestockRequest(@NotBlank String name, @Min(1) int quantity) {
    }

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return inventoryService.listProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> get(@PathVariable String sku) {
        return ResponseEntity.of(inventoryService.findProduct(sku).map(ProductResponse::from));
    }

    @PostMapping("/{sku}/restock")
    public ProductResponse restock(@PathVariable String sku, @Valid @RequestBody RestockRequest request) {
        return ProductResponse.from(inventoryService.restock(sku, request.name(), request.quantity()));
    }
}
