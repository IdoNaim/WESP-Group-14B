package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.tickets.*;

public class TicketService {
    private TicketListener ticketListener;
    private TicketPublisher ticketPublisher;
    private IBarCodeGateway barCodeGateway;
    public TicketService(TicketListener ticketListener, TicketPublisher ticketPublisher, IBarCodeGateway barCodeGateway) {
        this.ticketListener = ticketListener;
        this.ticketPublisher = ticketPublisher;
        this.barCodeGateway = barCodeGateway;
    }
}