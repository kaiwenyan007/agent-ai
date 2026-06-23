package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ModelsResponse {

    private List<String> models;
    /** true=远端拉取，false=默认列表兜底 */
    private boolean fromRemote;
}
