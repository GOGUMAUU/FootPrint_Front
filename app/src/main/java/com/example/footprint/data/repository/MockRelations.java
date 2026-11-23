package com.example.footprint.data.repository;

import java.util.*;

/** In-memory relationship state shared by mock profiles and management screens. */
public final class MockRelations {
    private static final class State {
        final Map<Long, String> outgoing = new HashMap<>();
        final Set<Long> blocked = new HashSet<>();
        final Set<Long> incoming = new HashSet<>(Arrays.asList(2L, 4L));
        final Set<Long> followers = new HashSet<>();
    }
    private static final Map<String, State> accounts = new HashMap<>();
    private static String account = "demo@example.com";
    private static State state() { return accounts.computeIfAbsent(account, key -> new State()); }
    public static synchronized void select(String email) { account = email; }
    public static synchronized void delete(String email) { accounts.remove(email); }
    public static synchronized String status(long id) { return state().outgoing.getOrDefault(id, "NONE"); }
    public static synchronized boolean blocked(long id) { return state().blocked.contains(id); }
    public static synchronized boolean requested(long id) { return state().incoming.contains(id); }
    public static synchronized void follow(long id) {
        if (state().blocked.contains(id)) throw new IllegalStateException("차단을 먼저 해제해 주세요.");
        state().outgoing.putIfAbsent(id, id % 2 == 0 ? "PENDING" : "ACCEPTED");
    }
    public static synchronized void unfollow(long id) { state().outgoing.remove(id); }
    public static synchronized void block(long id) { state().blocked.add(id); state().outgoing.remove(id); state().incoming.remove(id); state().followers.remove(id); }
    public static synchronized void unblock(long id) { state().blocked.remove(id); }
    public static synchronized void accept(long id) { if (state().incoming.remove(id)) state().followers.add(id); }
    public static synchronized void reject(long id) { state().incoming.remove(id); }
    public static synchronized List<Long> list(String type) {
        List<Long> ids = new ArrayList<>();
        if ("blocks".equals(type)) ids.addAll(state().blocked);
        else if ("requests".equals(type)) ids.addAll(state().incoming);
        else if ("sent".equals(type)) state().outgoing.forEach((id, status) -> { if ("PENDING".equals(status)) ids.add(id); });
        else if ("followers".equals(type)) ids.addAll(state().followers);
        else state().outgoing.forEach((id, status) -> { if ("ACCEPTED".equals(status)) ids.add(id); });
        Collections.sort(ids); return ids;
    }
    public static synchronized List<Long> list(String type, long ownerId) {
        if (ownerId == 0 || type.equals("requests") || type.equals("blocks")) return list(type);
        List<Long> ids = new ArrayList<>();
        if (type.equals("followers") && "ACCEPTED".equals(status(ownerId))) ids.add(0L);
        if (type.equals("following") && state().followers.contains(ownerId)) ids.add(0L);
        return ids;
    }
}
