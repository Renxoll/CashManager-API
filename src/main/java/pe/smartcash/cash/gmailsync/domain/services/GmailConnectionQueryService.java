package pe.smartcash.cash.gmailsync.domain.services;

import java.util.List;
import pe.smartcash.cash.gmailsync.domain.model.queries.FindGmailConnectionsByUserQuery;

public interface GmailConnectionQueryService {

  List<GmailConnectionDetail> handle(FindGmailConnectionsByUserQuery query);
}
