package br.com.erpsystem.mssales.services;

import br.com.erpsystem.mssales.client.CustomerClient;
import br.com.erpsystem.mssales.client.ProductClient;
import br.com.erpsystem.mssales.constants.BusinessErrorConstants;
import br.com.erpsystem.mssales.constants.ErrorCodes;
import br.com.erpsystem.mssales.dto.CustomerDTO;
import br.com.erpsystem.mssales.dto.OrderDTO;
import br.com.erpsystem.mssales.dto.ProductDTO;
import br.com.erpsystem.mssales.dto.http.request.CreateOrderRequestDTO;
import br.com.erpsystem.mssales.dto.http.response.CreateOrderResponseDTO;
import br.com.erpsystem.mssales.dto.http.response.SearchOrderResponseDTO;
import br.com.erpsystem.mssales.dto.http.response.SearchOrdersResponseDTO;
import br.com.erpsystem.mssales.entity.Order;
import br.com.erpsystem.mssales.exceptions.ComercialException;
import br.com.erpsystem.mssales.exceptions.ExceptionResponse;
import br.com.erpsystem.mssales.exceptions.ProductOutOfStockException;
import br.com.erpsystem.mssales.mapper.OrderMapper;
import br.com.erpsystem.mssales.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.erpsystem.mssales.constants.BusinessErrorConstants.PRODUCT_OUT_OF_STOCK;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper mapper;

    @Override
    public CreateOrderResponseDTO createOrder(CreateOrderRequestDTO orderDTO) {

        log.info("OrderServiceImpl.createOrder - Start - order: {}", orderDTO);

        CustomerDTO customerDTO = findCustomer(orderDTO);

        if(orderDTO.getOrderDTO().getProductsOrder().isEmpty() || ObjectUtils.isEmpty(customerDTO)){
            log.info("OrderServiceImpl.createOrder - Error - order: {}", orderDTO);
            throw new ComercialException();
        }
        orderDTO.getOrderDTO().setCreateDate(LocalDate.now());
        orderDTO.getOrderDTO().setTotalPrice(calculateTotalPrice(orderDTO));
        Order order = orderRepository.save(mapper.orderDTOToOrder(orderDTO.getOrderDTO()));

        log.info("OrderServiceImpl.createOrder - End");
        return CreateOrderResponseDTO.builder().orderId(mapper.orderToOrderDTO(order).getId()).build();
    }

    @Override
    @Transactional
    public SearchOrderResponseDTO searchOrderById(String id) {
        log.info("OrderServiceImpl.searchOrderById - Start - Id: {}", id);

        OrderDTO orderDTO = mapper.orderToOrderDTO(orderRepository.getReferenceById(UUID.fromString(id)));
        log.info("OrderServiceImpl.searchOrderById - End");
        return SearchOrderResponseDTO.builder().orderDTO(orderDTO).build();
    }

    @Override
    @Transactional
    public SearchOrdersResponseDTO searchOrderByCpf(String cpf) {
        List<OrderDTO> orderDTOS = mapper.ordersToOrdersDTO(orderRepository.findOrdersBycustomerCpf(cpf));
        return SearchOrdersResponseDTO.builder().orderDTO(orderDTOS).build();
    }

    private CustomerDTO findCustomer(CreateOrderRequestDTO orderRequestDTO){
        return customerClient.findCustomerByCpf(orderRequestDTO.getOrderDTO().getCustomerCpf());
    }

    private Double calculateTotalPrice(CreateOrderRequestDTO orderRequestDTO){
        getUnitPrice(orderRequestDTO);
        return orderRequestDTO.getOrderDTO().getProductsOrder().stream().mapToDouble(order -> order.getUnitPrice() * order.getQuantity()).sum();
    }

    private void getUnitPrice(CreateOrderRequestDTO orderRequestDTO){
        orderRequestDTO.getOrderDTO().getProductsOrder().forEach(orderItemDTO -> {
            ProductDTO productDTO = productClient.findProductById(orderItemDTO.getProductId()).getProductDTO();

            if(validateProductStock(productDTO, orderItemDTO.getQuantity())){
                orderItemDTO.setUnitPrice(productDTO.getUnitPrice());
            }else{
                log.error("OrderServiceImpl.getUnitPrice - Product Out Of Stock");
                throw new ProductOutOfStockException(new ExceptionResponse(ErrorCodes.INVALID_REQUEST, PRODUCT_OUT_OF_STOCK));
            }

        });
    }

    private boolean validateProductStock(ProductDTO productDTO, Integer orderQuantity){
        return orderQuantity >= productDTO.getQuantityInStock();
    }

}
