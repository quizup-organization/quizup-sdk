package io.github.quizup.axon.autoconfigure.erasure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

/**
 * Effacement physique des événements d'un agrégat supprimé (RGPD).
 *
 * <p>Outil d'exploitation dédié : il n'est jamais utilisé pour réécrire un agrégat vivant. Il
 * supprime la lignée d'événements et le snapshot, et libère les <em>large objects</em>
 * PostgreSQL associés (payload + metadata) pour éviter une fuite de stockage. Les agrégats
 * métier ne doivent appeler cet outil qu'après la diffusion de l'événement de suppression et
 * le traitement des projections.</p>
 */
public class AxonEventStoreErasureService {

    private static final Logger logger = LoggerFactory.getLogger(AxonEventStoreErasureService.class);

    private final JdbcTemplate jdbcTemplate;

    public AxonEventStoreErasureService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional
    public void eraseAggregates(Collection<String> aggregateIds) {
        aggregateIds.forEach(this::eraseAggregate);
    }

    @Transactional
    public void eraseAggregate(String aggregateId) {
        unlinkPayloads("domain_event_entry", aggregateId);
        unlinkPayloads("snapshot_event_entry", aggregateId);

        int events = jdbcTemplate.update(
                "DELETE FROM domain_event_entry WHERE aggregate_identifier = ?", aggregateId);
        int snapshots = jdbcTemplate.update(
                "DELETE FROM snapshot_event_entry WHERE aggregate_identifier = ?", aggregateId);
        logger.info("Event store erased for aggregate {}: {} event(s), {} snapshot(s)",
                aggregateId, events, snapshots);
    }

    private void unlinkPayloads(String table, String aggregateId) {
        List<Object> payloads = jdbcTemplate.queryForList(
                "SELECT payload FROM " + table + " WHERE aggregate_identifier = ?",
                Object.class, aggregateId);
        List<Object> metadata = jdbcTemplate.queryForList(
                "SELECT meta_data FROM " + table + " WHERE aggregate_identifier = ?",
                Object.class, aggregateId);
        payloads.forEach(this::unlink);
        metadata.forEach(this::unlink);
    }

    private void unlink(Object oid) {
        if (oid == null) {
            return;
        }
        try {
            jdbcTemplate.queryForObject(
                    "SELECT lo_unlink(CAST(? AS oid))", Long.class, oid.toString());
        } catch (RuntimeException exception) {
            // La ligne d'événement reste supprimée juste après : on ne bloque pas la suppression
            // du compte pour un large object orphelin (nettoyage d'exploitation documenté).
            logger.warn("Could not unlink event-store large object {}: {}", oid, exception.toString());
        }
    }
}
