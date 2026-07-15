package micropolisj.engine;

import static micropolisj.engine.Micropolis.isTileDozeable;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.RESCLR;
import static micropolisj.engine.TileConstants.RUBBLE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IsTileDozeableTest {

  // Top-left corner member tile of the 3x3 residential zone building rooted at RESCLR.
  // It has no "bulldozable" attribute of its own, but is owned by the zone center tile.
  private static final int ZONE_MEMBER_TILE = RESCLR - 4;

  @Test
  public void isDozeableWhenTileIsMarkedBulldozable() {
    assertThat(isTileDozeable(toolEffect(RUBBLE, DIRT))).isTrue();
  }

  @Test
  public void isNotDozeableWhenTileIsNotBulldozableAndHasNoOwner() {
    assertThat(isTileDozeable(toolEffect(DIRT, DIRT))).isFalse();
  }

  @Test
  public void isNotDozeableWhenOwnedZoneCenterIsStillIntact() {
    // the owner tile at the expected relative position still matches the zone center tile
    assertThat(isTileDozeable(toolEffect(ZONE_MEMBER_TILE, RESCLR))).isFalse();
  }

  @Test
  public void isDozeableWhenOwnedZoneCenterHasBeenDestroyed() {
    // the owner tile at the expected relative position no longer matches the zone center tile
    assertThat(isTileDozeable(toolEffect(ZONE_MEMBER_TILE, DIRT))).isTrue();
  }

  /**
   * Builds a {@link ToolEffectIfc} stub returning {@code myTile} for the tile itself (0,0), and
   * {@code baseTile} for any other queried position (used by the owner-tile check).
   */
  private static ToolEffectIfc toolEffect(int myTile, int baseTile) {
    return new ToolEffectIfc() {
      @Override
      public int getTile(int dx, int dy) {
        return dx == 0 && dy == 0 ? myTile : baseTile;
      }

      @Override
      public void makeSound(int dx, int dy, Sound sound) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void setTile(int dx, int dy, int tileValue) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void spend(int amount) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void toolResult(ToolResult tr) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
