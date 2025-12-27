pipeline {
    agent any

    tools {
        maven 'M3'
    }

    environment {
        PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Ananya1174/FlightBookingMicroservices_Docker_and_security.git'
            }
        }

        stage('Build JARs') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Images') {
            steps {
                sh '''
                docker build -t flightbookingmicro-api-gateway ./api-gateway
                docker build -t flightbookingmicro-auth-service ./auth-service
                docker build -t flightbookingmicro-booking-service ./booking-service
                docker build -t flightbookingmicro-flight-service ./flight-service
                docker build -t flightbookingmicro-notification-service ./notification-service
                docker build -t flightbookingmicro-config-server ./config-server
                docker build -t flightbookingmicro-service-registry ./service-registry
                '''
            }
        }

        stage('Deploy Containers') {
            steps {
                sh 'docker compose up -d --build'
            }
        }
    }
}