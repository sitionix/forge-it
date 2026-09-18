package com.sitionix.forgeit.mockmvc.internal.executor;

/** Transport boundary. No servlet request or synthetic MVC result crosses it. */
@FunctionalInterface
public interface MockMvcExecutor {
    MockMvcResponse execute(MockMvcRequest request) throws Exception;
}
