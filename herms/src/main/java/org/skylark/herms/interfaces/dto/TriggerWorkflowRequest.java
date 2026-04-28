package org.skylark.herms.interfaces.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * REST request body for POST /api/workflows/{id}/trigger.
 */
@Getter
@Setter
@NoArgsConstructor
public class TriggerWorkflowRequest {
    private Map<String, Object> initialContext;
}
