package io.github.quizup.axon.console;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API interne de lecture de l'<b>event store local</b> et des <b>deadlines persistantes</b>,
 * consommée par {@code quizup-console} (M2M, jamais ingressed). Contenu aligné sur les endpoints
 * natifs Axon Server (agrégats, snapshots, recherche, scheduled events).
 */
@RestController
@RequestMapping("/internal/console")
public class ConsoleEventStoreController {

    private final ConsoleEventStoreService service;

    public ConsoleEventStoreController(ConsoleEventStoreService service) {
        this.service = service;
    }

    @GetMapping("/eventstore/stats")
    public ConsoleEventStoreService.EventStoreStats stats() {
        return service.stats();
    }

    @GetMapping("/eventstore/aggregates/{aggregateId}/events")
    public List<ConsoleEventStoreService.AggregateEventEntry> aggregateEvents(
            @PathVariable String aggregateId,
            @RequestParam(required = false) Long minSequence) {
        return service.aggregateEvents(aggregateId, minSequence);
    }

    @GetMapping("/eventstore/aggregates/{aggregateId}/snapshots")
    public List<ConsoleEventStoreService.SnapshotEntry> snapshots(@PathVariable String aggregateId) {
        return service.snapshots(aggregateId);
    }

    @GetMapping("/eventstore/events")
    public List<ConsoleEventStoreService.EventSearchEntry> searchEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Integer limit) {
        return service.searchEvents(type, from, limit);
    }

    @GetMapping("/scheduled-events")
    public List<ConsoleEventStoreService.ScheduledEventEntry> scheduledEvents(
            @RequestParam(required = false) Integer limit) {
        return service.scheduledEvents(limit);
    }
}
