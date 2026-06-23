# WESP-Group-14B

## System Environment and Database

This project is configured to run locally using an in-memory **H2 Database** rather than a remote Google Cloud database. This ensures a fast, isolated environment for development and testing.

The application behavior and data initialization are controlled via Spring Boot profiles:
* **dev**: Used for standard local development. This profile boots the application, connects to the local H2 database, and automatically runs initialization scripts to seed the database with test data.
* **prod**: Used for the production environment.

---

## Booting the System

To boot the system locally using the development profile, run the following Maven command in your terminal from the root directory:

    mvn clean spring-boot:run "-Dspring-boot.run.profiles=dev"

Once the application is running, you can access the H2 database console at http://localhost:8080/h2-console to inspect your tables and data.

---

## Configuration Files

Environment configurations are handled via standard Spring Boot properties files. You must ensure your application-dev.properties is configured correctly to use the H2 database.

Add the following configuration to your application-dev.properties file:

    # Database Connection
    spring.datasource.url=jdbc:h2:mem:ticketdb;DB_CLOSE_DELAY=-1
    spring.datasource.driverClassName=org.h2.Driver
    spring.datasource.username=sa
    spring.datasource.password=password
    
    # JPA / Hibernate
    spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
    spring.jpa.hibernate.ddl-auto=create-drop
    
    # Enable H2 Web Console
    spring.h2.console.enabled=true
    spring.h2.console.path=/h2-console

---

## Initial-State File Format

To seed the database with initial state data (users, companies, events, active orders), the system uses a custom initialization text file. This file is parsed by the InitCommandParser.

### Syntax Rules

The parser reads the initialization file line by line. Every command must adhere to these strict formatting rules:

1. **Comments:** Any line starting with a hash # is ignored. Blank lines are also ignored.
2. **Line Termination:** Every command must end with a semicolon ;.
3. **Command Structure:** Commands must be written as commandName(arg1, arg2, ...);.
4. **Variable Assignment (Optional):** You can store the result of a command in a variable by prefixing the line with $variableName =.
5. **Arguments:** Arguments are separated by commas ,. The parser automatically trims leading and trailing whitespace around arguments.

### Example Initialization File

Below is an example of a valid initialization file. You can use this as a template to write your own database seeding scripts:

    # --- System Initialization Script ---
    
    # 1. Create users
    $guest1 = guest-entry();
    register($guest1, alice, Alice Smith, pass123, alice@example.com, NONE);
    $alice = login($guest1, alice, pass123);
    
    $guest2 = guest-entry();
    register($guest2, bob, Bob Jones, pass456, bob@example.com, STUDENT);
    $bob = login($guest2, bob, pass456);
    
    # 2. Create production companies
    $company1 = create-production-company($alice, Live Events Co., Premier event organizer, contact@liveevents.com);
    
    # 3. Create events and configure seating maps
    $event1 = create-event($alice, 1, $company1, Rock Night, 500, 2026-07-11T20:00, true, Tel Aviv Arena, 120.0);
    configure-event-seating-map($alice, $event1, 10, 10, 120.0, 10, 10, 90.0, 5, 10, 60.0);
    
    # 4. Generate active orders
    $activeOrder1 = create-active-order($bob, bob, $event1);
    add-seats-to-order($bob, $activeOrder1, 0_1_1, 0_1_2, 0_1_3);
    
    # 5. Cleanup sessions
    logout(alice, $alice);
    logout(bob, $bob);
