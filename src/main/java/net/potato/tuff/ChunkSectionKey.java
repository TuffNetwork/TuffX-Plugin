package net.potato.tuff;

import java.util.Objects;
import java.util.UUID;

public final class ChunkSectionKey {
    private final UUID playerId;
    private final String worldName;
    private final int cx;
    private final int cz;
    private final int sectionY;

    public ChunkSectionKey(UUID playerId, String worldName, int cx, int cz, int sectionY) {
        this.playerId = playerId;
        this.worldName = worldName != null ? worldName : "";
        this.cx = cx;
        this.cz = cz;
        this.sectionY = sectionY;
    }

    public UUID playerId() {
        return playerId;
    }

    public String worldName() {
        return worldName;
    }

    public int cx() {
        return cx;
    }

    public int cz() {
        return cz;
    }

    public int sectionY() {
        return sectionY;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ChunkSectionKey) obj;
        return Objects.equals(this.playerId, that.playerId) &&
                Objects.equals(this.worldName, that.worldName) &&
                this.cx == that.cx &&
                this.cz == that.cz &&
                this.sectionY == that.sectionY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, worldName, cx, cz, sectionY);
    }

    @Override
    public String toString() {
        return "ChunkSectionKey[" +
                "playerId=" + playerId + ", " +
                "worldName=" + worldName + ", " +
                "cx=" + cx + ", " +
                "cz=" + cz + ", " +
                "sectionY=" + sectionY + ']';
    }
}
