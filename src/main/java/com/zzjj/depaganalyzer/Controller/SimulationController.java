package com.zzjj.depaganalyzer.Controller;

import com.zzjj.depaganalyzer.dto.sim.SimulationCreateResponse;
import com.zzjj.depaganalyzer.dto.sim.SimulationRequest;
import com.zzjj.depaganalyzer.service.SimulationsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationsService simulationsService;

    public SimulationController(SimulationsService simulationsService) {
        this.simulationsService = simulationsService;
    }

    @PostMapping
    public ResponseEntity<SimulationCreateResponse> create(@Valid @RequestBody SimulationRequest req) {
        var created = simulationsService.createSimulation(req);
        return ResponseEntity.accepted().body(created);
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable String id) {
        return simulationsService.getSimulation(id);
    }
}
