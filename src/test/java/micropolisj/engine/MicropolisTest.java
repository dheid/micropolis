package micropolisj.engine;

import static micropolisj.engine.TileConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

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
  public void testTestBounds() {
    assertThat(micropolis.testBounds(0, 0)).isTrue();
    assertThat(micropolis.testBounds(119, 99)).isTrue();
    assertThat(micropolis.testBounds(-1, 0)).isFalse();
    assertThat(micropolis.testBounds(120, 0)).isFalse();
    assertThat(micropolis.testBounds(0, 100)).isFalse();
  }

  @Test
  public void testPower() {
    micropolis.setTile(5, 5, POWERPLANT);
    micropolis.setTilePower(5, 5, true);
    assertThat(micropolis.isTilePowered(5, 5)).isTrue();

    micropolis.setTilePower(5, 5, false);
    assertThat(micropolis.isTilePowered(5, 5)).isFalse();
  }

  @Test
  public void testPowerScanConnectsPowerPlantToAdjacentWire() {
    micropolis.setTile(10, 10, POWERPLANT);
    micropolis.setTile(11, 10, POWERBASE);

    // 22 animate() calls trigger 11 simulation steps, enough for one
    // mapScan pass (registers the plant) followed by one powerScan pass
    for (int i = 0; i < 22; i++) {
      micropolis.animate();
    }

    assertThat(micropolis.getCoalCount()).isEqualTo(1);
    assertThat(micropolis.isTilePowered(10, 10)).isTrue();
    assertThat(micropolis.hasPower(11, 10)).isTrue();
    // an unconnected, far away wire tile stays unpowered
    assertThat(micropolis.hasPower(50, 50)).isFalse();
  }

  @Test
  public void testSpendReducesFunds() {
    micropolis.setFunds(1000);
    micropolis.spend(300);
    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(700);
  }

  @Test
  public void testDemandValvesForEmptyCity() {
    // an empty city has no jobs: residents and industry demand growth,
    // while commerce (which needs customers) does not
    for (int i = 0; i < 64; i++) {
      micropolis.animate();
    }

    assertThat(micropolis.getResValve()).isEqualTo(180);
    assertThat(micropolis.getComValve()).isEqualTo(-600);
    assertThat(micropolis.getIndValve()).isEqualTo(600);
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

  @Test
  public void spendReducesTotalFundsByAmount() {
    micropolis.setFunds(1000);

    micropolis.spend(300);

    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(700);
  }

  @Test
  public void spendWithZeroAmountLeavesFundsUnchanged() {
    micropolis.setFunds(500);

    micropolis.spend(0);

    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(500);
  }

  @Test
  public void spendWithNegativeAmountIncreasesFunds() {
    micropolis.setFunds(500);

    micropolis.spend(-200);

    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(700);
  }

  @Test
  public void spendMoreThanAvailableFundsResultsInNegativeBalance() {
    micropolis.setFunds(100);

    micropolis.spend(150);

    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(-50);
  }

  @Test
  public void spendCalledMultipleTimesAccumulatesDeductions() {
    micropolis.setFunds(1000);

    micropolis.spend(100);
    micropolis.spend(250);
    micropolis.spend(50);

    assertThat(micropolis.getBudget().getTotalFunds()).isEqualTo(600);
  }

  @Test
  public void spendNotifiesFundsChangedListener() {
    boolean[] notified = {false};
    micropolis.addListener(fundsChangedListener(() -> notified[0] = true));

    micropolis.spend(100);

    assertThat(notified[0]).isTrue();
  }

  /**
   * Builds a {@link CityListener} stub that runs the given callback on {@code fundsChanged()} and
   * throws on any other invocation, since those are irrelevant to this test.
   */
  private static CityListener fundsChangedListener(Runnable onFundsChanged) {
    return new CityListener() {
      @Override
      public void cityMessage(MicropolisMessage message, CityLocation loc) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void citySound(Sound sound, CityLocation loc) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void fundsChanged() {
        onFundsChanged.run();
      }

      @Override
      public void optionsChanged() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
