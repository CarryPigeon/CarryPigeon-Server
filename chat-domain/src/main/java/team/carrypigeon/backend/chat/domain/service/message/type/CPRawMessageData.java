package team.carrypigeon.backend.chat.domain.service.message.type;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;

/**
 * Raw message wrapper for non-core domains (plugin domains).
 * <p>
 * Schema validation is performed by API layer before constructing this object.
 */
public class CPRawMessageData implements CPMessageData {

    private final String domain;
    private final JsonNode data;

    public CPRawMessageData(String domain, JsonNode data) {
        this.domain = domain;
        this.data = data;
    }

    @Override
    public CPMessageData parse(JsonNode data) {
        return new CPRawMessageData(domain, data);
    }

    @Override
    public String getSContent() {
        return "";
    }

    @Override
    public JsonNode getData() {
        return data;
    }

    @Override
    public String getDomain() {
        return domain;
    }
}

