package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.application.MachineService;
import com.thilaga.fruitmachine.domain.PlayResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints API-01 to API-04 (design 5.2). Holds no game rules; delegates to the service. */
@RestController
@RequestMapping("/api/v1/machines")
class MachineController {

  private final MachineService service;

  MachineController(MachineService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<MachineResponse> configure(@Valid @RequestBody CreateMachineRequest request) {
    UUID id = service.configure(request.toConfig());
    return ResponseEntity.created(URI.create("/api/v1/machines/" + id))
        .body(MachineResponse.from(service.read(id)));
  }

  @GetMapping("/{id}")
  MachineResponse read(@PathVariable UUID id) {
    return MachineResponse.from(service.read(id));
  }

  @PostMapping("/{id}/plays")
  PlayResponse play(@PathVariable UUID id, @Valid @RequestBody PlayRequest request) {
    PlayResult result = service.play(id, request.useFreePlay());
    List<String> colours = service.read(id).machine().config().colours();
    return PlayResponse.from(result, colours);
  }

  @PostMapping("/{id}/refills")
  MachineResponse refill(@PathVariable UUID id, @Valid @RequestBody RefillRequest request) {
    service.refill(id, request.amountPence());
    return MachineResponse.from(service.read(id));
  }
}
