package pe.smartcash.cash.gmailsync.domain.model.commands;

import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

public record DisconnectGmailConnectionCommand(GmailConnectionId connectionId, UserId requestingUserId) {}
