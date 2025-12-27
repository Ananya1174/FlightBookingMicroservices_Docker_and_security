### ✈️ Flight Booking Backend – Microservices Architecture

This repository contains the backend implementation of the Flight Booking System, built using Spring Boot Microservices, Docker, JWT-based Security, and RabbitMQ.
Each service is independently deployable and communicates through REST and messaging.

## 🛠 Tech Stack
	•	Java 17
	•	Spring Boot 3
	•	Spring Security + JWT
	•	Spring Cloud Gateway
	•	MySQL
	•	RabbitMQ
	•	Docker & Maven

## 🚀 Services & Ports

| Service Name | Description | Port |
|-------------|------------|------|
| API Gateway | Central entry point, routing, JWT validation | 8080 |
| Auth Service | Authentication, JWT, password reset | 8087 |
| Flight Service | Flight inventory management & search | 8081 |
| Booking Service | Flight booking, cancellation, PNR | 8082 |
| Notification Service | Email notifications (RabbitMQ consumer) | 8083 |
| RabbitMQ | Message broker for async communication | 5672 |
| MySQL | Database | 3306 |

## 🔐 Security
	•	JWT-based authentication
	•	Role-based access (ADMIN, USER)
	•	Centralized security via API Gateway
 

