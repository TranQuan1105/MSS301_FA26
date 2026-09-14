package com.fudn.product_service.service;

import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.dto.ProductResponse;
import com.fudn.product_service.model.Product;
import com.fudn.product_service.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final IProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest productRequest) {
        // Ánh xạ từ ProductRequest sang Product entity nếu cần thiết
        // sau đó lưu vào cơ sở dữ liệu thông qua repository.

        Product product = Product.builder()
                .id(productRequest.id())
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .build();

        productRepository.save(product);
        log.info("Product {} saved.", product.getId());

        // Ánh xạ từ Product entity sang ProductResponse để trả về cho client.
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice());
        // Logic to create a product
    }

    public List<ProductResponse> getAllProducts() {
        // Lấy tất cả sản phẩm từ cơ sở dữ liệu thông qua repository,
        // sau đó ánh xạ chúng sang ProductResponse để trả về cho client.

        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice()))
                .toList();
    }
}