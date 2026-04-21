package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderUpdateDTO;
import com.caioamorimr.ordermanagement.entities.Order;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.repositories.OrderRepository;
import com.caioamorimr.ordermanagement.repositories.UserRepository;
import com.caioamorimr.ordermanagement.services.exceptions.DatabaseException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<OrderDTO> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderDTO::new);
    }

    @Transactional(readOnly = true)
    public OrderDTO findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return new OrderDTO(order);
    }

    @Transactional
    public OrderDTO insert(OrderInsertDTO dto) {
        User client = userRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(dto.getClientId()));

        Order order = new Order();
        order.setMoment(dto.getMoment() != null ? dto.getMoment() : Instant.now());
        order.setOrderStatus(dto.getOrderStatus());
        order.setClient(client);

        return new OrderDTO(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        try {
            orderRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Transactional
    public OrderDTO update(Long id, OrderUpdateDTO dto) {
        try {
            Order entity = orderRepository.getReferenceById(id);
            entity.setMoment(dto.getMoment());
            entity.setOrderStatus(dto.getOrderStatus());
            return new OrderDTO(orderRepository.save(entity));
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }
}