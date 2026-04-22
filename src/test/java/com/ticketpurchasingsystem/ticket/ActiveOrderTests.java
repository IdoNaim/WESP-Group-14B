package com.ticketpurchasingsystem.ticket;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;

public class ActiveOrderTests {
    @BeforeEach
    public void setUp() {
        IActiveOrderRepo activeOrderRepo = Mockito.mock(IActiveOrderRepo.class);
        ActiveOrderService activeOrderService = new ActiveOrderService(new ActiveOrderListener(), new ActiveOrderPublisher(), activeOrderRepo);
    }
    @Test
    public void GivenNotThrownException_WhenSaveOrder_thenReturnTrue() {
       ActiveOrderItem activeOrder = new ActiveOrderItem(0, 0, 0, 2);
       Mockito.when(activeOrderRepo.save(activeOrder)).thenReturn(true);
        
    }
}
