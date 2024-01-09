![logo](./logos/SLAC-lab-hires.png)

# Code Work Management (CWM)

## SLAC National Accelerator Laboratory
The SLAC National Accelerator Laboratory is operated by Stanford University for the US Departement of Energy.  
[DOE/Stanford Contract](https://legal.slac.stanford.edu/sites/default/files/Conformed%20Prime%20Contract%20DE-AC02-76SF00515%20as%20of%202022.10.01.pdf)

## License
Copyright (c) 2017-2023, The Board of Trustees of the Leland Stanford Junior University, through SLAC National Accelerator Laboratory... the complete license is [here](LICENSE.md)

## Overview: Work and Activity Management System

### Purpose

This software is designed to streamline and manage workflows centered around 'works' and their associated 'activities'. It is a flexible and scalable solution aimed at improving task management, scheduling, and execution within organizations. The system provides a structured approach to handling a variety of activities, including those linked to external data sources, and offers robust user authorization for activity management.

### Key Features

- **Activity Management**: Central to the software is the ability to create, track, and update activities. Each activity is linked to a specific 'work', allowing for organized and focused task management.

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
### Activity

```json lines
{
  "_id": "1", 
  "workId": "",
  "title": "Activity Title",
  "description": "",
  "type": {},
  "current_status": {
    status: {Created|Cancelled|Rejected|Authorized|Pending|InProgress|Completed},
    changed_on: Date,
    changed_by: user_id
  },
  "status_history": [
    {
      status: {Created|Cancelled|Rejected|Authorized|Pending|InProgress|Completed},
      changed_on: Date,
      changed_by: user_id
    },
    // ... more status history records
  ]
}

```