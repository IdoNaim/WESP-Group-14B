package com.ticketpurchasingsystem.project.domain.User;

import com.ticketpurchasingsystem.project.domain.User.UserEvents.userEvents;

public class UserListener {
        private IUserRepo userRepo;
        private UserPublisher publisher;
    
        public UserListener(IUserRepo userRepo, UserPublisher publisher) {
            this.userRepo = userRepo;
            this.publisher = publisher;
        }
    
        public void onUserCreated(userEvents event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User created: " + event.getUserId());
        }
    
}
