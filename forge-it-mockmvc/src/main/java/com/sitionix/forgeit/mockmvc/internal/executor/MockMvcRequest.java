package com.sitionix.forgeit.mockmvc.internal.executor;

import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import java.util.List;
import java.util.Map;

public record MockMvcRequest(HttpMethod method, String pathTemplate, Map<String, ?> pathParameters,
                             Map<String, List<String>> queryParameters, Map<String, String> headers,
                             Map<String, String> cookies, String body) {
}
