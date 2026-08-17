package com.example.wine.telemetry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ExceptionChainUtils {
    private ExceptionChainUtils() {
    }

    static boolean containsExceptionClass(Throwable throwable, String className) {
        return collectExceptionClassNames(throwable).contains(className);
    }

    static String summarizeExceptionChain(Throwable throwable) {
        return String.join(" -> ", collectExceptionClassNames(throwable));
    }

    private static List<String> collectExceptionClassNames(Throwable throwable) {
        Set<String> names = new LinkedHashSet<>();
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        Throwable current = throwable;
        while (current != null && !visited.containsKey(current)) {
            visited.put(current, Boolean.TRUE);
            names.add(current.getClass().getName());
            current = current.getCause();
        }
        return new ArrayList<>(names);
    }
}
