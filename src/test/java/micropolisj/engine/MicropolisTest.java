package micropolisj.engine;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

public class MicropolisTest {

  private final Micropolis micropolis = new Micropolis();

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
