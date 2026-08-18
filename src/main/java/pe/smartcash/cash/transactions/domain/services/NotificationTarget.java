package pe.smartcash.cash.transactions.domain.services;

/** Destino de una notificación push, ya resuelto (token FCM no vacío). */
public record NotificationTarget(String fcmToken) {}
