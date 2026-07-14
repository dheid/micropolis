package micropolisj.engine;

import org.junit.jupiter.api.Test;

import static micropolisj.engine.TileConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class MicropolisTest {

  private final Micropolis micropolis = new Micropolis();

  @Test
  public void testInitialization() {
    assertThat(micropolis.getWidth()).isEqualTo(120);
    assertThat(micropolis.getHeight()).isEqualTo(100);
    assertThat(micropolis.getTile(0, 0)).isEqualTo(DIRT);
  }

  @Test
  public void testSetAndGetTile() {
    micropolis.setTile(5, 5, FIRE);
    assertThat(micropolis.getTile(5, 5)).isEqualTo(FIRE);
  }

  @Test
  public void testPower() {
    micropolis.setTile(5, 5, POWERPLANT);
    micropolis.setTilePower(5, 5, true);
    assertThat(micropolis.isTilePowered(5, 5)).isTrue();
  }

  @Test
  public void testMakeFire() {
    assertThatNoException().isThrownBy(micropolis::makeFire);
  }

  @Test
  public void testMakeEarthquake() {
    assertThatNoException().isThrownBy(micropolis::makeEarthquake);
  }

  @Test
  public void testAnimate() {
    assertThatNoException().isThrownBy(micropolis::animate);
  }
}
