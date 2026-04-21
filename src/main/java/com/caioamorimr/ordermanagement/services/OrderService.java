package com.caioamorimr.ordermanagement.services;


import com.caioamorimr.ordermanagement.entities.Order;
import com.caioamorimr.ordermanagement.repositories.OrderRepository;
import com.caioamorimr.ordermanagement.services.exceptions.DatabaseException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Order insert(Order order) {
        return orderRepository.save(order);
    }

    public void delete(Long id) {
        if(!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        try {
            orderRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public Order update(Long id, Order order) {
        try {
            Order entity = orderRepository.getReferenceById(id);
            updateData(entity, order);
            return orderRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Order entity, Order order) {
        entity.setMoment(order.getMoment());
        entity.setOrderStatus(order.getOrderStatus());
    }
}
