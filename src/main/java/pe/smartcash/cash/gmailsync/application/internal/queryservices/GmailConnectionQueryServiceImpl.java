package pe.smartcash.cash.gmailsync.application.internal.queryservices;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.queries.FindGmailConnectionsByUserQuery;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionDetail;
import pe.smartcash.cash.gmailsync.domain.services.GmailConnectionQueryService;

@Service
class GmailConnectionQueryServiceImpl implements GmailConnectionQueryService {

  private final GmailConnectionRepository repository;

  GmailConnectionQueryServiceImpl(GmailConnectionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<GmailConnectionDetail> handle(FindGmailConnectionsByUserQuery query) {
    return repository.findAllByUserId(query.userId()).stream().map(this::toDetail).toList();
  }

  private GmailConnectionDetail toDetail(GmailConnection connection) {
    return new GmailConnectionDetail(connection.id(), connection.email(), connection.connectedAt(), connection.lastSyncedAt());
  }
}
