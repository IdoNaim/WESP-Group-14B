package com.ticketpurchasingsystem.project.domain.Orders.OrderEvents;

public abstract  class OrderEvent {
        private String orderId;
        private String eventType;
    
        public OrderEvent(String orderId, String eventType) {
            this.orderId = orderId;
            this.eventType = eventType;
        }
    
        public String getOrderId() {
            return orderId;
        }
    
        public String getEventType() {
            return eventType;
        }
}
