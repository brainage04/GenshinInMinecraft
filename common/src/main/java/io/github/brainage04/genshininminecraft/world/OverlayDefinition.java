package io.github.brainage04.genshininminecraft.world;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Small authored content binding; coordinates are feet, never block-edit instructions. */
public record OverlayDefinition(int version, String id, String dimension, Position arrival,
                                List<TravelPoint> points, List<Camp> camps) {
    private static final Gson JSON = new Gson();
    public record Position(double x, double y, double z, float yaw) {
        public Position {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw))
                throw new IllegalArgumentException("Non-finite overlay position");
        }
    }
    public record TravelPoint(String id, String name, String kind, Position position, Position arrival) {
        public TravelPoint {
            requireId(id);
            if (name == null || name.isBlank() || !("statue".equals(kind) || "waypoint".equals(kind))
                    || position == null || arrival == null) throw new IllegalArgumentException("Invalid travel point " + id);
        }
        public boolean statue() { return kind.equals("statue"); }
    }
    public record Member(Position position, int level, String role) {
        public Member {
            if (position == null || level < 1 || level > 20 || role == null) throw new IllegalArgumentException("Invalid camp member");
        }
    }
    public record Camp(String id, String name, Position position, long respawnMillis, List<Member> members) {
        public Camp {
            requireId(id);
            if (name == null || position == null || respawnMillis <= 0 || members == null || members.isEmpty()
                    || members.size() > HilichurlCamp.MAX_COUNT) throw new IllegalArgumentException("Invalid camp " + id);
            members = List.copyOf(members);
        }
    }
    public OverlayDefinition {
        requireId(id);
        if (version != 1 || dimension == null || arrival == null || points == null || camps == null)
            throw new IllegalArgumentException("Unsupported/incomplete overlay version " + version);
        points = List.copyOf(points);
        camps = List.copyOf(camps);
        var ids = new HashSet<String>();
        for (var point : points) if (!ids.add(point.id())) throw new IllegalArgumentException("Duplicate overlay ID " + point.id());
        for (var camp : camps) if (!ids.add(camp.id())) throw new IllegalArgumentException("Duplicate overlay ID " + camp.id());
    }
    private static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid overlay ID " + id);
    }
    public TravelPoint point(String pointId) {
        for (var point : points) if (point.id().equals(pointId)) return point;
        return null;
    }
    public UUID entityId(String objectId) {
        return UUID.nameUUIDFromBytes(("genshininminecraft:overlay/" + id + "/" + objectId).getBytes(StandardCharsets.UTF_8));
    }
    public JsonObject json() { return JSON.toJsonTree(this).getAsJsonObject(); }
    public static OverlayDefinition parse(JsonObject json) { return JSON.fromJson(json, OverlayDefinition.class); }
    public static OverlayDefinition mondstadt() {
        try (var stream = OverlayDefinition.class.getResourceAsStream("/data/genshininminecraft/overlay/mondstadt.json")) {
            if (stream == null) throw new IllegalStateException("Missing Mondstadt overlay");
            return parse(com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot read Mondstadt overlay", exception); }
    }
}
