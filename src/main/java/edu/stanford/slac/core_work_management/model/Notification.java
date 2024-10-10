package edu.stanford.slac.core_work_management.model;

import java.time.LocalDateTime;

/**
 * Define the notification
 * correlate the notification type and the destination
 */
public class Notification {
    /**
     * The type of the notification
     */
    String notificationType;
    /**
     * The destination of the notification
     */
    String destination;
    /**
     * The date and time of the notification
     */
    private LocalDateTime changed_on;
}
