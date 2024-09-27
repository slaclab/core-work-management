<div style="display: flex; align-items: flex-start;">
<img src="./logos/logo.webp" alt="Project Logo" height="85" style="margin-right: 20px;"/>
<img src="./logos/SLAC-lab-hires.png" alt="SLAC Logo"/>
</div>

# Code Work Management (CWM)

## SLAC National Accelerator Laboratory
The SLAC National Accelerator Laboratory is operated by Stanford University for the US Departement of Energy.  
[DOE/Stanford Contract](https://legal.slac.stanford.edu/sites/default/files/Conformed%20Prime%20Contract%20DE-AC02-76SF00515%20as%20of%202022.10.01.pdf)

## License
Copyright (c) 2017-2023, The Board of Trustees of the Leland Stanford Junior University, through SLAC National Accelerator Laboratory... the complete license is [here](LICENSE.md)

## Overview: Work Management System

### Purpose

This software is designed to streamline and manage workflows centered around 'works' and their associated 'activities'. It is a flexible and scalable solution aimed at improving task management, scheduling, and execution within organizations. The system provides a structured approach to handling a variety of activities, including those linked to external data sources, and offers robust user authorization for activity management.

### Key Features

- **Work-Centric Approach**: The software is designed around the 'work' entity, which is a collection of activities. This approach provides a comprehensive view of all tasks associated with a particular project or objective.

- **User Authorization and Role Management**: Users are granted specific permissions to create, modify, or complete activities based on their roles, ensuring secure and efficient workflow management.

- **Status Tracking and Workflow**: Each activity has a status (e.g., Pending, In Progress, Completed) that is dynamically updated, reflecting the current state of the task.

- **External Data Integration**: The system can incorporate data from external sources, linking it to relevant activities, which aids in data-driven decision-making.

- **Future Activities Scheduling**: Activities can be scheduled for future dates, with the system providing reminders and notifications as the scheduled date approaches.

- **Activity History Tracking**: The software maintains a log of all status changes and updates made to an activity, providing a clear history and audit trail.

- **Real-time Dashboard**: A user-friendly dashboard offers a real-time overview of all activities and their statuses, aiding in quick decision-making and management.

- **Consistency Checks and Data Integrity**: Regular maintenance routines ensure data consistency and integrity throughout the system.

- **Feedback Mechanism**: Users can provide feedback on activity statuses, contributing to continuous improvement and accuracy in workflow management.

### Architecture

The system is built on a MongoDB database, utilizing its flexible document-oriented model to manage complex data structures. The schema is designed with the following primary collections:

- **Work Collection**: Represents the main entity, containing a grouping of activities.
- **Activity Collection**: Details specific tasks or actions within a work, including status, scheduling, and external data linkages. 

The architecture is scalable and robust, capable of handling large volumes of data and complex workflows, making it ideal for various organizational needs.

## Data Structure

### Work
```json lines
{
  "id":"1",
 "name":"Project", 
 "description":null
}
```

### Location

```json lines
{
  "id": "1",
  "name": "Location Name",
  "description": "Location Description",
  "shopGroupId": "1",
  "areaManagerUserId": "1"
}
```

### Shop Group

```json lines
{
  "id": "1",
  "name": "Shop Group Name",
  "description": "Shop Group Description",
  "userEmails": ["user@domain.com"]
}
```

## Configuration

below is the standard configuration of the CIS backend application
```yaml
logging:
  level:
    edu.stanford.slac.code_inventory_system: ${CWM_LOG_LEVEL:DEBUG}

server:
  tomcat:
    mbeanregistry:
      enabled: true

spring:
  application:
    name: 'CWM'
  cache:
    type: hazelcast
  ldap:
    urls: ${CWM_LDAP_URI:ldap://localhost:8389}
    base: ${CWM_LDAP_BASE:dc=sdf,dc=slac,dc=stanford,dc=edu}
  data:
    mongodb:
      uri: ${CWM_MONGODB_URI:mongodb://cwm:cwm@localhost:27017/cwm?authSource=cwm}
  servlet:
    multipart:
      enabled: true
      file-size-threshold: 1MB
      max-file-size: ${CWM_MAX_POST_SIZE:100MB}
      max-request-size: ${CWM_MAX_POST_SIZE:100MB}

edu:
  stanford:
    slac:
      ad:
        eed:
          baselib:
            app-token-prefix: ${spring.application.name}
            app-token-jwt-key: ${CWM_APP_TOKEN_JWT:token-header-key}
            user-header-name: ${CWM_AUTH_HEADER:x-vouch-idp-accesstoken}
            oauth-server-discover: ${CWM_OIDC_CONFIGURATION_ENDPOINT:https://dex.slac.stanford.edu/.well-known/openid-configuration}
            root-user-list: ${CWM_ROOT_USERS}
            root-authentication-token-list-json: ${CWM_ROOT_AUTHENTICATION_TOKEN_JSON:[]}
          mongodb:
            db_admin_uri: ${CWM_ADMIN_MONGODB_URI:mongodb://admin:admin@localhost:27017/?authSource=admin}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${spring.application.name}

# swagger-ui custom path
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    path: /elog.json

mongock:
  migration-scan-package:
    - edu.stanford.slac.core_work_management.migration
  throw-exception-if-cannot-obtain-lock: true #Default true
  track-ignored: false #Default true
  transaction-enabled: false
  runner-type: initializingbean
  enabled: true #Default true
```

## Demo

### Starting the Demo with Docker-Compose Files

To initiate the demo, use the provided docker-compose files. The `docker-compose.yml` is the default file
for starting the necessary services for the CIS backend to conduct unit and integration tests. Alongside,
the `docker-compose-app.yml` is used to enable the CIS backend in demo mode.

```shell
docker compose -f docker-compose.yml -f docker-compose-app.yml up
```
in case of application updates the docker image need to be rebuilt so in this case this command can be used:
```shell
docker compose -f docker-compose.yml -f docker-compose-app.yml up --build backend
```