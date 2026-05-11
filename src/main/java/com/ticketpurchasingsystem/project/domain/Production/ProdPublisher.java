package com.ticketpurchasingsystem.project.domain.Production;

import com.ticketpurchasingsystem.project.domain.Utils.Publisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;
import java.util.ArrayList;
import java.util.List;

public class ProdPublisher extends Publisher {
    private static ProdPublisher instance;
    private final List<ProdListener> listeners = new ArrayList<>();

    public static ProdPublisher getInstance() {
        if (instance == null) {
            instance = new ProdPublisher();
        }
        return instance;
    }

    public void addListener(ProdListener listener) {
        listeners.add(listener);
    }

    public void publish(NewProdEvent event) {
        for (ProdListener listener : listeners) {
            try {
                listener.onNewProductionCompany(event);
            } catch (Exception e) {
                loggerDef logger = loggerDef.getInstance();
                logger.error("Failed to publish new production company event: " + event.getCompany().getCompanyName());
            }
        }
    }

    public void publish(AssignOwnerEvent event) {
        for (ProdListener listener : listeners) {
            try {
                listener.onAssignOwner(event);
            } catch (Exception e) {
                loggerDef.getInstance().error(
                        "Failed to publish AssignOwnerEvent: company="
                                + event.getCompany().getCompanyName()
                                + ", appointee=" + event.getAppointeeId());
            }
        }
    }
}
