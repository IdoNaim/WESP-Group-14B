package com.ticketpurchasingsystem.project.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public class HistoryOrderService implements IHistoryOrderService {
    @Override
    public void saveHistoryOrder(String orderId, String userId, int companyId, String eventName, int ticketsPurchased, String purchaseDate) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteHistoryOrder(String orderId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getHistoryOrder(SessionToken sessionToken, String orderId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrdersByUser(SessionToken sessionToken, String userId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrders(SessionToken sessionToken) {
        // TODO Auto-generated method stub
    }  
}
